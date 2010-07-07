package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.Arrays;
import junit.framework.TestCase;

public class UtilsTest extends TestCase {
    private static final String TAG = "UtilsTest";

    private static final int [] testData = new int [] {
        /* outWidth, outHeight, minSideLength, maxNumOfPixels, sample size */
        0, 0, 0, 0, 1,
        1, 1, 1, 1, 1,
        100, 100, 100, 10000, 1,
        100, 100, 100, 2500, 2,
        99, 66, 33, 10000, 2,
        66, 99, 33, 10000, 2,
        99, 66, 34, 10000, 1,
        99, 66, 22, 10000, 4,
        99, 66, 16, 10000, 4,

        10000, 10000, 20000, 1000000, 16,

        100, 100, 100, 10000, 1, // 1
        100, 100, 50, 10000, 2,  // 2
        100, 100, 30, 10000, 4,  // 3->4
        100, 100, 22, 10000, 4,  // 4
        100, 100, 20, 10000, 8,  // 5->8
        100, 100, 11, 10000, 16, // 9->16
        100, 100, 5,  10000, 24, // 20->24
        100, 100, 2,  10000, 56, // 50->56

        100, 100, 100, 10000 - 1, 2,                  // a bit less than 1
        100, 100, 100, 10000 / (2 * 2) - 1, 4,        // a bit less than 2
        100, 100, 100, 10000 / (3 * 3) - 1, 4,        // a bit less than 3
        100, 100, 100, 10000 / (4 * 4) - 1, 8,        // a bit less than 4
        100, 100, 100, 10000 / (8 * 8) - 1, 16,       // a bit less than 8
        100, 100, 100, 10000 / (16 * 16) - 1, 24,     // a bit less than 16
        100, 100, 100, 10000 / (24 * 24) - 1, 32,     // a bit less than 24
        100, 100, 100, 10000 / (32 * 32) - 1, 40,     // a bit less than 32

        640, 480, 480, Utils.UNCONSTRAINED, 1,  // 1
        640, 480, 240, Utils.UNCONSTRAINED, 2,  // 2
        640, 480, 160, Utils.UNCONSTRAINED, 4,  // 3->4
        640, 480, 120, Utils.UNCONSTRAINED, 4,  // 4
        640, 480, 96, Utils.UNCONSTRAINED,  8,  // 5->8
        640, 480, 80, Utils.UNCONSTRAINED,  8,  // 6->8
        640, 480, 60, Utils.UNCONSTRAINED,  8,  // 8
        640, 480, 48, Utils.UNCONSTRAINED, 16,  // 10->16
        640, 480, 40, Utils.UNCONSTRAINED, 16,  // 12->16
        640, 480, 30, Utils.UNCONSTRAINED, 16,  // 16
        640, 480, 24, Utils.UNCONSTRAINED, 24,  // 20->24
        640, 480, 20, Utils.UNCONSTRAINED, 24,  // 24
        640, 480, 16, Utils.UNCONSTRAINED, 32,  // 30->32
        640, 480, 12, Utils.UNCONSTRAINED, 40,  // 40
        640, 480, 10, Utils.UNCONSTRAINED, 48,  // 48
        640, 480, 8, Utils.UNCONSTRAINED,  64,  // 60->64
        640, 480, 6, Utils.UNCONSTRAINED,  80,  // 80
        640, 480, 4, Utils.UNCONSTRAINED, 120,  // 120
        640, 480, 3, Utils.UNCONSTRAINED, 160,  // 160
        640, 480, 2, Utils.UNCONSTRAINED, 240,  // 240
        640, 480, 1, Utils.UNCONSTRAINED, 480,  // 480

        640, 480, Utils.UNCONSTRAINED, Utils.UNCONSTRAINED, 1,
        640, 480, Utils.UNCONSTRAINED, 640 * 480, 1,                  // 1
        640, 480, Utils.UNCONSTRAINED, 640 * 480 - 1, 2,              // a bit less than 1
        640, 480, Utils.UNCONSTRAINED, 640 * 480 / 4, 2,              // 2
        640, 480, Utils.UNCONSTRAINED, 640 * 480 / 4 - 1, 4,          // a bit less than 2
        640, 480, Utils.UNCONSTRAINED, 640 * 480 / 9, 4,              // 3
        640, 480, Utils.UNCONSTRAINED, 640 * 480 / 9 - 1, 4,          // a bit less than 3
        640, 480, Utils.UNCONSTRAINED, 640 * 480 / 16, 4,             // 4
        640, 480, Utils.UNCONSTRAINED, 640 * 480 / 16 - 1, 8,         // a bit less than 4
        640, 480, Utils.UNCONSTRAINED, 640 * 480 / 64, 8,             // 8
        640, 480, Utils.UNCONSTRAINED, 640 * 480 / 64 - 1, 16,        // a bit less than 8
        640, 480, Utils.UNCONSTRAINED, 640 * 480 / 256, 16,           // 16
        640, 480, Utils.UNCONSTRAINED, 640 * 480 / 256 - 1, 24,       // a bit less than 16
        640, 480, Utils.UNCONSTRAINED, 640 * 480 / (24 * 24) - 1, 32, // a bit less than 24
    };

    public void testComputeSampleSize() {
        Options options = new Options();

        for (int i = 0; i < testData.length; i += 5) {
            int w = testData[i];
            int h = testData[i + 1];
            int minSide = testData[i + 2];
            int maxPixels = testData[i + 3];
            int sampleSize = testData[i + 4];
            options.outWidth = w;
            options.outHeight = h;
            int result = Utils.computeSampleSize(options, minSide, maxPixels);
            if (result != sampleSize) {
                Log.v(TAG, w + "x" + h + ", minSide = " + minSide + ", maxPixels = "
                        + maxPixels + ", sampleSize = " + sampleSize + ", result = "
                        + result);
            }
            assertTrue(sampleSize == result);
        }
    }
}
