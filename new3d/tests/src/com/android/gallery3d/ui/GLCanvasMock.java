package com.android.gallery3d.ui;

import android.util.Log;

import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL10Ext;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

public class GLCanvasMock extends GLCanvasStub {
    // fillRect
    int mFillRectCalled;
    float mFillRectWidth;
    float mFillRectHeight;
    int mFillRectColor;
    // drawMixed
    int mDrawMixedCalled;
    float mDrawMixedRatio;
    // drawTexture;
    int mDrawTextureCalled;

    private GL11 mGL;

    public GLCanvasMock(GL11 gl) {
        mGL = gl;
    }
    
    public GLCanvasMock() {
        mGL = new GLStub();
    }

    public GL11 getGLInstance() {
        return mGL;
    }

    public void fillRect(float x, float y, float width, float height, int color) {
        mFillRectCalled++;
        mFillRectWidth = width;
        mFillRectHeight = height;
        mFillRectColor = color;
    }

    public void drawTexture(
                BasicTexture texture, int x, int y, int width, int height) {
        mDrawTextureCalled++;
    }

    public void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int w, int h) {
        mDrawMixedCalled++;
        mDrawMixedRatio = ratio;
    }
}
