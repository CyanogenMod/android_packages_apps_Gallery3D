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
    protected void onCalculate(float progress) {
        mCurrent = mFrom + (int)((mTo - mFrom) * progress + .5f);
    }

    public int get() {
        return mCurrent;
    }
}
