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

public abstract class HeadUpDisplay extends GLView {
    private static final String TAG = "HeadUpDisplay";
    private static final int POPUP_WINDOW_OVERLAP = 20;
    private static final int POPUP_TRIANGLE_OFFSET = 15;

    private static final float MAX_HEIGHT_RATIO = 0.8f;
    private static final float MAX_WIDTH_RATIO = 0.8f;

    private static int sPopupWindowOverlap = -1;
    private static int sPopupTriangleOffset;

    private final MenuItemBar mBottomBar;
    private final MenuBar mTopBar;
    private final Context mContext;
    private final Pathbar mPathbar;
    private final NinePatchTexture mHighlight;

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
        mBottomBar = new MenuItemBar();
        mTopBar = new MenuBar();
        mPathbar = new Pathbar(context);

        mHighlight = new NinePatchTexture(context, R.drawable.menu_highlight);
        mTopBar.setBackground(
                new NinePatchTexture(context, R.drawable.top_menu_bar_bg));
        mBottomBar.setBackground(
                new NinePatchTexture(context, R.drawable.menu_bar_bg));
        mBottomBar.setOnSelectedListener(new MySelectedListener());

        super.addComponent(mPathbar);
        super.addComponent(mTopBar);
        super.addComponent(mBottomBar);

        initialize();
    }

    @Override
    protected void onLayout(
            boolean changesize, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.UNSPECIFIED;
        mPathbar.measure(widthSpec, heightSpec);
        mTopBar.measure(widthSpec, heightSpec);
        mBottomBar.measure(widthSpec, heightSpec);

        mPathbar.layout(0, 0, width, mPathbar.getMeasuredHeight());
        int offset = mPathbar.getMeasuredHeight();
        mTopBar.layout(0, offset, width, offset + mTopBar.getMeasuredHeight());
        mBottomBar.layout(
                0, height - mBottomBar.getMeasuredHeight(), width, height);
        if (mPopupWindow != null
                && mPopupWindow.getVisibility() == GLView.VISIBLE) {
            this.layoutPopupWindow(mAnchorView);
        }
    }

    protected MenuItem addBottomMenuItem(int iconId, int stringId) {
        MenuItem item = new MenuItem(mContext, iconId, stringId);
        item.setHighlight(mHighlight);
        mBottomBar.addComponent(item);
        return item;
    }

    private void addTopMenuButton(int stringId) {
        MenuButton button =
                new MenuButton(mContext, IconLabel.NULL_ID, stringId);
        button.setHighlight(mHighlight);
        mTopBar.addComponent(button);
    }

    abstract protected void initializeMenu();

    private void initialize() {
        Context context = mContext;

        mPathIcons = new ResourceTexture[] {
                new ResourceTexture(context, R.drawable.icon_home_small),
                new ResourceTexture(context, R.drawable.icon_camera_small),
                new ResourceTexture(context, R.drawable.icon_folder_small),
                new ResourceTexture(context, R.drawable.icon_picasa_small)};

        mPathTitle = new String[] {"Gallery", "Camera", "Folder", "Picasa"};

        addTopMenuButton(R.string.select_all);
        mTopBar.addComponent(new IconLabel(
                context, IconLabel.NULL_ID, R.string.items));
        addTopMenuButton(R.string.deselect_all);

        initializeMenu();

        mPathbar.push(mPathIcons[0], mPathTitle[0]);
        mPathbar.push(mPathIcons[1], mPathTitle[1]);
        mPathbar.push(mPathIcons[2], mPathTitle[2]);

        mPathbar.setOnClickedListener(new MyPathbarListener());
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
        mListView.setOnItemSelectedListener(new MyMenuItemListener());
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

    protected void buildShareMenu(MenuItem menu) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        // TODO: the type should match to the selected items
        intent.setType("image/jpeg");
        PackageManager packageManager = mContext.getPackageManager();
        for(ResolveInfo info
                : packageManager.queryIntentActivities(intent, 0)) {
            String label = info.loadLabel(packageManager).toString();
            Drawable icon = info.loadIcon(packageManager);
            menu.addMenuItem(icon, label);
        }
    }

    private class MyMenuItemListener implements GLListView.OnItemSelectedListener {
        @Override
        public void onItemSelected(GLView view, int position) {
            MenuItem item = (MenuItem) view;
            item.notifyClickListeners();
            mPopupWindow.popoff();
        }
    }

    private class MySelectedListener implements MenuItemBar.OnSelectedListener {

        @Override
        public void onSelected(MenuItem source) {
            if (source == null) {
                mPopupWindow.popoff();
            } else {
                if (mPopupWindow == null) initializePopupWindow();
                mListView.setDataModel(source.getSubMenu());
                layoutPopupWindow(source);
                if (mPopupWindow.getVisibility() != GLView.VISIBLE) {
                    mPopupWindow.popup();
                }
            }
        }
    }

    private class MyPathbarListener implements Pathbar.OnClickedListener {

        @Override
        public void onClicked(Pathbar source, int index) {
            int size = mPathbar.size();
            if (index < size - 1) {
                mPathbar.pop(size - index - 1);
            } else {
                // click on the last item
                if (size == mPathIcons.length) {
                    source.get(index).setTitle("No more item");
                } else {
                    source.push(mPathIcons[index + 1], mPathTitle[index + 1]);
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
