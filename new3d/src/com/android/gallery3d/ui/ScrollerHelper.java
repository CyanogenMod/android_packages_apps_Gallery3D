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

import com.android.gallery3d.util.Utils;

public class ScrollerHelper {
    private static final long START_ANIMATION = -1;
    private static final long NO_ANIMATION = -2;

    private static final int ANIM_KIND_FLING = 1;
    private static final int ANIM_KIND_SCROLL = 2;
    private static final int DECELERATED_FACTOR = 4;

    private long mStartTime = NO_ANIMATION;

    private int mDuration; // in millisecond

    private int mStart;
    private int mFinal;
    private int mPosition;

    private int mAnimationKind;

    // The fling duration when velocity is 1 pixel / second
    private float FLING_DURATION_PARAM = 200f; // 200ms

    /**
     * Call this when you want to know the new location.  If it returns true,
     * the animation is not yet finished.  loc will be altered to provide the
     * new location.
     */
    public boolean advanceAnimation(long currentTimeMillis) {
        if (mStartTime == NO_ANIMATION) return false;
        if (mStartTime == START_ANIMATION) mStartTime = currentTimeMillis;

        int timePassed = (int)(currentTimeMillis - mStartTime);
        if (timePassed < mDuration) {
            float progress = (float) timePassed / mDuration;
            float f = 1 - progress;
            if (mAnimationKind == ANIM_KIND_SCROLL) {
                f = 1 - f;  // linear
            } else if (mAnimationKind == ANIM_KIND_FLING) {
                f = 1 - f * f * f * f;  // x ^ DECELERATED_FACTOR
            }
            mPosition = Math.round(mStart + (mFinal - mStart) * f);
            Log.v("Fling", String.format("mStart = %s, mFinal = %s, mPosition = %s, f = %s, progress = %s",
                    mStart, mFinal, mPosition, f, progress));
            if (mPosition == mFinal) {
                mStartTime = NO_ANIMATION;
                return false;
            }
            return true;
        } else {
            mPosition = mFinal;
            mStartTime = NO_ANIMATION;
            return false;
        }
    }

    public void forceFinished() {
        mStartTime = NO_ANIMATION;
        mFinal = mPosition;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public void fling(float velocity, int min, int max) {
        /*
         * The position formula: x = s + (e - s) * (1 - (1 - t / T) ^ d)
         *     velocity formula: v = d * (e - s) * (1 - t / T) ^ (d - 1) / T
         * Thus,
         *     v0 = (e - s) / T * d => (e - s) = v0 * T / d
         */
        mStartTime = START_ANIMATION;
        mAnimationKind = ANIM_KIND_FLING;
        mStart = mPosition;
        double x = Math.pow(Math.abs(velocity), 1.0 / (DECELERATED_FACTOR - 1));
        mDuration = (int) Math.round(FLING_DURATION_PARAM * x);
        int distance = Math.round(
                velocity * mDuration / DECELERATED_FACTOR / 1000);
        mFinal = Utils.clamp(mStart + distance, min, max);

        // If the left space is not enough, get duration base on the left space
        if (mFinal - mStart != distance && distance != 0) {
            mDuration *= Math.pow(
                    (double) (mFinal - mStart) / distance, 1.0 / (DECELERATED_FACTOR));
        }
    }

    public void startScroll(int distance, int min, int max) {
        mStartTime = START_ANIMATION;
        mAnimationKind = ANIM_KIND_SCROLL;
        mStart = mPosition;
        mFinal = Utils.clamp(mFinal + distance, min, max);
        mDuration = 0;
    }
}
