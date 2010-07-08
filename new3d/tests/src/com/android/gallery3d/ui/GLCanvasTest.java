package com.android.gallery3d.ui;

import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import java.nio.Buffer;
import java.util.Arrays;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import junit.framework.TestCase;

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
        final float X = 65535f;
        return makeColor4f(red / X, green / X, blue / X, alpha / X);
    }

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
            // This test assumes we use pre-multipled alpha blending.
            int[] testColors = new int[] {
                0, 0xFFFFFFFF, 0xFF000000, 0x00FFFFFF, 0x80FF8001,
                0x7F010101, 0xFEFEFDFC, 0x017F8081, 0x027F8081, 0x2ADE4C4D
            };

            GLCanvas canvas = new GLCanvasImp(this);
            // Test one color to make sure blend is called.
            assertEquals(0, mCalled);
            canvas.setColor(0x7F804020);
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
                canvas.setColor(c);
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

            assertEquals(0, mCalled);
            canvas.setColor(0xFF804020);
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

    private static class DrawLineTest extends GLStub {
        private int mCalled = 0;
        private int mCalledColor;
        private boolean mBlendCalled;
        private PointerInfo mVertexPointer;
        private int[] mResult = new int[4];

        @Override
        public void glVertexPointer(int size, int type, int stride, Buffer pointer) {
            mVertexPointer = new PointerInfo(size, type, stride, pointer);
        }

        @Override
        public void glDrawArrays(int mode, int first, int count) {
            assertNotNull(mVertexPointer);
            assertEquals(2, count);
            mVertexPointer.bindByteBuffer();

            double[] coord = new double[4];
            mVertexPointer.getArrayElement(first, coord);
            mResult[0] = (int)coord[0];
            mResult[1] = (int)coord[1];
            mVertexPointer.getArrayElement(first + 1, coord);
            mResult[2] = (int)coord[0];
            mResult[3] = (int)coord[1];
            mCalled++;
        }

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
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
    public void testGetGLInstance() {
        GL11 glStub = new GLStub();
        GLCanvas canvas = new GLCanvasImp(glStub);
        assertSame(glStub, canvas.getGLInstance());
    }
}
