package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.graphics.RectF;

import javax.microedition.khronos.opengles.GL11;

public class GLCanvasStub implements GLCanvas {
    public void setSize(int width, int height) {}
    public void clearBuffer() {}
    public void setCurrentAnimationTimeMillis(long time) {}
    public long currentAnimationTimeMillis() {
        throw new UnsupportedOperationException();
    }
    public void setAlpha(float alpha) {}
    public float getAlpha() {
        throw new UnsupportedOperationException();
    }
    public void multiplyAlpha(float alpha) {}
    public void translate(float x, float y, float z) {}
    public void scale(float sx, float sy, float sz) {}
    public void rotate(float angle, float x, float y, float z) {}
    public boolean clipRect(int left, int top, int right, int bottom) {
        throw new UnsupportedOperationException();
    }
    public int save() {
        throw new UnsupportedOperationException();
    }
    public int save(int saveFlags) {
        throw new UnsupportedOperationException();
    }
    public void restore() {}
    public void drawLine(int x1, int y1, int x2, int y2, int color) {}
    public void fillRect(float x, float y, float width, float height, int color) {}
    public void drawTexture(
            BasicTexture texture, int x, int y, int width, int height) {}
    public void drawNinePatch(
            NinePatchTexture tex, int x, int y, int width, int height) {}
    public void drawTexture(BasicTexture texture,
            int x, int y, int width, int height, float alpha) {}
    public void drawTexture(BasicTexture texture, RectF source, RectF target) {}
    public void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int w, int h) {}
    public void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int width, int height, float alpha) {}
    public BasicTexture copyTexture(int x, int y, int width, int height) {
        throw new UnsupportedOperationException();
    }
    public GL11 getGLInstance() {
        throw new UnsupportedOperationException();
    }
    public boolean unloadTexture(BasicTexture texture) {
        throw new UnsupportedOperationException();
    }
    public void deleteRecycledTextures() {}
}
