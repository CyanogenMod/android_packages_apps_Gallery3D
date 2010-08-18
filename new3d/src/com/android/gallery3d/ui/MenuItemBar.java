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
