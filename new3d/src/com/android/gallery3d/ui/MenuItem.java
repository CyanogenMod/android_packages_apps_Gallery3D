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
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;

import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaOperation;

public class MenuItem extends IconLabel {
    private static final String TAG = "MenuItem";
    private boolean mSelected;
    private Texture mHighlight;
    protected Context mContext;
    private SubMenu mSubMenu;
    private final ArrayList<MenuItemClickListener> mListeners = new ArrayList<MenuItemClickListener>();

    private static class SubMenu implements GLListView.Model {
        ArrayList<MenuItem> mItems;

        public MenuItem addMenuItem(MenuItem item) {
            if (mItems == null) {
                mItems = new ArrayList<MenuItem>();
            }
            mItems.add(item);
            return item;
        }

        @Override
        public GLView getView(int index) {
            if (mItems == null) return null;
            return mItems.get(index);
        }

        @Override
        public boolean isSelectable(int index) {
            return true;
        }

        @Override
        public int size() {
            if (mItems == null) return 0;
            return mItems.size();
        }
    }

    public interface MenuItemClickListener {
        public void onClick();
    }

    public MenuItem(Context context, int icon, int label) {
        super(context, icon, label);
        mContext = context;
        mSubMenu = new SubMenu();
    }

    public MenuItem(Context context, BasicTexture texture, String label) {
        super(context, texture, label);
    }

    public MenuItem addMenuItem(MenuItem item) {
        return mSubMenu.addMenuItem(item);
    }

    public MenuItem addMenuItem(int icon, int title) {
        return mSubMenu.addMenuItem(new MenuItem(mContext, icon, title));
    }

    public MenuItem addMenuItem(Drawable icon, String title) {
        DrawableTexture iconTexture = null;
        if (icon != null) {
            iconTexture = new DrawableTexture(icon);
            float target = 45;
            int width = icon.getIntrinsicWidth();
            int height = icon.getIntrinsicHeight();
            float scale = target / Math.max(width, height);
            iconTexture.setSize((int) (width * scale + .5f),
                    (int) (height * scale + .5f));
        }

        return mSubMenu.addMenuItem(new MenuItem(mContext, iconTexture, title));
    }

    public GLListView.Model getSubMenu() {
        return mSubMenu;
    }

    public void addClickListener(MenuItemClickListener listener) {
        mListeners.add(listener);
    }

    public void removeClickListener(MenuItemClickListener listener) {
        mListeners.remove(listener);
    }

    public void notifyClickListeners() {
        for (MenuItemClickListener listener : mListeners) {
            listener.onClick();
        }
    }

    public void setHighlight(Texture texture) {
        mHighlight = texture;
    }

    protected void setSelected(boolean selected) {
        if (selected == mSelected)
            return;
        mSelected = selected;
        invalidate();
    }

    @Override
    protected void render(GLCanvas canvas) {
        if (mSelected) {
            int width = getWidth();
            int height = getHeight();
            if (mHighlight instanceof NinePatchTexture) {
                Rect p = ((NinePatchTexture) mHighlight).getPaddings();
                mHighlight.draw(canvas, -p.left, -p.top, width + p.left
                        + p.right, height + p.top + p.bottom);
            } else {
                mHighlight.draw(canvas, 0, 0, width, height);
            }
        }
        super.render(canvas);
    }
}
