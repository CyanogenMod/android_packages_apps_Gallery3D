package com.cooliris.media;

import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLUtils;

public abstract class CanvasLayer extends Layer {
    private int mTextureId;
    private int mTextureWidth;
    private int mTextureHeight;
    private float mNormalizedWidth;
    private float mNormalizedHeight;

    private final Canvas mCanvas = new Canvas();
    private final Bitmap.Config mBitmapConfig;
    private Bitmap mBitmap = null;
    private boolean mNeedsDraw = false;
    private boolean mNeedsResize = false;

    public CanvasLayer(Bitmap.Config bitmapConfig) {
        mBitmapConfig = bitmapConfig;
    }

    public void setNeedsDraw() {
        mNeedsDraw = true;
    }

    public final float getNormalizedWidth() {
        return mNormalizedWidth;
    }

    public final float getNormalizedHeight() {
        return mNormalizedHeight;
    }

    @Override
    protected void onSizeChanged() {
        mNeedsResize = true;
    }

    @Override
    protected void onSurfaceCreated(RenderView view, GL11 gl) {
        mTextureId = 0;
    }

    protected boolean bind(GL11 gl) {
        int width = (int) mWidth;
        int height = (int) mHeight;
        int textureId = mTextureId;
        int textureWidth = mTextureWidth;
        int textureHeight = mTextureHeight;
        Canvas canvas = mCanvas;
        Bitmap bitmap = mBitmap;
        boolean updateSubTexture = true;

        if (mNeedsResize) {
            // Clear the resize flag and mark as needing draw.
            mNeedsResize = false;
            mNeedsDraw = true;

            // Compute the power-of-2 padded size for the texture.
            int newTextureWidth = Shared.nextPowerOf2(width);
            int newTextureHeight = Shared.nextPowerOf2(height);

            // Reallocate the bitmap only if the padded size has changed.
            if (textureWidth != newTextureWidth || textureHeight != newTextureHeight) {
                // Mark that the partial texture update should not be used.
                updateSubTexture = false;

                // Allocate a texture if needed.
                if (textureId == 0) {
                    int[] textureIdOut = new int[1];
                    gl.glGenTextures(1, textureIdOut, 0);
                    textureId = textureIdOut[0];
                    mTextureId = textureId;

                    // Set texture parameters.
                    gl.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
                    gl.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP_TO_EDGE);
                    gl.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP_TO_EDGE);
                    gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                    gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                }

                // Set the new texture width and height.
                textureWidth = newTextureWidth;
                textureHeight = newTextureHeight;
                mTextureWidth = newTextureWidth;
                mTextureHeight = newTextureHeight;
                mNormalizedWidth = (float) width / textureWidth;
                mNormalizedHeight = (float) height / textureHeight;

                // Recycle the existing bitmap and create a new one.
                if (bitmap != null)
                    bitmap.recycle();
                bitmap = Bitmap.createBitmap(textureWidth, textureHeight, mBitmapConfig);
                canvas.setBitmap(bitmap);
                mBitmap = bitmap;
            }
        }

        // Bind the texture to the context.
        if (textureId == 0)
            return false;
        gl.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Redraw the contents of the texture if needed.
        if (mNeedsDraw) {
            mNeedsDraw = false;
            draw(canvas, bitmap, width, height);
            if (updateSubTexture) {
                GLUtils.texSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, bitmap);
            } else {
                int[] cropRect = { 0, height, width, -height };
                gl.glTexParameteriv(GL11.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES, cropRect, 0);
                GLUtils.texImage2D(GL11.GL_TEXTURE_2D, 0, bitmap, 0);
            }
        }

        return true;
    }

    protected abstract void draw(Canvas canvas, Bitmap backing, int width, int height);
}
