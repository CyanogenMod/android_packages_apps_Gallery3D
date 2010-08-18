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

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryContext;

import java.util.ArrayList;
import java.util.Hashtable;

public class HudMenu implements HudMenuInterface, SelectionManager.SelectionListener {
    private static final int DELETE_MODEL = 0;
    private static final int MORE_MODEL = 1;
    private static final int SHARE_MODEL = 2;
    private static final int TOTAL_MODEL_COUNT = 3;

    GalleryContext mContext;
    MenuBar mTopBar;
    MenuItemBar mBottomBar;
    NinePatchTexture mHighlight;
    SelectionManager mSelectionManager;
    MenuModel[] mMenuModels;
    MenuItem mShare;
    MenuItem mDelete;
    MenuItem mMore;

    public HudMenu(GalleryContext context, SelectionManager manager) {
        mContext = context;
        mSelectionManager = manager;
        mHighlight = new NinePatchTexture(context.getAndroidContext(), R.drawable.menu_highlight);
        manager.setSelectionListener(this);
        mMenuModels = new MenuModel[TOTAL_MODEL_COUNT];
    }

    public MenuBar getTopMenuBar() {
        if (mTopBar == null) createTopMenuBar();
        return mTopBar;
    }

    public MenuItemBar getBottomMenuBar() {
        if (mBottomBar == null) createBottomMenuBar();
        return mBottomBar;
    }

    protected void createTopMenuBar() {
        Context context = mContext.getAndroidContext();
        mTopBar = new MenuBar(GLView.INVISIBLE);
        mTopBar.setBackground(new NinePatchTexture(context, R.drawable.top_menu_bar_bg));

        // Select all
        MenuButton btn = new MenuButton(context, IconLabel.NULL_ID, R.string.select_all);
        btn.setHighlight(mHighlight);
        mTopBar.addComponent(btn);

        // Deselect all
        mTopBar.addComponent(new IconLabel(context, IconLabel.NULL_ID, R.string.items));
        btn = new MenuButton(context, IconLabel.NULL_ID, R.string.deselect_all);
        btn.setHighlight(mHighlight);
        mTopBar.addComponent(btn);
    }

    public class MenuModel implements GLListView.Model {
        ArrayList<MenuItem> mItems;
        public MenuModel() {
            mItems = new ArrayList<MenuItem>();
        }

        public MenuModel(MenuItem[] items) {
            mItems = new ArrayList<MenuItem>(items.length);
            for (MenuItem item : items) {
                mItems.add(item);
            }
        }

        public void addItem(MenuItem item) {
            mItems.add(item);
        }

        public GLView getView(int index) {
            return mItems.get(index);
        }

        public boolean isSelectable(int index) {
            return true;
        }

        public int size() {
            return mItems.size();
        }

        /**
         * Handle the menu operation here.
         */
        public void onItemSelected(int position) {
        }
    }

    protected void createBottomMenuBar() {
        Context context = mContext.getAndroidContext();
        mBottomBar = new MenuItemBar(GLView.INVISIBLE);
        mBottomBar.setBackground(
                new NinePatchTexture(context, R.drawable.top_menu_bar_bg));

        // Share menu
        mShare = new MenuItem(context, R.drawable.icon_share, R.string.share, mHighlight);
        mMenuModels[SHARE_MODEL] = new MenuModel();
        mBottomBar.addComponent(mShare);

        // Delete menu & its submenu
        mDelete = new MenuItem(context, R.drawable.icon_delete, R.string.delete
                , mHighlight);
        mMenuModels[DELETE_MODEL] = new MenuModel(new MenuItem[] {
                new MenuItem(context, R.drawable.icon_delete, R.string.confirm_delete,
                        mHighlight),
                new MenuItem(context, R.drawable.icon_cancel, R.string.cancel,
                        mHighlight)
         });
        mBottomBar.addComponent(mDelete);

        // More menu & its submenu
        mMore = new MenuItem(context, R.drawable.icon_more, R.string.more, mHighlight);
        mMenuModels[MORE_MODEL] = new MenuModel(new MenuItem[] {
                new MenuItem(context, R.drawable.icon_details, R.string.details,
                        mHighlight),
                new MenuItem(context, R.drawable.icon_details, R.string.rotate_right,
                        mHighlight),
                new MenuItem(context, R.drawable.icon_details, R.string.rotate_left,
                        mHighlight),
        });
        mBottomBar.addComponent(mMore);
    }

    public GLListView.Model getMenuModel(GLView item) {
         if (item == mDelete) {
            return mMenuModels[DELETE_MODEL];
        } else if (item == mMore) {
            return mMenuModels[MORE_MODEL];
        } else if (item == mShare) {
            // TODO: generate the right model
            return mMenuModels[SHARE_MODEL];
        }

        return null;
    }

    public void onMenuItemSelected(GLListView.Model listViewModel, int position) {
        MenuModel model = (MenuModel) listViewModel;
        model.onItemSelected(position);
    }

    public void onSelectionModeChange(boolean inSelectionMode) {
        int visibility = inSelectionMode ? GLView.VISIBLE : GLView.INVISIBLE;
        getTopMenuBar().setVisibility(visibility);
        getBottomMenuBar().setVisibility(visibility);
    }
}
