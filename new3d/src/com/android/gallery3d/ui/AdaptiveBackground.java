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
import android.graphics.LightingColorFilter;
import android.graphics.Paint;

import com.android.gallery3d.anim.FloatAnimation;

public class AdaptiveBackground extends GLView {

    private static final int BACKGROUND_WIDTH = 128;
    private static final int BACKGROUND_HEIGHT = 64;
    private static final int FILTERED_COLOR = 0xffaaaaaa;
    private static final int ANIMATION_DURATION = 500;

    private BasicTexture mOldBackground;
    private BasicTexture mBackground;

    private final Paint mPaint;
    private Bitmap mPendingBitmap;
    private final FloatAnimation mAnimation =
            new FloatAnimation(0, 1, ANIMATION_DURATION);

    public AdaptiveBackground() {
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setColorFilter(new LightingColorFilter(FILTERED_COLOR, 0));
        mPaint = paint;
    }

    public Bitmap getAdaptiveBitmap(Bitmap bitmap) {
        Bitmap target = Bitmap.createBitmap(
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int left = 0;
        int top = 0;
        if (width * BACKGROUND_HEIGHT > height * BACKGROUND_WIDTH) {
            float scale = (float) BACKGROUND_HEIGHT / height;
            canvas.scale(scale, scale);
            left = (BACKGROUND_WIDTH - (int) (width * scale + 0.5)) / 2;
        } else {
            float scale = (float) BACKGROUND_WIDTH / width;
            canvas.scale(scale, scale);
            top = (BACKGROUND_HEIGHT - (int) (height * scale + 0.5)) / 2;
        }
        canvas.drawBitmap(bitmap, left, top, mPaint);
        BoxBlurFilter.apply(target,
                BoxBlurFilter.MODE_REPEAT, BoxBlurFilter.MODE_CLAMP);
        return target;
    }

    private void startTransition(Bitmap bitmap) {
        BitmapTexture texture = new BitmapTexture(bitmap);
        if (mBackground == null) {
            mBackground = texture;
        } else {
            if (mOldBackground != null) mOldBackground.recycle();
            mOldBackground = mBackground;
            mBackground = texture;
            mAnimation.start();
        }
        invalidate();
    }

    public void setImage(Bitmap bitmap) {
        if (mAnimation.isActive()) {
            mPendingBitmap = bitmap;
        } else {
            startTransition(bitmap);
        }
    }

    public void setScrollPosition(int position) {
        if (mScrollX == position) return;
        mScrollX = position;
        invalidate();
    }

    @Override
    protected void render(GLCanvas canvas) {
        if (mBackground == null) return;

        int height = getHeight();
        float scale = (float) height / BACKGROUND_HEIGHT;
        int width = (int) (BACKGROUND_WIDTH * scale + 0.5f);
        int scroll = mScrollX;
        int start = (scroll / width) * width;

        if (mOldBackground == null) {
            for (int i = start, n = scroll + getWidth(); i < n; i += width) {
                mBackground.draw(canvas, i - scroll, 0, width, height);
            }
        } else {
            boolean moreAnimation =
                    mAnimation.calculate(canvas.currentAnimationTimeMillis());
            float ratio = mAnimation.get();
            for (int i = start, n = scroll + getWidth(); i < n; i += width) {
                canvas.drawMixed(mOldBackground,
                        mBackground, ratio, i - scroll, 0, width, height);
            }
            if (moreAnimation) {
                invalidate();
            } else if (mPendingBitmap != null) {
                startTransition(mPendingBitmap);
                mPendingBitmap = null;
            }
        }
    }
}
