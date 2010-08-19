
package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Message;
import android.util.Log;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MediaSet.MediaSetListener;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.Utils;

public class AlbumDataAdapter implements MediaSetListener {
    private static final String TAG = "GalleryAdapter";

    private static final int UPDATE_LIMIT = 10;

    private static final int STATE_INVALID = 0;
    private static final int STATE_VALID = 1;
    private static final int STATE_UPDATING = 2;
    private static final int STATE_RECYCLED = 3;
    private static final int STATE_ERROR = -1;

    private static final int MSG_UPDATE_IMAGE = 0;

    public static interface Listener {
        public void onSizeChanged(int size);
        public void onContentInvalidated();
        public void onWindowContentChanged(
                int slot, DisplayItem old, DisplayItem update);
    }

    private final MediaSet mSource;
    private int mSize;

    private int mContentStart = 0;
    private int mContentEnd = 0;

    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    private Listener mListener;

    private final CoverDisplayItem mData[];
    private final SelectionManager mSelectionManager;
    private final ColorTexture mWaitLoadingTexture;

    private SynchronizedHandler mHandler;

    private int mActiveRequestCount = 0;

    public AlbumDataAdapter(GalleryContext context,
            SelectionManager manager, MediaSet source, int cacheSize) {
        source.setContentListener(this);
        mSource = source;
        mSelectionManager = manager;
        mData = new CoverDisplayItem[cacheSize];
        mSize = source.getMediaItemCount();

        mWaitLoadingTexture = new ColorTexture(Color.GRAY);
        mWaitLoadingTexture.setSize(64, 48);

        mHandler = new SynchronizedHandler(context.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.Assert(message.what == MSG_UPDATE_IMAGE);
                ((CoverDisplayItem) message.obj).updateImage();
            }
        };
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public DisplayItem get(int slotIndex) {
        if (!isActiveSlot(slotIndex)) {
            throw new IllegalArgumentException(
                    String.format("invalid slot: %s outsides (%s, %s)",
                    slotIndex, mActiveStart, mActiveEnd));
        }
        Utils.Assert(isActiveSlot(slotIndex));
        return mData[slotIndex % mData.length];
    }

    public int size() {
        return mSize;
    }

    public boolean isActiveSlot(int slotIndex) {
        return slotIndex >= mActiveStart && slotIndex < mActiveEnd;
    }

    private void setContentWindow(int contentStart, int contentEnd) {

        if (contentStart == mContentStart && contentEnd == mContentEnd) return;

        Log.v(TAG, String.format("content range: %s, %s", contentStart, contentEnd));
        if (contentStart >= mContentEnd || mContentStart >= contentEnd) {
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                freeSlotContent(i);
            }
            for (int i = contentStart; i < contentEnd; ++i) {
                prepareSlotContent(i);
            }
        } else {
            for (int i = mContentStart; i < contentStart; ++i) {
                freeSlotContent(i);
            }
            for (int i = contentEnd, n = mContentEnd; i < n; ++i) {
                freeSlotContent(i);
            }
            for (int i = contentStart, n = mContentStart; i < n; ++i) {
                prepareSlotContent(i);
            }
            for (int i = mContentEnd; i < contentEnd; ++i) {
                prepareSlotContent(i);
            }
        }

        mContentStart = contentStart;
        mContentEnd = contentEnd;
    }

    public void setActiveWindow(int start, int end) {

        Utils.Assert(start <= end
                && end - start <= mData.length && end <= mSize);
        DisplayItem data[] = mData;

        mActiveStart = start;
        mActiveEnd = end;

        // If no data is visible, keep the cache content
        if (start == end) return;

        int contentStart = Utils.clamp((start + end) / 2 - data.length / 2,
                0, Math.max(0, mSize - data.length));
        int contentEnd = Math.min(contentStart + data.length, mSize);
        if (mContentStart > start || mContentEnd < end
                || Math.abs(contentStart - mContentStart) > UPDATE_LIMIT) {
            setContentWindow(contentStart, contentEnd);
        }
        updateAllImageRequests();
    }

    // We would like to request non active slots in the following order:
    // Order:    8 6 4 2                   1 3 5 7
    //         |---------|---------------|---------|
    //                   |<-  active  ->|
    //         |<-------- cached range ----------->|
    private void requestNonactiveImages() {
        Log.v(TAG, "request non active images");
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0 ;i < range; ++i) {
            requestSlotImage(mActiveEnd + i, false);
            requestSlotImage(mActiveStart - 1 - i, false);
        }
    }

    private void requestSlotImage(int slotIndex, boolean isActive) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd) return;
        CoverDisplayItem item = mData[slotIndex % mData.length];
        item.requestImageIfNeed();
    }

    private void freeSlotContent(int slotIndex) {
        CoverDisplayItem data[] = mData;
        int index = slotIndex % data.length;
        CoverDisplayItem original = data[index];
        if (original != null) {
            original.recycle();
            data[index] = null;
        }
    }

    private void prepareSlotContent(final int slotIndex) {
        mData[slotIndex % mData.length] = new CoverDisplayItem(
                slotIndex, mSource.getMediaItem(slotIndex));
    }

    private void updateSlotContent(final int slotIndex) {

        MediaItem item = mSource.getMediaItem(slotIndex);
        CoverDisplayItem data[] = mData;
        int index = slotIndex % data.length;
        CoverDisplayItem original = data[index];
        CoverDisplayItem update = new CoverDisplayItem(slotIndex, item);
        data[index] = update;
        if (mListener != null && isActiveSlot(slotIndex)) {
            mListener.onWindowContentChanged(slotIndex, original, update);
        }
        if (original != null) original.recycle();
        updateAllImageRequests();
    }

    public void onContentChanged() {
        int oldSize = mSize;
        mSize = mSource.getMediaItemCount();
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            updateSlotContent(i);
        }
        if (mSize != oldSize && mListener != null) {
            mListener.onSizeChanged(mSize);
        }
        updateAllImageRequests();
    }

    public void onContentDirty() {
    }

    private void updateAllImageRequests() {
        mActiveRequestCount = 0;
        CoverDisplayItem data[] = mData;
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            CoverDisplayItem item = data[i % data.length];
            if (item.requestImageIfNeed()) {
                ++mActiveRequestCount;
            } else if (item.mState == STATE_UPDATING) {
                ++mActiveRequestCount;
            }
        }
        if (mActiveRequestCount == 0) requestNonactiveImages();
    }

    private class CoverDisplayItem
            extends DisplayItem implements FutureListener<Bitmap> {

        private final int mSlotIndex;
        private final MediaItem mMediaItem;

        private int mState = STATE_INVALID;
        private Future<Bitmap> mFuture;
        private Texture mContent;
        private Bitmap mBitmap;

        public CoverDisplayItem(int slotIndex, MediaItem item) {
            Log.v(TAG, "create slot: " + slotIndex);
            mSlotIndex = slotIndex;
            mMediaItem = item;
            updateContent(mWaitLoadingTexture);
        }

        public void updateImage() {
            if (mState != STATE_UPDATING) {
                Log.v(TAG, String.format(
                        "invalid state %s for slot %s: update image fail", mState, mSlotIndex));
                mFuture = null;
                return; /* RECYCLED*/
            }

            Utils.Assert(mBitmap == null);

            if (isActiveSlot(mSlotIndex)) {
                --mActiveRequestCount;
                if (mActiveRequestCount == 0) requestNonactiveImages();
            }
            try {
                mBitmap = mFuture.get();
                mState = STATE_VALID;
            } catch (Exception e){
                mState = STATE_ERROR;
                Log.w(TAG, "cannot get image" , e);
                return;
            } finally {
                mFuture = null;
            }
            updateContent(new BitmapTexture(mBitmap));
            if (mListener != null) mListener.onContentInvalidated();
        }

        private void updateContent(Texture content) {
            mContent = content;

            int width = mContent.getWidth();
            int height = mContent.getHeight();

            float scale = (float) Math.sqrt(
                    GalleryView.EXPECTED_AREA / (width * height));
            width = (int) (width * scale + 0.5f);
            height = (int) (height * scale + 0.5f);

            int widthLimit = GalleryView.LENGTH_LIMIT;
            int heightLimit = GalleryView.LENGTH_LIMIT;

            if (width > widthLimit || height > heightLimit) {
                if (width * heightLimit > height * widthLimit) {
                    height = height * widthLimit / width;
                    width = widthLimit;
                } else {
                    width = width * heightLimit / height;
                    height = heightLimit;
                }
            }
            setSize(width, height);
        }

        @Override
        public void render(GLCanvas canvas) {
            SelectionManager manager = mSelectionManager;
            boolean checked = manager.isSlotSelected(mSlotIndex);

            manager.getSelectionDrawer().draw(
                    canvas, mContent, mWidth, mHeight, checked);
        }

        @Override
        public long getIdentity() {
            // TODO: should use item's id
            return System.identityHashCode(this);
        }

        public void recycle() {
            if (mBitmap != null) {
                ((BasicTexture) mContent).recycle();
                mBitmap.recycle();
            }
            mState = STATE_RECYCLED;
        }

        public void onFutureDone(Future<? extends Bitmap> future) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_IMAGE, this));
        }

        public boolean requestImageIfNeed() {
            if (mState != STATE_INVALID) return false;
            mState = STATE_UPDATING;
            mFuture = mMediaItem.requestImage(MediaItem.TYPE_MICROTHUMBNAIL, this);
            return true;
        }
    }

}
