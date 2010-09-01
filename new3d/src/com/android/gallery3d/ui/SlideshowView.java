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
import android.graphics.PointF;

import com.android.gallery3d.anim.CanvasAnimation;
import com.android.gallery3d.anim.FloatAnimation;

import java.util.Random;

import javax.microedition.khronos.opengles.GL11;

public class SlideshowView extends GLView {
    private static final String TAG = "SlideshowView";

    private static final int SLIDESHOW_DURATION = 3500;
    private static final int TRANSITION_DURATION = 1000;

    private static final float SCALE_SPEED = 0.20f ;
    private static final float MOVE_SPEED = SCALE_SPEED;

    private BitmapTexture mCurrentTexture;
    private SlideshowAnimation mCurrentAnimation;

    private BitmapTexture mPrevTexture;
    private SlideshowAnimation mPrevAnimation;

    private final FloatAnimation mTransitionAnimation =
            new FloatAnimation(0, 1, TRANSITION_DURATION);

    private Random mRandom = new Random();

    public static interface SlideshowListener {
        public void onSlideshowEnd();
    }

    public SlideshowView() {
    }

    public void next(Bitmap bitmap) {

        mTransitionAnimation.start();

        if (mPrevTexture != null) {
            mPrevTexture.getBitmap().recycle();
            mPrevTexture.recycle();
        }

        mPrevTexture = mCurrentTexture;
        mPrevAnimation = mCurrentAnimation;

        mCurrentTexture = new BitmapTexture(bitmap);
        mCurrentAnimation = new SlideshowAnimation(mCurrentTexture, mRandom);
        mCurrentAnimation.start();

        invalidate();
    }

    public void close() {
        if (mPrevTexture != null) {
            mPrevTexture.getBitmap().recycle();
            mPrevTexture.recycle();
            mPrevTexture = null;
        }
        if (mCurrentTexture != null) {
            mCurrentTexture.getBitmap().recycle();
            mCurrentTexture.recycle();
            mCurrentTexture = null;
        }
    }

    @Override
    protected void render(GLCanvas canvas) {
        long currentTimeMillis = canvas.currentAnimationTimeMillis();
        boolean requestRender = mTransitionAnimation.calculate(currentTimeMillis);
        GL11 gl = canvas.getGLInstance();
        gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        float alpha = mPrevTexture == null ? 1f : mTransitionAnimation.get();

        if (mPrevTexture != null && alpha != 1f) {
            requestRender |= mPrevAnimation.calculate(currentTimeMillis);
            canvas.save(GLCanvas.SAVE_FLAG_ALPHA
                    | mPrevAnimation.getCanvasSaveFlags());
            canvas.setAlpha(1f - alpha);
            mPrevAnimation.apply(canvas);
            mPrevTexture.draw(canvas, -mPrevTexture.getWidth() / 2,
                    -mPrevTexture.getHeight() / 2);
            canvas.restore();
        }
        if (mCurrentTexture != null) {
            requestRender |= mCurrentAnimation.calculate(currentTimeMillis);
            canvas.save(GLCanvas.SAVE_FLAG_ALPHA
                    | mCurrentAnimation.getCanvasSaveFlags());
            canvas.setAlpha(alpha);
            mCurrentAnimation.apply(canvas);
            mCurrentTexture.draw(canvas, -mCurrentTexture.getWidth() / 2,
                    -mCurrentTexture.getHeight() / 2);
            canvas.restore();
        }
        if (requestRender) invalidate();
        gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private class SlideshowAnimation extends CanvasAnimation {
        private final int mWidth;
        private final int mHeight;

        private final PointF mMovingVector;
        private float mProgress;

        public SlideshowAnimation(Texture texture, Random random) {
            mWidth = texture.getWidth();
            mHeight = texture.getHeight();
            mMovingVector = new PointF(
                    MOVE_SPEED * mWidth * (random.nextFloat() - 0.5f),
                    MOVE_SPEED * mHeight * (random.nextFloat() - 0.5f));
            setDuration(SLIDESHOW_DURATION);
        }

        @Override
        public void apply(GLCanvas canvas) {
            int viewWidth = getWidth();
            int viewHeight = getHeight();

            float initScale = Math.min(2f, Math.min((float)
                    viewWidth / mWidth, (float) viewHeight / mHeight));
            float scale = initScale * (1 + SCALE_SPEED * mProgress);

            float centerX = viewWidth / 2 + mMovingVector.x * mProgress;
            float centerY = viewHeight / 2 + mMovingVector.y * mProgress;

            canvas.translate(centerX, centerY, 0);
            canvas.scale(scale, scale, 0);
        }

        @Override
        public int getCanvasSaveFlags() {
            return GLCanvas.SAVE_FLAG_MATRIX;
        }

        @Override
        protected void onCalculate(float progress) {
            mProgress = progress;
        }
    }
}
