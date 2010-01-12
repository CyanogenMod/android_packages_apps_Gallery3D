package com.cooliris.media;

import android.graphics.Bitmap;

public abstract class Texture {

    public static final int STATE_UNLOADED = 0;
    public static final int STATE_QUEUED = 1;
    public static final int STATE_LOADING = 2;
    public static final int STATE_LOADED = 3;
    public static final int STATE_ERROR = 4;

    int mState = STATE_UNLOADED;
    int mId;
    int mWidth;
    int mHeight;
    float mNormalizedWidth;
    float mNormalizedHeight;
    Bitmap mBitmap;

    public boolean isCached() {
        return false;
    }

    public final void clear() {
        mId = 0;
        mState = STATE_UNLOADED;
        mWidth = 0;
        mHeight = 0;
        mNormalizedWidth = 0;
        mNormalizedHeight = 0;
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    public final boolean isLoaded() {
        return mState == STATE_LOADED;
    }

    public final int getState() {
        return mState;
    }

    public final int getWidth() {
        return mWidth;
    }

    public final int getHeight() {
        return mHeight;
    }

    public final float getNormalizedWidth() {
        return mNormalizedWidth;
    }

    public final float getNormalizedHeight() {
        return mNormalizedHeight;
    }

    /** If this returns true, the texture will be enqueued. */
    protected boolean shouldQueue() {
        return true;
    }

    /** Returns a bitmap, or null if an error occurs. */
    protected abstract Bitmap load(RenderView view);

    public boolean isUncachedVideo() {
        return false;
    }
}
