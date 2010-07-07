package com.android.gallery3d.ui;

import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import java.util.Arrays;
import junit.framework.TestCase;

@SmallTest
public class UtilTest extends TestCase {
    private static final String TAG = "UtilTest";

    public void testAssert() {
        // This should not throw an exception.
        Util.Assert(true);

        // This should throw an exception.
        try {
            Util.Assert(false);
            fail();
        } catch (AssertionError ex) {
            // expected.
        }
    }

    public void testCheckNotNull() {
        // These should not throw an expection.
        Util.checkNotNull(new Object());
        Util.checkNotNull(0);
        Util.checkNotNull("");

        // This should throw an expection.
        try {
            Util.checkNotNull(null);
            fail();
        } catch (NullPointerException ex) {
            // expected.
        }
    }

    public void testEquals() {
        Object a = new Object();
        Object b = new Object();

        assertTrue(Util.equals(null, null));
        assertTrue(Util.equals(a, a));
        assertFalse(Util.equals(null, a));
        assertFalse(Util.equals(a, null));
        assertFalse(Util.equals(a, b));
    }

    public void testIsPowerOf2() {
        for (int i = 0; i < 31; i++) {
            int v = (1 << i);
            assertTrue(Util.isPowerOf2(v));
        }

        int[] f = new int[] {3, 5, 6, 7, 9, 10, 65535, Integer.MAX_VALUE - 1,
                Integer.MAX_VALUE };
        for (int v : f) {
            assertFalse(Util.isPowerOf2(v));
        }

        int[] e = new int[] {0, -1, -2, -4, -65536, Integer.MIN_VALUE + 1,
                Integer.MIN_VALUE };
        for (int v : e) {
            try {
                Util.isPowerOf2(v);
                fail();
            } catch (IllegalArgumentException ex) {
                // expected.
            }
        }
    }

    public void testNextPowerOf2() {
        int[] q = new int[] {1, 2, 3, 4, 5, 6, 10, 65535, (1 << 30) - 1, (1 << 30)};
        int[] a = new int[] {1, 2, 4, 4, 8, 8, 16, 65536, (1 << 30)    , (1 << 30)};

        for (int i = 0; i < q.length; i++) {
            assertEquals(a[i], Util.nextPowerOf2(q[i]));
        }

        int[] e = new int[] {0, -1, -2, -4, -65536, (1 << 30) + 1, Integer.MAX_VALUE};

        for (int v : e) {
            try {
                Util.nextPowerOf2(v);
                fail();
            } catch (IllegalArgumentException ex) {
                // expected.
            }
        }
    }

    public void testDistance() {
        assertFloatEq(0f, Util.distance(0, 0, 0, 0));
        assertFloatEq(1f, Util.distance(0, 1, 0, 0));
        assertFloatEq(1f, Util.distance(0, 0, 0, 1));
        assertFloatEq(2f, Util.distance(1, 2, 3, 2));
        assertFloatEq(5f, Util.distance(1, 2, 1 + 3, 2 + 4));
        assertFloatEq(5f, Util.distance(1, 2, 1 + 3, 2 + 4));
        assertFloatEq(Float.MAX_VALUE, Util.distance(Float.MAX_VALUE, 0, 0, 0));
    }

    public void testClamp() {
        assertEquals(1000, Util.clamp(300, 1000, 2000));
        assertEquals(1300, Util.clamp(1300, 1000, 2000));
        assertEquals(2000, Util.clamp(2300, 1000, 2000));

        assertEquals(0.125f, Util.clamp(0.1f, 0.125f, 0.5f));
        assertEquals(0.25f, Util.clamp(0.25f, 0.125f, 0.5f));
        assertEquals(0.5f, Util.clamp(0.9f, 0.125f, 0.5f));
    }

    public void testIsOpaque() {
        assertTrue(Util.isOpaque(0xFF000000));
        assertTrue(Util.isOpaque(0xFFFFFFFF));
        assertTrue(Util.isOpaque(0xFF123456));

        assertFalse(Util.isOpaque(0xFEFFFFFF));
        assertFalse(Util.isOpaque(0x8FFFFFFF));
        assertFalse(Util.isOpaque(0x00FF0000));
        assertFalse(Util.isOpaque(0x5500FF00));
        assertFalse(Util.isOpaque(0xAA0000FF));
    }

    public static void assertFloatEq(float expected, float actual) {
        if (Math.abs(actual - expected) > 1e-6) {
            Log.v(TAG, "expected: " + expected + ", actual: " + actual);
            fail();
        }
    }
}
