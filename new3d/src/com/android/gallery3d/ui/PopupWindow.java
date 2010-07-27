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
import android.util.Log;
import android.view.View.MeasureSpec;
import android.view.animation.OvershootInterpolator;

import com.android.gallery3d.anim.AlphaAnimation;
import com.android.gallery3d.anim.AnimationSet;
import com.android.gallery3d.anim.CanvasAnimation;
import com.android.gallery3d.anim.ScaleAnimation;
import com.android.gallery3d.util.Utils;

import javax.microedition.khronos.opengles.GL11;

class PopupWindow extends GLView {
    private static final String TAG = "PopupWindow";

    protected BasicTexture mAnchor;
    protected int mAnchorOffset;

    protected int mAnchorPosition;
    private GLView mContent;

    protected Texture mBackground;
    private boolean mUsingStencil;

    public PopupWindow() {
    }

    @Override
    protected void onAttachToRoot(GLRoot root) {
        super.onAttachToRoot(root);
        mUsingStencil = root.hasStencil();
    }

    public void setBackground(Texture background) {
        if (background == mBackground) return;
        mBackground = background;
        if (background != null && background instanceof NinePatchTexture) {
            setPaddings(((NinePatchTexture) mBackground).getPaddings());
        } else {
            setPaddings(0, 0, 0, 0);
        }
        invalidate();
    }

    public void setAnchor(BasicTexture anchor, int offset) {
        mAnchor = anchor;
        mAnchorOffset = offset;
    }

    @Override
    public void addComponent(GLView component) {
        throw new UnsupportedOperationException("use setContent(GLView)");
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        Rect p = mPaddings;

        int widthMode = MeasureSpec.getMode(widthSpec);
        if (widthMode != MeasureSpec.UNSPECIFIED) {
            int width = MeasureSpec.getSize(widthSpec);
            widthSpec = MeasureSpec.makeMeasureSpec(
                    Math.max(0, width - p.left - p.right), widthMode);
        }

        int heightMode = MeasureSpec.getMode(heightSpec);
        if (heightMode != MeasureSpec.UNSPECIFIED) {
            int height = MeasureSpec.getSize(heightSpec);
            heightSpec = MeasureSpec.makeMeasureSpec(
                    Math.max(0, height - p.top - p.bottom
                    - mAnchor.getHeight() + mAnchorOffset), heightMode);
        }

        GLView child = mContent;
        child.measure(widthSpec, heightSpec);

        setMeasuredSize(child.getMeasuredWidth() + p.left + p.right,
                child.getMeasuredHeight() + p.top + p.bottom
                + mAnchor.getHeight() - mAnchorOffset);
    }

    @Override
    protected void onLayout(
            boolean change, int left, int top, int right, int bottom) {
        Rect p = getPaddings();
        GLView view = mContent;
        view.layout(p.left, p.top, getWidth() - p.right,
                getHeight() - p.bottom - mAnchor.getHeight() + mAnchorOffset);
    }

    public void setAnchorPosition(int xoffset) {
        mAnchorPosition = xoffset;
    }

    private void renderBackgroundWithStencil(GLCanvas canvas) {
        int width = getWidth();
        int height = getHeight();
        int aWidth = mAnchor.getWidth();
        int aHeight = mAnchor.getHeight();

        Rect p = mPaddings;

        int aXoffset = Utils.clamp(mAnchorPosition - aWidth / 2,
                p.left, width - p.right - aWidth);
        int aYoffset = height - aHeight;
        GL11 gl = canvas.getGLInstance();
        if (mAnchor != null) {
            gl.glEnable(GL11.GL_STENCIL_TEST);
            gl.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            gl.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            gl.glStencilFunc(GL11.GL_ALWAYS, 1, 1);
            mAnchor.draw(canvas, aXoffset, aYoffset);
            gl.glStencilFunc(GL11.GL_NOTEQUAL, 1, 1);
            gl.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        }

        if (mBackground != null) {
            mBackground.draw(canvas, 0, 0,
                    width, height - aHeight + mAnchorOffset);
        }

        if (mAnchor != null) {
            gl.glDisable(GL11.GL_STENCIL_TEST);
        }
    }

    private void renderBackgroundWithoutStencil(GLCanvas canvas) {
        int width = getWidth();
        int height = getHeight();
        int aWidth = mAnchor.getWidth();
        int aHeight = mAnchor.getHeight();

        Rect p = mPaddings;

        int aXoffset = Utils.clamp(mAnchorPosition - aWidth / 2,
                p.left, width - p.right - aWidth);
        int aYoffset = height - aHeight;

        if (mAnchor != null) {
            mAnchor.draw(canvas, aXoffset, aYoffset);
        }

        BasicTexture backup = null;

        // Copy the current drawing results of the triangle area into
        // "backup", so that we can restore the content after it is
        // overlaid by the background.
        backup = canvas.copyTexture(aXoffset, aYoffset, aWidth, aHeight);

        if (mBackground != null) {
            mBackground.draw(canvas, 0, 0,
                    width, height - aHeight + mAnchorOffset);
        }

        // restore the backup with alpha = 1
        canvas.drawTexture(backup, aXoffset, aYoffset, aWidth, aHeight, 1f);
        backup.recycle();
    }

    @Override
    protected void renderBackground(GLCanvas root) {
        if (mUsingStencil) {
            renderBackgroundWithStencil(root);
        } else {
            renderBackgroundWithoutStencil(root);
        }
    }

    public void setContent(GLView content) {
        if (mContent != null) {
            super.removeComponent(mContent);
        }
        mContent = content;
        super.addComponent(content);
    }

    @Override
    public void removeAllComponents() {
        throw new UnsupportedOperationException();
    }

    public void popup() {
        setVisibility(GLView.VISIBLE);

        AnimationSet set = new AnimationSet();
        CanvasAnimation scale = new ScaleAnimation(
                0.7f, 1f, 0.7f, 1f, mAnchorPosition, getHeight());
        CanvasAnimation alpha = new AlphaAnimation(0.5f, 1.0f);
        set.addAnimation(scale);
        set.addAnimation(alpha);
        scale.setDuration(150);
        alpha.setDuration(100);
        scale.setInterpolator(new OvershootInterpolator());
        startAnimation(set);
    }

    public void popoff() {
        setVisibility(GLView.INVISIBLE);
        AlphaAnimation alpha = new AlphaAnimation(0.7f, 0.0f);
        alpha.setDuration(100);
        startAnimation(alpha);
    }
}
