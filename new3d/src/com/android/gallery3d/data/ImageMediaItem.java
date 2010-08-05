package com.android.gallery3d.data;

import com.android.gallery3d.util.Utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class ImageMediaItem extends DatabaseMediaItem {

    private static final int MICRO_TARGET_PIXELS = 128 * 128;
    private static final int JPEG_MARK_POSITION = 60 * 1024;

    private static final int FULLIMAGE_TARGET_SIZE = 2048;
    private static final int FULLIMAGE_MAX_NUM_PIXELS = 5 * 1024 * 1024;
    private static final String TAG = "ImageMediaItem";

    // Must preserve order between these indices and the order of the terms in
    // PROJECTION_IMAGES.
    private static final int INDEX_ID = 0;
    private static final int INDEX_CAPTION = 1;
    private static final int INDEX_MIME_TYPE = 2;
    private static final int INDEX_LATITUDE = 3;
    private static final int INDEX_LONGITUDE = 4;
    private static final int INDEX_DATE_TAKEN = 5;
    private static final int INDEX_DATE_ADDED = 6;
    private static final int INDEX_DATE_MODIFIED = 7;
    private static final int INDEX_DATA = 8;
    private static final int INDEX_ORIENTATION = 9;

    private static final String[] PROJECTION_IMAGES =  {
            ImageColumns._ID,           // 0
            ImageColumns.TITLE,         // 1
            ImageColumns.MIME_TYPE,     // 2
            ImageColumns.LATITUDE,      // 3
            ImageColumns.LONGITUDE,     // 4
            ImageColumns.DATE_TAKEN,    // 5
            ImageColumns.DATE_ADDED,    // 6
            ImageColumns.DATE_MODIFIED, // 7
            ImageColumns.DATA,          // 8
            ImageColumns.ORIENTATION};  // 9

    private final BitmapFactory.Options mOptions = new BitmapFactory.Options();

    private int mRotation;

    protected ImageMediaItem(ImageService imageService) {
        super(imageService);
    }

    protected Bitmap decodeImage(String path) throws IOException {
        // TODO: need to figure out why simply setting JPEG_MARK_POSITION doesn't work!
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
                Log.w(TAG, "failed in resetting the buffer after reading the jpeg header", e);
                bis.close();
                bis = new BufferedInputStream(new FileInputStream(path));
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

    public static ImageMediaItem load(ImageService imageService, Cursor cursor) {
        ImageMediaItem item = new ImageMediaItem(imageService);

        item.mId = cursor.getInt(INDEX_ID);
        item.mCaption = cursor.getString(INDEX_CAPTION);
        item.mMimeType = cursor.getString(INDEX_MIME_TYPE);
        item.mLatitude = cursor.getDouble(INDEX_LATITUDE);
        item.mLongitude = cursor.getDouble(INDEX_LONGITUDE);
        item.mDateTakenInMs = cursor.getLong(INDEX_DATE_TAKEN);
        item.mDateAddedInSec = cursor.getLong(INDEX_DATE_ADDED);
        item.mDateModifiedInSec = cursor.getLong(INDEX_DATE_MODIFIED);
        item.mFilePath = cursor.getString(INDEX_DATA);
        item.mRotation = cursor.getInt(INDEX_ORIENTATION);

        return item;
    }

    public static Cursor queryImageInBucket(
            ContentResolver resolver, int bucketId) {
        // Build the where clause
        StringBuilder builder = new StringBuilder(ImageColumns.BUCKET_ID);
        builder.append(" = ").append(bucketId);
        String whereClause = builder.toString();

        return resolver.query(
                Images.Media.EXTERNAL_CONTENT_URI,
                PROJECTION_IMAGES, whereClause, null, null);
    }

}
