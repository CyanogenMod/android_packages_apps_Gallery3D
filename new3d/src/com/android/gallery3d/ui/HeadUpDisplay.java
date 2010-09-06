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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View.MeasureSpec;

import com.android.gallery3d.R;
import com.android.gallery3d.util.Utils;

public class HeadUpDisplay extends GLView {
    private static final int POPUP_WINDOW_OVERLAP = 20;
    private static final int POPUP_TRIANGLE_OFFSET = 15;

    private static final float MAX_HEIGHT_RATIO = 0.8f;
    private static final float MAX_WIDTH_RATIO = 0.8f;

    private static int sPopupWindowOverlap = -1;
    private static int sPopupTriangleOffset;

    private MenuItemBar mBottomBar;
    private MenuBar mTopBar;
    private HudMenuInterface mMenu;

    private final Context mContext;

    private PopupWindow mPopupWindow;
    private GLListView mListView;

    private GLView mAnchorView;

    private ResourceTexture mPathIcons[];
    private String mPathTitle[];

    private static void initializeStaticVariables(Context context) {
        if (sPopupWindowOverlap >= 0) return;

        sPopupWindowOverlap = Utils.dpToPixel(context, POPUP_WINDOW_OVERLAP);
        sPopupTriangleOffset = Utils.dpToPixel(context, POPUP_TRIANGLE_OFFSET);
    }

    public HeadUpDisplay(Context context) {
        initializeStaticVariables(context);
        mContext = context;
    }

    @Override
    protected void onLayout(
            boolean changesize, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.UNSPECIFIED;
        mTopBar.measure(widthSpec, heightSpec);
        mBottomBar.measure(widthSpec, heightSpec);

        mTopBar.layout(0, 0, width, mTopBar.getMeasuredHeight());
        mBottomBar.layout(
                0, height - mBottomBar.getMeasuredHeight(), width, height);
        if (mPopupWindow != null
                && mPopupWindow.getVisibility() == GLView.VISIBLE) {
            this.layoutPopupWindow(mAnchorView);
        }
    }

    public void setMenu(HudMenuInterface menu) {
        mMenu = menu;

        mTopBar = menu.getTopMenuBar();
        mBottomBar = menu.getBottomMenuBar();
        addComponent(mTopBar);
        addComponent(mBottomBar);

        mBottomBar.setOnSelectedListener(new BottomBarSelectedListener());
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        if (mPopupWindow != null &&
                mPopupWindow.getVisibility() == GLView.VISIBLE) {
            mBottomBar.setSelectedItem(null);
            return true;
        }
        return false;
    }

    @Override
    protected boolean dispatchTouchEvent(MotionEvent event,
            int x, int y, GLView component, boolean checkBounds) {
        if (mPopupWindow != null
                && mPopupWindow.getVisibility() == GLView.VISIBLE
                && component != mPopupWindow && component != mBottomBar) {
            return false;
        }
        return super.dispatchTouchEvent(event, x, y, component, checkBounds);
    }

    private void layoutPopupWindow(GLView anchorView) {
        mAnchorView = anchorView;

        Rect rect = new Rect();
        getBoundsOf(anchorView, rect);

        int width = (int) (getWidth() * MAX_WIDTH_RATIO + .5);
        int height = (int) (getHeight() * MAX_HEIGHT_RATIO + .5);

        mPopupWindow.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));

        width = mPopupWindow.getMeasuredWidth();
        height = mPopupWindow.getMeasuredHeight();

        int anchorX = (rect.left + rect.right) / 2;
        int anchorY = rect.top + sPopupWindowOverlap;

        int xoffset = Utils.clamp(anchorX - width / 2, 0, getWidth() - width);
        int yoffset = Math.max(0, anchorY - height);

        mPopupWindow.setAnchorPosition(anchorX - xoffset);
        mPopupWindow.layout(
                xoffset, yoffset, xoffset + width, yoffset + height);
    }

    private void initializePopupWindow() {
        Context context = mContext;
        mListView = new GLListView(context);
        mListView.setOnItemSelectedListener(new PopupMenuItemListener());
        mPopupWindow = new PopupWindow();

        mPopupWindow.setBackground(
                new NinePatchTexture(context, R.drawable.popup));
        mPopupWindow.setAnchor(new ResourceTexture(
                context, R.drawable.popup_triangle_bottom),
                (int) (sPopupTriangleOffset + 0.5));
        mListView.setHighLight(new NinePatchTexture(
                context, R.drawable.popup_option_selected));
        mPopupWindow.setContent(mListView);
        mPopupWindow.setVisibility(GLView.INVISIBLE);
        super.addComponent(mPopupWindow);
    }

    private class PopupMenuItemListener implements GLListView.OnItemSelectedListener {
        public void onItemSelected(GLView view, int position) {
            if (mMenu != null) mMenu.onMenuItemSelected(mListView.getDataModel(), position);
            mPopupWindow.popoff();
        }
    }

    private class BottomBarSelectedListener implements OnSelectedListener {
        public void onSelected(GLView source) {
            if (source == null) {
                mPopupWindow.popoff();
            } else {
                if (mPopupWindow == null) initializePopupWindow();
                mListView.setDataModel(mMenu.getMenuModel(source));
                layoutPopupWindow(source);
                if (mPopupWindow.getVisibility() != GLView.VISIBLE) {
                    mPopupWindow.popup();
                }
            }
        }
    }

    public int getTopBarBottomPosition() {
        Rect rect = new Rect();
        getBoundsOf(mTopBar, rect);
        return rect.bottom;
    }

    public int getBottomBarTopPosition() {
        Rect rect = new Rect();
        getBoundsOf(mBottomBar, rect);
        return rect.top;
    }
}
