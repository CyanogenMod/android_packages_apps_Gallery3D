// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.anim;

import com.android.gallery3d.ui.GLCanvas;

public class ScaleAnimation extends CanvasAnimation {
    private final float mFromX;
    private final float mFromY;
    private final float mFromZ;
    private final float mToX;
    private final float mToY;
    private final float mToZ;

    private float mCurrentX;
    private float mCurrentY;
    private float mCurrentZ;

    private final float mPivotX;
    private final float mPivotY;
    private final float mPivotZ;

    public ScaleAnimation(float fromX, float toX,
            float fromY, float toY, float pivotX, float pivotY) {
        this(fromX, toX, fromY, toY, 0, 0, pivotX, pivotY, 0);
    }

    public ScaleAnimation(float fromX, float toX, float fromY, float toY,
            float fromZ, float toZ, float px, float py, float pz) {
        mFromX = fromX;
        mFromY = fromY;
        mFromZ = fromZ;
        mToX = toX;
        mToY = toY;
        mToZ = toZ;
        mPivotX = px;
        mPivotY = py;
        mPivotZ = pz;
    }

    @Override
    public void apply(GLCanvas canvas) {
        if (mPivotX != 0 || mPivotY != 0 || mPivotZ != 0)  {
            canvas.translate(mPivotX, mPivotY, mPivotZ);
            canvas.scale(mCurrentX, mCurrentY, mCurrentZ);
            canvas.translate(-mPivotX, -mPivotY, -mPivotZ);
        } else {
            canvas.scale(mCurrentX, mCurrentY, mCurrentZ);
        }
    }

    @Override
    public int getCanvasSaveFlags() {
        return GLCanvas.SAVE_FLAG_MATRIX;
    }

    @Override
    protected void onCalculate(float progress) {
        mCurrentX = mFromX + (mToX - mFromX) * progress;
        mCurrentY = mFromY + (mToY - mFromY) * progress;
        mCurrentZ = mFromZ + (mToZ - mFromZ) * progress;
    }
}
