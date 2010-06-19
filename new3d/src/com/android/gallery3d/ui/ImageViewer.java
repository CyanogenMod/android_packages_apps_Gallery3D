package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class ImageViewer extends GLView {
    private static final String TAG = "ImageViewer";

    private static final int TILE_SIZE = 128;
    private static final int UPLOAD_LIMIT = 4;

    private final Bitmap mScaledBitmaps[];
    private final BitmapTexture mBackupTexture;

    private int mCenterX = Integer.MIN_VALUE; // some invalid value
    private int mCenterY = Integer.MIN_VALUE;

    private float mScale = -1; // some invalid value;
    private int mIndex = 0;

    // The offsets of the (left, top) of the upper-left tile to the (left, top)
    // of the view.
    private int mOffsetX;
    private int mOffsetY;

    private int mUploadQuota;
    private boolean mRenderComplete;

    private final RectF mSourceRect = new RectF();
    private final Rect mTargetRect = new Rect();

    private final ScaleGestureDetector mScaleDetector;
    private final GestureDetector mGestureDetector;

    private final HashMap<TileKey, TileTexture> mActiveTiles =
            new HashMap<TileKey, TileTexture>();
    private Iterator<TileTexture> mUploadIter;

    private TileTexture mRecycledHead = null;

    private final int mImageWidth;
    private final int mImageHeight;

    private final TileKey mTileKey = new TileKey(0, 0, 0);
    private final Rect mTileRange = new Rect();
    private final Rect mActiveRange[] =  {new Rect(), new Rect()};

    private final Uploader mUploader = new Uploader();

    public ImageViewer(Context context, Bitmap scaledBitmaps[], Bitmap backup) {
        mScaledBitmaps = scaledBitmaps;
        mBackupTexture = new BitmapTexture(backup);

        mImageWidth = scaledBitmaps[0].getWidth();
        mImageHeight = scaledBitmaps[0].getHeight();
        setPosition(mImageWidth / 2, mImageHeight / 2, 0.51f);

        mScaleDetector = new ScaleGestureDetector(context, new MyScaleListener());
        mGestureDetector = new GestureDetector(context, new MyGestureListener());
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            mGestureDetector.onTouchEvent(event);
        }
        return mScaleDetector.onTouchEvent(event);
    }

    private static int floorLog2(int value) {
        // return the max x such that (1 << x) <= value
        if (value < 2) return 0;
        int s = 0, e = 31;
        for (int x = (s + e) >> 1; s + 1 < e; x = (s + e) >> 1) {
            if ((1 << x) <= value) s = x; else e = x;
        }
        return s;
    }

    @Override
    protected void onLayout(boolean changeSize, int l, int t, int r, int b) {
        if (changeSize) layoutTiles(mCenterX, mCenterY, mScale);
    }

    private void layoutTiles(int centerX, int centerY, float scale) {

        // TODO: use clip region to solve the transparency issue
        int levelCount = mScaledBitmaps.length;
        int width = getWidth();
        int height = getHeight();

        // get layout position
        int fromIndex;

        mIndex = Util.clamp(floorLog2((int) (1f / scale)), 0, levelCount);
        if (mIndex != levelCount) {
            Rect range = mTileRange;
            getRange(range, centerX, centerY, mIndex, scale);
            mOffsetX = (int) (width / 2f + (range.left - centerX) * scale);
            mOffsetY = (int) (height / 2f + (range.top - centerY) * scale);
            fromIndex = Math.max(0,
                    mScale * (1 << mIndex) > 0.75f ? mIndex - 1 : mIndex);
        } else {
            mOffsetX = (int) (width / 2 - mCenterX * mScale);
            mOffsetY = (int) (height / 2 - mCenterY * mScale);
            fromIndex = Math.max(mIndex - 2, 0);
        }

        int endIndex  = Math.min(fromIndex + 2, levelCount);
        Rect range[] = mActiveRange;
        for (int i = fromIndex; i < endIndex; ++i) {
            getRange(range[i - fromIndex], centerX, centerY, i);
        }

        // remove unused tiles;
        Iterator<Map.Entry<TileKey, TileTexture>>
                iter = mActiveTiles.entrySet().iterator();
        while (iter.hasNext()) {
            TileTexture tile = iter.next().getValue();
            int index = tile.mScaleIndex;
            if (index < fromIndex || index >= endIndex
                    || !range[index - fromIndex].contains(tile.mX, tile.mY)) {
                iter.remove();
                tile.mNextFree = mRecycledHead;
                mRecycledHead = tile;
            }
        }

        for (int i = fromIndex; i < endIndex; ++i) {
            int size = TILE_SIZE << i;
            Rect r = range[i - fromIndex];
            for (int y = r.top, bottom = r.bottom; y < bottom; y += size) {
                for (int x = r.left, right = r.right; x < right; x += size) {
                    activateTileTexture(x, y, i);
                }
            }
        }
        mUploadIter = mActiveTiles.values().iterator();
    }

    private void getRange(Rect out, int cX, int cY, int index) {
        getRange(out, cX, cY, index, 1f / (1 << (index + 1)));
    }

    // Get a rectangle range from a mScaledBitmap. The rectangle is centered
    // at (cX, cY) and will fill the ImageView.
    private void getRange(Rect out, int cX, int cY, int index, float scale) {
        int width = getWidth();
        int height = getHeight();

        int left = (int) (cX - width / (2f * scale) + 0.5f);
        int top = (int) (cY - height / (2f * scale) + 0.5f);
        int right = left + (int) (width / scale + 0.5f);
        int bottom = top + (int) (height / scale + 0.5f);

        // align the rectangle to tile boundary
        int length = TILE_SIZE << index;
        left = Math.max(0, length * (left / length));
        top = Math.max(0, length * (top / length));
        right = Math.min(mImageWidth, right);
        bottom = Math.min(mImageHeight, bottom);

        out.set(left, top, right, bottom);
    }

    public void setPosition(int centerX, int centerY, float scale) {
        if (centerX == mCenterX && centerY == mCenterY && scale == mScale) {
            return;
        }

        mCenterX = centerX;
        mCenterY = centerY;
        mScale = scale;

        layoutTiles(centerX, centerY, scale);
        invalidate();
    }

    public void releaseTiles() {
        GLRootView root = getGLRootView();
        FutureTask<Void> task = new FutureTask<Void>(new ReleaseTiles());
        synchronized (root) {
            root.queueEvent(task);
            try {
                task.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                Log.e(TAG, "release tiles fail", e);
            }
        }
    }

    private class ReleaseTiles implements Callable<Void> {
        public Void call() throws Exception {
            ArrayList<TileTexture> tiles = new ArrayList<TileTexture>();
            tiles.addAll(mActiveTiles.values());
            mActiveTiles.clear();

            TileTexture tile = mRecycledHead;
            while (tile != null) {
                tiles.add(tile);
                tile = tile.mNextFree;
            }
            mRecycledHead = null;
            GLRootView root = getGLRootView();
            if (root != null) {
                root.getCanvas().releaseTextures(tiles);
            }
            return null;
        }
    }

    @Override
    protected void render(GLCanvas canvas) {

        mUploadQuota = UPLOAD_LIMIT;
        mRenderComplete = true;

        int index = mIndex;
        if (index == mScaledBitmaps.length) {
            mBackupTexture.draw(canvas, mOffsetX, mOffsetY,
                    (int) (mImageWidth * mScale), (int) (mImageHeight * mScale));
        } else {
            int tileLength = (TILE_SIZE << index);
            int length = (int) (tileLength * mScale);
            Rect r = mTileRange;
            for (int y = mOffsetY, ty = r.top, bottom = r.bottom;
                    ty < bottom; y += length, ty += tileLength) {
                for (int x = mOffsetX, tx = r.left, right = r.right;
                        tx < right; x += length, tx += tileLength) {
                    TileTexture tile = getTileTexture(tx, ty, index);
                    tile.drawTile(canvas, x, y, length);
                }
            }
        }
        if (mRenderComplete) {
            if (mUploadIter.hasNext() && !mUploader.mActive) {
                mUploader.mActive = true;
                getGLRootView().addOnGLIdleListener(mUploader);
            }
        } else {
            invalidate();
        }
    }

    private void activateTileTexture(int x, int y, int scaleIndex) {
        TileKey key = mTileKey.set(x, y, scaleIndex);
        TileTexture tile = mActiveTiles.get(key);
        if (tile != null) return;

        if (mRecycledHead != null) {
            tile = mRecycledHead;
            mRecycledHead = tile.mNextFree;
            tile.update(x, y, scaleIndex);
        } else {
            tile = new TileTexture(x, y, scaleIndex);
        }
        mActiveTiles.put(key.clone(), tile);
    }

    private TileTexture getTileTexture(int x, int y, int scaleIndex) {
        return mActiveTiles.get(mTileKey.set(x, y, scaleIndex));
    }

    public static class TileKey implements Cloneable {
        private int mX;
        private int mY;
        private int mScaleIndex;

        public TileKey(int x, int y, int index) {
            set(x, y, index);
        }

        public TileKey set(int x, int y, int index) {
            mX = x;
            mY = y;
            mScaleIndex = index;
            return this;
        }

        @Override
        public TileKey clone() {
            try {
                return (TileKey) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        @Override
        public int hashCode() {
            int hashCode = 31 + mX;
            hashCode = hashCode * 31 + mY;
            return hashCode * 31 + mScaleIndex;
        }

        @Override
        public boolean equals(Object another) {
            if (!(another instanceof TileKey)) return false;
            TileKey t = (TileKey) another;
            return t.mX == mX && t.mY == mY && t.mScaleIndex == mScaleIndex;
        }
    }

    static boolean drawTile(
            TileTexture tile, GLCanvas canvas, RectF source, Rect target) {
        while (true) {
            if (tile.isContentValid(canvas)) {
                tile.bind(canvas);
                canvas.setTextureCoords(source);
                canvas.fillRect(target);
                return true;
            }

            // Parent can be divided to four quads and tile is one of the four.
            TileTexture parent = tile.getParentTile();
            if (parent == null) return false;
            if (tile.mX == parent.mX) {
                source.left /= 2f;
                source.right /= 2f;
            } else {
                source.left = 0.5f + source.left / 2f;
                source.right = 0.5f + source.right / 2f;
            }
            if (tile.mY == parent.mY) {
                source.top /= 2f;
                source.bottom /= 2f;
            } else {
                source.top = 0.5f + source.top / 2f;
                source.bottom = 0.5f + source.bottom / 2f;
            }
            tile = parent;
        }
    }

    private class Uploader implements GLRootView.OnGLIdleListener {

        protected boolean mActive;

        public boolean onGLIdle(GLRootView root) {
            int quota = UPLOAD_LIMIT;
            GLCanvas canvas = root.getCanvas();

            Iterator<TileTexture> iter = mUploadIter;
            while (iter.hasNext() && quota > 0) {
                TileTexture tile = iter.next();
                if (!tile.isContentValid(canvas)) {
                    tile.updateContent(canvas);
                    Log.v(TAG, String.format(
                            "update tile in background: %s %s %s",
                            tile.mX, tile.mY, tile.mScaleIndex));
                    --quota;
                }
            }
            mActive = iter.hasNext();
            return mActive;
        }
    }

    private class TileTexture extends UploadedTexture {
        int mX;
        int mY;
        int mScaleIndex;
        TileTexture mNextFree;

        public TileTexture(int x, int y, int scale) {
            mX = x;
            mY = y;
            mScaleIndex = scale;
        }

        @Override
        protected void onFreeBitmap(Bitmap bitmap) {
            bitmap.recycle();
        }

        @Override
        protected Bitmap onGetBitmap() {
            int index = mScaleIndex;
            Bitmap source = mScaledBitmaps[index];
            Bitmap bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE,
                    source.hasAlpha() ? Config.ARGB_8888 : Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(source, -(mX >> index), -(mY >> index), null);
            return bitmap;
        }

        public void update(int x, int y, int index) {
            mX = x;
            mY = y;
            mScaleIndex = index;
            invalidateContent();
        }

        public void drawTile(GLCanvas canvas, int x, int y, int length) {
            RectF source = mSourceRect;
            Rect target = mTargetRect;
            target.set(x, y, x + length, y + length);
            source.set(0, 0, 1f, 1f);
            drawTile(canvas, source, target);
        }

        public TileTexture getParentTile() {
            if (mScaleIndex + 1 == mScaledBitmaps.length) return null;
            int size = TILE_SIZE << (mScaleIndex + 1);
            int x = size * (mX / size);
            int y = size * (mY / size);
            return getTileTexture(x, y, mScaleIndex + 1);
        }

        public void drawTile(GLCanvas canvas, RectF source, Rect target) {
            if (!isContentValid(canvas)) {
                if (mUploadQuota > 0) {
                    --mUploadQuota;
                    updateContent(canvas);
                } else {
                    mRenderComplete = false;
                }
            }
            if (!ImageViewer.drawTile(this, canvas, source, target)) {
                BitmapTexture backup = mBackupTexture;
                int width = mImageWidth;
                int height = mImageHeight;
                float scale = width > height
                        ? (float) backup.getWidth() / width
                        : (float) backup.getHeight() / height;
                int length = TILE_SIZE << mScaleIndex;

                // bind backup first to get mTextureWidth and mTextureHeight
                backup.bind(canvas);
                float xScale = scale / backup.mTextureWidth;
                float yScale = scale / backup.mTextureHeight;
                source.set(mX * xScale, mY * yScale,
                        (mX + length) * xScale, (mY + length) * yScale);

                // In case that the rendering range is beyond the bound of the
                // backup texture
                float xBound = (backup.mWidth - 0.5f) / backup.mTextureWidth;
                float yBound = (backup.mHeight - 0.5f) / backup.mTextureHeight;
                if (source.right > xBound) {
                    target.right = (int) (target.left + target.width() *
                            (xBound - source.left) / source.width() + 0.5f);
                    source.right = xBound;
                }
                if (source.bottom > yBound) {
                    target.bottom = (int) (target.top + target.height() *
                            (yBound - source.top) / source.height() + 0.5f);
                    source.bottom = yBound;
                }
                canvas.setTextureCoords(source);
                canvas.fillRect(target);
            }
        }
    }

    private class MyGestureListener
            extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(
                MotionEvent e1, MotionEvent e2, float dx, float dy) {
            setPosition((int) (mCenterX + dx / mScale),
                    (int) (mCenterY + dy / mScale), mScale);
            return true;
        }
    }

    private class MyScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        // The offsets of the focus point to the center of the image on the
        // image domain.
        private float mPrevOffsetX;
        private float mPrevOffsetY;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();
            if (Float.isNaN(scale) || Float.isInfinite(scale)) return true;
            scale = Util.clamp(scale * mScale, 0.02f, 2);

            //  The focus point should keep this position on the ImageView.
            // So, mCenterX + mPrevOffsetX = mCenterX' + offsetX.
            // mCenterY + mPrevOffsetY = mCenterY' + offsetY.
            float offsetX = (detector.getFocusX() - getWidth() / 2) / scale;
            float offsetY = (detector.getFocusY() - getHeight() / 2) / scale;
            setPosition((int) (mCenterX - (offsetX - mPrevOffsetX) + 0.5),
                    (int) (mCenterY - (offsetY - mPrevOffsetY) + 0.5), scale);
            mPrevOffsetX = offsetX;
            mPrevOffsetY = offsetY;

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mPrevOffsetX = (detector.getFocusX() - getWidth() / 2) / mScale;
            mPrevOffsetY = (detector.getFocusY() - getHeight() / 2) / mScale;
            return true;
        }
    }
}
