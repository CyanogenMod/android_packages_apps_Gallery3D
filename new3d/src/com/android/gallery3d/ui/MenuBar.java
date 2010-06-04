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

import android.graphics.Rect;
import android.view.View.MeasureSpec;

import javax.microedition.khronos.opengles.GL11;

class MenuBar extends GLView {

    private static final int BORDER_SIZE = 1; // 1 pixel on all devices
    private static final int BORDER_COLOR = 0x33FFFFFF;

    private NinePatchTexture mBackground;

    public MenuBar() {
    }

    public void setBackground(NinePatchTexture background) {
        if (mBackground == background) return;
        mBackground = background;
        if (background != null) {
            setPaddings(background.getPaddings());
        } else {
            setPaddings(0, 0, 0, 0);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = 0;
        int height = 0;
        int n = getComponentCount();
        for (int i = 0; i < n; ++i) {
            GLView component = getComponent(i);
            component.measure(
                    MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            height = Math.max(height, component.getMeasuredHeight());
            width += component.getMeasuredWidth();
        }
        width += (n - 1) * BORDER_SIZE;
        height += 2 * BORDER_SIZE;
        new MeasureHelper(this)
                .setPreferredContentSize(width, height)
                .measure(widthSpec, heightSpec);
    }

    @Override
    protected void onLayout(
            boolean changed, int left, int top, int right, int bottom) {
        Rect p = mPaddings;

        int remainings = right - left - p.left - p.right;
        int n = getComponentCount();
        for (int i = 0; i < n; ++i) {
            GLView component = getComponent(i);
            component.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            remainings -= component.getMeasuredWidth();
        }
        remainings -= (n - 1) * BORDER_SIZE;

        int layoutLeft = p.left;
        int layoutTop = p.top + BORDER_SIZE;
        int layoutBottom = bottom - top - p.bottom - BORDER_SIZE;
        for (int i = 0; i < n; ++i) {
            GLView component = getComponent(i);
            int space = remainings / (n - i);
            remainings -= space;
            int width = component.getMeasuredWidth() + space;
            int layoutRight = layoutLeft + width;
            component.layout(layoutLeft, layoutTop, layoutRight, layoutBottom);
            layoutLeft = layoutRight + BORDER_SIZE;
        }
    }

    @Override
    protected void renderBackground(GLRootView root, GL11 gl) {
        int width = getWidth();
        int height = getHeight();
        if (mBackground != null) {
            mBackground.draw(root, 0, 0, width, height);
        }
        Rect p = mPaddings;

        width -= p.left + p.right;
        height -= p.top + p.bottom;

        int top = p.top;
        int bottom = top + height;
        int left = p.left;
        int right = left + width;

        root.setColor(BORDER_COLOR);
        root.drawLine(left, top, right, top);
        root.drawLine(left, bottom - 1, right, bottom -1);
        for (int i = 0, n = getComponentCount() - 1; i < n; ++i) {
            Rect bounds = getComponent(i).mBounds;
            root.drawLine(bounds.right, top, bounds.right, bottom);
        }
    }
}
