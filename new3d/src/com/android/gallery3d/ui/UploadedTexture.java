package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.opengl.GLUtils;

import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

abstract class UploadedTexture extends BasicTexture {

    @SuppressWarnings("unused")
    private static final String TAG = "Texture";
    private boolean mOpaque;

    protected UploadedTexture() {
        super(null, 0, STATE_UNLOADED);
    }

    @Override
    public int getWidth() {
        if (mWidth == UNSPECIFIED) getBitmap();
        return mWidth;
    }

    @Override
    public int getHeight() {
        if (mWidth == UNSPECIFIED) getBitmap();
        return mHeight;
    }

    protected abstract Bitmap getBitmap();

    protected abstract void freeBitmap(Bitmap bitmap);

    private void uploadToGL(GL11 gl) throws GLOutOfMemoryException {
        Bitmap bitmap = getBitmap();
        int glError = GL11.GL_NO_ERROR;
        if (bitmap != null) {
            int[] textureId = new int[1];
            try {
                // Define a vertically flipped crop rectangle for
                // OES_draw_texture.
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] cropRect = {0,  height, width, -height};

                // Upload the bitmap to a new texture.
                gl.glGenTextures(1, textureId, 0);
                gl.glBindTexture(GL11.GL_TEXTURE_2D, textureId[0]);
                gl.glTexParameteriv(GL11.GL_TEXTURE_2D,
                        GL11Ext.GL_TEXTURE_CROP_RECT_OES, cropRect, 0);
                gl.glTexParameteri(GL11.GL_TEXTURE_2D,
                        GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP_TO_EDGE);
                gl.glTexParameteri(GL11.GL_TEXTURE_2D,
                        GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP_TO_EDGE);
                gl.glTexParameterf(GL11.GL_TEXTURE_2D,
                        GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                gl.glTexParameterf(GL11.GL_TEXTURE_2D,
                        GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                int widthExt = Util.nextPowerOf2(width);
                int heightExt = Util.nextPowerOf2(height);
                int format = GLUtils.getInternalFormat(bitmap);
                mOpaque =
                        (format == GL11.GL_RGB || format == GL11.GL_LUMINANCE);
                int type = GLUtils.getType(bitmap);


                mTextureWidth = widthExt;
                mTextureHeight = heightExt;
                gl.glTexImage2D(GL11.GL_TEXTURE_2D, 0, format,
                        widthExt, heightExt, 0, format, type, null);
                GLUtils.texSubImage2D(
                        GL11.GL_TEXTURE_2D, 0, 0, 0, bitmap, format, type);
            } finally {
                freeBitmap(bitmap);
            }
            if (glError == GL11.GL_OUT_OF_MEMORY) {
                throw new GLOutOfMemoryException();
            }
            if (glError != GL11.GL_NO_ERROR) {
                mId = 0;
                mState = STATE_UNLOADED;
                throw new RuntimeException(
                        "Texture upload fail, glError " + glError);
            } else {
                // Update texture state.
                mGL = gl;
                mId = textureId[0];
                mState = UploadedTexture.STATE_LOADED;
            }
        } else {
            mState = STATE_ERROR;
            throw new RuntimeException("Texture load fail, no bitmap");
        }
    }

    @Override
    protected boolean bind(GLCanvas canvas) {
        GL11 gl = canvas.getGLInstance();
        if (mState == UploadedTexture.STATE_UNLOADED || mGL != gl) {
            mState = UploadedTexture.STATE_UNLOADED;
            try {
                uploadToGL(gl);
            } catch (GLOutOfMemoryException e) {
                canvas.handleLowMemory();
                return false;
            }
        } else {
            gl.glBindTexture(GL11.GL_TEXTURE_2D, getId());
        }
        return true;
    }

    public boolean isOpaque() {
        return mOpaque ;
    }
}
