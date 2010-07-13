package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class DrawableTexture extends CanvasTexture {

    private final Drawable mDrawable;

    public DrawableTexture(Drawable drawable) {
        super(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        mDrawable = drawable;
    }

    @Override
    protected void onDraw(Canvas canvas, Bitmap backing) {
        mDrawable.setBounds(0, 0, mWidth, mHeight);
        mDrawable.draw(canvas);
    }
}
