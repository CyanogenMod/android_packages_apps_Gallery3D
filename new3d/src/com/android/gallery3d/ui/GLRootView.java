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

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.os.Process;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

import com.android.gallery3d.anim.CanvasAnimation;
import com.android.gallery3d.util.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

// The root component of all <code>GLView</code>s. The rendering is done in GL
// thread while the event handling is done in the main thread.  To synchronize
// the two threads, the entry points of this package need to synchronize on the
// <code>GLRootView</code> instance unless it can be proved that the rendering
// thread won't access the same thing as the method. The entry points include:
// (1) The public methods of HeadUpDisplay
// (2) The public methods of CameraHeadUpDisplay
// (3) The overridden methods in GLRootView.
public class GLRootView extends GLSurfaceView
        implements GLSurfaceView.Renderer, GLRoot {
    private static final String TAG = "GLRootView";

    private final boolean DEBUG_FPS = false;
    private int mFrameCount = 0;
    private long mFrameCountingStart = 0;

    private final boolean DEBUG_INVALIDATE = false;
    private int mInvalidateColor = 0;

    private final boolean DEBUG_DRAWING_STAT = false;

    private static final int FLAG_INITIALIZED = 1;
    private static final int FLAG_NEED_LAYOUT = 2;

    private GL11 mGL;
    private GLCanvasImp mCanvas;

    private GLView mContentView;
    private DisplayMetrics mDisplayMetrics;

    private int mFlags = FLAG_NEED_LAYOUT;
    private volatile boolean mRenderRequested = false;

    private Rect mClipRect = new Rect();
    private int mClipRetryCount = 0;

    private final GalleryEGLConfigChooser mEglConfigChooser =
            new GalleryEGLConfigChooser();

    private final ArrayList<CanvasAnimation> mAnimations =
            new ArrayList<CanvasAnimation>();

    private final LinkedList<OnGLIdleListener> mIdleListeners =
            new LinkedList<OnGLIdleListener>();

    private final IdleRunner mIdleRunner = new IdleRunner();

    private ReentrantLock mRenderLock = new ReentrantLock();

    private static final int TARGET_FRAME_TIME = 33;
    private long mLastDrawFinishTime;

    public GLRootView(Context context) {
        this(context, null);
    }

    public GLRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFlags |= FLAG_INITIALIZED;
        setBackgroundDrawable(null);
        setEGLConfigChooser(mEglConfigChooser);
        setRenderer(this);

        // Uncomment this to enable gl error check.
        //setDebugFlags(DEBUG_CHECK_GL_ERROR);
    }

    public GalleryEGLConfigChooser getEGLConfigChooser() {
        return mEglConfigChooser;
    }

    public boolean hasStencil() {
        return getEGLConfigChooser().getStencilBits() > 0;
    }

    public void registerLaunchedAnimation(CanvasAnimation animation) {
        // Register the newly launched animation so that we can set the start
        // time more precisely. (Usually, it takes much longer for first
        // rendering, so we set the animation start time as the time we
        // complete rendering)
        mAnimations.add(animation);
    }

    public void addOnGLIdleListener(OnGLIdleListener listener) {
        mRenderLock.lock();
        try {
            mIdleListeners.addLast(listener);
            if (!mRenderRequested && !mIdleRunner.mActive) {
                mIdleRunner.mActive = true;
                queueEvent(mIdleRunner);
            }
        } finally {
            mRenderLock.unlock();
        }
    }

    public void setContentPane(GLView content) {
        if (mContentView != null) {
            mContentView.detachFromRoot();
            BasicTexture.yieldAllTextures();
        }
        if (content != null) {
            mContentView = content;
            content.attachToRoot(this);
            requestLayoutContentPane();
        }
    }

    public GLView getContentPane() {
        return mContentView;
    }

    @Override
    public void requestRender() {
        if (DEBUG_INVALIDATE) {
            StackTraceElement e = Thread.currentThread().getStackTrace()[4];
            String caller = e.getFileName() + ":" + e.getLineNumber() + " ";
            Log.v(TAG, "invalidate: " + caller);
        }
        if (mRenderRequested) return;
        mRenderRequested = true;
        super.requestRender();
    }

    public void requestLayoutContentPane() {
        mRenderLock.lock();
        try {
            if (mContentView == null || (mFlags & FLAG_NEED_LAYOUT) != 0) return;

            // "View" system will invoke onLayout() for initialization(bug ?), we
            // have to ignore it since the GLThread is not ready yet.
            if ((mFlags & FLAG_INITIALIZED) == 0) return;

            mFlags |= FLAG_NEED_LAYOUT;
            requestRender();
        } finally {
            mRenderLock.unlock();
        }
    }

    private void layoutContentPane() {
        mFlags &= ~FLAG_NEED_LAYOUT;
        int width = getWidth();
        int height = getHeight();
        Log.v(TAG, "layout content pane " + width + "x" + height);
        if (mContentView != null && width != 0 && height != 0) {
            mContentView.layout(0, 0, width, height);
        }
        // Uncomment this to dump the view hierarchy.
        //mContentView.dumpTree("");
    }

    @Override
    protected void onLayout(
            boolean changed, int left, int top, int right, int bottom) {
        if (changed) requestLayoutContentPane();
    }

    /**
     * Called when the context is created, possibly after automatic destruction.
     */
    // This is a GLSurfaceView.Renderer callback
    public void onSurfaceCreated(GL10 gl1, EGLConfig config) {
        GL11 gl = (GL11) gl1;
        if (mGL != null) {
            // The GL Object has changed
            Log.i(TAG, "GLObject has changed from " + mGL + " to " + gl);
        }
        mGL = gl;
        mCanvas = new GLCanvasImp(gl);
        if (!DEBUG_FPS) {
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        } else {
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }
    }

    /**
     * Called when the OpenGL surface is recreated without destroying the
     * context.
     */
    // This is a GLSurfaceView.Renderer callback
    public void onSurfaceChanged(GL10 gl1, int width, int height) {
        Log.v(TAG, "onSurfaceChanged: " + width + "x" + height
                + ", gl10: " + gl1.toString());
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
        Utils.setRenderThread();
        GL11 gl = (GL11) gl1;
        Utils.Assert(mGL == gl);

        mCanvas.setSize(width, height);

        mClipRect.set(0, 0, width, height);
        mClipRetryCount = 2;
    }

    private void outputFps() {
        long now = System.nanoTime();
        if (mFrameCountingStart == 0) {
            mFrameCountingStart = now;
        } else if ((now - mFrameCountingStart) > 1000000000) {
            Log.v(TAG, "fps: " + (double) mFrameCount
                    * 1000000000 / (now - mFrameCountingStart));
            mFrameCountingStart = now;
            mFrameCount = 0;
        }
        ++mFrameCount;
    }

    public void onDrawFrame(GL10 gl) {
        long begin = SystemClock.uptimeMillis();
        mRenderLock.lock();
        try {
            onDrawFrameLocked(gl);
        } finally {
            mRenderLock.unlock();
        }
        long end = SystemClock.uptimeMillis();

        if (mLastDrawFinishTime != 0) {
            long wait = mLastDrawFinishTime + TARGET_FRAME_TIME - end;
            if (wait > 0) {
                SystemClock.sleep(wait);
            }
        }
        mLastDrawFinishTime = SystemClock.uptimeMillis();
    }

    private void onDrawFrameLocked(GL10 gl) {
        if (DEBUG_FPS) outputFps();

        // release the unbound textures and deleted buffers.
        mCanvas.deleteRecycledResources();

        // reset texture upload limit
        UploadedTexture.resetUploadLimit();

        mRenderRequested = false;

        if ((mFlags & FLAG_NEED_LAYOUT) != 0) layoutContentPane();

        // OpenGL seems having a bug causing us not being able to reset the
        // scissor box in "onSurfaceChanged()". We have to do it in the second
        // onDrawFrame().
        if (mClipRetryCount > 0) {
            --mClipRetryCount;
            Rect clip = mClipRect;
            gl.glScissor(clip.left, clip.top, clip.width(), clip.height());
        }

        mCanvas.setCurrentAnimationTimeMillis(SystemClock.uptimeMillis());
        if (mContentView != null) {
           mContentView.render(mCanvas);
        }

        if (!mAnimations.isEmpty()) {
            long now = SystemClock.uptimeMillis();
            for (int i = 0, n = mAnimations.size(); i < n; i++) {
                mAnimations.get(i).setStartTime(now);
            }
            mAnimations.clear();
        }

        if (UploadedTexture.uploadLimitReached()) {
            requestRender();
        }

        if (!mRenderRequested
                && !mIdleRunner.mActive && !mIdleListeners.isEmpty()) {
            mIdleRunner.mActive = true;
            queueEvent(mIdleRunner);
        }

        if (DEBUG_INVALIDATE) {
            mCanvas.fillRect(10, 10, 5, 5, mInvalidateColor);
            mInvalidateColor = ~mInvalidateColor;
        }

        if (DEBUG_DRAWING_STAT) {
            mCanvas.dumpStatisticsAndClear();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mRenderLock.lock();
        try {
            // If this has been detached from root, we don't need to handle event
            return mContentView != null
                    ? mContentView.dispatchTouchEvent(event)
                    : false;
        } finally {
            mRenderLock.unlock();
        }
    }

    public DisplayMetrics getDisplayMetrics() {
        if (mDisplayMetrics == null) {
            mDisplayMetrics = new DisplayMetrics();
            ((Activity) getContext()).getWindowManager()
                    .getDefaultDisplay().getMetrics(mDisplayMetrics);
        }
        return mDisplayMetrics;
    }

    public GLCanvas getCanvas() {
        return mCanvas;
    }

    private class IdleRunner implements Runnable {
        protected boolean mActive = false;

        private boolean runInternal() {
            if (mRenderRequested) return false;
            if (mIdleListeners.isEmpty()) return false;
            OnGLIdleListener listener = mIdleListeners.removeFirst();
            if (listener.onGLIdle(GLRootView.this, mCanvas)) {
                mIdleListeners.addLast(listener);
            }
            return mIdleListeners.isEmpty();
        }

        public void run() {
            mRenderLock.lock();
            try {
                mActive = runInternal();
                if (mActive) queueEvent(this);
            } finally {
                mRenderLock.unlock();
            }
        }
    }

    public void lockRenderThread() {
        mRenderLock.lock();
    }

    public void unlockRenderThread() {
        mRenderLock.unlock();
    }
}
