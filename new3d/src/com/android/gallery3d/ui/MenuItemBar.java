package com.android.gallery3d.ui;

import android.view.MotionEvent;

public class MenuItemBar extends MenuBar {
    public static final int INDEX_NONE = -1;

    private OnSelectedListener mOnSelectedListener;
    private MenuItem mSelectedItem;
    private boolean mSelectionChanged = false;

    public void setSelectedItem(MenuItem source) {
        if (mSelectedItem == source) return;
        mSelectionChanged = true;
        if (mSelectedItem != null) mSelectedItem.setSelected(false);
        mSelectedItem = source;
        if (mSelectedItem != null) mSelectedItem.setSelected(true);

        if (mOnSelectedListener != null) {
            mOnSelectedListener.onSelected(mSelectedItem);
        }
    }

    public void setOnSelectedListener(OnSelectedListener listener) {
        mOnSelectedListener = listener;
    }

    @Override
    protected boolean dispatchTouchEvent(MotionEvent event) {
        // do not dispatch to children
        return onTouch(event);
    }

    @SuppressWarnings("fallthrough")
    @Override
    protected boolean onTouch(MotionEvent event) {
        int x = (int) event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mSelectionChanged = false;
                // fall-through
            case MotionEvent.ACTION_MOVE:
                for (int i = 0, n = getComponentCount(); i < n; ++i) {
                    GLView component = getComponent(i);
                    if (x <= component.mBounds.right) {
                        setSelectedItem((MenuItem) component);
                        return true;
                    }
                }
                setSelectedItem(null);
                break;
            case MotionEvent.ACTION_UP:
                if (mSelectionChanged == false) {
                    setSelectedItem(null);
                }
        }
        return true;

    }
}
