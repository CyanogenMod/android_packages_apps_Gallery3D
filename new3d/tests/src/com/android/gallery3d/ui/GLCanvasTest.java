package com.android.gallery3d.ui;

import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import junit.framework.TestCase;

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

    private static class ClearBufferTest extends GLMock {
        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            assertEquals(0, mGLClearCalled);
            canvas.clearBuffer();
            assertEquals(GL10.GL_COLOR_BUFFER_BIT, mGLClearMask);
            assertEquals(1, mGLClearCalled);
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

    // This test assumes we use pre-multipled alpha blending and should
    // set the blending function and color correctly.
    private static class SetColorTest extends GLMock {
        void run() {
            int[] testColors = new int[] {
                0, 0xFFFFFFFF, 0xFF000000, 0x00FFFFFF, 0x80FF8001,
                0x7F010101, 0xFEFEFDFC, 0x017F8081, 0x027F8081, 0x2ADE4C4D
            };

            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(400, 300);
            // Test one color to make sure blend function is set.
            assertEquals(0, mGLColorCalled);
            canvas.drawLine(0, 0, 1, 1, 0x7F804020);
            assertEquals(1, mGLColorCalled);
            assertEquals(0x7F402010, mGLColor);
            assertPremultipliedBlending(this);

            // Test other colors to make sure premultiplication is right
            for (int c : testColors) {
                float a = (c >>> 24) / 255f;
                float r = ((c >> 16) & 0xff) / 255f;
                float g = ((c >> 8) & 0xff) / 255f;
                float b = (c & 0xff) / 255f;
                int pre = makeColor4f(a * r, a * g, a * b, a);

                mGLColorCalled = 0;
                canvas.drawLine(0, 0, 1, 1, c);
                assertEquals(1, mGLColorCalled);
                assertEquals(pre, mGLColor);
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

    private static class AlphaTest extends GLMock {
        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(400, 300);

            assertEquals(0, mGLColorCalled);
            canvas.setAlpha(0.48f);
            canvas.drawLine(0, 0, 1, 1, 0xFF804020);
            assertPremultipliedBlending(this);
            assertEquals(1, mGLColorCalled);
            assertEquals(0x7A3D1F0F, mGLColor);
        }
    }

    @SmallTest
    public void testDrawLine() {
        new DrawLineTest().run();
    }

    // This test assumes the drawLine() function use glDrawArrays() with
    // GL_LINE_STRIP mode to draw the line and the input coordinates are used
    // directly.
    private static class DrawLineTest extends GLMock {
        private int mDrawArrayCalled = 0;
        private final int[] mResult = new int[4];

        @Override
        public void glDrawArrays(int mode, int first, int count) {
            assertNotNull(mGLVertexPointer);
            assertEquals(GL10.GL_LINE_STRIP, mode);
            assertEquals(2, count);
            mGLVertexPointer.bindByteBuffer();

            double[] coord = new double[4];
            mGLVertexPointer.getArrayElement(first, coord);
            mResult[0] = (int) coord[0];
            mResult[1] = (int) coord[1];
            mGLVertexPointer.getArrayElement(first + 1, coord);
            mResult[2] = (int) coord[0];
            mResult[3] = (int) coord[1];
            mDrawArrayCalled++;
        }

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(400, 300);
            canvas.drawLine(2, 7, 1, 8, 0 /* color */);
            assertTrue(mGLVertexArrayEnabled);
            assertEquals(1, mDrawArrayCalled);

            Log.v(TAG, "result = " + Arrays.toString(mResult));
            int[] answer = new int[] {2, 7, 1, 8};
            for (int i = 0; i < answer.length; i++) {
                assertEquals(answer[i], mResult[i]);
            }
        }
    }

    @SmallTest
    public void testFillRect() {
        new FillRectTest().run();
    }

    // This test assumes the drawLine() function use glDrawArrays() with
    // GL_TRIANGLE_STRIP mode to draw the line and the input coordinates
    // are used directly.
    private static class FillRectTest extends GLMock {
        private int mDrawArrayCalled = 0;
        private final int[] mResult = new int[8];

        @Override
        public void glDrawArrays(int mode, int first, int count) {
            assertNotNull(mGLVertexPointer);
            assertEquals(GL10.GL_TRIANGLE_STRIP, mode);
            assertEquals(4, count);
            mGLVertexPointer.bindByteBuffer();

            double[] coord = new double[4];
            for (int i = 0; i < 4; i++) {
                mGLVertexPointer.getArrayElement(first + i, coord);
                mResult[i * 2 + 0] = (int) coord[0];
                mResult[i * 2 + 1] = (int) coord[1];
            }

            mDrawArrayCalled++;
        }

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(400, 300);
            canvas.fillRect(2, 7, 1, 8, 0 /* color */);
            assertTrue(mGLVertexArrayEnabled);
            assertEquals(1, mDrawArrayCalled);
            Log.v(TAG, "result = " + Arrays.toString(mResult));

            // These are the four vertics that should be used.
            int[] answer = new int[] {
                2, 7,
                3, 7,
                3, 15,
                2, 15};
            int count[] = new int[4];

            // Count the number of appearances for each vertex.
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (answer[i * 2] == mResult[j * 2] &&
                        answer[i * 2 + 1] == mResult[j * 2 + 1]) {
                        count[i]++;
                    }
                }
            }

            // Each vertex should appear exactly once.
            for (int i = 0; i < 4; i++) {
                assertEquals(1, count[i]);
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
    private static class TransformTest extends GLMock {
        private final float[] mModelViewMatrixUsed = new float[16];
        private final float[] mProjectionMatrixUsed = new float[16];

        @Override
        public void glDrawArrays(int mode, int first, int count) {
            copy(mModelViewMatrixUsed, mGLModelViewMatrix);
            copy(mProjectionMatrixUsed, mGLProjectionMatrix);
        }

        private void copy(float[] dest, float[] src) {
            System.arraycopy(src, 0, dest, 0, 16);
        }

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(40, 50);
            int color = 0;

            // Initial matrix
            canvas.drawLine(0, 0, 1, 1, color);
            assertMatrixEq(new float[] {
                    1,  0, 0, 0,
                    0, -1, 0, 0,
                    0,  0, 1, 0,
                    0, 50, 0, 1
                    }, mModelViewMatrixUsed);

            assertMatrixEq(new float[] {
                    2f / 40,       0,  0, 0,
                          0, 2f / 50,  0, 0,
                          0,       0, -1, 0,
                         -1,      -1,  0, 1
                    }, mProjectionMatrixUsed);

            // Translation
            canvas.translate(3, 4, 5);
            canvas.drawLine(0, 0, 1, 1, color);
            assertMatrixEq(new float[] {
                    1,  0, 0, 0,
                    0, -1, 0, 0,
                    0,  0, 1, 0,
                    3, 46, 5, 1
                    }, mModelViewMatrixUsed);
            canvas.save();

            // Scaling
            canvas.scale(0.7f, 0.6f, 0.5f);
            canvas.drawLine(0, 0, 1, 1, color);
            assertMatrixEq(new float[] {
                    0.7f,     0,    0, 0,
                    0,    -0.6f,    0, 0,
                    0,        0, 0.5f, 0,
                    3,       46,    5, 1
                    }, mModelViewMatrixUsed);

            // Rotation
            canvas.rotate(90, 0, 0, 1);
            canvas.drawLine(0, 0, 1, 1, color);
            assertMatrixEq(new float[] {
                        0, -0.6f,    0, 0,
                    -0.7f,     0,    0, 0,
                        0,     0, 0.5f, 0,
                        3,    46,    5, 1
                    }, mModelViewMatrixUsed);
            canvas.restore();

            // After restoring to the point just after translation,
            // do rotation again.
            canvas.rotate(180, 1, 0, 0);
            canvas.drawLine(0, 0, 1, 1, color);
            assertMatrixEq(new float[] {
                    1,  0,  0, 0,
                    0,  1,  0, 0,
                    0,  0, -1, 0,
                    3, 46,  5, 1
                    }, mModelViewMatrixUsed);
        }
    }

    @SmallTest
    public void testClipRect() {
        // The test is currently broken, waiting for the fix
        // new ClipRectTest().run();
    }

    private static class ClipRectTest extends GLStub {
        int mX, mY, mWidth, mHeight;

        @Override
        public void glScissor(int x, int y, int width, int height) {
            mX = x;
            mY = 100 - y - height;  // flip in Y direction
            mWidth = width;
            mHeight = height;
        }

        private void assertClipRect(int x, int y, int width, int height) {
            assertEquals(x, mX);
            assertEquals(y, mY);
            assertEquals(width, mWidth);
            assertEquals(height, mHeight);
        }

        private void assertEmptyClipRect() {
            assertEquals(0, mWidth);
            assertEquals(0, mHeight);
        }

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(100, 100);
            canvas.save();
            assertClipRect(0, 0, 100, 100);

            assertTrue(canvas.clipRect(10, 10, 70, 70));
            canvas.save();
            assertClipRect(10, 10, 60, 60);

            assertTrue(canvas.clipRect(30, 30, 90, 90));
            canvas.save();
            assertClipRect(30, 30, 40, 40);

            assertTrue(canvas.clipRect(40, 40, 60, 90));
            assertClipRect(40, 40, 20, 30);

            assertFalse(canvas.clipRect(30, 30, 70, 40));
            assertEmptyClipRect();
            assertFalse(canvas.clipRect(0, 0, 100, 100));
            assertEmptyClipRect();

            canvas.restore();
            assertClipRect(30, 30, 40, 40);

            canvas.restore();
            assertClipRect(10, 10, 60, 60);

            canvas.restore();
            assertClipRect(0, 0, 100, 100);

            canvas.translate(10, 20, 30);
            assertTrue(canvas.clipRect(10, 10, 70, 70));
            canvas.save();
            assertClipRect(20, 30, 60, 60);
        }
    }

    @SmallTest
    public void testSaveRestore() {
        new SaveRestoreTest().run();
    }

    private static class SaveRestoreTest extends GLStub {
        int mX, mY, mWidth, mHeight;

        @Override
        public void glScissor(int x, int y, int width, int height) {
            mX = x;
            mY = 100 - y - height;  // flip in Y direction
            mWidth = width;
            mHeight = height;
        }

        private void assertClipRect(int x, int y, int width, int height) {
            assertEquals(x, mX);
            assertEquals(y, mY);
            assertEquals(width, mWidth);
            assertEquals(height, mHeight);
        }

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(100, 100);

            canvas.setAlpha(0.7f);
            assertTrue(canvas.clipRect(10, 10, 70, 70));

            canvas.save(canvas.SAVE_FLAG_CLIP);
            canvas.setAlpha(0.6f);
            assertTrue(canvas.clipRect(30, 30, 90, 90));

            canvas.save(canvas.SAVE_FLAG_CLIP | canvas.SAVE_FLAG_ALPHA);
            canvas.setAlpha(0.5f);
            assertTrue(canvas.clipRect(40, 40, 60, 90));

            assertEquals(0.5f, canvas.getAlpha());
            assertClipRect(40, 40, 20, 30);

            canvas.restore();  // now both clipping rect and alpha are restored.
            assertEquals(0.6f, canvas.getAlpha());
            assertClipRect(30, 30, 40, 40);

            canvas.restore();  // now only clipping rect is restored.

            canvas.save(0);
            canvas.save(0);
            canvas.restore();
            canvas.restore();

            assertEquals(0.6f, canvas.getAlpha());
            assertTrue(canvas.clipRect(10, 10, 60, 60));
        }
    }

    @SmallTest
    public void testDrawTexture() {
        new DrawTextureTest().run();
        new DrawTextureMixedTest().run();
    }

    private static class MyTexture extends BasicTexture {
        boolean mIsOpaque;
        int mBindCalled;

        MyTexture(GL11 gl, int id, boolean isOpaque) {
            super(gl, id, STATE_LOADED);
            setSize(1, 1);
            mIsOpaque = isOpaque;
        }

        @Override
        protected void onBind(GLCanvas canvas) {
            mBindCalled++;
        }

        public boolean isOpaque() {
            return mIsOpaque;
        }
    }

    private static class DrawTextureTest extends GLMock {
        int mDrawTexiOESCalled;
        int mDrawArrayCalled;
        int[] mResult = new int[4];

        @Override
        public void glDrawTexiOES(int x, int y, int z,
                int width, int height) {
            mDrawTexiOESCalled++;
        }

        @Override
        public void glDrawArrays(int mode, int first, int count) {
            assertNotNull(mGLVertexPointer);
            assertEquals(GL10.GL_TRIANGLE_STRIP, mode);
            assertEquals(4, count);
            mGLVertexPointer.bindByteBuffer();

            double[] coord = new double[4];
            mGLVertexPointer.getArrayElement(first, coord);
            mResult[0] = (int) coord[0];
            mResult[1] = (int) coord[1];
            mGLVertexPointer.getArrayElement(first + 1, coord);
            mResult[2] = (int) coord[0];
            mResult[3] = (int) coord[1];
            mDrawArrayCalled++;
        }

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(400, 300);
            MyTexture texture = new MyTexture(this, 42, false);  // non-opaque
            MyTexture texture_o = new MyTexture(this, 47, true);  // opaque

            // Draw a non-opaque texture
            canvas.drawTexture(texture, 100, 200, 300, 400);
            assertEquals(42, mGLBindTextureId);
            assertEquals(GL_REPLACE, getTexEnvi(GL_TEXTURE_ENV_MODE));
            assertPremultipliedBlending(this);
            assertFalse(mGLStencilEnabled);

            // Draw an opaque texture
            canvas.drawTexture(texture_o, 100, 200, 300, 400);
            assertEquals(47, mGLBindTextureId);
            assertEquals(GL_REPLACE, getTexEnvi(GL_TEXTURE_ENV_MODE));
            assertFalse(mGLBlendEnabled);

            // Draw a non-opaque texture with alpha = 0.5
            canvas.setAlpha(0.5f);
            canvas.drawTexture(texture, 100, 200, 300, 400);
            assertEquals(42, mGLBindTextureId);
            assertEquals(0x80808080, mGLColor);
            assertEquals(GL_MODULATE, getTexEnvi(GL_TEXTURE_ENV_MODE));
            assertPremultipliedBlending(this);

            // Draw an non-opaque texture with overriden alpha = 1
            canvas.drawTexture(texture, 100, 200, 300, 400, 1f);
            assertEquals(42, mGLBindTextureId);
            assertEquals(GL_REPLACE, getTexEnvi(GL_TEXTURE_ENV_MODE));
            assertPremultipliedBlending(this);

            // Draw an opaque texture with overriden alpha = 1
            canvas.drawTexture(texture_o, 100, 200, 300, 400, 1f);
            assertEquals(47, mGLBindTextureId);
            assertEquals(GL_REPLACE, getTexEnvi(GL_TEXTURE_ENV_MODE));
            assertFalse(mGLBlendEnabled);

            // Draw an opaque texture with overridden alpha = 0.25
            canvas.drawTexture(texture_o, 100, 200, 300, 400, 0.25f);
            assertEquals(47, mGLBindTextureId);
            assertEquals(0x40404040, mGLColor);
            assertEquals(GL_MODULATE, getTexEnvi(GL_TEXTURE_ENV_MODE));
            assertPremultipliedBlending(this);

            // Draw an opaque texture with overridden alpha = 0.125
            // but with some rotation so it will use DrawArray.
            canvas.save();
            canvas.rotate(30, 0, 0, 1);
            canvas.drawTexture(texture_o, 100, 200, 300, 400, 0.125f);
            canvas.restore();
            assertEquals(47, mGLBindTextureId);
            assertEquals(0x20202020, mGLColor);
            assertEquals(GL_MODULATE, getTexEnvi(GL_TEXTURE_ENV_MODE));
            assertPremultipliedBlending(this);

            // We have drawn seven textures above.
            assertEquals(1, mDrawArrayCalled);
            assertEquals(6, mDrawTexiOESCalled);

            // translate and scale does not affect whether we
            // can use glDrawTexiOES, but rotate may.
            canvas.translate(10, 20, 30);
            canvas.drawTexture(texture, 100, 200, 300, 400);
            assertEquals(7, mDrawTexiOESCalled);

            canvas.scale(10, 20, 30);
            canvas.drawTexture(texture, 100, 200, 300, 400);
            assertEquals(8, mDrawTexiOESCalled);

            canvas.rotate(90, 1, 2, 3);
            canvas.drawTexture(texture, 100, 200, 300, 400);
            assertEquals(8, mDrawTexiOESCalled);

            canvas.rotate(-90, 1, 2, 3);
            canvas.drawTexture(texture, 100, 200, 300, 400);
            assertEquals(9, mDrawTexiOESCalled);

            canvas.rotate(180, 0, 0, 1);
            canvas.drawTexture(texture, 100, 200, 300, 400);
            assertEquals(9, mDrawTexiOESCalled);

            canvas.rotate(180, 0, 0, 1);
            canvas.drawTexture(texture, 100, 200, 300, 400);
            assertEquals(10, mDrawTexiOESCalled);

            assertEquals(3, mDrawArrayCalled);

            assertTrue(texture.isLoaded(canvas));
            assertTrue(canvas.unloadTexture(texture));
            assertFalse(texture.isLoaded(canvas));
            canvas.deleteRecycledTextures();

            assertTrue(texture_o.isLoaded(canvas));
            assertTrue(canvas.unloadTexture(texture_o));
            assertFalse(texture_o.isLoaded(canvas));
        }
    }

    private static class DrawTextureMixedTest extends GLMock {

        boolean mTexture2DEnabled0, mTexture2DEnabled1;
        @Override
        public void glEnable(int cap) {
            if (cap == GL_TEXTURE_2D) {
                texture2DEnable(true);
            }
        }

        @Override
        public void glDisable(int cap) {
            if (cap == GL_TEXTURE_2D) {
                texture2DEnable(false);
            }
        }

        private void texture2DEnable(boolean enable) {
            if (mGLActiveTexture == GL_TEXTURE0) {
                mTexture2DEnabled0 = enable;
            } else if (mGLActiveTexture == GL_TEXTURE1) {
                mTexture2DEnabled1 = enable;
            } else {
                fail();
            }
        }

        @Override
        public void glTexEnvfv(int target, int pname, float[] params, int offset) {
            if (target == GL_TEXTURE_ENV && pname == GL_TEXTURE_ENV_COLOR) {
                assertEquals(0.5f, params[offset + 3]);
            }
        }

        @Override
        public void glBindTexture(int target, int texture) {
            if (target == GL_TEXTURE_2D) {
                if (mGLActiveTexture == GL_TEXTURE0) {
                    assertEquals(42, texture);
                } else if (mGLActiveTexture == GL_TEXTURE1) {
                    assertEquals(47, texture);
                } else {
                    fail();
                }
            }
        }

        void run() {
            GLCanvas canvas = new GLCanvasImp(this);
            canvas.setSize(400, 300);
            MyTexture from = new MyTexture(this, 42, false);  // non-opaque
            MyTexture to = new MyTexture(this, 47, true);  // opaque

            canvas.drawMixed(from, to, 0.5f, 100, 200, 300, 400, 1.0f);
            assertEquals(GL_COMBINE, getTexEnvi(GL_TEXTURE_ENV_MODE));
            assertEquals(GL_INTERPOLATE, getTexEnvi(GL_COMBINE_RGB));
            assertEquals(GL_INTERPOLATE, getTexEnvi(GL_COMBINE_ALPHA));
            assertEquals(GL_CONSTANT, getTexEnvi(GL_SRC2_RGB));
            assertEquals(GL_CONSTANT, getTexEnvi(GL_SRC2_ALPHA));
            assertEquals(GL_SRC_ALPHA, getTexEnvi(GL_OPERAND2_RGB));
            assertEquals(GL_SRC_ALPHA, getTexEnvi(GL_OPERAND2_ALPHA));
            assertTrue(mTexture2DEnabled0);
            assertFalse(mTexture2DEnabled1);
            assertFalse(mGLBlendEnabled);

            // The test is currently broken, waiting for the fix
            canvas.setAlpha(0.3f);
            canvas.drawMixed(from, to, 0.5f, 100, 200, 300, 400, 1.0f);
            assertEquals(GL_COMBINE, getTexEnvi(GL_TEXTURE_ENV_MODE));
        }
    }

    @SmallTest
    public void testGetGLInstance() {
        GL11 glStub = new GLStub();
        GLCanvas canvas = new GLCanvasImp(glStub);
        assertSame(glStub, canvas.getGLInstance());
    }

    private static void assertPremultipliedBlending(GLMock mock) {
        assertTrue(mock.mGLBlendFuncCalled > 0);
        assertTrue(mock.mGLBlendEnabled);
        assertEquals(GL11.GL_ONE, mock.mGLBlendFuncSFactor);
        assertEquals(GL11.GL_ONE_MINUS_SRC_ALPHA, mock.mGLBlendFuncDFactor);
    }

    private static void assertMatrixEq(float[] expected, float[] actual) {
        try {
            for (int i = 0; i < 16; i++) {
                assertFloatEq(expected[i], actual[i]);
            }
        } catch (Throwable t) {
            Log.v(TAG, "expected = " + Arrays.toString(expected) +
                    ", actual = " + Arrays.toString(actual));
            fail();
        }
    }

    public static void assertFloatEq(float expected, float actual) {
        if (Math.abs(actual - expected) > 1e-6) {
            Log.v(TAG, "expected: " + expected + ", actual: " + actual);
            fail();
        }
    }
}
