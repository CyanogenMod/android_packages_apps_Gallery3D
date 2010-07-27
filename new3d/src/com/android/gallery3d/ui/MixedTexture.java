/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.ui;

// MixedTexture is a texture whose color is mixed from two textures: source
// and destination:
//   color = (1 - r) * source + r * destination
// The mix ratio r is set by setMixtureRatio().
// The two textures must have the same size.
//
// Initially only the destination texture is set in the constructor, and there
// is no source texture. When setNewDestination() is called, the original
// destination texture becomes source texture, and the new destination texture
// is set.
//
// If there is no source texture, hasSource() returns false, and draw() just
// draws the destination texture.
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

    public void draw(GLCanvas canvas, int x, int y) {
        draw(canvas, x, y, mWidth, mHeight);
    }

    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        if (mMixRatio >= 1 || mSource == null) {
            mDestination.draw(canvas, x, y, w, h);
        } else if (mMixRatio <= 0) {
            mSource.draw(canvas, x, y, w, h);
        } else {
            canvas.drawMixed(mSource, mDestination, mMixRatio, x, y, w, h);
        }
    }

    public boolean hasSource() {
        return mSource != null;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public boolean isOpaque() {
        return true;
    }
}
