package com.android.gallery3d.ui;

import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import junit.framework.TestCase;

import javax.microedition.khronos.opengles.GL11;

@SmallTest
public class TextureTest extends TestCase {
    private static final String TAG = "TextureTest";

    class MyBasicTexture extends BasicTexture {
        int mOnBindCalled;
        int mOpaqueCalled;

        MyBasicTexture(GL11 gl, int id) {
            super(gl, id, BasicTexture.STATE_UNLOADED);
        }

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
        canvas.setSize(400, 300);
        MyBasicTexture texture = new MyBasicTexture(glStub, 47);

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
}
