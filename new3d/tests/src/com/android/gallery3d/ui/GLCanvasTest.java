package com.android.gallery3d.ui;

import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import java.util.Arrays;
import javax.microedition.khronos.opengles.GL11;
import junit.framework.TestCase;

@SmallTest
public class GLCanvasTest extends TestCase {
    private static final String TAG = "GLCanvasTest";

    GL11 mGlStub;
    GLCanvas mCanvas;

    public void setUp() {
        mGlStub = new GLStub();
        mCanvas = new GLCanvasImp(mGlStub);
    }

    public void testGetGLInstance() {
        assertSame(mGlStub, mCanvas.getGLInstance());
    }

    public void testAnimationTime() {
        long[] testData = {0, 1, 2, 1000, 10000, Long.MAX_VALUE};

        for (long v : testData) {
            mCanvas.setCurrentAnimationTimeMillis(v);
            assertEquals(v, mCanvas.currentAnimationTimeMillis());
        }

        try {
            mCanvas.setCurrentAnimationTimeMillis(-1);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected.
        }
    }
}
