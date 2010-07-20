package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.graphics.RectF;

import javax.microedition.khronos.opengles.GL11;

//
// GLCanvas gives a convenient interface to draw using OpenGL.
//
// When a rectangle is specified in this interface, it means the region
// [x, x+width) * [y, y+height)
//
public interface GLCanvas {
    // Tells GLCanvas the size of the underlying GL surface. This should be
    // called before first drawing and when the size of GL surface is changed.
    // This is called by GLRootView and should not be called by the clients
    // who only want to draw on the GLCanvas. Both width and height must be
    // nonnegative.
    public void setSize(int width, int height);

    // Clear the drawing buffers. This should only be used by GLRootView.
    public void clearBuffer();

    // This is the time value used to calculate the animation in the current
    // frame. The "set" function should only called by GLRootView, and the
    // "time" parameter must be nonnegative.
    public void setCurrentAnimationTimeMillis(long time);
    public long currentAnimationTimeMillis();

    // Sets and gets the current alpha, alpha must be in [0, 1].
    public void setAlpha(float alpha);
    public float getAlpha();

    // (current alpha) = (current alpha) * alpha
    public void multiplyAlpha(float alpha);

    // Change the current transform matrix.
    public void translate(float x, float y, float z);
    public void scale(float sx, float sy, float sz);
    public void rotate(float angle, float x, float y, float z);

    // Modifies the current clip with the specified rectangle.
    // (current clip) = (current clip) intersect (specified rectangle).
    // Returns true if the result clip is non-empty.
    public boolean clipRect(int left, int top, int right, int bottom);

    // Pushes the configuration state (matrix, alpha, and clip) onto
    // a private stack.
    public int save();

    // Same as save(), but only save those specified in saveFlags.
    public int save(int saveFlags);

    public static final int SAVE_FLAG_ALL = 0xFFFFFFFF;
    public static final int SAVE_FLAG_CLIP = 0x01;
    public static final int SAVE_FLAG_ALPHA = 0x02;
    public static final int SAVE_FLAG_MATRIX = 0x04;

    // Pops from the top of the stack as current configuration state (matrix,
    // alpha, and clip). This call balances a previous call to save(), and is
    // used to remove all modifications to the configuration state since the
    // last save call.
    public void restore();

    // Draws a line using the specified color from (x1, y1) to (x2, y2).
    // (Both end points are included).
    public void drawLine(int x1, int y1, int x2, int y2, int color);

    // Fills the specified rectange with the specified color.
    public void fillRect(float x, float y, float width, float height, int color);

    // Draws a texture to the specified rectangle.
    public void drawTexture(
            BasicTexture texture, int x, int y, int width, int height);
    public void drawNinePatch(
            NinePatchTexture tex, int x, int y, int width, int height);

    // Draws a texture to the specified rectangle. The "alpha" parameter
    // overrides the current drawing alpha value.
    public void drawTexture(BasicTexture texture,
            int x, int y, int width, int height, float alpha);

    // Draws a the source rectangle part of the texture to the target rectangle.
    public void drawTexture(BasicTexture texture, RectF source, RectF target);

    // Draw two textures to the specified rectange. The actual texture used is
    // from * (1 - ratio) + to * ratio
    public void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int w, int h);

    public void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int width, int height, float alpha);

    // Return a texture copied from the specified rectangle.
    public BasicTexture copyTexture(int x, int y, int width, int height);

    // Gets the underlying GL instance. This is used only when direct access to
    // GL is needed.
    public GL11 getGLInstance();

    // Unloads the specified texture from the canvas. The resource allocated
    // to draw the texture will be released. The specified texture will return
    // to the unloaded state.
    public boolean unloadTexture(BasicTexture texture);

    // Delete the textures in GL side. This function should only be called in
    // GL thread.
    public void deleteRecycledTextures();
}
