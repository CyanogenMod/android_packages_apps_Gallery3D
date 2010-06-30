package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore.Images;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class ImageMediaItem extends DatabaseMediaItem {

    private static final int MICRO_TARGET_PIXELS = 128 * 128;
    private static final int JPEG_MARK_POSITION = 10 * 1024;

    private static final int FULLIMAGE_TARGET_SIZE = 1024;
    private static final int FULLIMAGE_MAX_NUM_PIXELS = 2 * 1024 * 1024;

    public int mRotation;

    private final BitmapFactory.Options mOptions = new BitmapFactory.Options();

    protected Bitmap decodeImage(String path) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(path), JPEG_MARK_POSITION);
        try {
            // Decode bufferedInput for calculating a sample size.
            final BitmapFactory.Options options = mOptions;
            options.inJustDecodeBounds = true;
            bis.mark(JPEG_MARK_POSITION);
            BitmapFactory.decodeStream(bis, null, options);

            if (options.mCancel) return null;

            try {
                bis.reset();
            } catch (IOException e) {
                throw new AssertionError();
            }

            options.inSampleSize =  Utils.computeSampleSize(options,
                    FULLIMAGE_TARGET_SIZE, FULLIMAGE_MAX_NUM_PIXELS);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeStream(bis, null, options);
        } finally {
            bis.close();
        }
    }

    @Override
    protected void cancelImageGeneration(ContentResolver resolver, int type) {
        switch (type) {
            case TYPE_FULL_IMAGE:
                mOptions.requestCancelDecode();
                break;
            case TYPE_THUMBNAIL:
            case TYPE_MICROTHUMBNAIL:
                Images.Thumbnails.cancelThumbnailRequest(resolver, mId);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    protected Bitmap generateImage(ContentResolver resolver, int type)
            throws Exception {

        switch (type) {
            case TYPE_FULL_IMAGE: {
                mOptions.mCancel = false;
                return decodeImage(mFilePath);
            }
            case TYPE_THUMBNAIL:
                return Images.Thumbnails.getThumbnail(
                        resolver, mId, Images.Thumbnails.MINI_KIND, null);
            case TYPE_MICROTHUMBNAIL: {
                Bitmap bitmap = Images.Thumbnails.getThumbnail(
                        resolver, mId, Images.Thumbnails.MINI_KIND, null);
                return bitmap == null
                        ? null
                        : Utils.resize(bitmap, MICRO_TARGET_PIXELS);
            }
            default:
                throw new IllegalArgumentException();
        }
    }

}
