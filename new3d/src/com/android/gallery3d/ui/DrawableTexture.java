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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

// DrawableTexture is a texture whose content is from a Drawable.
// The width and height of a DrawableTexture is its intrinsic width and height.
public class DrawableTexture extends CanvasTexture {

    private final Drawable mDrawable;

    public DrawableTexture(Drawable drawable) {
        super(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        mDrawable = drawable;
    }

    @Override
    protected void onDraw(Canvas canvas, Bitmap backing) {
        mDrawable.setBounds(0, 0, mWidth, mHeight);
        mDrawable.draw(canvas);
    }
}
