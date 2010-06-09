package com.android.gallery3d.anim;

import android.view.animation.Interpolator;

import com.android.gallery3d.ui.Util;

abstract public class Animation {
    private static final long ANIMATION_START = 0;
    private static final long NO_ANIMATION = -1;

    private long mStartTime = NO_ANIMATION;
    private int mDuration;
    private Interpolator mInterpolator;

    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public void start() {
        mStartTime = ANIMATION_START;
    }

    public boolean isActive() {
        return mStartTime != NO_ANIMATION;
    }

    public boolean calculate(long currentTimeMillis) {
        if (mStartTime == NO_ANIMATION) return false;
        if (mStartTime == ANIMATION_START) mStartTime = currentTimeMillis;
        int elapse = (int) (currentTimeMillis - mStartTime);
        float x = Util.clamp((float) elapse / mDuration, 0f, 1f);
        if (mInterpolator != null) x = mInterpolator.getInterpolation(x);
        if (onCalculate(x)) return true;
        mStartTime = NO_ANIMATION;
        return false;
    }

    abstract protected boolean onCalculate(float progress);
}
