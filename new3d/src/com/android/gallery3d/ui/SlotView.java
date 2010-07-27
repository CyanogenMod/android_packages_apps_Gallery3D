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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

public class SlotView extends GLView {

    private static final String TAG = "SlotView";
    private static final int MAX_VELOCITY = 2500;
    private static final int NOT_AT_SLOTPOSITION = -1;

    public static interface Model {
        public int size();
        public int getSlotHeight();
        public int getSlotWidth();
        public void putSlot(int index, int x, int y, DisplayItemPanel panel);
        public void freeSlot(int index, DisplayItemPanel panel);
    }

    private Model mModel;
    private final DisplayItemPanel mPanel;

    private int mSlotCount;
    private int mVerticalGap;
    private int mHorizontalGap;
    private int mSlotWidth;
    private int mSlotHeight;
    private int mRowCount;
    private int mScrollLimit;

    private int mVisibleStart = 0;
    private int mVisibleEnd = 0;

    private final GestureDetector mGestureDetector;
    private final ScrollerHelper mScroller;
    private SlotTapListener mSlotTapListener;

    public SlotView(Context context) {
        mPanel = new DisplayItemPanel();
        super.addComponent(mPanel);
        mGestureDetector =
                new GestureDetector(context, new MyGestureListener());
        mScroller = new ScrollerHelper(context, new DecelerateInterpolator(1));
    }

    @Override
    public void addComponent(GLView view) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeComponent(GLView view) {
        throw new UnsupportedOperationException();
    }

    public void setModel(Model model) {
        if (model == mModel) return;
        if (mModel != null) {
            // free all the slot in the old model
            setVisibleRange(0, 0);
        }
        mModel = model;
        notifyDataChanged();
    }

    private void initializeLayoutParams() {
        mSlotCount = mModel.size();
        mSlotWidth = mModel.getSlotWidth();
        mSlotHeight = mModel.getSlotHeight();
        int rowCount = (getHeight() - mVerticalGap)
                / (mVerticalGap + mSlotHeight);
        if (rowCount == 0) rowCount = 1;
        mRowCount = rowCount;
        mScrollLimit = ((mSlotCount + rowCount - 1) / rowCount)
                * (mHorizontalGap + mSlotWidth)
                + mHorizontalGap - getWidth();
        if (mScrollLimit < 0) mScrollLimit = 0;
    }

    @Override
    protected void onLayout(boolean changeSize, int l, int t, int r, int b) {
        mPanel.layout(0, 0, r - l, b - t);
        mVisibleStart = 0;
        mVisibleEnd = 0;
//        mPanel.prepareTransition();
        initializeLayoutParams();
        // The scroll limit could be changed
        setScrollPosition(mPanel.mScrollX, true);
//        mPanel.startTransition();
    }

    private void setScrollPosition(int position, boolean force) {
        position = Util.clamp(position, 0, mScrollLimit);
        if (!force && position == mPanel.mScrollX) return;
        mPanel.mScrollX = position;

        int colWidth = mHorizontalGap + mSlotWidth;
        int rowHeight = mVerticalGap + mSlotHeight;
        int startColumn = position / colWidth;
        int endColumn = (position + getWidth() + mSlotWidth - 1) / colWidth;
        setVisibleRange(startColumn, endColumn);
        invalidate();
    }

    public void notifyDataChanged() {
        // free all slots in previous data
        setVisibleRange(0, 0);

        if (mModel != null) initializeLayoutParams();
        setScrollPosition(0, true);
        notifyDataInvalidate();
    }

    public void notifyDataInvalidate() {
        invalidate();
    }

    @Override
    protected boolean dispatchTouchEvent(MotionEvent event) {
        // Don't pass the event to the child (MediaDisplayPanel).
        // Handle it here
        return onTouch(event);
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mScroller.forceFinished();
                break;
        }
        return true;
    }

    @Override
    protected void render(GLCanvas canvas) {
        super.render(canvas);
        if (mScroller.computeScrollOffset(canvas.currentAnimationTimeMillis())) {
            setScrollPosition(mScroller.getCurrentPosition(), false);
            invalidate();
        }
    }

    private void freeSlotsInColumn(int columnIndex) {
        int rowCount = mRowCount;
        DisplayItemPanel panel = mPanel;
        for (int i = columnIndex * rowCount,
                n = Math.min(mSlotCount, i + rowCount); i < n; ++i) {
            mModel.freeSlot(i, panel);
        }
    }

    private void putSlotsInColumn(int columnIndex) {
        int x = columnIndex * (mHorizontalGap + mSlotWidth) + mHorizontalGap;
        int y = mVerticalGap;
        int rowHeight = mVerticalGap + mSlotHeight;
        int rowCount = mRowCount;

        DisplayItemPanel panel = mPanel;
        for (int i = columnIndex * rowCount,
                n = Math.min(mSlotCount, i + rowCount); i < n; ++i) {
            mModel.putSlot(i, x, y, panel);
            y += rowHeight;
        }
    }

    public void notifySlotInvalidate(int slotIndex) {
        int columnIndex = slotIndex / mRowCount;
        if (columnIndex >= mVisibleStart && columnIndex < mVisibleEnd) {
            mModel.freeSlot(slotIndex, mPanel);
            int x = columnIndex * (mHorizontalGap + mSlotWidth) + mHorizontalGap;
            int rowIndex = slotIndex - (columnIndex * mRowCount);
            int y = mVerticalGap + (mVerticalGap + mSlotHeight) * rowIndex;
            mModel.putSlot(slotIndex, x, y, mPanel);
        }
    }

    // start: inclusive, end: exclusive
    private void setVisibleRange(int start, int end) {
        if (start == mVisibleStart && end == mVisibleEnd) return;
        int rowCount = mRowCount;
        if (start >= mVisibleEnd || end <= mVisibleStart) {
            for (int i = mVisibleStart, n = mVisibleEnd; i < n; ++i) {
                freeSlotsInColumn(i);
            }
            for (int i = start; i < end; ++i) {
                putSlotsInColumn(i);
            }
        } else {
            for (int i = mVisibleStart; i < start; ++i) {
                freeSlotsInColumn(i);
            }
            for (int i = end, n = mVisibleEnd; i < n; ++i) {
                freeSlotsInColumn(i);
            }
            for (int i = start, n = mVisibleStart; i < n; ++i) {
                putSlotsInColumn(i);
            }
            for (int i = mVisibleEnd; i < end; ++i) {
                putSlotsInColumn(i);
            }
        }
        mVisibleStart = start;
        mVisibleEnd = end;
    }

    private class MyGestureListener
            extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1,
                MotionEvent e2, float velocityX, float velocityY) {
            if (mScrollLimit == 0) return false;
            velocityX = Util.clamp(velocityX, -MAX_VELOCITY, MAX_VELOCITY);
            mScroller.fling(mPanel.mScrollX, -(int) velocityX, 0, mScrollLimit);
            invalidate();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1,
                MotionEvent e2, float distanceX, float distanceY) {
            if (mScrollLimit == 0) return false;
            setScrollPosition(mPanel.mScrollX + (int) distanceX, false);
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            int index = getSlotIndexByPosition(x, y);
            if (index != SlotView.NOT_AT_SLOTPOSITION) {
                mSlotTapListener.onSingleTapUp(index);
            }
            return true;
        }
    }

    public void setSlotTapListener(SlotTapListener listener) {
        mSlotTapListener = listener;
    }

    private int getSlotIndexByPosition(float x, float y) {
        int columnWidth = mHorizontalGap + mSlotWidth;
        float absoluteX = x + mPanel.mScrollX;
        int columnIdx = (int) (absoluteX + 0.5) / columnWidth;
        if ((absoluteX - columnWidth * columnIdx) < mHorizontalGap) {
            return NOT_AT_SLOTPOSITION;
        }

        int rowHeight = mVerticalGap + mSlotHeight;
        float absoluteY = y + mPanel.mScrollY;
        int rowIdx = (int) (absoluteY + 0.5) / rowHeight;
        if (((absoluteY - rowHeight * rowIdx) < mVerticalGap)
            || rowIdx >= mRowCount) {
            return NOT_AT_SLOTPOSITION;
        }
        int index = columnIdx * mRowCount + rowIdx;
        return index >= mSlotCount ? NOT_AT_SLOTPOSITION : index;
    }

    public interface SlotTapListener {
        public void onSingleTapUp(int slotIndex);
    }

    public void setGaps(int horizontalGap, int verticalGap) {
        if (mHorizontalGap != horizontalGap || mVerticalGap != verticalGap) {
            mHorizontalGap = horizontalGap;
            mVerticalGap = verticalGap;
            if (mModel != null) {
                initializeLayoutParams();
                invalidate();
            }
        }
    }
}
