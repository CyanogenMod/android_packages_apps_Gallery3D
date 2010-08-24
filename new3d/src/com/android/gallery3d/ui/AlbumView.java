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
import com.android.gallery3d.ui.PositionRepository.Position;

public class AlbumView extends SlotView {
    private static final String TAG = "AlbumView";
    private static final int CACHE_SIZE = 64;

    static final int SLOT_WIDTH = 144;
    static final int SLOT_HEIGHT = 144;
    private static final int HORIZONTAL_GAP = 0;
    private static final int VERTICAL_GAP = 0;

    private int mVisibleStart = 0;
    private int mVisibleEnd = 0;

    private AlbumSlidingWindow mDataWindow;
    private final GalleryContext mContext;
    private final SelectionManager mSelectionManager;

    public static interface Model {
        public int size();
        public MediaItem get(int index);
        public void setActiveWindow(int start, int end);
        public void setListener(ModelListener listener);
    }

    public static interface ModelListener {
        public void onWindowContentChanged(
                int index, MediaItem old, MediaItem update);
        public void onSizeChanged(int size);
    }

    public AlbumView(GalleryContext context, SelectionManager selectionManager) {
        super(context.getAndroidContext(), context.getPositionRepository());
        setSlotSize(SLOT_WIDTH, SLOT_HEIGHT);
        setSlotGaps(HORIZONTAL_GAP, VERTICAL_GAP, true);
        mContext = context;
        mSelectionManager = selectionManager;
    }

    public void setModel(Model model) {
        if (mDataWindow != null) {
            mDataWindow.setListener(null);
            setSlotCount(0);
            mDataWindow = null;
        }
        if (model != null) {
            mDataWindow = new AlbumSlidingWindow(
                    mContext, mSelectionManager, model, CACHE_SIZE);
            mDataWindow.setListener(new MyDataModelListener());
            setSlotCount(model.size());
            updateVisibleRange(getVisibleStart(), getVisibleEnd());
        }
    }

    private void putSlotContent(int slotIndex, DisplayItem item) {
        Rect rect = getSlotRect(slotIndex);
        Position position = new Position(
                (rect.left + rect.right) / 2, (rect.top + rect.bottom) / 2, 0);
        putDisplayItem(position, item);
    }

    private void updateVisibleRange(int start, int end) {
        if (start == mVisibleStart && end == mVisibleEnd) return;

        if (start >= mVisibleEnd || mVisibleStart >= end) {
            for (int i = mVisibleStart, n = mVisibleEnd; i < n; ++i) {
                removeDisplayItem(mDataWindow.get(i));
            }
            mDataWindow.setActiveWindow(start, end);
            for (int i = start; i < end; ++i) {
                putSlotContent(i, mDataWindow.get(i));
            }
        } else {
            for (int i = mVisibleStart; i < start; ++i) {
                removeDisplayItem(mDataWindow.get(i));
            }
            for (int i = end, n = mVisibleEnd; i < n; ++i) {
                removeDisplayItem(mDataWindow.get(i));
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
    }

    @Override
    protected void onLayoutChanged(int width, int height) {
        // Reput all the items
        updateVisibleRange(0, 0);
        updateVisibleRange(getVisibleStart(), getVisibleEnd());
    }

    @Override
    protected void onScrollPositionChanged(int position) {
        updateVisibleRange(getVisibleStart(), getVisibleEnd());
    }

    private class MyDataModelListener implements AlbumSlidingWindow.Listener {

        public void onContentInvalidated() {
            invalidate();
        }

        public void onSizeChanged(int size) {
            setSlotCount(size);
            updateVisibleRange(getVisibleStart(), getVisibleEnd());
        }

        public void onWindowContentChanged(
                int slotIndex, DisplayItem old, DisplayItem update) {
            removeDisplayItem(old);
            putSlotContent(slotIndex, update);
        }
    }
}
