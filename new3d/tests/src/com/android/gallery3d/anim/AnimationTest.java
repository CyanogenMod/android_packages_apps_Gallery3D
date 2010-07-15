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

package com.android.gallery3d.anim;

import android.util.Log;
import android.view.animation.Interpolator;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

@SmallTest
public class AnimationTest extends TestCase {
    private static final String TAG = "AnimationTest";

    public void testIntAnimation() {
        IntAnimation a = new IntAnimation(0, 100, 10);  // value 0 to 100, duration 10
        a.start();                 // start animation
        assertTrue(a.isActive());  // should be active now
        a.calculate(0);            // set start time = 0
        assertTrue(a.get() == 0);  // start value should be 0
        a.calculate(1);            // calculate value for time 1
        assertTrue(a.get() == 10); //
        a.calculate(5);            // calculate value for time 5
        assertTrue(a.get() == 50); //
        a.calculate(9);            // calculate value for time 9
        assertTrue(a.get() == 90); //
        a.calculate(10);           // calculate value for time 10
        assertTrue(!a.isActive()); // should be inactive now
        assertTrue(a.get() == 100);//
        a.start();                 // restart
        assertTrue(a.isActive());  // should be active now
        a.calculate(5);            // set start time = 5
        assertTrue(a.get() == 0);  // start value should be 0
        a.calculate(5+9);          // calculate for time 5+9
        assertTrue(a.get() == 90);
    }

    public void testFloatAnimation() {
        FloatAnimation a = new FloatAnimation(0f, 1f, 10);  // value 0 to 1.0, duration 10
        a.start();                 // start animation
        assertTrue(a.isActive());  // should be active now
        a.calculate(0);            // set start time = 0
        assertTrue(a.get() == 0);  // start value should be 0
        a.calculate(1);            // calculate value for time 1
        assertFloatEq(a.get(), 0.1f);
        a.calculate(5);            // calculate value for time 5
        assertTrue(a.get() == 0.5);//
        a.calculate(9);            // calculate value for time 9
        assertFloatEq(a.get(), 0.9f);
        a.calculate(10);           // calculate value for time 10
        assertTrue(!a.isActive()); // should be inactive now
        assertTrue(a.get() == 1.0);//
        a.start();                 // restart
        assertTrue(a.isActive());  // should be active now
        a.calculate(5);            // set start time = 5
        assertTrue(a.get() == 0);  // start value should be 0
        a.calculate(5+9);          // calculate for time 5+9
        assertFloatEq(a.get(), 0.9f);
    }

    private static class MyInterpolator implements Interpolator {
        public float getInterpolation(float input) {
            return 4f * (input - 0.5f);  // maps [0,1] to [-2,2]
        }
    }

    public void testInterpolator() {
        FloatAnimation a = new FloatAnimation(0f, 1f, 10);  // value 0 to 1.0, duration 10
        a.setInterpolator(new MyInterpolator());
        a.start();                 // start animation
        a.calculate(0);            // set start time = 0
        assertTrue(a.get() == -2); // start value should be -2
        a.calculate(1);            // calculate value for time 1
        assertFloatEq(a.get(), -1.6f);
        a.calculate(5);            // calculate value for time 5
        assertTrue(a.get() == 0);  //
        a.calculate(9);            // calculate value for time 9
        assertFloatEq(a.get(), 1.6f);
        a.calculate(10);           // calculate value for time 10
        assertTrue(a.get() == 2);  //
    }

    public static void assertFloatEq(float expected, float actual) {
        if (Math.abs(actual - expected) > 1e-6) {
            Log.v(TAG, "expected: " + expected + ", actual: " + actual);
            fail();
        }
    }
}
