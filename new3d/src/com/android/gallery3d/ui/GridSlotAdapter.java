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

import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.ui.PositionRepository.Position;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

public class GridSlotAdapter implements SlotView.Listener {
    private static final String TAG = "GridSlotAdapter";

    private static final int LENGTH_LIMIT = 162;
    private static final double EXPECTED_AREA = 150 * 120;
    private static final int SLOT_WIDTH = 162;
    private static final int SLOT_HEIGHT = 132;
    private static final int CACHE_CAPACITY = 48;
    private static final int HORIZONTAL_GAP = 5;
    private static final int VERTICAL_GAP = 5;

    private final Map<Integer, MyDisplayItem> mItemMap =
            new HashMap<Integer, MyDisplayItem>(CACHE_CAPACITY);
    private final LinkedHashSet<Integer> mLruSlot =
            new LinkedHashSet<Integer>(CACHE_CAPACITY);
    private final MediaSet mMediaSet;
    private final Texture mWaitLoadingTexture;
    private final SlotView mSlotView;
    private final SelectionManager mSelectionManager;
    private boolean mContentInvalidated = false;

    private int mVisibleStart = 0;
    private int mVisibleEnd = 0;

    public GridSlotAdapter(Context context, MediaSet mediaSet, SlotView slotView,
            SelectionManager selectionManager) {
        mSlotView = slotView;
        mMediaSet = mediaSet;
        mSelectionManager = selectionManager;
        ColorTexture gray = new ColorTexture(Color.GRAY);
        gray.setSize(64, 48);
        mWaitLoadingTexture = gray;
        mediaSet.setContentListener(new MyContentListener());
        mSlotView.setSlotSize(SLOT_WIDTH, SLOT_HEIGHT);
        mSlotView.setSlotCount(mMediaSet.getMediaItemCount());
        mSlotView.setSlotGaps(HORIZONTAL_GAP, VERTICAL_GAP);
    }

    private void freeSlot(int slotIndex) {
        MyDisplayItem displayItem = mItemMap.get(slotIndex);
        mSlotView.removeDisplayItem(displayItem);
        mLruSlot.add(slotIndex);
    }

    private void putSlot(int slotIndex) {
        MyDisplayItem displayItem = mItemMap.get(slotIndex);
        if (displayItem == null || mContentInvalidated) {
            MediaItem item = mMediaSet.getMediaItem(slotIndex);
            displayItem = new MyDisplayItem(slotIndex, mWaitLoadingTexture,
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

        Rect rect = mSlotView.getSlotRect(slotIndex);
        Position position = new Position(
                (rect.left + rect.right) / 2, (rect.top + rect.bottom) / 2, 0);

        mSlotView.putDisplayItem(position, displayItem);
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
                mSlotView.invalidate();
            } catch (Exception e) {
                Log.v(TAG, "cannot get image", e);
            }
        }
    }

    private void onContentChanged() {
        // remove all items
        updateVisibleRange(0, 0);
        mItemMap.clear();
        mLruSlot.clear();

        mSlotView.setSlotCount(mMediaSet.getMediaItemCount());
        updateVisibleRange(mSlotView.getVisibleStart(), mSlotView.getVisibleEnd());
    }

    private class MyDisplayItem extends DisplayItem {

        private Texture mContent;
        private final SelectionDrawer mDrawer;
        private int mIndex;

        public MyDisplayItem(int index, Texture content, SelectionDrawer drawer) {
            mIndex = index;
            mDrawer = drawer;
            updateContent(content);
        }

        public synchronized void updateContent(Texture content) {
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
        public synchronized void render(GLCanvas canvas) {
            boolean checked = mSelectionManager.isSlotSelected(mIndex);
            mDrawer.draw(canvas, mContent, mWidth, mHeight, checked);
        }

        @Override
        public long getIdentity() {
            // TODO: change to use the item's id
            return System.identityHashCode(this);
        }
    }

    private class MyContentListener implements MediaSet.MediaSetListener {
        public void onContentChanged() {
            GridSlotAdapter.this.onContentChanged();
        }
    }

    public void updateVisibleRange(int start, int end) {
        if (start == mVisibleStart && end == mVisibleEnd) return;

        if (start >= mVisibleEnd || mVisibleStart >= end) {
            for (int i = mVisibleStart, n = mVisibleEnd; i < n; ++i) {
                freeSlot(i);
            }
            for (int i = start; i < end; ++i) {
                putSlot(i);
            }
        } else {
            for (int i = mVisibleStart; i < start; ++i) {
                freeSlot(i);
            }
            for (int i = end, n = mVisibleEnd; i < n; ++i) {
                freeSlot(i);
            }
            for (int i = start, n = mVisibleStart; i < n; ++i) {
                putSlot(i);
            }
            for (int i = mVisibleEnd; i < end; ++i) {
                putSlot(i);
            }
        }
    }

    public void onLayoutChanged(int width, int height) {
        updateVisibleRange(0, 0);
        updateVisibleRange(mSlotView.getVisibleStart(), mSlotView.getVisibleEnd());
    }

    public void onScrollPositionChanged(int position) {
        updateVisibleRange(mSlotView.getVisibleStart(), mSlotView.getVisibleEnd());
    }
}
