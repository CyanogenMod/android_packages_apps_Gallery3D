/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Collection of utility functions used in this package.
 */
class Util {
    @SuppressWarnings("unused")
    private static final String TAG = "Util";

    private static float sPixelDensity = -1f;

    private Util() {
    }

    public static void Assert(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    public static <T> T checkNotNull(T object) {
        if (object == null) throw new NullPointerException();
        return object;
    }

    public static boolean equals(Object a, Object b) {
        return (a == b) || (a == null ? false : a.equals(b));
    }

    public static boolean isPowerOf2(int n) {
        return (n & -n) == n;
    }

    public static int nextPowerOf2(int n) {
        n -= 1;
        n |= n >>> 16;
        n |= n >>> 8;
        n |= n >>> 4;
        n |= n >>> 2;
        n |= n >>> 1;
        return n + 1;
    }

    public static float distance(float x, float y, float sx, float sy) {
        float dx = x - sx;
        float dy = y - sy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public static int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    public static float clamp(float x, float min, float max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    public synchronized static float dpToPixel(Context context, float dp) {
        if (sPixelDensity < 0) {
            DisplayMetrics metrics = new DisplayMetrics();
            ((Activity) context).getWindowManager()
                    .getDefaultDisplay().getMetrics(metrics);
            sPixelDensity =  metrics.density;
        }
        return sPixelDensity * dp;
    }

    public static int dpToPixel(Context context, int dp) {
        return (int)(dpToPixel(context, (float) dp) + .5f);
    }

}
