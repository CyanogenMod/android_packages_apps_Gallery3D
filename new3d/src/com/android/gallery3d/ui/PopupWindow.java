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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;

import javax.microedition.khronos.opengles.GL11;

class PopupWindow extends GLView {
    private static final String TAG = "PopupWindow";

    protected BasicTexture mAnchor;
    protected int mAnchorOffset;

    protected int mAnchorPosition;
    private RawTexture mBackupTexture;
    private GLView mContent;

    protected Texture mBackground;
    private boolean mUsingStencil;

    public PopupWindow() {
    }

    @Override
    protected void onAttachToRoot(GLRootView root) {
        super.onAttachToRoot(root);
        mUsingStencil = root.getEGLConfigChooser().getStencilBits() > 0;
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

    private void renderBackgroundWithStencil(GLRootView root, GL11 gl) {
        int width = getWidth();
        int height = getHeight();
        int aWidth = mAnchor.getWidth();
        int aHeight = mAnchor.getHeight();

        Rect p = mPaddings;

        int aXoffset = Util.clamp(mAnchorPosition - aWidth / 2,
                p.left, width - p.right - aWidth);
        int aYoffset = height - aHeight;

        if (mAnchor != null) {
            gl.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            gl.glStencilFunc(GL11.GL_ALWAYS, 1, 1);
            mAnchor.draw(root, aXoffset, aYoffset);
            gl.glStencilFunc(GL11.GL_NOTEQUAL, 1, 1);
            gl.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        }

        if (mBackground != null) {
            mBackground.draw(root, 0, 0,
                    width, height - aHeight + mAnchorOffset);
        }
    }

    private void renderBackgroundWithoutStencil(GLRootView root, GL11 gl) {
        int width = getWidth();
        int height = getHeight();
        int aWidth = mAnchor.getWidth();
        int aHeight = mAnchor.getHeight();

        Rect p = mPaddings;

        int aXoffset = Util.clamp(mAnchorPosition - aWidth / 2,
                p.left, width - p.right - aWidth);
        int aYoffset = height - aHeight;

        if (mAnchor != null) {
            mAnchor.draw(root, aXoffset, aYoffset);
        }

        if (mBackupTexture == null || mBackupTexture.getBoundGL() != gl) {
            mBackupTexture = RawTexture.newInstance(gl);
        }

        RawTexture backup = mBackupTexture;
        try {
            // Copy the current drawing results of the triangle area into
            // "backup", so that we can restore the content after it is
            // overlaid by the background.
            root.copyTexture2D(backup, aXoffset, aYoffset, aWidth, aHeight);
        } catch (GLOutOfMemoryException e) {
            Log.e(TAG, "out of memory", e);
        }

        if (mBackground != null) {
            mBackground.draw(root, 0, 0,
                    width, height - aHeight + mAnchorOffset);
        }

        gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO);
        backup.drawBack(root, aXoffset, aYoffset, aWidth, aHeight);
        gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    protected void renderBackground(GLRootView root, GL11 gl) {
        if (mUsingStencil) {
            renderBackgroundWithStencil(root, gl);
        } else {
            renderBackgroundWithoutStencil(root, gl);
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
    public void clearComponents() {
        throw new UnsupportedOperationException();
    }

    public void popup() {
        setVisibility(GLView.VISIBLE);

        AnimationSet set = new AnimationSet(false);
        Animation scale = new ScaleAnimation(
                0.7f, 1f, 0.7f, 1f, mAnchorPosition, getHeight());
        Animation alpha = new AlphaAnimation(0.5f, 1.0f);

        set.addAnimation(scale);
        set.addAnimation(alpha);
        scale.setDuration(150);
        alpha.setDuration(100);
        scale.setInterpolator(new OvershootInterpolator());
        startAnimation(set);
    }

    public void popoff() {
        setVisibility(GLView.INVISIBLE);
        Animation alpha = new AlphaAnimation(0.7f, 0.0f);
        alpha.setDuration(100);
        startAnimation(alpha);
    }
}
