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
import com.android.gallery3d.ui.PositionRepository.Position;
import com.android.gallery3d.util.Utils;

import java.util.Random;

public class GalleryView implements SlotView.Listener {
    private static final String TAG = GalleryView.class.getSimpleName();

    static final int SLOT_WIDTH = 144;
    static final int SLOT_HEIGHT = 144;
    private static final int HORIZONTAL_GAP = 42;
    private static final int VERTICAL_GAP = 42;

    final SlotView mSlotView;

    private int mVisibleStart;
    private int mVisibleEnd;

    private Random mRandom = new Random();
    private long mSeed = mRandom.nextLong();

    final GalleryAdapter mModel;

    public static class GalleryItem {
        public DisplayItem[] covers;
    }

    public GalleryView(GalleryContext context, GalleryAdapter model, SlotView slotView) {

        mModel = Utils.checkNotNull(model);
        mSlotView = slotView;
        mSlotView.setSlotSize(SLOT_WIDTH, SLOT_HEIGHT);
        mSlotView.setSlotGaps(HORIZONTAL_GAP, VERTICAL_GAP, false);
        mSlotView.setSlotCount(model.size());
        mModel.setListener(new MyCacheListener());
        updateVisibleRange(
                slotView.getVisibleStart(), slotView.getVisibleEnd());
    }

    private void putSlotContent(int slotIndex, GalleryItem entry) {
        // Get displayItems from mItemsetMap or create them from MediaSet.
        Utils.Assert(entry != null);
        Rect rect = mSlotView.getSlotRect(slotIndex);

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
            mSlotView.putDisplayItem(position, item);
        }
        if (items.length > 0) {
            Position position = new Position();
            position.set(x, y, 0, 0, 1f);
            mSlotView.putDisplayItem(position, items[0]);
        }
    }

    private void freeSlotContent(int index, GalleryItem entry) {
        for (DisplayItem item : entry.covers) {
            mSlotView.removeDisplayItem(item);
        }
    }

    public int size() {
        return mModel.size();
    }

    public void onLayoutChanged(int width, int height) {
        updateVisibleRange(0, 0);
        updateVisibleRange(mSlotView.getVisibleStart(), mSlotView.getVisibleEnd());
    }

    public void onScrollPositionChanged(int position) {
        updateVisibleRange(mSlotView.getVisibleStart(), mSlotView.getVisibleEnd());
    }

    private void updateVisibleRange(int start, int end) {
        if (start == mVisibleStart && end == mVisibleEnd) return;
        if (start >= mVisibleEnd || mVisibleStart >= end) {
            for (int i = mVisibleStart, n = mVisibleEnd; i < n; ++i) {
                freeSlotContent(i, mModel.get(i));
            }
            mModel.setActiveWindow(start, end);
            for (int i = start; i < end; ++i) {
                putSlotContent(i, mModel.get(i));
            }
        } else {
            for (int i = mVisibleStart; i < start; ++i) {
                freeSlotContent(i, mModel.get(i));
            }
            for (int i = end, n = mVisibleEnd; i < n; ++i) {
                freeSlotContent(i, mModel.get(i));
            }
            mModel.setActiveWindow(start, end);
            for (int i = start, n = mVisibleStart; i < n; ++i) {
                putSlotContent(i, mModel.get(i));
            }
            for (int i = mVisibleEnd; i < end; ++i) {
                putSlotContent(i, mModel.get(i));
            }
        }
        mVisibleStart = start;
        mVisibleEnd = end;
        mSlotView.invalidate();
    }

    private class MyCacheListener implements GalleryAdapter.Listener {

        public void onSizeChanged(int size) {
            SlotView slotView = mSlotView;
            slotView.setSlotCount(size);
            updateVisibleRange(
                    slotView.getVisibleStart(), slotView.getVisibleEnd());
        }

        public void onWindowContentChanged(int slot, GalleryItem old, GalleryItem update) {
            SlotView slotView = mSlotView;
            freeSlotContent(slot, old);
            putSlotContent(slot, update);
            slotView.invalidate();
        }

        public void onContentInvalidated() {
            mSlotView.invalidate();
        }
    }
}