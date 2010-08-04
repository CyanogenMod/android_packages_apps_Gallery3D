/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.ui;

import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.ArrayList;

public class DisplayItemPanel extends GLView {

    private static final int TRANSITION_DURATION = 1000;

    // The state of the display item.
    // The state of those items to be removed
    private static final int STATE_REMOVED = 1;

    // The state of the items that is just added to this panel
    private static final int STATE_NEWBIE = 2;

    // The state of the items that changed position
    private static final int STATE_MOVED = 3;

    private static final long START_ANIMATION = -1;
    private static final long NO_ANIMATION = -2;

    private ArrayList<DisplayItem> mItems = new ArrayList<DisplayItem>();

    private long mAnimationStartTime = NO_ANIMATION;
    private boolean mPrepareTransition = false;
    private final Interpolator mInterpolator = new DecelerateInterpolator(4);

    /**
     * Puts the item center at the given position and angle.
     */
    public void putDisplayItem(
            DisplayItem item, float x, float y, float theata) {
        lockRendering();
        try {
            putDisplayItemLocked(item, x, y, theata);
        } finally {
            unlockRendering();
        }
    }

    private void putDisplayItemLocked(
            DisplayItem item, float x, float y, float theata) {
        if (item.mPanel != this && item.mPanel != null) {
            throw new IllegalArgumentException();
        }
        item.mTarget.set(x, y, theata);

        if (item.mPanel != this) {
            item.mPanel = this;
            item.mState = STATE_NEWBIE;
            mItems.add(item);
            item.mCurrent.set(x, y, theata);
        } else {
            item.mState = STATE_MOVED;
            if (mPrepareTransition) {
                item.mSource.set(item.mCurrent);
            } else {
                item.mCurrent.set(x, y, theata);
            }
        }
        if (!mPrepareTransition) invalidate();
    }

    public void removeDisplayItem(DisplayItem item) {
        lockRendering();
        try {
            if (item.mPanel != this) throw new IllegalArgumentException();
            mItems.remove(item);
            item.mPanel = null;
        } finally {
            unlockRendering();
        }
    }

    public void prepareTransition() {
        lockRendering();
        try {
            mPrepareTransition = true;
            for (DisplayItem item : mItems) {
                item.mState = STATE_REMOVED;
            }
        } finally {
            unlockRendering();
        }
    }

    public void startTransition() {
        lockRendering();
        try {
            mPrepareTransition = false;
            mAnimationStartTime = START_ANIMATION;
            invalidate();
        } finally {
            unlockRendering();
        }
    }

    private void onTransitionComplete() {
        lockRendering();
        try {
            ArrayList<DisplayItem> list = new ArrayList<DisplayItem>();
            for (DisplayItem item: mItems) {
                if (item.mState == STATE_REMOVED) {
                    item.mPanel = null;
                } else {
                    list.add(item);
                }
            }
            mItems = list;
        } finally {
            unlockRendering();
        }
    }

    @Override
    protected void render(GLCanvas canvas) {
        canvas.translate(-mScrollX, 0, 0);
        if (mAnimationStartTime == NO_ANIMATION) {
            for (int i = 0, n = mItems.size(); i < n; i++) {
                renderItem(canvas, mItems.get(i));
            }
        } else {
            long now = canvas.currentAnimationTimeMillis();
            if (mAnimationStartTime == START_ANIMATION) {
                mAnimationStartTime = now;
            }
            float timeRatio = Util.clamp((float)
                    (now - mAnimationStartTime) / TRANSITION_DURATION,  0, 1);
            float interpolate = mInterpolator.getInterpolation(timeRatio);
            for (DisplayItem item: mItems) {
                renderItem(canvas, item, interpolate);
            }
            if (timeRatio == 1.0f) {
                onTransitionComplete();
                mAnimationStartTime = NO_ANIMATION;
            } else {
                invalidate();
            }
        }
        canvas.translate(mScrollX, 0, 0);
    }

    private void renderItem(GLCanvas canvas, DisplayItem item) {
        canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
        item.mCurrent.apply(canvas);
        item.render(canvas);
        canvas.restore();
    }

    private void renderItem(
            GLCanvas canvas, DisplayItem item, float interpolate) {
        canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
        switch (item.mState) {
            case STATE_MOVED:
                item.updateCurrentPosition(interpolate);
                break;
            case STATE_NEWBIE:
                canvas.multiplyAlpha(interpolate);
                break;
            case STATE_REMOVED:
                canvas.multiplyAlpha(1.0f - interpolate);
                break;
        }
        item.mCurrent.apply(canvas);
        item.render(canvas);
        canvas.restore();
    }
}
