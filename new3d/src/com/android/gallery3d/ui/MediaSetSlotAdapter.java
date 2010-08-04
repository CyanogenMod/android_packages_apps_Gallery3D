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
import java.util.Random;
import java.util.concurrent.Future;

public class MediaSetSlotAdapter implements SlotView.Model {
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

    public MediaSetSlotAdapter(
            Context context, MediaSet rootSet, SlotView slotView, SelectionManager manager) {
        mRootSet = rootSet;
        mSelectionManager = manager;
        ColorTexture gray = new ColorTexture(Color.GRAY);
        gray.setSize(64, 48);
        mWaitLoadingTexture = gray;
        mSlotView = slotView;

        rootSet.setContentListener(new MyContentListener());
    }

    public void putSlot(
            int slotIndex, int x, int y, DisplayItemPanel panel) {

        // Get displayItems from mItemsetMap or create them from MediaSet.
        MyDisplayItems displayItems = mItemsetMap.get(slotIndex);
        if (displayItems == null
                || mContentInvalidated || mInvalidateIndex == slotIndex) {
            displayItems = createDisplayItems(slotIndex);
        }

        // Reclaim the slot
        mLruSlot.remove(slotIndex);

        // Put displayItems to the panel.
        int left = x + MARGIN_TO_SLOTSIDE;
        int right = x + getSlotWidth() - MARGIN_TO_SLOTSIDE;
        x += getSlotWidth() / 2;
        y += getSlotHeight() / 2;

        displayItems.putSlot(panel, x, y, left, right, slotIndex);

    }

    private MyDisplayItems createDisplayItems(int slotIndex) {
        MediaSet set = mRootSet.getSubMediaSet(slotIndex);
        set.setContentListener(new SlotContentListener(slotIndex));

        MediaItem[] items = set.getCoverMediaItems();
        MyDisplayItems displayItems = new MyDisplayItems(mSelectionManager, items.length);
        addSlotToCache(slotIndex, displayItems);

        for (int i = 0; i < items.length; ++i) {
            items[i].requestImage(MediaItem.TYPE_MICROTHUMBNAIL,
                    new MyMediaItemListener(slotIndex, i));
            displayItems.setDisplayItem(i, mWaitLoadingTexture);
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

    public int getSlotHeight() {
        return SLOT_HEIGHT;
    }

    public int getSlotWidth() {
        return SLOT_WIDTH;
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
                mSlotView.notifyDataInvalidate();
            } catch (Exception e) {
                Log.v(TAG, "cannot get image", e);
            }
        }
    }

    private static class MyDisplayItems {
        MyDisplayItem[] mDisplayItems;
        SelectionManager mSelectionManager;
        Random mRandom = new Random();

        private static class MyDisplayItem extends DisplayItem {
            private Texture mContent;
            private boolean mChecked;
            private boolean mTopItem;
            private final SelectionDrawer mDrawer;

            public MyDisplayItem(Texture content, SelectionDrawer drawer) {
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
                mDrawer.draw(canvas, mContent, mWidth, mHeight, mChecked, mTopItem);
            }
        }

        public MyDisplayItems(SelectionManager manager, int size) {
            mSelectionManager = manager;
            mDisplayItems = new MyDisplayItem[size];
        }

        public void setDisplayItem(int itemIndex, Texture texture) {
            mDisplayItems[itemIndex] = new MyDisplayItem(texture,
                    mSelectionManager.getSelectionDrawer());
        }

        public void updateContent(int itemIndex, Texture texture) {
            mDisplayItems[itemIndex].updateContent(texture);
        }

        public void putSlot(DisplayItemPanel panel, int x, int y, int l, int r, int slotIndex) {
            // Put the cover items in reverse order, so that the first item is on
            // top of the rest.
            for (int i = mDisplayItems.length -1; i > 0; --i) {
                int dx = mRandom.nextInt(11) - 5;
                int itemX = (i & 0x01) == 0
                        ? l + dx + mDisplayItems[i].getWidth() / 2
                        : r + dx - mDisplayItems[i].getWidth() / 2;
                int dy = mRandom.nextInt(11) - 10;
                int theta = mRandom.nextInt(31) - 15;
                panel.putDisplayItem(mDisplayItems[i], itemX, y + dy, theta);
            }
            if (mDisplayItems.length > 0) {
                if (mSelectionManager.isSelectionMode()) {
                    mDisplayItems[0].mTopItem = true;
                    mDisplayItems[0].mChecked = mSelectionManager.isSlotSelected(slotIndex);
                }
                panel.putDisplayItem(mDisplayItems[0], x, y, 0);
            }
        }

        public void removeDisplayItems(DisplayItemPanel panel) {
            for (MyDisplayItem item : mDisplayItems) {
                panel.removeDisplayItem(item);
            }
        }
    }

    public void freeSlot(int index, DisplayItemPanel panel) {
        MyDisplayItems displayItems;
        if (mContentInvalidated) {
            displayItems = mItemsetMap.remove(index);
        } else {
            displayItems = mItemsetMap.get(index);
            mLruSlot.add(index);
        }
        displayItems.removeDisplayItems(panel);
    }

    private void onContentChanged() {
        // 1. Remove the original visible itemsets from the display panel.
        //    These itemsets will be recorded in mLruSlot.
        // 2. Add the new visible itemsets to the display panel and cache
        //    (mItemsetMap).
        mContentInvalidated = true;
        mSlotView.notifyDataChanged();
        mContentInvalidated = false;

        // Clean up the cache by removing all itemsets recorded in mLruSlot.
        for (Integer index : mLruSlot) {
            mItemsetMap.remove(index);
        }
        mLruSlot.clear();
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
                mInvalidateIndex = mSlotIndex;
                mSlotView.notifySlotInvalidate(mSlotIndex);
                mInvalidateIndex = INDEX_NONE;
            }
        }
    }

    private class MyContentListener implements MediaSet.MediaSetListener {

        public void onContentChanged() {
            MediaSetSlotAdapter.this.onContentChanged();
        }
    }
}

