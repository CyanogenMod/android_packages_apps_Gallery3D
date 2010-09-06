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

import android.graphics.Rect;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.ui.PositionRepository.Position;
import com.android.gallery3d.util.Utils;

import java.util.Random;

public class AlbumSetView extends SlotView {
    private static final String TAG = "AlbumSetView";
    private static final int CACHE_SIZE = 32;

    static final int SLOT_WIDTH = 144;
    static final int SLOT_HEIGHT = 144;
    private static final int HORIZONTAL_GAP = 42;
    private static final int VERTICAL_GAP = 42;

    private int mVisibleStart;
    private int mVisibleEnd;

    private Random mRandom = new Random();
    private long mSeed = mRandom.nextLong();

    private AlbumSetSlidingWindow mDataWindow;
    private final GalleryContext mContext;
    private final SelectionManager mSelectionManager;

    public static interface Model {
        public MediaItem[] getMediaItems(int index);
        public MediaSet getMediaSet(int index);
        public int size();
        public void setActiveWindow(int start, int end);
        public void setListener(ModelListener listener);
    }

    public static interface ModelListener {
        public void onWindowContentChanged(
                int index, MediaItem old[], MediaItem update[]);
        public void onSizeChanged(int size);
    }

    public static class AlbumSetItem {
        public DisplayItem[] covers;
    }

    public AlbumSetView(GalleryContext context, SelectionManager selectionManager) {
        super(context.getAndroidContext(), context.getPositionRepository());
        mContext = context;
        mSelectionManager = selectionManager;
        setSlotSize(SLOT_WIDTH, SLOT_HEIGHT);
        setSlotGaps(HORIZONTAL_GAP, VERTICAL_GAP, false);
    }

    public void setModel(AlbumSetView.Model model) {

        if (mDataWindow != null) {
            mDataWindow.setListener(null);
            setSlotCount(0);
            mDataWindow = null;
        }
        if (model != null) {
            mDataWindow = new AlbumSetSlidingWindow(
                        mContext, mSelectionManager, model, CACHE_SIZE);
            mDataWindow.setListener(new MyCacheListener());
            setSlotCount(mDataWindow.size());
            updateVisibleRange(getVisibleStart(), getVisibleEnd());
        }
    }

    private void putSlotContent(int slotIndex, AlbumSetItem entry) {
        // Get displayItems from mItemsetMap or create them from MediaSet.
        Utils.Assert(entry != null);
        Rect rect = getSlotRect(slotIndex);

        DisplayItem[] items = entry.covers;
        mRandom.setSeed(slotIndex ^ mSeed);

        int x = (rect.left + rect.right) / 2;
        int y = (rect.top + rect.bottom) / 2;

        // Put the cover items in reverse order, so that the first item is on
        // top of the rest.
        for (int i = items.length -1; i > 0; --i) {
            DisplayItem item = items[i];
            int dx = 0;
            int dy = 0;
            int theta = 0;
            if (i != 0) {
                int seed = i;
                int sign = (seed % 2 == 0) ? 1 : -1;
                theta = (int) (30.0f * (0.5f - (float) Math.random()));
                dx = (int) (sign * 12.0f * seed + (0.5f - mRandom.nextFloat()) * 4 * seed);
                dy = (int) (sign * 4 + ((sign == 1) ? -8.0f : sign * (mRandom.nextFloat()) * 16.0f));
            }
            Position position = new Position();
            position.set(rect.left + item.getWidth() / 2 + dx, y + dy, 0, theta, 1f);
            putDisplayItem(position, item);
        }
        if (items.length > 0) {
            Position position = new Position();
            position.set(x, y, 0, 0, 1f);
            putDisplayItem(position, items[0]);
        }
    }

    private void freeSlotContent(int index, AlbumSetItem entry) {
        if (entry == null) return;
        for (DisplayItem item : entry.covers) {
            removeDisplayItem(item);
        }
    }

    public int size() {
        return mDataWindow.size();
    }

    @Override
    public void onLayoutChanged(int width, int height) {
        updateVisibleRange(0, 0);
        updateVisibleRange(getVisibleStart(), getVisibleEnd());
    }

    @Override
    public void onScrollPositionChanged(int position) {
        updateVisibleRange(getVisibleStart(), getVisibleEnd());
    }

    private void updateVisibleRange(int start, int end) {
        if (start == mVisibleStart && end == mVisibleEnd) return;
        if (start >= mVisibleEnd || mVisibleStart >= end) {
            for (int i = mVisibleStart, n = mVisibleEnd; i < n; ++i) {
                freeSlotContent(i, mDataWindow.get(i));
            }
            mDataWindow.setActiveWindow(start, end);
            for (int i = start; i < end; ++i) {
                putSlotContent(i, mDataWindow.get(i));
            }
        } else {
            for (int i = mVisibleStart; i < start; ++i) {
                freeSlotContent(i, mDataWindow.get(i));
            }
            for (int i = end, n = mVisibleEnd; i < n; ++i) {
                freeSlotContent(i, mDataWindow.get(i));
            }
            mDataWindow.setActiveWindow(start, end);
            for (int i = start, n = mVisibleStart; i < n; ++i) {
                putSlotContent(i, mDataWindow.get(i));
            }
            for (int i = mVisibleEnd; i < end; ++i) {
                putSlotContent(i, mDataWindow.get(i));
            }
        }
        mVisibleStart = start;
        mVisibleEnd = end;

        invalidate();
    }

    private class MyCacheListener implements AlbumSetSlidingWindow.Listener {

        public void onSizeChanged(int size) {
            setSlotCount(size);
            updateVisibleRange(getVisibleStart(), getVisibleEnd());
        }

        public void onWindowContentChanged(int slot, AlbumSetItem old, AlbumSetItem update) {
            freeSlotContent(slot, old);
            putSlotContent(slot, update);
            invalidate();
        }

        public void onContentInvalidated() {
            invalidate();
        }
    }
}