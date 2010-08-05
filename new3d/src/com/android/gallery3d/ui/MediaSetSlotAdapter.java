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
import java.util.Random;

public class MediaSetSlotAdapter implements SlotView.Listener {
    private static final String TAG = MediaSetSlotAdapter.class.getSimpleName();

    private static final int LENGTH_LIMIT = 180;
    private static final double EXPECTED_AREA = LENGTH_LIMIT * LENGTH_LIMIT / 2;
    private static final int SLOT_WIDTH = 220;
    private static final int SLOT_HEIGHT = 200;
    private static final int MARGIN_TO_SLOTSIDE = 10;
    private static final int CACHE_CAPACITY = 32;
    private static final int INDEX_NONE = -1;

    private final MediaSet mRootSet;
    private final Texture mWaitLoadingTexture;

    private final Map<Integer, MyDisplayItems> mItemsetMap =
            new HashMap<Integer, MyDisplayItems>(CACHE_CAPACITY);
    private final LinkedHashSet<Integer> mLruSlot =
            new LinkedHashSet<Integer>(CACHE_CAPACITY);

    private final SlotView mSlotView;
    private final SelectionManager mSelectionManager;

    private boolean mContentInvalidated = false;
    private int mInvalidateIndex = INDEX_NONE;

    private int mVisibleStart;
    private int mVisibleEnd;

    public MediaSetSlotAdapter(Context context,
            MediaSet rootSet, SlotView slotView, SelectionManager manager) {
        mRootSet = rootSet;
        mSelectionManager = manager;
        ColorTexture gray = new ColorTexture(Color.GRAY);
        gray.setSize(64, 48);
        mWaitLoadingTexture = gray;

        mSlotView = slotView;
        mSlotView.setSlotSize(SLOT_WIDTH, SLOT_HEIGHT);

        rootSet.setContentListener(new MyContentListener());
    }

    private void putSlot(int slotIndex) {

        // Get displayItems from mItemsetMap or create them from MediaSet.
        MyDisplayItems displayItems = mItemsetMap.get(slotIndex);
        if (displayItems == null
                || mContentInvalidated || mInvalidateIndex == slotIndex) {
            displayItems = createDisplayItems(slotIndex);
        }
        // Reclaim the slot
        mLruSlot.remove(slotIndex);

        Rect rect = mSlotView.getSlotRect(slotIndex);

        displayItems.putSlot(mSlotView,
                (rect.left + rect.right) / 2, (rect.top + rect.bottom) / 2,
                rect.left + MARGIN_TO_SLOTSIDE, rect.right - MARGIN_TO_SLOTSIDE,
                slotIndex);
    }

    private MyDisplayItems createDisplayItems(int slotIndex) {
        MediaSet set = mRootSet.getSubMediaSet(slotIndex);
        set.setContentListener(new SlotContentListener(slotIndex));

        MediaItem[] items = set.getCoverMediaItems();
        MyDisplayItems displayItems = new MyDisplayItems(mSelectionManager, items.length);
        addSlotToCache(slotIndex, displayItems);

        SelectionDrawer drawer = mSelectionManager.getSelectionDrawer();
        for (int i = 0; i < items.length; ++i) {
            items[i].requestImage(MediaItem.TYPE_MICROTHUMBNAIL,
                    new MyMediaItemListener(slotIndex, i));
            int itemIndex = i == 0 ? slotIndex : INDEX_NONE;
            MyDisplayItem item = new MyDisplayItem(
                    itemIndex, mWaitLoadingTexture, drawer);
            displayItems.setDisplayItem(i, item);
        }

        return displayItems;
    }

    private void addSlotToCache(int slotIndex, MyDisplayItems displayItems) {
        mItemsetMap.put(slotIndex, displayItems);

        // Remove an itemset if the size of mItemsetMap is no less than
        // INITIAL_CACHE_CAPACITY and there exists a slot in mLruSlot.
        Iterator<Integer> iter = mLruSlot.iterator();
        for (int i = mItemsetMap.size() - CACHE_CAPACITY;
                i >= 0 && iter.hasNext(); --i) {
            mItemsetMap.remove(iter.next());
            iter.remove();
        }
    }

    public int size() {
        return mRootSet.getSubMediaSetCount();
    }

    private class MyMediaItemListener implements FutureListener<Bitmap> {

        private final int mSlotIndex;
        private final int mItemIndex;

        public MyMediaItemListener(int slotIndex, int itemIndex) {
            mSlotIndex = slotIndex;
            mItemIndex = itemIndex;
        }

        public void onFutureDone(Future<? extends Bitmap> future) {
            try {
                MyDisplayItems items = mItemsetMap.get(mSlotIndex);
                items.updateContent(mItemIndex, new BitmapTexture(future.get()));
                mSlotView.invalidate();
            } catch (Exception e) {
                Log.v(TAG, "cannot get image", e);
            }
        }
    }

    private class MyDisplayItem extends DisplayItem {
        private int mIndex;
        private Texture mContent;
        private final SelectionDrawer mDrawer;

        public MyDisplayItem(int index, Texture content, SelectionDrawer drawer) {
            mIndex = index;
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
            boolean topItem = mIndex != INDEX_NONE;
            boolean checked = mSelectionManager.isSlotSelected(mIndex);
            mDrawer.draw(canvas, mContent, mWidth, mHeight, checked, topItem);
        }

        @Override
        public long getIdentity() {
            // TODO: should use item's id
            return System.identityHashCode(this);
        }
    }

    private static class MyDisplayItems {
        MyDisplayItem[] mDisplayItems;
        SelectionManager mSelectionManager;
        Random mRandom = new Random();

        public MyDisplayItems(SelectionManager manager, int size) {
            mSelectionManager = manager;
            mDisplayItems = new MyDisplayItem[size];
        }

        public void setDisplayItem(int itemIndex, MyDisplayItem displayItem) {
            mDisplayItems[itemIndex] = displayItem;
        }

        public void updateContent(int itemIndex, Texture texture) {
            mDisplayItems[itemIndex].updateContent(texture);
        }

        public void putSlot(
                SlotView panel, int x, int y, int l, int r, int slotIndex) {
            // Put the cover items in reverse order, so that the first item is on
            // top of the rest.
            for (int i = mDisplayItems.length -1; i > 0; --i) {
                int dx = mRandom.nextInt(11) - 5;
                int itemX = (i & 0x01) == 0
                        ? l + dx + mDisplayItems[i].getWidth() / 2
                        : r + dx - mDisplayItems[i].getWidth() / 2;
                int dy = mRandom.nextInt(11) - 10;
                int theta = mRandom.nextInt(31) - 15;
                Position position = new Position();
                position.set(itemX, y + dy, 0, theta, 1f);
                panel.putDisplayItem(position, mDisplayItems[i]);
            }
            if (mDisplayItems.length > 0) {
                Position position = new Position();
                position.set(x, y, 0, 0, 1f);
                panel.putDisplayItem(position, mDisplayItems[0]);
            }
        }

        public void removeDisplayItems(SlotView panel) {
            for (MyDisplayItem item : mDisplayItems) {
                panel.removeDisplayItem(item);
            }
        }
    }

    private void freeSlot(int index) {
        MyDisplayItems displayItems;
        if (mContentInvalidated) {
            displayItems = mItemsetMap.remove(index);
        } else {
            displayItems = mItemsetMap.get(index);
            mLruSlot.add(index);
        }

        displayItems.removeDisplayItems(mSlotView);
    }

    private void onContentChanged() {
        updateVisibleRange(0, 0);
        mItemsetMap.clear();
        mLruSlot.clear();

        mSlotView.setSlotCount(mRootSet.getSubMediaSetCount());
        updateVisibleRange(mSlotView.getVisibleStart(), mSlotView.getVisibleEnd());
    }

    private class SlotContentListener implements MediaSet.MediaSetListener {
        private final int mSlotIndex;

        public SlotContentListener(int slotIndex) {
            mSlotIndex = slotIndex;
        }

        public void onContentChanged() {
            // Update the corresponding itemset to the slot based on whether
            // the slot is visible or in mLruSlot.
            // Remove the original corresponding itemset from cache if the
            // slot was already in mLruSlot. That is the itemset was invisible
            // and freed before.
            if (mLruSlot.remove(mSlotIndex)) {
                mItemsetMap.remove(mSlotIndex);
            } else {
                // Refresh the corresponding items in the slot if the slot is
                // visible. Note that only visible slots are refreshed in
                // mSlotView.notifySlotInvalidate(mSlotIndex).
                freeSlot(mSlotIndex);
                mItemsetMap.remove(mSlotIndex);
                putSlot(mSlotIndex);
                mSlotView.invalidate();
            }
        }
    }

    private class MyContentListener implements MediaSet.MediaSetListener {

        public void onContentChanged() {
            MediaSetSlotAdapter.this.onContentChanged();
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