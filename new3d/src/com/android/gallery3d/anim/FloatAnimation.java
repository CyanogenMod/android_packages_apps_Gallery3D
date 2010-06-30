package com.android.gallery3d.anim;

public class FloatAnimation extends Animation {

    private final float mFrom;
    private final float mTo;
    private float mCurrent;

    public FloatAnimation(float from, float to, int duration) {
        mFrom = from;
        mTo = to;
        setDuration(duration);
    }

    @Override
    protected void onCalculate(float progress) {
        mCurrent = mFrom + (mTo - mFrom) * progress;
    }

    public float get() {
        return mCurrent;
    }
}
