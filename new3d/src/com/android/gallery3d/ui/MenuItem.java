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

public class MenuItem extends IconLabel {
    private static final int ACTION_NONE = 0;
    private boolean mSelected;
    private Texture mHighlight;
    private int mItemId;

    public MenuItem(Context context, int icon, int label, Texture highlight) {
        this(context, icon, label, highlight, ACTION_NONE);
    }

    public MenuItem(Context context, int icon, int label, Texture highlight, int itemId) {
        super(context, icon, label);
        mHighlight = highlight;
        mItemId = itemId;
    }

    public MenuItem(Context context, BasicTexture texture, String label) {
        super(context, texture, label);
    }

    public int getItemId() {
        return mItemId;
    }

    public void setHighlight(Texture texture) {
        mHighlight = texture;
    }

    protected void setSelected(boolean selected) {
        if (selected == mSelected) return;
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
                mHighlight.draw(canvas, -p.left, -p.top,
                        width + p.left + p.right, height + p.top + p.bottom);
            } else {
                mHighlight.draw(canvas, 0, 0, width, height);
            }
        }
        super.render(canvas);
    }
}
