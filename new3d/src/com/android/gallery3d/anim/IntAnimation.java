package com.android.gallery3d.anim;

public class IntAnimation extends Animation {

    private final int mFrom;
    private final int mTo;
    private int mCurrent;

    public IntAnimation(int from, int to, int duration) {
        mFrom = from;
        mTo = to;
        setDuration(duration);
    }

    @Override
    protected boolean onCalculate(float progress) {
        if (progress < 1f) {
            mCurrent = mFrom + (int)((mTo - mFrom) * progress + .5f);
            return true;
        } else {
            mCurrent = mTo;
            return false;
        }
    }

    public int get() {
        return mCurrent;
    }
}
