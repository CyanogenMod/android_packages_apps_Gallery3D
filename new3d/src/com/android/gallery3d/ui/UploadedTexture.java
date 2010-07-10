package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.opengl.GLUtils;

import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

abstract class UploadedTexture extends BasicTexture {

    @SuppressWarnings("unused")
    private static final String TAG = "Texture";
    private boolean mOpaque;
    private boolean mContentValid = true;

    protected Bitmap mBitmap;

    protected UploadedTexture() {
        super(null, 0, STATE_UNLOADED);
    }

    private Bitmap getBitmap() {
        if (mBitmap == null) {
            mBitmap = onGetBitmap();
            if (mWidth == UNSPECIFIED) {
                setSize(mBitmap.getWidth(), mBitmap.getHeight());
            } else if (mWidth != mBitmap.getWidth()
                    || mHeight != mBitmap.getHeight()) {
                throw new IllegalStateException("cannot change content size");
            }
        }
        return mBitmap;
    }

    private void freeBitmap() {
        Util.Assert(mBitmap != null);
        onFreeBitmap(mBitmap);
        mBitmap = null;
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

    protected abstract Bitmap onGetBitmap();

    protected abstract void onFreeBitmap(Bitmap bitmap);

    protected void invalidateContent() {
        if (mBitmap != null) freeBitmap();
        if (mState == STATE_LOADED) mContentValid = false;
    }

    /**
     * Whether the content on GPU is valid.
     */
    public boolean isContentValid(GLCanvas canvas) {
        return isLoaded(canvas) && mContentValid;
    }

    /**
     * Updates the content on GPU's memory.
     * @param canvas
     */
    public void updateContent(GLCanvas canvas) {
        if (!isLoaded(canvas)) {
            uploadToGL(canvas.getGLInstance());
        } else if (!mContentValid) {
            Bitmap bitmap = getBitmap();
            int format = GLUtils.getInternalFormat(bitmap);
            int type = GLUtils.getType(bitmap);
            mGL.glBindTexture(GL11.GL_TEXTURE_2D, mId);
            GLUtils.texSubImage2D(
                    GL11.GL_TEXTURE_2D, 0, 0, 0, bitmap, format, type);
            freeBitmap();
            mContentValid = true;
        }
    }

    private void uploadToGL(GL11 gl) {
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
                freeBitmap();
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
    protected void bind(GLCanvas canvas) {
        updateContent(canvas);
        canvas.bindTexture(this);
    }

    public boolean isOpaque() {
        return mOpaque ;
    }

    public void recycle() {
        if (mBitmap != null) freeBitmap();
    }
}
