package com.android.gallery3d.anim;

public class FloatAnimation {
    private static final long ANIMATION_START = 0;
    private static final long NO_ANIMATION = -1;

    private long mStartTime = NO_ANIMATION;
    private final int mDuration;

    private final float mFrom;
    private final float mTo;
    private float mCurrent;

    public FloatAnimation(float from, float to, int duration) {
        mFrom = from;
        mTo = to;
        mDuration = duration;
    }

    public void start() {
        mStartTime = ANIMATION_START;
    }

    public boolean calculate(long currentTimeMillis) {
        if (mStartTime == NO_ANIMATION) return false;
        if (mStartTime == ANIMATION_START) mStartTime = currentTimeMillis;
        int elapse = (int) (currentTimeMillis - mStartTime);
        if (elapse < mDuration) {
            float x = (float) elapse / mDuration;
            mCurrent = mFrom + (mTo - mFrom) * x;
        } else {
            mCurrent = mTo;
            mStartTime = NO_ANIMATION;
        }
        return true;
    }

    public boolean isActive() {
        return mStartTime != NO_ANIMATION;
    }

    public float get() {
        return mCurrent;
    }
}
