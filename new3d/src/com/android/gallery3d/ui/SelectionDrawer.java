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

import com.android.gallery3d.R;

/**
 * Drawer class responsible for drawing selectable frame.
 */
public class SelectionDrawer {
    private final NinePatchTexture mFrame;
    private final ResourceTexture mCheckedItem;
    private final ResourceTexture mUnCheckedItem;
    private boolean mSelectionMode;

    public SelectionDrawer(Context context) {
        mFrame = new NinePatchTexture(context, R.drawable.grid_frame);
        mCheckedItem = new ResourceTexture(context, R.drawable.grid_check_on);
        mUnCheckedItem = new ResourceTexture(context, R.drawable.grid_check_off);
    }

    public void setSelectionMode(boolean selectionMode) {
        mSelectionMode = selectionMode;
    }

    public Rect getFramePadding() {
        return mFrame.getPaddings();
    }

    public void draw(GLCanvas canvas, Texture content, int width, int height,
            boolean checked, boolean drawCheckedBox) {
        int x = -width / 2;
        int y = -height / 2;
        Rect p = mFrame.getPaddings();
        content.draw(canvas, x + p.left, y + p.top,
                width - p.left - p.right, height - p.top - p.bottom);
        mFrame.draw(canvas, x, y, width, height);

        if (drawCheckedBox && mSelectionMode) {
            int w = mCheckedItem.getWidth() / 2;
            int h = mCheckedItem.getHeight() / 2;
            x = width / 2 - w - p.left;
            y = height / 2 - h - p.left;
            if (checked)
                mCheckedItem.draw(canvas, x, y, w, h);
            else
                mUnCheckedItem.draw(canvas, x, y, w, h);
        }
    }

    public void draw(GLCanvas canvas, Texture content, int width, int height, boolean checked) {
        draw(canvas, content, width, height, checked, true);
    }
}
