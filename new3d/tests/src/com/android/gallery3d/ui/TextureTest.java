package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

import javax.microedition.khronos.opengles.GL11;

@SmallTest
public class TextureTest extends TestCase {
    private static final String TAG = "TextureTest";

    class MyBasicTexture extends BasicTexture {
        int mOnBindCalled;
        int mOpaqueCalled;

        MyBasicTexture(GLCanvas canvas, int id) {
            super(canvas, id, BasicTexture.STATE_UNLOADED);
        }

        @Override
        protected void onBind(GLCanvas canvas) {
            mOnBindCalled++;
        }

        public boolean isOpaque() {
            mOpaqueCalled++;
            return true;
        }

        void upload() {
            mState = STATE_LOADED;
        }
    }

    @SmallTest
    public void testBasicTexture() {
        GL11 glStub = new GLStub();
        GLCanvas canvas = new GLCanvasImp(glStub);
        MyBasicTexture texture = new MyBasicTexture(canvas, 47);

        assertEquals(47, texture.getId());
        texture.setSize(1, 1);
        assertEquals(1, texture.getWidth());
        assertEquals(1, texture.getHeight());
        assertEquals(1, texture.getTextureWidth());
        assertEquals(1, texture.getTextureHeight());
        texture.setSize(3, 5);
        assertEquals(3, texture.getWidth());
        assertEquals(5, texture.getHeight());
        assertEquals(4, texture.getTextureWidth());
        assertEquals(8, texture.getTextureHeight());

        assertFalse(texture.isLoaded(canvas));
        texture.upload();
        assertTrue(texture.isLoaded(canvas));

        // For a different GL, it's not loaded.
        GLCanvas canvas2 = new GLCanvasImp(new GLStub());
        assertFalse(texture.isLoaded(canvas2));

        assertEquals(0, texture.mOnBindCalled);
        assertEquals(0, texture.mOpaqueCalled);
        texture.draw(canvas, 100, 200, 1, 1);
        assertEquals(1, texture.mOnBindCalled);
        assertEquals(1, texture.mOpaqueCalled);
        texture.draw(canvas, 0, 0);
        assertEquals(2, texture.mOnBindCalled);
        assertEquals(2, texture.mOpaqueCalled);
    }

    @SmallTest
    public void testRawTexture() {
        GL11 glStub = new GLStub();
        GLCanvas canvas = new GLCanvasImp(glStub);
        RawTexture texture = RawTexture.newInstance(canvas);
        texture.onBind(canvas);

        GLCanvas canvas2 = new GLCanvasImp(new GLStub());
        try {
            texture.onBind(canvas2);
            fail();
        } catch (RuntimeException ex) {
            // expected.
        }

        assertTrue(texture.isOpaque());
    }

    @SmallTest
    public void testColorTexture() {
        GLCanvasMock canvas = new GLCanvasMock();
        ColorTexture texture = new ColorTexture(0x12345678);

        texture.setSize(42, 47);
        assertEquals(texture.getWidth(), 42);
        assertEquals(texture.getHeight(), 47);
        assertEquals(0, canvas.mFillRectCalled);
        texture.draw(canvas, 0, 0);
        assertEquals(1, canvas.mFillRectCalled);
        assertEquals(0x12345678, canvas.mFillRectColor);
        assertEquals(42f, canvas.mFillRectWidth);
        assertEquals(47f, canvas.mFillRectHeight);
        assertFalse(texture.isOpaque());
        assertTrue(new ColorTexture(0xFF000000).isOpaque());
    }

    private class MyUploadedTexture extends UploadedTexture {
        int mGetCalled;
        int mFreeCalled;
        Bitmap mBitmap;
        @Override
        protected Bitmap onGetBitmap() {
            mGetCalled++;
            Config config = Config.ARGB_8888;
            mBitmap = Bitmap.createBitmap(47, 42, config);
            return mBitmap;
        }
        @Override
        protected void onFreeBitmap(Bitmap bitmap) {
            mFreeCalled++;
            assertSame(mBitmap, bitmap);
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    @SmallTest
    public void testUploadedTexture() {
        GL11 glStub = new GLStub();
        GLCanvas canvas = new GLCanvasImp(glStub);
        MyUploadedTexture texture = new MyUploadedTexture();

        // draw it and the bitmap should be fetched.
        assertEquals(0, texture.mFreeCalled);
        assertEquals(0, texture.mGetCalled);
        texture.draw(canvas, 0, 0);
        assertEquals(1, texture.mGetCalled);
        assertTrue(texture.isLoaded(canvas));
        assertTrue(texture.isContentValid(canvas));

        // invalidate content and it should be freed.
        texture.invalidateContent();
        assertFalse(texture.isContentValid(canvas));
        assertEquals(1, texture.mFreeCalled);
        assertTrue(texture.isLoaded(canvas));  // But it's still loaded

        // draw it again and the bitmap should be fetched again.
        texture.draw(canvas, 0, 0);
        assertEquals(2, texture.mGetCalled);
        assertTrue(texture.isLoaded(canvas));
        assertTrue(texture.isContentValid(canvas));

        // recycle the texture and it should be freed again.
        texture.recycle();
        assertEquals(2, texture.mFreeCalled);
        // TODO: these two are broken and waiting for fix.
        //assertFalse(texture.isLoaded(canvas));
        //assertFalse(texture.isContentValid(canvas));
    }

    class MyTextureForMixed extends BasicTexture {
        MyTextureForMixed(GLCanvas canvas, int id) {
            super(canvas, id, BasicTexture.STATE_UNLOADED);
        }

        @Override
        protected void onBind(GLCanvas canvas) {
        }

        public boolean isOpaque() {
            return true;
        }
    }

    @SmallTest
    public void testBitmapTexture() {
        Config config = Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(47, 42, config);
        assertFalse(bitmap.isRecycled());
        BitmapTexture texture = new BitmapTexture(bitmap);
        texture.recycle();
        assertFalse(bitmap.isRecycled());
        bitmap.recycle();
        assertTrue(bitmap.isRecycled());
    }
}
