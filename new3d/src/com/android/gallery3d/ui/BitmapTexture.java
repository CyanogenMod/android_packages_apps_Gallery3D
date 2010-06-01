package com.android.gallery3d.ui;

import android.graphics.Bitmap;

public class BitmapTexture extends UploadedTexture {
    protected Bitmap mBitmap;

    public BitmapTexture(Bitmap bitmap) {
        mBitmap = bitmap;
        setSize(mBitmap.getWidth(), mBitmap.getHeight());
    }

    @Override
    protected void freeBitmap(Bitmap bitmap) {
        // Do nothing.
    }

    @Override
    protected Bitmap getBitmap() {
        return mBitmap;
    }
}
