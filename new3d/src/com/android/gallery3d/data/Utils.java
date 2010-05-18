package com.android.gallery3d.data;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

public class Utils {
    private static final int UNCONSTRAINED = -1;
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
     * Both size and minSideLength can be passed in as IImage.UNCONSTRAINED,
     * which indicates no care of the corresponding constraint.
     * The functions prefers returning a sample size that
     * generates a smaller bitmap, unless minSideLength = IImage.UNCONSTRAINED.
     *
     * Also, the function rounds up the sample size to a power of 2 or multiple
     * of 8 because BitmapFactory only honors sample size this way.
     * For example, BitmapFactory downsamples an image by 2 even though the
     * request is 3. So we round up the sample size to avoid OOM.
     */
    public static int computeSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8 ) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    public static int computeInitialSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 :
                (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == UNCONSTRAINED) ? 128 :
                (int) Math.min(Math.floor(w / minSideLength),
                Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if ((maxNumOfPixels == UNCONSTRAINED) &&
                (minSideLength == UNCONSTRAINED)) {
            return 1;
        } else if (minSideLength == UNCONSTRAINED) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static Bitmap generateBitmap(String filePath, int targetSideLength,
            int maxNumOfPixels) {
        FileInputStream stream;
        try {
            stream = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            Log.i(TAG, "Cannot open file: " + filePath, e);
            return null;
        }
        BufferedInputStream bufferedInput = new BufferedInputStream(stream);

        // Decode bufferedInput for calculating a sample size.
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        int minSideLength = targetSideLength / 2;
        bufferedInput.mark(10 * 1000);
        options.inSampleSize =  Utils.computeSampleSize(options, minSideLength,
                maxNumOfPixels);
        try {
            bufferedInput.reset();
        } catch (IOException e) {
            Log.i(TAG, "Cannot reset BufferedInputStream: " + filePath, e);
            return null;
        }

        // Decode bufferedInput again for getting the pixels
        options.inJustDecodeBounds = false;
        Thread timeoutThread = new Thread("BitmapTimeoutThread") {
            @Override
            public void run() {
                try {
                    Thread.sleep(6000);
                    options.requestCancelDecode();
                } catch (InterruptedException e) {
                }
            }
        };
        timeoutThread.start();

        return BitmapFactory.decodeStream(bufferedInput, null, options);
    }

    public static Bitmap getImageThumbnail(final ContentResolver cr,
            final long imageId, int kind) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        Thread timeoutThread = new Thread("GetThumbnailTimeoutThread") {
            @Override
            public void run() {
                try {
                    Thread.sleep(6000);
                    Images.Thumbnails.cancelThumbnailRequest(cr, imageId);
                } catch (InterruptedException e) {
                }
            }
        };
        timeoutThread.start();

        return Images.Thumbnails.getThumbnail(cr, imageId, kind, options);
    }

    public static Bitmap getVideoThumbnail(final ContentResolver cr,
            final long videoId, int kind) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        Thread timeoutThread = new Thread("GetThumbnailTimeoutThread") {
            @Override
            public void run() {
                try {
                    Thread.sleep(6000);
                    Video.Thumbnails.cancelThumbnailRequest(cr, videoId);
                } catch (InterruptedException e) {
                }
            }
        };
        timeoutThread.start();

        return Video.Thumbnails.getThumbnail(cr, videoId, kind, options);
    }
}
