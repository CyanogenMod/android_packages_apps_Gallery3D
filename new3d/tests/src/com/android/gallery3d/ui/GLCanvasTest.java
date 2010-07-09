package com.android.gallery3d.ui;

import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import junit.framework.TestCase;

import java.nio.Buffer;
import java.util.Arrays;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

@SmallTest
public class GLCanvasTest extends TestCase {
    private static final String TAG = "GLCanvasTest";

    @SmallTest
    public void testSetSize() {
        GL11 glStub = new GLStub();
        GLCanvas canvas = new GLCanvasImp(glStub);
        canvas.setSize(100, 200);
        canvas.setSize(1000, 100);
        try {
            canvas.setSize(-1, 100);
            fail();
        } catch (Throwable ex) {
            // expected.
        }
    }

    @SmallTest
    public void testClearBuffer() {
        new ClearBufferTest().run();
    }

    private static class ClearBufferTest extends GLStub {
        private int mCalled = 0;
        private int mCalledMask;

        @Override
        public void glClear(int mask) {
            mCalled++;
            mCalledMask = mask;
        };

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            assertEquals(0, mCalled);
            canvas.clearBuffer();
            assertTrue((mCalledMask & GL10.GL_COLOR_BUFFER_BIT) != 0);
            assertEquals(1, mCalled);
        }
    }

    @SmallTest
    public void testAnimationTime() {
        GL11 glStub = new GLStub();
        GLCanvas canvas = new GLCanvasImp(glStub);

        long[] testData = {0, 1, 2, 1000, 10000, Long.MAX_VALUE};

        for (long v : testData) {
            canvas.setCurrentAnimationTimeMillis(v);
            assertEquals(v, canvas.currentAnimationTimeMillis());
        }

        try {
            canvas.setCurrentAnimationTimeMillis(-1);
            fail();
        } catch (Throwable ex) {
            // expected.
        }
    }

    @SmallTest
    public void testSetColor() {
        new SetColorTest().run();
    }

    private static int makeColor4f(
        float red,
        float green,
        float blue,
        float alpha) {
        return (Math.round(alpha * 255) << 24) |
                (Math.round(red * 255) << 16) |
                (Math.round(green * 255) << 8) |
                Math.round(blue * 255);
    }

    private static int makeColor4x(
        int red,
        int green,
        int blue,
        int alpha) {
        final float X = 65536f;
        return makeColor4f(red / X, green / X, blue / X, alpha / X);
    }

    // This test assumes we use pre-multipled alpha blending and should
    // set the blending function and color correctly.
    private static class SetColorTest extends GLStub {
        private int mCalled = 0;
        private int mCalledColor;
        private boolean mBlendCalled;

        @Override
        public void glBlendFunc(int sfactor, int dfactor) {
            assertEquals(sfactor, GL11.GL_ONE);
            assertEquals(dfactor, GL11.GL_ONE_MINUS_SRC_ALPHA);
            mBlendCalled = true;
        }

        @Override
        public void glColor4f(
            float red,
            float green,
            float blue,
            float alpha) {
            mCalled++;
            mCalledColor = makeColor4f(red, green, blue, alpha);
        }

        @Override
        public void glColor4x(
            int red,
            int green,
            int blue,
            int alpha) {
            mCalled++;
            mCalledColor = makeColor4x(red, green, blue, alpha);
        }

        void run() {
            int[] testColors = new int[] {
                0, 0xFFFFFFFF, 0xFF000000, 0x00FFFFFF, 0x80FF8001,
                0x7F010101, 0xFEFEFDFC, 0x017F8081, 0x027F8081, 0x2ADE4C4D
            };

            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(400, 300);
            // Test one color to make sure blend is called.
            assertEquals(0, mCalled);
            canvas.bindColor(0x7F804020);
            canvas.drawLine(0, 0, 1, 1);
            assertTrue(mBlendCalled);
            assertEquals(1, mCalled);
            assertEquals(0x7F402010, mCalledColor);

            // Test other colors to make sure premultiplication is right
            for (int c : testColors) {
                float a = (c >>> 24) / 255f;
                float r = ((c >> 16) & 0xff) / 255f;
                float g = ((c >> 8) & 0xff) / 255f;
                float b = (c & 0xff) / 255f;
                int pre = makeColor4f(a * r, a * g, a * b, a);

                mCalled = 0;
                canvas.bindColor(c);
                canvas.drawLine(0, 0, 1, 1);
                assertEquals(1, mCalled);
                assertEquals(pre, mCalledColor);
            }
        }
    }

    @SmallTest
    public void testSetGetMultiplyAlpha() {
        GL11 glStub = new GLStub();
        GLCanvas canvas = new GLCanvasImp(glStub);

        canvas.setAlpha(1f);
        assertEquals(1f, canvas.getAlpha());

        canvas.setAlpha(0f);
        assertEquals(0f, canvas.getAlpha());

        canvas.setAlpha(0.5f);
        assertEquals(0.5f, canvas.getAlpha());

        canvas.multiplyAlpha(0.5f);
        assertEquals(0.25f, canvas.getAlpha());

        canvas.multiplyAlpha(0f);
        assertEquals(0f, canvas.getAlpha());

        try {
            canvas.setAlpha(-0.01f);
            fail();
        } catch (Throwable ex) {
            // expected.
        }

        try {
            canvas.setAlpha(1.01f);
            fail();
        } catch (Throwable ex) {
            // expected.
        }
    }

    @SmallTest
    public void testAlpha() {
        new AlphaTest().run();
    }

    private static class AlphaTest extends GLStub {
        private int mCalled = 0;
        private int mCalledColor;
        private boolean mBlendCalled;

        @Override
        public void glBlendFunc(int sfactor, int dfactor) {
            assertEquals(sfactor, GL11.GL_ONE);
            assertEquals(dfactor, GL11.GL_ONE_MINUS_SRC_ALPHA);
            mBlendCalled = true;
        }

        @Override
        public void glColor4f(
            float red,
            float green,
            float blue,
            float alpha) {
            mCalled++;
            mCalledColor = makeColor4f(red, green, blue, alpha);
        }

        @Override
        public void glColor4x(
            int red,
            int green,
            int blue,
            int alpha) {
            mCalled++;
            mCalledColor = makeColor4x(red, green, blue, alpha);
        }

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(400, 300);

            assertEquals(0, mCalled);
            canvas.bindColor(0xFF804020);
            canvas.setAlpha(0.48f);
            canvas.drawLine(0, 0, 1, 1);
            assertTrue(mBlendCalled);
            assertEquals(1, mCalled);
            // TODO: This is broken now, wait for the fix.
            //assertEquals(0x7A3D1F0F, mCalledColor);
        }
    }

    @SmallTest
    public void testDrawLine() {
        new DrawLineTest().run();
    }

    // This test assumes the drawLine() function use glDrawArrays() with
    // LINE_STRIP mode to/ draw the line and the input coordinates are used
    // directly.
    private static class DrawLineTest extends GLStub {
        private int mCalled = 0;
        private int mCalledColor;
        private boolean mBlendCalled;
        private PointerInfo mVertexPointer;
        private final int[] mResult = new int[4];

        @Override
        public void glVertexPointer(int size, int type, int stride, Buffer pointer) {
            mVertexPointer = new PointerInfo(size, type, stride, pointer);
        }

        @Override
        public void glDrawArrays(int mode, int first, int count) {
            assertNotNull(mVertexPointer);
            assertEquals(GL10.GL_LINE_STRIP, mode);
            assertEquals(2, count);
            mVertexPointer.bindByteBuffer();

            double[] coord = new double[4];
            mVertexPointer.getArrayElement(first, coord);
            mResult[0] = (int) coord[0];
            mResult[1] = (int) coord[1];
            mVertexPointer.getArrayElement(first + 1, coord);
            mResult[2] = (int) coord[0];
            mResult[3] = (int) coord[1];
            mCalled++;
        }

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(400, 300);
            canvas.drawLine(2, 7, 1, 8);
            assertEquals(1, mCalled);

            Log.v(TAG, "result = " + Arrays.toString(mResult));
            int[] answer = new int[] {2, 7, 1, 8};
            for (int i = 0; i < answer.length; i++) {
                assertEquals(answer[i], mResult[i]);
            }
        }
    }

    @SmallTest
    public void testTransform() {
        new TransformTest().run();
    }

    // This test assumes glLoadMatrixf is used to load the model view matrix,
    // and glOrthof is used to load the projection matrix.
    //
    // The projection matrix is set to an orthogonal projection which is the
    // inverse of viewport transform. So the model view matrix maps input
    // directly to screen coordinates (default no scaling, and the y-axis is
    // reversed).
    //
    // The matrix here are all listed in column major order.
    //
    private static class TransformTest extends GLStub {
        private int mCurrentMatrixMode = GL10.GL_MODELVIEW;
        private float[] mModelViewMatrix = new float[16];
        private float[] mModelViewMatrixUsed = new float[16];
        private float[] mProjectionMatrix = new float[16];
        private float[] mProjectionMatrixUsed = new float[16];

        @Override
        public void glMatrixMode(int mode) {
            mCurrentMatrixMode = mode;
        }

        @Override
        public void glLoadMatrixf(float[] m, int offset) {
            if (mCurrentMatrixMode == GL10.GL_MODELVIEW) {
                System.arraycopy(m, offset, mModelViewMatrix, 0, 16);
            } else if (mCurrentMatrixMode == GL10.GL_PROJECTION) {
                System.arraycopy(m, offset, mProjectionMatrix, 0, 16);
            }
        }

        @Override
        public void glDrawArrays(int mode, int first, int count) {
            copy(mModelViewMatrixUsed, mModelViewMatrix);
            copy(mProjectionMatrixUsed, mProjectionMatrix);
        }

        @Override
        public void glOrthof(
            float left,
            float right,
            float bottom,
            float top,
            float zNear,
            float zFar ) {
                float tx = - (right + left) / (right - left);
                float ty = - (top + bottom) / (top - bottom);
                float tz = - (zFar + zNear) / (zFar - zNear);
                float[] m = new float[] { 2/(right - left), 0, 0,  0,
                                          0, 2/(top - bottom), 0,  0,
                                          0, 0, -2/(zFar - zNear), 0,
                                          tx, ty, tz, 1
                                        };
                glLoadMatrixf(m, 0);
        }

        private void assertMatrixEq(float[] expected, float[] actual) {
            Log.v(TAG, "expected = " + Arrays.toString(expected) +
                    ", actual = " + Arrays.toString(actual));

            for (int i = 0; i < 16; i++) {
                assertFloatEq(expected[i], actual[i]);
            }
        }

        private void copy(float[] dest, float[] src) {
            System.arraycopy(src, 0, dest, 0, 16);
        }

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(40, 50);

            // Initial matrix
            canvas.drawLine(0, 0, 1, 1);
            assertMatrixEq(new float[] {1,  0, 0, 0,
                                        0, -1, 0, 0,
                                        0,  0, 1, 0,
                                        0, 50, 0, 1},
                    mModelViewMatrixUsed);

            assertMatrixEq(new float[] {2f/40,    0,  0,  0,
                                            0,2f/50,  0,  0,
                                            0,    0, -1,  0,
                                           -1,   -1,  0,  1},
                    mProjectionMatrixUsed);

            // Translation
            canvas.translate(3, 4, 5);
            canvas.drawLine(0, 0, 1, 1);
            assertMatrixEq(new float[] {1,  0, 0, 0,
                                        0, -1, 0, 0,
                                        0,  0, 1, 0,
                                        3, 46, 5, 1},
                    mModelViewMatrixUsed);
            canvas.save();

            // Scaling
            canvas.scale(0.7f, 0.6f, 0.5f);
            canvas.drawLine(0, 0, 1, 1);
            assertMatrixEq(new float[] {0.7f,     0,    0, 0,
                                           0, -0.6f,    0, 0,
                                           0,     0, 0.5f, 0,
                                           3,    46,    5, 1},
                    mModelViewMatrixUsed);

            // Rotation
            canvas.rotate(90, 0, 0, 1);
            canvas.drawLine(0, 0, 1, 1);
            assertMatrixEq(new float[] {    0, -0.6f,    0, 0,
                                        -0.7f,     0,    0, 0,
                                            0,     0, 0.5f, 0,
                                            3,    46,    5, 1},
                    mModelViewMatrixUsed);
            canvas.restore();

            // After restoring to the point just after translation,
            // do rotation again.
            canvas.rotate(180, 1, 0, 0);
            canvas.drawLine(0, 0, 1, 1);
            assertMatrixEq(new float[] {  1,  0,  0, 0,
                                          0,  1,  0, 0,
                                          0,  0, -1, 0,
                                          3, 46,  5, 1},
                    mModelViewMatrixUsed);
        }
    }

    @SmallTest
    public void testGetGLInstance() {
        GL11 glStub = new GLStub();
        GLCanvas canvas = new GLCanvasImp(glStub);
        assertSame(glStub, canvas.getGLInstance());
    }

    public static void assertFloatEq(float expected, float actual) {
        if (Math.abs(actual - expected) > 1e-6) {
            Log.v(TAG, "expected: " + expected + ", actual: " + actual);
            fail();
        }
    }
}
