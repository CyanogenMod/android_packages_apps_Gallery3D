package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

public class SlotView extends GLView {

    private static final String TAG = "SlotView";
    private static final int MAX_VELOCITY = 2500;

    public static interface Model {
        public int size();
        public int getSlotHeight();
        public int getSlotWidth();
        public void putSlot(int index, int x, int y, DisplayItemPanel panel);
        public void freeSlot(int index, DisplayItemPanel panel);
    }

    private Model mModel;
    private final DisplayItemPanel mPanel;

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
        mModel = model;
        if (model != null) initializeLayoutParams();
        notifyDataChanged();
    }

    private void initializeLayoutParams() {
        int size = mModel.size();
        mSlotWidth = mModel.getSlotWidth();
        mSlotHeight = mModel.getSlotHeight();
        int rowCount = (getHeight() - mVerticalGap)
                / (mVerticalGap + mSlotHeight);
        if (rowCount == 0) rowCount = 1;
        mRowCount = rowCount;
        mScrollLimit = ((size + rowCount - 1) / rowCount)
                * (mHorizontalGap + mSlotWidth)
                + mHorizontalGap - getWidth();
        if (mScrollLimit < 0) mScrollLimit = 0;
    }

    @Override
    protected void onLayout(boolean changeSize, int l, int t, int r, int b) {
        mPanel.layout(0, 0, r - l, b - t);
        mVisibleStart = 0;
        mVisibleEnd = 0;
        mPanel.prepareTransition();
        initializeLayoutParams();
        // The scroll limit could be changed
        setScrollPosition(mPanel.mScrollX, true);
        mPanel.startTransition();
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
        setScrollPosition(0, false);
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
                n = Math.min(mModel.size(), i + rowCount); i < n; ++i) {
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
                n = Math.min(mModel.size(), i + rowCount); i < n; ++i) {
            mModel.putSlot(i, x, y, panel);
            y += rowHeight;
        }
    }

    // start: inclusive, end: exclusive
    private void setVisibleRange(int start, int end) {
        if (start == mVisibleStart && end == mVisibleEnd) return;
        int rowCount = mRowCount;
        if (start >= mVisibleEnd || end < mVisibleStart) {
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
