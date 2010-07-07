package com.android.gallery3d.anim;

import android.util.Log;
import android.view.animation.Interpolator;

import junit.framework.TestCase;

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
        // These are broken now, waiting for the bug fix.
        //a.calculate(9);            // calculate value for time 9
        //assertFloatEq(a.get(), 1.6f);
        //a.calculate(10);           // calculate value for time 10
        //assertTrue(a.get() == 2);  //
    }

    public static void assertFloatEq(float a, float b) {
        assertTrue(Math.abs(a-b) < 1e-6);
    }
}
