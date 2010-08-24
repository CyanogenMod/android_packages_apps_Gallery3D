package com.android.gallery3d.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.util.DisplayMetrics;

import java.nio.charset.Charset;


public class Utils {
    public static final int UNCONSTRAINED = -1;
    private static final String TAG = "Utils";

    private static final long POLY64REV = 0x95AC9329AC4BC9B5L;
    private static final long INITIALCRC = 0xFFFFFFFFFFFFFFFFL;

    private static boolean sInit = false;
    private static long[] sCrcTable = new long[256];
    private static Charset sUtf8Codec = Charset.forName("utf-8");

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
                ? Utils.nextPowerOf2(initialSize)
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

    // Throws AssertionError if the input is false.
    public static void Assert(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    // Throws AssertionError if the input is false.
    public static void Assert(boolean cond, String message) {
        if (!cond) {
            throw new AssertionError(message);
        }
    }

    // Throws NullPointerException if the input is null.
    public static <T> T checkNotNull(T object) {
        if (object == null) throw new NullPointerException();
        return object;
    }

    // Returns true if two input Object are both null or equal
    // to each other.
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a == null ? false : a.equals(b));
    }

    // Returns true if the input is power of 2.
    // Throws IllegalArgumentException if the input is <= 0.
    public static boolean isPowerOf2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return (n & -n) == n;
    }

    // Returns the next power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0 or
    // the answer overflows.
    public static int nextPowerOf2(int n) {
        if (n <= 0 || n > (1 << 30)) throw new IllegalArgumentException();
        n -= 1;
        n |= n >> 16;
        n |= n >> 8;
        n |= n >> 4;
        n |= n >> 2;
        n |= n >> 1;
        return n + 1;
    }

    // Returns the euclidean distance between (x, y) and (sx, sy).
    public static float distance(float x, float y, float sx, float sy) {
        float dx = x - sx;
        float dy = y - sy;
        return (float) Math.hypot(dx, dy);
    }

    // Returns the input value x clamped to the range [min, max].
    public static int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    // Returns the input value x clamped to the range [min, max].
    public static float clamp(float x, float min, float max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    public synchronized static float dpToPixel(Context context, float dp) {
        if (Utils.sPixelDensity < 0) {
            DisplayMetrics metrics = new DisplayMetrics();
            ((Activity) context).getWindowManager()
                    .getDefaultDisplay().getMetrics(metrics);
            Utils.sPixelDensity = metrics.density;
        }
        return Utils.sPixelDensity * dp;
    }

    static float sPixelDensity = -1f;

    public static int dpToPixel(Context context, int dp) {
        return Math.round(dpToPixel(context, (float) dp));
    }

    public static boolean isOpaque(int color) {
        return color >>> 24 == 0xFF;
    }

    public static <T> void swap(T[] array, int i, int j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    /**
     * A function thats returns a 64-bit crc for string
     *
     * @param in: input string
     * @return 64-bit crc value
     */
    public static final long crc64Long(String in) {
        if (in == null || in.length() == 0) {
            return 0;
        }
        // http://bioinf.cs.ucl.ac.uk/downloads/crc64/crc64.c
        long crc = INITIALCRC, part;
        if (!sInit) {
            for (int i = 0; i < 256; i++) {
                part = i;
                for (int j = 0; j < 8; j++) {
                    int value = ((int) part & 1);
                    if (value != 0)
                        part = (part >> 1) ^ POLY64REV;
                    else
                        part >>= 1;
                }
                sCrcTable[i] = part;
            }
            sInit = true;
        }

        byte[] buffer = getBytesInUtf8(in);
        for (int k = 0, n = buffer.length; k < n; ++k) {
            crc = sCrcTable[(((int) crc) ^ buffer[k]) & 0xff] ^ (crc >> 8);
        }
        return crc;
    }

    public static byte[] getBytesInUtf8(String in) {
        return sUtf8Codec.encode(in).array();
    }
}
