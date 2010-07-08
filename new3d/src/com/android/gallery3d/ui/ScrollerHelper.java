package com.android.gallery3d.ui;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;

public class ScrollerHelper {
    private static final long START_ANIMATION = -1;

    private final float mDeceleration;

    private boolean mFinished;
    private int mVelocity;
    private int mDirection;
    private int mDuration; // in millisecond
    private long mStartTime;

    private int mMin;
    private int mMax;
    private int mStart;
    private int mFinal;
    private int mPosition;
    private final Interpolator mInterpolator;

    public ScrollerHelper(Context context, Interpolator interpolator) {
        mInterpolator = interpolator;
        mDeceleration = SensorManager.GRAVITY_EARTH   // g (m/s^2)
                * 39.37f                              // inch/meter
                * Util.dpToPixel(context, 160)        // pixels per inch
                * ViewConfiguration.getScrollFriction();
    }

    /**
     * Call this when you want to know the new location.  If it returns true,
     * the animation is not yet finished.  loc will be altered to provide the
     * new location.
     */
    public boolean computeScrollOffset(long currentTimeMillis) {
        if (mFinished) return false;
        if (mStartTime == START_ANIMATION) mStartTime = currentTimeMillis;

        int timePassed = (int)(currentTimeMillis - mStartTime);
        if (timePassed < mDuration) {
            if (mInterpolator != null) {
                timePassed = (int) (mDuration * mInterpolator.getInterpolation(
                        (float) timePassed / mDuration) + 0.5f);
            }
            float t = timePassed / 1000.0f;
            int distance = (int) ((
                    mVelocity * t) - (mDeceleration * t * t / 2.0f) + 0.5f);
            mPosition = Util.clamp(mStart + mDirection * distance, mMin, mMax);
        } else {
            mPosition = mFinal;
            mFinished = true;
        }
        return true;
    }

    public void forceFinished() {
        mFinished = true;
    }

    public int getCurrentPosition() {
        return mPosition;
    }

    public void fling(int start, int velocity, int min, int max) {
        mFinished = false;
        mVelocity = Math.abs(velocity);
        mDirection = velocity >= 0 ? 1 : -1;
        mDuration = (int) (1000 * mVelocity / mDeceleration);
        mStartTime = START_ANIMATION;
        mStart = start;
        mMin = min;
        mMax = max;
        double totalDistance = (double) mDirection
                * (velocity * velocity) / (2 * mDeceleration);
        mFinal = Util.clamp(start + (int) (totalDistance + 0.5), min, max);
    }
}
