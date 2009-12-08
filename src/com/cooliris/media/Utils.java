package com.cooliris.media;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.Toast;

public class Utils {
    public static void playVideo(final Context context, final MediaItem item) {
        // this is a video
        ((Gallery) context).getHandler().post(new Runnable() {
            public void run() {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.mContentUri));
                    intent.setDataAndType(Uri.parse(item.mContentUri), item.mMimeType);
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, context.getResources().getString(R.string.video_err), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static final void writeUTF(DataOutputStream dos, String string) throws IOException {
        if (string == null) {
            dos.writeUTF(new String());
        } else {
            dos.writeUTF(string);
        }
    }

    public static final String readUTF(DataInputStream dis) throws IOException {
        String retVal = dis.readUTF();
        if (retVal.length() == 0)
            return null;
        return retVal;
    }

    public static final Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        int width = maxSize;
        int height = maxSize;
        boolean needsResize = false;
        if (srcWidth > srcHeight) {
            if (srcWidth > maxSize) {
                needsResize = true;
                height = ((maxSize * srcHeight) / srcWidth);
            }
        } else {
            if (srcHeight > maxSize) {
                needsResize = true;
                width = ((maxSize * srcWidth) / srcHeight);
            }
        }
        if (needsResize) {
            Bitmap retVal = Bitmap.createScaledBitmap(bitmap, width, height, true);
            return retVal;
        } else {
            return bitmap;
        }
    }

    private static final long POLY64REV = 0x95AC9329AC4BC9B5L;
    private static final long INITIALCRC = 0xFFFFFFFFFFFFFFFFL;

    private static boolean init = false;
    private static long[] CRCTable = new long[256];

    /**
     * A function thats returns a 64-bit crc for string
     * 
     * @param in
     *            : input string
     * @return 64-bit crc value
     */
    public static final long Crc64Long(String in) {
        if (in == null || in.length() == 0) {
            return 0;
        }
        // http://bioinf.cs.ucl.ac.uk/downloads/crc64/crc64.c
        long crc = INITIALCRC, part;
        if (!init) {
            for (int i = 0; i < 256; i++) {
                part = i;
                for (int j = 0; j < 8; j++) {
                    int value = ((int) part & 1);
                    if (value != 0)
                        part = (part >> 1) ^ POLY64REV;
                    else
                        part >>= 1;
                }
                CRCTable[i] = part;
            }
            init = true;
        }
        int length = in.length();
        for (int k = 0; k < length; ++k) {
            char c = in.charAt(k);
            crc = CRCTable[(((int) crc) ^ c) & 0xff] ^ (crc >> 8);
        }
        return crc;
    }

    /**
     * A function that returns a human readable hex string of a Crx64
     * 
     * @param in
     *            : input string
     * @return hex string of the 64-bit CRC value
     */
    public static final String Crc64(String in) {
        if (in == null)
            return null;
        long crc = Crc64Long(in);
        /*
         * The output is done in two parts to avoid problems with
         * architecture-dependent word order
         */
        int low = ((int) crc) & 0xffffffff;
        int high = ((int) (crc >> 32)) & 0xffffffff;
        String outVal = Integer.toHexString(high) + Integer.toHexString(low);
        return outVal;
    }

    public static String getBucketNameFromUri(Uri uri) {
        String string = "";
        if (string == null || string.length() == 0) {
            List<String> paths = uri.getPathSegments();
            int numPaths = paths.size();
            if (numPaths > 1) {
                string = paths.get(paths.size() - 2);
            }
            if (string == null)
                string = "";
        }
        return string;
    }

    // Copies src file to dst file.
    // If the dst file does not exist, it is created
    public static void Copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
