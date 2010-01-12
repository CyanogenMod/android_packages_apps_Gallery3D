package com.cooliris.media;

import android.graphics.Bitmap;

public class BitmapTexture extends Texture {
    // A simple flexible texture class that enables a Texture from a bitmap.
    final Bitmap mBitmap;

    BitmapTexture(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    @Override
    protected Bitmap load(RenderView view) {
        return mBitmap;
    }

}
