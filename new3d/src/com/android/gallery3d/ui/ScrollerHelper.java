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

import android.content.Context;
import android.hardware.SensorManager;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;

public class ScrollerHelper {
    private static final long START_ANIMATION = -1;

    private final float mDeceleration;

    private boolean mFinished;
    private int mVelocity;
    private int mDirection;
    private int mDuration; // in millisecond
    private long mStartTime;

    private int mMin;
    private int mMax;
    private int mStart;
    private int mFinal;
    private int mPosition;
    private final Interpolator mInterpolator;

    public ScrollerHelper(Context context, Interpolator interpolator) {
        mInterpolator = interpolator;
        mDeceleration = SensorManager.GRAVITY_EARTH   // g (m/s^2)
                * 39.37f                              // inch/meter
                * Utils.dpToPixel(context, 160)        // pixels per inch
                * ViewConfiguration.getScrollFriction();
    }

    /**
     * Call this when you want to know the new location.  If it returns true,
     * the animation is not yet finished.  loc will be altered to provide the
     * new location.
     */
    public boolean computeScrollOffset(long currentTimeMillis) {
        if (mFinished) return false;
        if (mStartTime == START_ANIMATION) mStartTime = currentTimeMillis;

        int timePassed = (int)(currentTimeMillis - mStartTime);
        if (timePassed < mDuration) {
            if (mInterpolator != null) {
                timePassed = (int) (mDuration * mInterpolator.getInterpolation(
                        (float) timePassed / mDuration) + 0.5f);
            }
            float t = timePassed / 1000.0f;
            int distance = (int) ((
                    mVelocity * t) - (mDeceleration * t * t / 2.0f) + 0.5f);
            mPosition = Utils.clamp(mStart + mDirection * distance, mMin, mMax);
        } else {
            mPosition = mFinal;
            mFinished = true;
        }
        return true;
    }

    public void forceFinished() {
        mFinished = true;
    }

    public int getCurrentPosition() {
        return mPosition;
    }

    public void fling(int start, int velocity, int min, int max) {
        mFinished = false;
        mVelocity = Math.abs(velocity);
        mDirection = velocity >= 0 ? 1 : -1;
        mDuration = (int) (1000 * mVelocity / mDeceleration);
        mStartTime = START_ANIMATION;
        mStart = start;
        mMin = min;
        mMax = max;
        double totalDistance = (double) mDirection
                * (velocity * velocity) / (2 * mDeceleration);
        mFinal = Utils.clamp(start + (int) (totalDistance + 0.5), min, max);
    }
}
