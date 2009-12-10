package com.cooliris.media;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

public final class SimpleStringTexture extends Texture {

    private final String mString;
    private final StringTexture.Config mConfig;
    private float mBaselineHeight = 0.0f;

    SimpleStringTexture(String string, StringTexture.Config config) {
        mString = string;
        mConfig = config;
    }

    public float getBaselineHeight() {
        return mBaselineHeight;
    }

    @Override
    public boolean isCached() {
        return true;
    }

    @Override
    protected Bitmap load(RenderView view) {
        // Configure paint.
        StringTexture.Config config = mConfig;
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Shared.argb(config.a, config.r, config.g, config.b));
        paint.setShadowLayer(config.shadowRadius, 0f, 0f, Color.BLACK);
        paint.setTypeface(config.bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        paint.setTextSize(config.fontSize);
        paint.setUnderlineText(config.underline);
        paint.setStrikeThruText(config.strikeThrough);
        if (config.italic) {
            paint.setTextSkewX(-0.25f);
        }

        // Measure string.
        String string = mString;
        Rect bounds = new Rect();
        paint.getTextBounds(string, 0, string.length(), bounds);

        // Get font metrics.
        Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
        int height = metrics.bottom - metrics.top;

        // Draw string into bitmap with a 1px margin for anti-aliasing.
        // Ensure baseline alignment with other strings of the same size.
        int padding = 1 + config.shadowRadius;
        Bitmap bitmap = Bitmap
                .createBitmap(bounds.width() + padding + padding, height + padding + padding, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.translate(padding, padding - metrics.ascent);
        canvas.drawText(string, 0, 0, paint);

        mBaselineHeight = padding + metrics.bottom;

        return bitmap;
    }

}
