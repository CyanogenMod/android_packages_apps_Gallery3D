// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.anim;

import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.Util;

public class AlphaAnimation extends CanvasAnimation {
    private final float mStartAlpha;
    private final float mEndAlpha;
    private float mCurrentAlpha;

    public AlphaAnimation(float from, float to) {
        mStartAlpha = from;
        mEndAlpha = to;
        mCurrentAlpha = from;
    }

    @Override
    public void apply(GLCanvas canvas) {
        canvas.multiplyAlpha(mCurrentAlpha);
    }

    @Override
    public int getCanvasSaveFlags() {
        return GLCanvas.ALPHA_SAVE_FLAG;
    }

    @Override
    protected void onCalculate(float progress) {
        mCurrentAlpha = Util.clamp(mStartAlpha
                + (mEndAlpha - mStartAlpha) * progress, 0f, 1f);
    }
}
