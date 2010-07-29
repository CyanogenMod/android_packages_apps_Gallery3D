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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

import com.android.gallery3d.data.FutureListener;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.Future;

public class GridSlotAdapter implements SlotView.Model {
    private static final String TAG = "GridSlotAdapter";

    private static final int LENGTH_LIMIT = 162;
    private static final double EXPECTED_AREA = 150 * 120;
    private static final int SLOT_WIDTH = 162;
    private static final int SLOT_HEIGHT = 132;
    private static final int CACHE_CAPACITY = 48;

    private final Map<Integer, MyDisplayItem> mItemMap =
            new HashMap<Integer, MyDisplayItem>(CACHE_CAPACITY);
    private final LinkedHashSet<Integer> mLruSlot =
            new LinkedHashSet<Integer>(CACHE_CAPACITY);
    private final MediaSet mMediaSet;
    private final Texture mWaitLoadingTexture;
    private final SlotView mSlotView;
    private final SelectionManager mSelectionManager;
    private boolean mContentInvalidated = false;

    public GridSlotAdapter(Context context, MediaSet mediaSet, SlotView slotView,
            SelectionManager selectionManager) {
        mSlotView = slotView;
        mMediaSet = mediaSet;
        mSelectionManager = selectionManager;
        ColorTexture gray = new ColorTexture(Color.GRAY);
        gray.setSize(64, 48);
        mWaitLoadingTexture = gray;
        mediaSet.setContentListener(new MyContentListener());
    }

    public void putSlot(int slotIndex, int x, int y, DisplayItemPanel panel) {
        MyDisplayItem displayItem = mItemMap.get(slotIndex);
        if (displayItem == null || mContentInvalidated) {
            MediaItem item = mMediaSet.getMediaItem(slotIndex);
            displayItem = new MyDisplayItem(mWaitLoadingTexture,
                    mSelectionManager.getSelectionDrawer());
            mItemMap.put(slotIndex, displayItem);
            item.requestImage(MediaItem.TYPE_MICROTHUMBNAIL,
                    new MyMediaItemListener(slotIndex));

            // Remove an item if the size of mItemsetMap is no less than
            // CACHE_CAPACITY and there exists a slot in mLruSlot.
            Iterator<Integer> iter = mLruSlot.iterator();
            while (mItemMap.size() >= CACHE_CAPACITY && iter.hasNext()) {
                mItemMap.remove(iter.next());
                iter.remove();
            }
            mItemMap.put(slotIndex, displayItem);
        }

        // Reclaim the slot
        mLruSlot.remove(slotIndex);

        if (mSelectionManager.isSelectionMode())
            displayItem.mChecked = mSelectionManager.isSlotSelected(slotIndex);

        x += getSlotWidth() / 2;
        y += getSlotHeight() / 2;
        panel.putDisplayItem(displayItem, x, y, 0);
    }

    public int getSlotHeight() {
        return SLOT_HEIGHT;
    }

    public int getSlotWidth() {
        return SLOT_WIDTH;
    }

    public int size() {
        return mMediaSet.getMediaItemCount();
    }

    private class MyMediaItemListener implements FutureListener<Bitmap> {

        private final int mSlotIndex;

        public MyMediaItemListener(int slotIndex) {
            mSlotIndex = slotIndex;
        }

        public void onFutureDone(Future<? extends Bitmap> future) {
            try {
                MyDisplayItem displayItem = mItemMap.get(mSlotIndex);
                displayItem.updateContent(new BitmapTexture(future.get()));
                mSlotView.notifyDataInvalidate();
            } catch (Exception e) {
                Log.v(TAG, "cannot get image", e);
            }
        }
    }

    private void onContentChanged() {
        mContentInvalidated = true;
        mSlotView.notifyDataChanged();
        mContentInvalidated = false;

        for (Integer index : mLruSlot) {
            mItemMap.remove(index);
        }
    }

    private static class MyDisplayItem extends DisplayItem {

        private Texture mContent;
        private final SelectionDrawer mDrawer;
        private boolean mChecked;

        public MyDisplayItem(Texture content, SelectionDrawer drawer) {
            mDrawer = drawer;
            updateContent(content);
        }

        public void updateContent(Texture content) {
            mContent = content;
            Rect p = mDrawer.getFramePadding();

            int width = mContent.getWidth();
            int height = mContent.getHeight();

            float scale = (float) Math.sqrt(EXPECTED_AREA / (width * height));
            width = (int) (width * scale + 0.5f);
            height = (int) (height * scale + 0.5f);

            int widthLimit = LENGTH_LIMIT - p.left - p.right;
            int heightLimit = LENGTH_LIMIT - p.top - p.bottom;

            if (width > widthLimit || height > heightLimit) {
                if (width * heightLimit > height * widthLimit) {
                    height = height * widthLimit / width;
                    width = widthLimit;
                } else {
                    width = width * heightLimit / height;
                    height = heightLimit;
                }
            }
            setSize(width + p.left + p.right, height + p.top + p.bottom);
        }

        @Override
        public void render(GLCanvas canvas) {
            mDrawer.draw(canvas, mContent, mWidth, mHeight, mChecked);
        }
    }

    public void freeSlot(int index, DisplayItemPanel panel) {
        DisplayItem item = mItemMap.get(index);
        panel.removeDisplayItem(item);
        mLruSlot.add(index);
    }

    private class MyContentListener implements MediaSet.MediaSetListener {
        public void onContentChanged() {
            GridSlotAdapter.this.onContentChanged();
        }
    }

}
