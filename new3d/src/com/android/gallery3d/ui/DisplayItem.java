package com.android.gallery3d.ui;



public abstract class DisplayItem {

    public static class Position {
        public float mX;
        public float mY;
        public float mTheata;

        public void set(float x, float y, float theata) {
            mX = x;
            mY = y;
            mTheata = theata;
        }

        public void set(Position p) {
            mX = p.mX;
            mY = p.mY;
            mTheata = p.mTheata;
        }

        public void apply(GLCanvas canvas) {
            canvas.translate(mX, mY, 0);
            canvas.rotate(mTheata, 0, 0, 1);
        }
    }

    // The parameters in this section are used only by DisplayItemPanel

    protected Position mTarget = new Position();
    protected Position mSource = new Position();
    protected Position mCurrent = new Position();

    protected int mState;
    protected long mAnimationStartTime;
    protected DisplayItemPanel mPanel;

    protected int mWidth;
    protected int mHeight;

    protected void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public abstract void render(GLCanvas canvas);

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    private static float interpolateScale(
            float source, float target, float interpolate) {
        return source + interpolate * (target - source);
    }

    private static float interpolateAngle(
            float source, float target, float interpolate) {
        // interpolate the angle from source to target
        // We make the difference in the range of [-179, 180], this is the
        // shortest path to change source to target.
        float diff = target - source;
        if (diff < 0) diff += 360f;
        if (diff > 180) diff -= 360f;

        float result = source + diff * interpolate;
        return result < 0 ? result + 360f : result;
    }

    protected void updateCurrentPosition(float interpolate) {
        mCurrent.mX = interpolateScale(mSource.mX, mTarget.mX, interpolate);
        mCurrent.mY = interpolateScale(mSource.mY, mTarget.mY, interpolate);
        mCurrent.mTheata = interpolateAngle(
                mSource.mTheata, mTarget.mTheata, interpolate);
    }
}
