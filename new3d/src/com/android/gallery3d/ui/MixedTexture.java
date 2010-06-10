package com.android.gallery3d.ui;



public class MixedTexture implements Texture {
    private BasicTexture mSource;
    private BasicTexture mDestination;
    private float mMixRatio = 1.0f;
    private final int mWidth;
    private final int mHeight;

    public MixedTexture(BasicTexture texture) {
        mDestination = texture;
        mWidth = texture.getWidth();
        mHeight = texture.getHeight();
    }

    public BasicTexture setNewDestination(BasicTexture texture) {
        if (texture.getWidth() != mWidth || texture.getHeight() != mHeight) {
            throw new IllegalArgumentException();
        }
        mMixRatio = 0f;
        BasicTexture result = mSource;
        mSource = mDestination;
        mDestination = texture;
        return result;
    }

    public void setMixtureRatio(float ratio) {
        if (ratio > 1 || ratio < 0) {
            throw new IllegalArgumentException();
        }
        mMixRatio = ratio;
    }

    public void draw(GLRootView root, int x, int y) {
        draw(root, x, y, mWidth, mHeight);
    }

    public void draw(GLRootView root, int x, int y, int w, int h) {
        if (mMixRatio >= 1 || mSource == null) {
            mDestination.draw(root, x, y, w, h);
        } else if (mMixRatio <= 0) {
            mSource.draw(root, x, y, w, h);
        } else {
            root.drawMixed(mSource, mDestination, mMixRatio, x, y, w, h);
        }
    }

    public boolean isOpaque() {
        return true;
    }
}