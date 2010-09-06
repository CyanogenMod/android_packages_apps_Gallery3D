/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Message;
import android.util.Log;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.app.AlbumSetDataAdapter;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.AlbumSetView.AlbumSetItem;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.Utils;

public class AlbumSetSlidingWindow implements AlbumSetView.ModelListener {
    private static final String TAG = "GallerySlidingWindow";
    private static final int MSG_UPDATE_IMAGE = 0;

    public static interface Listener {
        public void onSizeChanged(int size);
        public void onContentInvalidated();
        public void onWindowContentChanged(
                int slot, AlbumSetItem old, AlbumSetItem update);
    }

    private final AlbumSetView.Model mSource;
    private int mSize;

    private int mContentStart = 0;
    private int mContentEnd = 0;

    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    private Listener mListener;

    private final AlbumSetItem mData[];
    private final SelectionManager mSelectionManager;
    private final ColorTexture mWaitLoadingTexture;

    private SynchronizedHandler mHandler;

    private int mActiveRequestCount = 0;

    public AlbumSetSlidingWindow(GalleryContext context,
            SelectionManager manager, AlbumSetView.Model source, int cacheSize) {
        source.setListener(this);
        mSource = source;
        mSelectionManager = manager;
        mData = new AlbumSetItem[cacheSize];
        mSize = source.size();

        mWaitLoadingTexture = new ColorTexture(Color.GRAY);
        mWaitLoadingTexture.setSize(1, 1);

        mHandler = new SynchronizedHandler(context.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.Assert(message.what == MSG_UPDATE_IMAGE);
                ((GalleryDisplayItem) message.obj).updateImage();
            }
        };

    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public AlbumSetItem get(int slotIndex) {
        if (!isActiveSlot(slotIndex)) {
            throw new IllegalArgumentException(
                    String.format("invalid slot: %s outsides (%s, %s)",
                    slotIndex, mActiveStart, mActiveEnd));
        }
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
                && end - start <= mData.length && end <= mSize);
        AlbumSetItem data[] = mData;

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
                mContentEnd - mActiveEnd, mActiveStart - mContentStart);
        for (int i = 0 ;i < range; ++i) {
            requestImagesInSlot(mActiveEnd + i);
            requestImagesInSlot(mActiveStart - 1 - i);
        }
    }

    private void cancelNonactiveImages() {
        int range = Math.max(
                mContentEnd - mActiveEnd, mActiveStart - mContentStart);
        for (int i = 0 ;i < range; ++i) {
            cancelImagesInSlot(mActiveEnd + i);
            cancelImagesInSlot(mActiveStart - 1 - i);
        }
    }

    private void requestImagesInSlot(int slotIndex) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd) return;
        AlbumSetItem items = mData[slotIndex % mData.length];
        for (DisplayItem item : items.covers) {
            ((GalleryDisplayItem) item).requestImage();
        }
    }

    private void cancelImagesInSlot(int slotIndex) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd) return;
        AlbumSetItem items = mData[slotIndex % mData.length];
        for (DisplayItem item : items.covers) {
            ((GalleryDisplayItem) item).cancelImageRequest();
        }
    }

    private void freeSlotContent(int slotIndex) {
        AlbumSetItem data[] = mData;
        int index = slotIndex % data.length;
        AlbumSetItem original = data[index];
        if (original != null) {
            data[index] = null;
            for (DisplayItem item : original.covers) {
                ((GalleryDisplayItem) item).recycle();
            }
        }
    }

    private void prepareSlotContent(final int slotIndex) {
        AlbumSetItem item = new AlbumSetItem();
        MediaItem[] coverItems = mSource.getMediaItems(slotIndex);
        item.covers = new GalleryDisplayItem[coverItems.length];
        for (int i = 0; i < coverItems.length; ++i) {
            item.covers[i] = new GalleryDisplayItem(slotIndex, i, coverItems[i]);
        }
        mData[slotIndex % mData.length] = item;
    }

    private void updateSlotContent(final int slotIndex) {
        AlbumSetItem data[] = mData;

        int index = slotIndex % data.length;
        AlbumSetItem original = data[index];
        AlbumSetItem update = new AlbumSetItem();
        data[index] = update;

        MediaItem[] coverItems = mSource.getMediaItems(slotIndex);
        update.covers = new GalleryDisplayItem[coverItems.length];
        for (int i = 0; i < coverItems.length; ++i) {
            GalleryDisplayItem cover =
                    new GalleryDisplayItem(slotIndex, i, coverItems[i]);
            update.covers[i] = cover;
        }
        if (mListener != null && isActiveSlot(slotIndex)) {
            mListener.onWindowContentChanged(slotIndex, original, update);
        }
        if (original != null) {
            for (DisplayItem item : original.covers) {
                ((GalleryDisplayItem) item).recycle();
            }
        }
    }

    private void notifySlotChanged(int slotIndex) {
        // If the updated content is not cached, ignore it
        if (slotIndex < mContentStart || slotIndex >= mContentEnd) {
            Log.w(TAG, String.format(
                    "invalid update: %s is outside (%s, %s)",
                    slotIndex, mContentStart, mContentEnd) );
            return;
        }
        updateSlotContent(slotIndex);
        boolean isActive = isActiveSlot(slotIndex);
        if (mActiveRequestCount == 0 || isActive) {
            for (DisplayItem item : mData[slotIndex % mData.length].covers) {
                GalleryDisplayItem galleryItem = (GalleryDisplayItem) item;
                galleryItem.requestImage();
                if (isActive && galleryItem.isRequestInProgress()) {
                    ++mActiveRequestCount;
                }
            }
        }
    }

    private void updateAllImageRequests() {
        mActiveRequestCount = 0;
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            for (DisplayItem item : mData[i % mData.length].covers) {
                GalleryDisplayItem coverItem = (GalleryDisplayItem) item;
                coverItem.requestImage();
                if (coverItem.isRequestInProgress()) ++mActiveRequestCount;
            }
        }
        if (mActiveRequestCount == 0) {
            requestNonactiveImages();
        } else {
            cancelNonactiveImages();
        }
    }

    private class GalleryDisplayItem extends AbstractDisplayItem {

        private final int mSlotIndex;
        private final int mCoverIndex;
        private Texture mContent;

        public GalleryDisplayItem(int slotIndex, int coverIndex, MediaItem item) {
            super(item);
            mSlotIndex = slotIndex;
            mCoverIndex = coverIndex;
            updateContent(mWaitLoadingTexture);
        }

        @Override
        protected void onBitmapAvailable(Bitmap bitmap) {
            if (isActiveSlot(mSlotIndex)) {
                --mActiveRequestCount;
                if (mActiveRequestCount == 0) requestNonactiveImages();
            }
            if (bitmap != null) {
                BitmapTexture texture = new BitmapTexture(bitmap);
                texture.setThrottled(true);
                updateContent(texture);
                if (mListener != null) mListener.onContentInvalidated();
            }
        }

        private void updateContent(Texture content) {
            mContent = content;

            int width = mContent.getWidth();
            int height = mContent.getHeight();

            float scalex = AlbumSetView.SLOT_WIDTH / (float) width;
            float scaley = AlbumSetView.SLOT_HEIGHT / (float) height;
            float scale = Math.min(scalex, scaley);

            width = (int) Math.floor(width * scale);
            height = (int) Math.floor(height * scale);

            setSize(width, height);
        }

        @Override
        public void render(GLCanvas canvas) {
            SelectionManager manager = mSelectionManager;
            boolean topItem = mCoverIndex == 0;
            boolean checked = false;
            if (topItem) {
                // TODO: add support for batch mode to improve performance
                long id = mSource.getMediaSet(mSlotIndex).getUniqueId();
                checked = manager.isItemSelected(id);
            }

            manager.getSelectionDrawer().draw(
                    canvas, mContent, mWidth, mHeight, checked, topItem);
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
            return String.format("GalleryDisplayItem(%s, %s)", mSlotIndex, mCoverIndex);
        }
    }

    public void onSizeChanged(int size) {
        if (mSize != size) {
            mSize = size;
            if (mListener != null) mListener.onSizeChanged(mSize);
        }
    }

    public void onWindowContentChanged(int index, MediaItem[] old, MediaItem[] update) {
        notifySlotChanged(index);
    }
}
