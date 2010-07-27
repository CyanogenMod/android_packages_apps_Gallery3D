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
import android.graphics.Color;
import android.graphics.Rect;

import com.android.gallery3d.util.Utils;

public class IconLabel extends GLView {

    public static final int NULL_ID = 0;

    private static final int FONT_COLOR = Color.WHITE;
    private static final float FONT_SIZE = 18;

    private static final int MINIMAL_HEIGHT = 32;

    private static final int NO_ICON_LEADING_SPACE = 10;
    private static final int TEXT_LEFT_PADDING = 6;
    private static final int TEXT_RIGHT_PADDING = 10;

    private static final float ENABLED_ALPHA = 1f;
    private static final float DISABLED_ALPHA = 0.3f;

    private static final int HORIZONTAL_PADDINGS = 4;
    private static final int VERTICAL_PADDINGS = 2;

    private static int sNoIconLeadingSpace;
    private static int sTextLeftPadding;
    private static int sTextRightPadding;
    private static int sMinimalHeight;
    private static float sFontSize;
    private static int sHorizontalPaddings = -1;
    private static int sVerticalPaddings;

    private final BasicTexture mIcon;
    private final StringTexture mText;
    private boolean mEnabled = true;

    private static void initializeStaticVariables(Context context) {
        if (sHorizontalPaddings >= 0) return;

        sNoIconLeadingSpace = Utils.dpToPixel(context, NO_ICON_LEADING_SPACE);
        sTextLeftPadding = Utils.dpToPixel(context, TEXT_LEFT_PADDING);
        sTextRightPadding = Utils.dpToPixel(context, TEXT_RIGHT_PADDING);
        sMinimalHeight = Utils.dpToPixel(context, MINIMAL_HEIGHT);
        sHorizontalPaddings = Utils.dpToPixel(context, HORIZONTAL_PADDINGS);
        sVerticalPaddings = Utils.dpToPixel(context, VERTICAL_PADDINGS);

        sFontSize = Utils.dpToPixel(context, FONT_SIZE);
    }

    public IconLabel(Context context, int iconId, int stringId) {
        this(context, iconId == NULL_ID
                ? null : new ResourceTexture(context, iconId),
                context.getString(stringId));
        initializeStaticVariables(context);
    }

    public IconLabel(Context context, BasicTexture icon, String label) {
        initializeStaticVariables(context);
        mIcon = icon;
        mText = StringTexture.newInstance(label, sFontSize, FONT_COLOR);
        setPaddings(sHorizontalPaddings,
                sVerticalPaddings, sHorizontalPaddings, sVerticalPaddings);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = mIcon == null ? sNoIconLeadingSpace : mIcon.getWidth();
        width += mText.getWidth();
        width += sTextRightPadding + sTextLeftPadding;
        int height = Math.max(
                mText.getHeight(), mIcon == null ? 0 : mIcon.getHeight());
        height = Math.max(sMinimalHeight, height);
        MeasureHelper.getInstance(this)
                .setPreferredContentSize(width, height)
                .measure(widthSpec, heightSpec);
    }

    @Override
    protected void render(GLCanvas canvas) {
        /*
         * The layout: |--padding--|--mIcon--|--sTextLeftPadding--|--mText--|
         *     --mTextRightPadding--|--padding--|
         */
        Rect p = mPaddings;

        int width = getWidth() - p.left - p.right;
        int height = getHeight() - p.top - p.bottom;

        int xoffset = p.left;

        float oldAlpha = canvas.getAlpha();
        canvas.multiplyAlpha(mEnabled ? ENABLED_ALPHA : DISABLED_ALPHA);

        BasicTexture icon = mIcon;
        if (icon != null) {
            icon.draw(canvas, xoffset,
                    p.top + (height - icon.getHeight()) / 2);
            xoffset += icon.getWidth();
        } else {
            xoffset += sNoIconLeadingSpace;
        }

        StringTexture title = mText;
        xoffset += sTextLeftPadding;
        int yoffset = p.top + (height - title.getHeight()) / 2;
        //TODO: cut the text if it is too long
        title.draw(canvas, xoffset, yoffset);

        canvas.setAlpha(oldAlpha);
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled == enabled) return;
        mEnabled = enabled;
        invalidate();
    }
}
