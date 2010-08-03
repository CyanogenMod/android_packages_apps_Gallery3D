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
import android.os.SystemClock;
import android.view.MotionEvent;

import com.android.gallery3d.anim.CanvasAnimation;

import java.util.ArrayList;

public class GLView {
    private static final String TAG = "GLView";

    public static final int VISIBLE = 0;
    public static final int INVISIBLE = 1;

    private static final int FLAG_INVISIBLE = 1;
    private static final int FLAG_SET_MEASURED_SIZE = 2;
    private static final int FLAG_LAYOUT_REQUESTED = 4;

    protected final Rect mBounds = new Rect();
    protected final Rect mPaddings = new Rect();

    private GLRoot mRoot;
    protected GLView mParent;
    private ArrayList<GLView> mComponents;
    private GLView mMotionTarget;

    private CanvasAnimation mAnimation;

    private int mViewFlags = 0;

    protected int mMeasuredWidth = 0;
    protected int mMeasuredHeight = 0;

    private int mLastWidthSpec = -1;
    private int mLastHeightSpec = -1;

    protected int mScrollY = 0;
    protected int mScrollX = 0;
    protected int mScrollHeight = 0;
    protected int mScrollWidth = 0;

    public void startAnimation(CanvasAnimation animation) {
        GLRoot root = getGLRoot();
        if (root == null) throw new IllegalStateException();

        mAnimation = animation;
        mAnimation.start();
        root.registerLaunchedAnimation(mAnimation);
        invalidate();
    }

    public void setVisibility(int visibility) {
        if (visibility == getVisibility()) return;
        if (visibility == VISIBLE) {
            mViewFlags &= ~FLAG_INVISIBLE;
        } else {
            mViewFlags |= FLAG_INVISIBLE;
        }
        onVisibilityChanged(visibility);
        invalidate();
    }

    public int getVisibility() {
        return (mViewFlags & FLAG_INVISIBLE) == 0 ? VISIBLE : INVISIBLE;
    }

    private boolean setBounds(int left, int top, int right, int bottom) {
        boolean sizeChanged = (right - left) != (mBounds.right - mBounds.left)
                || (bottom - top) != (mBounds.bottom - mBounds.top);
        mBounds.set(left, top, right, bottom);
        return sizeChanged;
    }

    // This should be called on the content pane (the topmost GLView).
    public void attachToRoot(GLRoot root) {
        Util.Assert(mParent == null && mRoot == null);
        onAttachToRoot(root);
    }

    // This should be called on the content pane (the topmost GLView).
    public void detachFromRoot() {
        Util.Assert(mParent == null && mRoot != null);
        onDetachFromRoot();
    }

    public int getComponentCount() {
        return mComponents == null ? 0 : mComponents.size();
    }

    public GLView getComponent(int index) {
        if (mComponents == null) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return mComponents.get(index);
    }

    public void addComponent(GLView component) {
        // Make sure the component doesn't have a parent currently.
        if (component.mParent != null) throw new IllegalStateException();

        // Build parent-child links
        if (mComponents == null) {
            mComponents = new ArrayList<GLView>();
        }
        mComponents.add(component);
        component.mParent = this;

        // If this is added after we have a root, tell the component.
        if (mRoot != null) {
            component.onAttachToRoot(mRoot);
        }
    }

    public boolean removeComponent(GLView component) {
        if (mComponents == null) return false;
        if (mComponents.remove(component)) {
            removeOneComponent(component);
            return true;
        }
        return false;
    }

    public void removeAllComponents() {
        for (int i = 0, n = mComponents.size(); i < n; ++i) {
            removeOneComponent(mComponents.get(i));
        }
        mComponents.clear();
    }

    private void removeOneComponent(GLView component) {
        if (mMotionTarget == component) {
            long now = SystemClock.uptimeMillis();
            dispatchTouchEvent(MotionEvent.obtain(
                    now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0));
            mMotionTarget = null;
        }
        component.onDetachFromRoot();
        component.mParent = null;
    }

    public Rect bounds() {
        return mBounds;
    }

    public int getWidth() {
        return mBounds.right - mBounds.left;
    }

    public int getHeight() {
        return mBounds.bottom - mBounds.top;
    }

    public GLRoot getGLRoot() {
        return mRoot;
    }

    public void invalidate() {
        GLRoot root = getGLRoot();
        if (root != null) root.requestRender();
    }

    public void requestLayout() {
        mViewFlags |= FLAG_LAYOUT_REQUESTED;
        if (mParent != null) {
            mParent.requestLayout();
        } else {
            // Is this a content pane ?
            GLRoot root = getGLRoot();
            if (root != null) root.requestLayoutContentPane();
        }
    }

    protected void render(GLCanvas canvas) {
        renderBackground(canvas);
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            renderChild(canvas, getComponent(i));
        }
    }

    protected void renderBackground(GLCanvas view) {
    }

    protected void renderChild(GLCanvas canvas, GLView component) {
        if (component.getVisibility() != GLView.VISIBLE
                && component.mAnimation == null) return;

        int xoffset = component.mBounds.left - mScrollX;
        int yoffset = component.mBounds.top - mScrollY;

        canvas.translate(xoffset, yoffset, 0);

        CanvasAnimation anim = component.mAnimation;
        if (anim != null) {
            canvas.save(anim.getCanvasSaveFlags());
            if (anim.calculate(canvas.currentAnimationTimeMillis())) {
                invalidate();
            } else {
                component.mAnimation = null;
            }
            anim.apply(canvas);
        }
        component.render(canvas);
        if (anim != null) canvas.restore();
        canvas.translate(-xoffset, -yoffset, 0);
    }

    protected boolean onTouch(MotionEvent event) {
        return false;
    }

    protected boolean dispatchTouchEvent(MotionEvent event,
            int x, int y, GLView component, boolean checkBounds) {
        Rect rect = component.mBounds;
        int left = rect.left;
        int top = rect.top;
        if (!checkBounds || rect.contains(x, y)) {
            event.offsetLocation(-left, -top);
            if (component.dispatchTouchEvent(event)) {
                event.offsetLocation(left, top);
                return true;
            }
            event.offsetLocation(left, top);
        }
        return false;
    }

    protected boolean dispatchTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        int action = event.getAction();
        if (mMotionTarget != null) {
            if (action == MotionEvent.ACTION_DOWN) {
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                mMotionTarget = null;
            } else {
                dispatchTouchEvent(event, x, y, mMotionTarget, false);
                if (action == MotionEvent.ACTION_CANCEL
                        || action == MotionEvent.ACTION_UP) {
                    mMotionTarget = null;
                }
                return true;
            }
        }
        if (action == MotionEvent.ACTION_DOWN) {
            // in the reverse rendering order
            for (int i = getComponentCount() - 1; i >= 0; --i) {
                GLView component = getComponent(i);
                if (component.getVisibility() != GLView.VISIBLE) continue;
                if (dispatchTouchEvent(event, x, y, component, true)) {
                    mMotionTarget = component;
                    return true;
                }
            }
        }
        return onTouch(event);
    }

    public Rect getPaddings() {
        return mPaddings;
    }

    public void setPaddings(Rect paddings) {
        mPaddings.set(paddings);
    }

    public void setPaddings(int left, int top, int right, int bottom) {
        mPaddings.set(left, top, right, bottom);
    }

    public void layout(int left, int top, int right, int bottom) {
        boolean sizeChanged = setBounds(left, top, right, bottom);
        if (sizeChanged) {
            mViewFlags &= ~FLAG_LAYOUT_REQUESTED;
            onLayout(true, left, top, right, bottom);
        } else if ((mViewFlags & FLAG_LAYOUT_REQUESTED)!= 0) {
            mViewFlags &= ~FLAG_LAYOUT_REQUESTED;
            onLayout(false, left, top, right, bottom);
        }
    }

    public void measure(int widthSpec, int heightSpec) {
        if (widthSpec == mLastWidthSpec && heightSpec == mLastHeightSpec
                && (mViewFlags & FLAG_LAYOUT_REQUESTED) == 0) {
            return;
        }

        mLastWidthSpec = widthSpec;
        mLastHeightSpec = heightSpec;

        mViewFlags &= ~FLAG_SET_MEASURED_SIZE;
        onMeasure(widthSpec, heightSpec);
        if ((mViewFlags & FLAG_SET_MEASURED_SIZE) == 0) {
            throw new IllegalStateException(getClass().getName()
                    + " should call setMeasuredSize() in onMeasure()");
        }
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
    }

    protected void setMeasuredSize(int width, int height) {
        mViewFlags |= FLAG_SET_MEASURED_SIZE;
        mMeasuredWidth = width;
        mMeasuredHeight = height;
    }

    public int getMeasuredWidth() {
        return mMeasuredWidth;
    }

    public int getMeasuredHeight() {
        return mMeasuredHeight;
    }

    protected void onLayout(
            boolean changeSize, int left, int top, int right, int bottom) {
    }

    /**
     * Gets the bounds of the given descendant that relative to this view.
     */
    public boolean getBoundsOf(GLView descendant, Rect out) {
        int xoffset = 0;
        int yoffset = 0;
        GLView view = descendant;
        while (view != this) {
            if (view == null) return false;
            Rect bounds = view.mBounds;
            xoffset += bounds.left;
            yoffset += bounds.top;
            view = view.mParent;
        }
        out.set(xoffset, yoffset, xoffset + descendant.getWidth(),
                yoffset + descendant.getHeight());
        return true;
    }

    protected void onVisibilityChanged(int visibility) {
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            GLView child = getComponent(i);
            if (child.getVisibility() == GLView.VISIBLE) {
                child.onVisibilityChanged(visibility);
            }
        }
    }

    protected void onAttachToRoot(GLRoot root) {
        mRoot = root;
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            getComponent(i).onAttachToRoot(root);
        }
    }

    protected void onDetachFromRoot() {
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            getComponent(i).onDetachFromRoot();
        }
        mRoot = null;
    }

    protected void lockRendering() {
        if (mRoot != null) {
            mRoot.lockRenderThread();
        }
    }

    protected void unlockRendering() {
        if (mRoot != null) {
            mRoot.unlockRenderThread();
        }
    }

    // This is for debugging only.
    // Dump the view hierarchy into log.
    void dumpTree(String prefix) {
        Log.v(TAG, prefix + getClass().getSimpleName());
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            getComponent(i).dumpTree(prefix + "....");
        }
    }
}
