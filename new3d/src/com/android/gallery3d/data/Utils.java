package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;

import com.android.gallery3d.ui.Util;

public class Utils {
    public static final int UNCONSTRAINED = -1;
    private static final String TAG = "Utils";

    /*
     * Compute the sample size as a function of minSideLength
     * and maxNumOfPixels.
     * minSideLength is used to specify that minimal width or height of a
     * bitmap.
     * maxNumOfPixels is used to specify the maximal size in pixels that is
     * tolerable in terms of memory usage.
     *
     * The function returns a sample size based on the constraints.
     * Both size and minSideLength can be passed in as UNCONSTRAINED,
     * which indicates no care of the corresponding constraint.
     * The functions prefers returning a sample size that
     * generates a smaller bitmap, unless minSideLength = UNCONSTRAINED.
     *
     * Also, the function rounds up the sample size to a power of 2 or multiple
     * of 8 because BitmapFactory only honors sample size this way.
     * For example, BitmapFactory downsamples an image by 2 even though the
     * request is 3. So we round up the sample size to avoid OOM.
     */
    public static int computeSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(
                options, minSideLength, maxNumOfPixels);

        return initialSize <= 8
                ? Util.nextPowerOf2(initialSize)
                : (initialSize + 7) / 8 * 8;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        if (maxNumOfPixels == UNCONSTRAINED && minSideLength == UNCONSTRAINED) {
            return 1;
        }

        int w = options.outWidth;
        int h = options.outHeight;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 :
                (int) Math.ceil(Math.sqrt((double) (w * h) / maxNumOfPixels));

        if (minSideLength == UNCONSTRAINED) {
            return lowerBound;
        } else {
            int sampeSize = Math.min(w / minSideLength, h / minSideLength);
            return Math.max(sampeSize, lowerBound);
        }
    }

    public static Bitmap resize(Bitmap original, int targetPixels) {
        int width = original.getWidth();
        int height = original.getHeight();
        float scale = (float) Math.sqrt(
                (double) targetPixels / (width * height));
        if (scale >= 1.0f) return original;

        int scaledWidth = (int) (width * scale + 0.5f);
        int scaledHeight = (int) (height * scale + 0.5f);
        Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight,
                original.hasAlpha() ? Config.ARGB_8888 : Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale((float) scaledWidth / width, (float) scaledHeight / height);
        canvas.drawBitmap(original, 0, 0, null);
        original.recycle();
        return bitmap;
    }

}
