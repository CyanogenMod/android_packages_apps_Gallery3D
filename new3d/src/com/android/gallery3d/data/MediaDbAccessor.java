package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.util.Log;

public class MediaDbAccessor {
    private static final String TAG = "MediaDbAccessor";

    private static final String BASE_CONTENT_STRING_IMAGES =
        (Images.Media.EXTERNAL_CONTENT_URI).toString() + "/";
    private static final String BASE_CONTENT_STRING_VIDEOS =
        (Video.Media.EXTERNAL_CONTENT_URI).toString() + "/";

    // Must preserve order between these indices and the order of the terms in
    // BUCKET_PROJECTION_IMAGES, BUCKET_PROJECTION_VIDEOS.
    // Not using SortedHashMap for efficiency reasons.
    private static final int BUCKET_ID_INDEX = 0;
    private static final int BUCKET_NAME_INDEX = 1;
    private static final String[] BUCKET_PROJECTION_IMAGES = new String[] {
            ImageColumns.BUCKET_ID, ImageColumns.BUCKET_DISPLAY_NAME };
    private static final String[] BUCKET_PROJECTION_VIDEOS = new String[] {
            VideoColumns.BUCKET_ID, VideoColumns.BUCKET_DISPLAY_NAME };

    // Must preserve order between these indices and the order of the terms in
    // INITIAL_PROJECTION_IMAGES and
    // INITIAL_PROJECTION_VIDEOS.
    private static final int MEDIA_ID_INDEX = 0;
    private static final int MEDIA_CAPTION_INDEX = 1;
    private static final int MEDIA_MIME_TYPE_INDEX = 2;
    private static final int MEDIA_LATITUDE_INDEX = 3;
    private static final int MEDIA_LONGITUDE_INDEX = 4;
    private static final int MEDIA_DATE_TAKEN_INDEX = 5;
    private static final int MEDIA_DATE_ADDED_INDEX = 6;
    private static final int MEDIA_DATE_MODIFIED_INDEX = 7;
    private static final int MEDIA_DATA_INDEX = 8;
    private static final int MEDIA_ORIENTATION = 9;
    private static final int DURATION_INDEX = 9;
    private static final int MEDIA_BUCKET_ID_INDEX = 10;
    private static final String[] PROJECTION_IMAGES = new String[] {
            ImageColumns._ID, ImageColumns.TITLE,
            ImageColumns.MIME_TYPE, ImageColumns.LATITUDE,
            ImageColumns.LONGITUDE, ImageColumns.DATE_TAKEN,
            ImageColumns.DATE_ADDED, ImageColumns.DATE_MODIFIED,
            ImageColumns.DATA, ImageColumns.ORIENTATION,
            ImageColumns.BUCKET_ID };

    private static final String[] PROJECTION_VIDEOS = new String[] {
            VideoColumns._ID, VideoColumns.TITLE, VideoColumns.MIME_TYPE,
            VideoColumns.LATITUDE, VideoColumns.LONGITUDE,
            VideoColumns.DATE_TAKEN, VideoColumns.DATE_ADDED,
            VideoColumns.DATE_MODIFIED, VideoColumns.DATA,
            VideoColumns.DURATION, VideoColumns.BUCKET_ID };

    private static final String DEFAULT_BUCKET_SORT_ORDER = "upper(" +
            ImageColumns.BUCKET_DISPLAY_NAME + ") ASC";
    private static final String DEFAULT_IMAGE_SORT_ORDER =
            ImageColumns.DATE_TAKEN + " ASC";
    private static final String DEFAULT_VIDEO_SORT_ORDER =
            VideoColumns.DATE_TAKEN + " ASC";

    public static LocalMediaSet getMediaSets(Context context) {
        LocalMediaSet rootSet = new LocalMediaSet(LocalMediaSet.ROOT_SET_ID,
                "All Albums");

        final Uri uriImages = Images.Media.EXTERNAL_CONTENT_URI.buildUpon().
                appendQueryParameter("distinct", "true").build();
        final Uri uriVideos = Video.Media.EXTERNAL_CONTENT_URI.buildUpon().
                appendQueryParameter("distinct", "true").build();
        final ContentResolver cr = context.getContentResolver();

        SortCursor sortCursor = null;
        try {
            final Cursor cursorImages = cr.query(uriImages,
                    BUCKET_PROJECTION_IMAGES, null, null,
                    DEFAULT_BUCKET_SORT_ORDER);
            final Cursor cursorVideos = cr.query(uriVideos,
                    BUCKET_PROJECTION_VIDEOS, null, null,
                    DEFAULT_BUCKET_SORT_ORDER);
            Cursor[] cursors = new Cursor[2];
            cursors[0] = cursorImages;
            cursors[1] = cursorVideos;
            sortCursor = new SortCursor(cursors,
                    ImageColumns.BUCKET_DISPLAY_NAME, SortCursor.TYPE_STRING,
                    true);

            if (sortCursor.moveToFirst()) {
                do {
                    int setId = sortCursor.getInt(BUCKET_ID_INDEX);
                    LocalMediaSet set =
                            (LocalMediaSet) rootSet.getSubMediaSetById(setId);
                    if (set == null) {
                        set = new LocalMediaSet(setId,
                                sortCursor.getString(BUCKET_NAME_INDEX));
                        rootSet.addSubMediaSet(set);
                    }
                } while (sortCursor.moveToNext());
            }
        } finally {
            if (sortCursor != null) {
                sortCursor.close();
            }
        }
        populateMediaItemsForSets(context, rootSet);
        return rootSet;
    }

    private static void populateMediaItemsForSets(Context context,
            LocalMediaSet rootSet) {
        final Uri uriImages = Images.Media.EXTERNAL_CONTENT_URI;
        final Uri uriVideos = Video.Media.EXTERNAL_CONTENT_URI;
        final ContentResolver cr = context.getContentResolver();

        String whereClause = composeWhereClause(rootSet);

        try {
            final Cursor cursorImages = cr.query(uriImages, PROJECTION_IMAGES,
                    whereClause, null, DEFAULT_IMAGE_SORT_ORDER);
            final Cursor cursorVideos = cr.query(uriVideos, PROJECTION_VIDEOS,
                    whereClause, null, DEFAULT_VIDEO_SORT_ORDER);
            final Cursor[] cursors = new Cursor[2];
            cursors[0] = cursorImages;
            cursors[1] = cursorVideos;
            final SortCursor sortCursor = new SortCursor(cursors,
                    ImageColumns.DATE_TAKEN, SortCursor.TYPE_NUMERIC, true);
            try {
                if (sortCursor.moveToFirst()) {
                    do {
                        final int setId =
                                sortCursor.getInt(MEDIA_BUCKET_ID_INDEX);
                        LocalMediaSet set = (LocalMediaSet)
                                rootSet.getSubMediaSetById(setId);
                        MediaItem item;
                        final boolean isVideo =
                                (sortCursor.getCurrentCursorIndex() == 1);
                        if (isVideo) {
                            item = createVideoMediaItemFromCursor(sortCursor,
                                    BASE_CONTENT_STRING_VIDEOS);
                        } else {
                            item = createImageMediaItemFromCursor(sortCursor,
                                    BASE_CONTENT_STRING_IMAGES);
                        }
                        Log.i(TAG, "Item to add in: " + item.getTitle());
                        set.addMediaItem(item);
                    } while (sortCursor.moveToNext());
                }
            } finally {
                if (sortCursor != null)
                    sortCursor.close();
            }
        } catch (Exception e) {
            // If the database operation failed for any reason
            Log.e(TAG, "Failed to complete the database operation!", e);
        }

    }

    private static VideoMediaItem createVideoMediaItemFromCursor(Cursor cursor,
            String baseUri) {
        VideoMediaItem item = new VideoMediaItem();
        populateAbstractMediaItemFromCursor(cursor, baseUri, item);
        item.mDurationInSec = cursor.getInt(DURATION_INDEX);
        return item;
    }

    private static ImageMediaItem createImageMediaItemFromCursor(Cursor cursor,
            String baseUri) {
         ImageMediaItem item = new ImageMediaItem();
         populateAbstractMediaItemFromCursor(cursor, baseUri, item);
         item.mRotation = cursor.getInt(MEDIA_ORIENTATION);
         return item;
    }

    private static void populateAbstractMediaItemFromCursor(Cursor cursor,
            String baseUri, AbstractMediaItem item) {
        item.mId = cursor.getInt(MEDIA_ID_INDEX);
        item.mCaption = cursor.getString(MEDIA_CAPTION_INDEX);
        item.mMimeType = cursor.getString(MEDIA_MIME_TYPE_INDEX);
        item.mLatitude = cursor.getDouble(MEDIA_LATITUDE_INDEX);
        item.mLongitude = cursor.getDouble(MEDIA_LONGITUDE_INDEX);
        item.mDateTakenInMs = cursor.getLong(MEDIA_DATE_TAKEN_INDEX);
        item.mDateAddedInSec = cursor.getLong(MEDIA_DATE_ADDED_INDEX);
        item.mDateModifiedInSec = cursor.getLong(MEDIA_DATE_MODIFIED_INDEX);
        item.mFilePath = cursor.getString(MEDIA_DATA_INDEX);
        if (baseUri != null)
            item.mContentUri = baseUri + item.mId;
    }

    private static String composeWhereClause(LocalMediaSet rootSet) {
        int count = rootSet.getSubMediaSetCount();
        if (count <= 0) {
            return null;
        }

        StringBuilder whereString = new StringBuilder(
                ImageColumns.BUCKET_ID + " in (");
        for (int i = 0; i < count - 1; ++i) {
            whereString.append(((LocalMediaSet) rootSet.getSubMediaSet(i))
                    .getBucketId()).append(",");
        }
        whereString.append(((LocalMediaSet) rootSet.getSubMediaSet(count-1))
                .getBucketId()).append(")");
        return whereString.toString();
    }

}