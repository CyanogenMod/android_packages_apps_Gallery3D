// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.anim;

import com.android.gallery3d.ui.GLCanvas;

import java.util.ArrayList;

public class AnimationSet extends CanvasAnimation {

    private final ArrayList<CanvasAnimation> mAnimations =
            new ArrayList<CanvasAnimation>();
    private int mSaveFlags = 0;


    public void addAnimation(CanvasAnimation anim) {
        mAnimations.add(anim);
        mSaveFlags |= anim.getCanvasSaveFlags();
    }

    @Override
    public void apply(GLCanvas canvas) {
        for (CanvasAnimation anim : mAnimations) {
            anim.apply(canvas);
        }
    }

    @Override
    public int getCanvasSaveFlags() {
        return mSaveFlags;
    }

    @Override
    protected void onCalculate(float progress) {
        // DO NOTHING
    }

    @Override
    public boolean calculate(long currentTimeMillis) {
        boolean more = false;
        for (CanvasAnimation anim : mAnimations) {
            more |= anim.calculate(currentTimeMillis);
        }
        return more;
    }

    @Override
    public void start() {
        for (CanvasAnimation anim : mAnimations) {
            anim.start();
        }
    }

    @Override
    public boolean isActive() {
        for (CanvasAnimation anim : mAnimations) {
            if (anim.isActive()) return true;
        }
        return false;
    }

}
