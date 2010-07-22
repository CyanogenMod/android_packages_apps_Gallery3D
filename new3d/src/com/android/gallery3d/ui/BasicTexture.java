package com.android.gallery3d.ui;

import javax.microedition.khronos.opengles.GL11;

// BasicTexture is a Texture corresponds to a real GL texture.
// The state of a BasicTexture indicates whether its data is loaded to GL memory.
// If a BasicTexture is loaded into GL memory, it has a GL texture id.
abstract class BasicTexture implements Texture {

    protected static final int UNSPECIFIED = -1;

    protected static final int STATE_UNLOADED = 0;
    protected static final int STATE_LOADED = 1;
    protected static final int STATE_ERROR = -1;

    protected GL11 mGL;

    protected int mId;
    protected int mState;

    protected int mWidth = UNSPECIFIED;
    protected int mHeight = UNSPECIFIED;

    private int mTextureWidth;
    private int mTextureHeight;

    protected BasicTexture(GL11 gl, int id, int state) {
        mGL = gl;
        mId = id;
        mState = state;
    }

    /**
     * Sets the content size of this texture. In OpenGL, the actual texture
     * size must be of power of 2, the size of the content may be smaller.
     */
    protected void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        mTextureWidth = Util.nextPowerOf2(width);
        mTextureHeight = Util.nextPowerOf2(height);
    }

    public int getId() {
        return mId;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    // Returns the width rounded to the next power of 2.
    public int getTextureWidth() {
        return mTextureWidth;
    }

    // Returns the height rounded to the next power of 2.
    public int getTextureHeight() {
        return mTextureHeight;
    }

    public void draw(GLCanvas canvas, int x, int y) {
        canvas.drawTexture(this, x, y, mWidth, mHeight);
    }

    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        canvas.drawTexture(this, x, y, w, h);
    }

    // onBind is called before GLCanvas binds this texture.
    // It should make sure the data is uploaded to GL memory.
    abstract protected void onBind(GLCanvas canvas);

    public boolean isLoaded(GLCanvas canvas) {
        return mState == STATE_LOADED && mGL == canvas.getGLInstance();
    }
}
