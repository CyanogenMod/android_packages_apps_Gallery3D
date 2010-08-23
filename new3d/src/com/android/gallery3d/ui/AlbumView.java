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
import android.graphics.Rect;

import com.android.gallery3d.ui.PositionRepository.Position;

public class AlbumView implements SlotView.Listener {
    private static final String TAG = "AlbumView";

    static final int SLOT_WIDTH = 144;
    static final int SLOT_HEIGHT = 144;
    private static final int HORIZONTAL_GAP = 0;
    private static final int VERTICAL_GAP = 0;

    private final SlotView mSlotView;

    private int mVisibleStart = 0;
    private int mVisibleEnd = 0;

    private AlbumDataAdapter mModel;

    public AlbumView(Context context, AlbumDataAdapter model, SlotView slotView) {
        mSlotView = slotView;
        mModel = model;
        mModel.setListener(new MyDataModelListener());
        mSlotView.setSlotSize(SLOT_WIDTH, SLOT_HEIGHT);
        mSlotView.setSlotGaps(HORIZONTAL_GAP, VERTICAL_GAP, true);
        mSlotView.setSlotCount(model.size());
    }

    private void putSlotContent(int slotIndex, DisplayItem item) {
        Rect rect = mSlotView.getSlotRect(slotIndex);
        Position position = new Position(
                (rect.left + rect.right) / 2, (rect.top + rect.bottom) / 2, 0);
        mSlotView.putDisplayItem(position, item);
    }

    private void updateVisibleRange(int start, int end) {
        if (start == mVisibleStart && end == mVisibleEnd) return;
        Log.v(TAG, String.format("visible range: %s - %s", start, end));
        if (start >= mVisibleEnd || mVisibleStart >= end) {
            for (int i = mVisibleStart, n = mVisibleEnd; i < n; ++i) {
                mSlotView.removeDisplayItem(mModel.get(i));
            }
            mModel.setActiveWindow(start, end);
            for (int i = start; i < end; ++i) {
                putSlotContent(i, mModel.get(i));
            }
        } else {
            for (int i = mVisibleStart; i < start; ++i) {
                mSlotView.removeDisplayItem(mModel.get(i));
            }
            for (int i = end, n = mVisibleEnd; i < n; ++i) {
                mSlotView.removeDisplayItem(mModel.get(i));
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
    }

    public void onLayoutChanged(int width, int height) {
        updateVisibleRange(0, 0);
        updateVisibleRange(mSlotView.getVisibleStart(), mSlotView.getVisibleEnd());
    }

    public void onScrollPositionChanged(int position) {
        updateVisibleRange(mSlotView.getVisibleStart(), mSlotView.getVisibleEnd());
    }

    private class MyDataModelListener implements AlbumDataAdapter.Listener {

        public void onContentInvalidated() {
            mSlotView.invalidate();
        }

        public void onSizeChanged(int size) {
            SlotView slotView = mSlotView;
            slotView.setSlotCount(size);
            updateVisibleRange(
                    slotView.getVisibleStart(), slotView.getVisibleEnd());
        }

        public void onWindowContentChanged(
                int slotIndex, DisplayItem old, DisplayItem update) {
            mSlotView.removeDisplayItem(old);
            putSlotContent(slotIndex, update);
        }
    }
}
