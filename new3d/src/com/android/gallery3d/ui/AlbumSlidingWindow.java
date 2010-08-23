
package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Message;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.Utils;

public class AlbumSlidingWindow implements AlbumView.ModelListener {
    private static final String TAG = "AlbumSlidingWindow";
    private static final int MSG_UPDATE_IMAGE = 0;

    public static interface Listener {
        public void onSizeChanged(int size);
        public void onContentInvalidated();
        public void onWindowContentChanged(
                int slot, DisplayItem old, DisplayItem update);
    }

    private final AlbumView.Model mSource;
    private int mSize;

    private int mContentStart = 0;
    private int mContentEnd = 0;

    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    private Listener mListener;

    private final AlbumDisplayItem mData[];
    private final SelectionManager mSelectionManager;
    private final ColorTexture mWaitLoadingTexture;

    private SynchronizedHandler mHandler;

    private int mActiveRequestCount = 0;

    public AlbumSlidingWindow(GalleryContext context,
            SelectionManager manager, AlbumView.Model source, int cacheSize) {
        source.setListener(this);
        mSource = source;
        mSelectionManager = manager;
        mData = new AlbumDisplayItem[cacheSize];
        mSize = source.size();

        mWaitLoadingTexture = new ColorTexture(Color.GRAY);
        mWaitLoadingTexture.setSize(1, 1);

        mHandler = new SynchronizedHandler(context.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.Assert(message.what == MSG_UPDATE_IMAGE);
                ((AlbumDisplayItem) message.obj).updateImage();
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

        if (contentStart >= mContentEnd || mContentStart >= contentEnd) {
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                freeSlotContent(i);
            }
            mSource.setActiveWindow(contentStart, contentEnd);
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
            mSource.setActiveWindow(contentStart, contentEnd);
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
                && end - start <= mData.length && end <= mSize,
                String.format("%s, %s, %s", start, end, mData.length));
        DisplayItem data[] = mData;

        mActiveStart = start;
        mActiveEnd = end;

        // If no data is visible, keep the cache content
        if (start == end) return;

        int contentStart = Utils.clamp((start + end) / 2 - data.length / 2,
                0, Math.max(0, mSize - data.length));
        int contentEnd = Math.min(contentStart + data.length, mSize);
        setContentWindow(contentStart, contentEnd);
        updateAllImageRequests();
    }

    // We would like to request non active slots in the following order:
    // Order:    8 6 4 2                   1 3 5 7
    //         |---------|---------------|---------|
    //                   |<-  active  ->|
    //         |<-------- cached range ----------->|
    private void requestNonactiveImages() {
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0 ;i < range; ++i) {
            requestSlotImage(mActiveEnd + i, false);
            requestSlotImage(mActiveStart - 1 - i, false);
        }
    }

    private void requestSlotImage(int slotIndex, boolean isActive) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd) return;
        AlbumDisplayItem item = mData[slotIndex % mData.length];
        item.requestImage();
    }

    private void cancelNonactiveImages() {
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0 ;i < range; ++i) {
            cancelSlotImage(mActiveEnd + i, false);
            cancelSlotImage(mActiveStart - 1 - i, false);
        }
    }

    private void cancelSlotImage(int slotIndex, boolean isActive) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd) return;
        AlbumDisplayItem item = mData[slotIndex % mData.length];
        item.cancelImageRequest();
    }

    private void freeSlotContent(int slotIndex) {
        AlbumDisplayItem data[] = mData;
        int index = slotIndex % data.length;
        AlbumDisplayItem original = data[index];
        if (original != null) {
            original.recycle();
            data[index] = null;
        }
    }

    private void prepareSlotContent(final int slotIndex) {
        mData[slotIndex % mData.length] = new AlbumDisplayItem(
                slotIndex, mSource.get(slotIndex));
    }

    private void updateSlotContent(final int slotIndex) {

        MediaItem item = mSource.get(slotIndex);
        AlbumDisplayItem data[] = mData;
        int index = slotIndex % data.length;
        AlbumDisplayItem original = data[index];
        AlbumDisplayItem update = new AlbumDisplayItem(slotIndex, item);
        data[index] = update;
        if (mListener != null && isActiveSlot(slotIndex)) {
            mListener.onWindowContentChanged(slotIndex, original, update);
        }
        if (original != null) original.recycle();
        updateAllImageRequests();
    }

    private void updateAllImageRequests() {
        mActiveRequestCount = 0;
        AlbumDisplayItem data[] = mData;
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            AlbumDisplayItem item = data[i % data.length];
            item.requestImage();
            if (item.isRequestInProgress()) ++mActiveRequestCount;
        }
        if (mActiveRequestCount == 0) {
            requestNonactiveImages();
        } else {
            cancelNonactiveImages();
        }
    }

    private class AlbumDisplayItem extends AbstractDisplayItem {

        private final int mSlotIndex;
        private Texture mContent;

        public AlbumDisplayItem(int slotIndex, MediaItem item) {
            super(item);
            mSlotIndex = slotIndex;
            updateContent(mWaitLoadingTexture);
        }

        @Override
        public void onBitmapAvailable(Bitmap bitmap) {
            if (isActiveSlot(mSlotIndex)) {
                --mActiveRequestCount;
                if (mActiveRequestCount == 0) requestNonactiveImages();
            }
            if (bitmap != null) {
                updateContent(new BitmapTexture(bitmap));
                if (mListener != null) mListener.onContentInvalidated();
            }
        }

        private void updateContent(Texture content) {
            mContent = content;

            int width = mContent.getWidth();
            int height = mContent.getHeight();

            float scalex = AlbumView.SLOT_WIDTH / (float) width;
            float scaley = AlbumView.SLOT_HEIGHT / (float) height;
            float scale = Math.min(scalex, scaley);

            width = (int) Math.floor(width * scale);
            height = (int) Math.floor(height * scale);

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

        @Override
        public void onFutureDone(Future<? extends Bitmap> future) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_IMAGE, this));
        }

        @Override
        public String toString() {
            return String.format("AlbumDisplayItem[%s]", mSlotIndex);
        }
    }

    public void onSizeChanged(int size) {
        if (mSize != size) {
            mSize = size;
            if (mListener != null) mListener.onSizeChanged(mSize);
        }
    }

    public void onWindowContentChanged(int index, MediaItem old, MediaItem update) {
        if (index >= mContentStart && index < mContentEnd) {
            updateSlotContent(index);
        }
    }
}
