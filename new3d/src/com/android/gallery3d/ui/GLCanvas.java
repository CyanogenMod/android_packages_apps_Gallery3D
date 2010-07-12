package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.graphics.RectF;

import java.util.Collection;

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

    // Bindss the current drawing color to the canvas. It will be used for
    // drawLine and fillRect. The format is 0xaarrggbb.
    public void bindColor(int color);

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

    // Draws a line using current drawing color from (x1, y1) to (x2, y2).
    // (Both end points are included).
    public void drawLine(int x1, int y1, int x2, int y2);

    // Fills the specified rectange with the current drawing color or
    // the color of texture.
    public void fillRect(int x, int y, int width, int height);
    public void fillRect(Rect r);

    // Sets the texture coordinates for fillRect calls.
    public void setTextureCoords(RectF source);

    // Draws a texture to the specified rectangle.
    public void drawTexture(
            BasicTexture texture, int x, int y, int width, int height);
    public void drawNinePatch(
            NinePatchTexture tex, int x, int y, int width, int height);

    // Draws a texture to the specified rectangle. The "alpha" parameter
    // overrides the current drawing alpha value.
    public void drawTexture(BasicTexture texture,
            int x, int y, int width, int height, float alpha);

    // Draw two textures to the specified rectange. The actual texture used is
    // from * (1 - ratio) + to * ratio
    public void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int w, int h);

    public void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int width, int height, float alpha);

    // Copies the specified rectangle to the RawTexture.
    public void copyTexture2D(
            RawTexture texture, int x, int y, int width, int height);

    // TODO: Remove this or document it.
    public void releaseTextures(Collection<? extends BasicTexture> c);

    // Gets the underlying GL instance. This is used when
    public GL11 getGLInstance();

    // Binds the texture to the canvas for the following drawing calls. This
    // function should only be called in Texture.
    public void bindTexture(BasicTexture texture);
}
