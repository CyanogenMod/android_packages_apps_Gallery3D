/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.data;

import com.android.gallery3d.util.Utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video.VideoColumns;

public class LocalVideo extends LocalMediaItem {

    private static final int MICRO_TARGET_PIXELS = 128 * 128;

    // Must preserve order between these indices and the order of the terms in
    // PROJECTION_VIDEOS.
    private static final int INDEX_ID = 0;
    private static final int INDEX_CAPTION = 1;
    private static final int INDEX_MIME_TYPE = 2;
    private static final int INDEX_LATITUDE = 3;
    private static final int INDEX_LONGITUDE = 4;
    private static final int INDEX_DATE_TAKEN = 5;
    private static final int INDEX_DATE_ADDED = 6;
    private static final int INDEX_DATE_MODIFIED = 7;
    private static final int INDEX_DATA = 8;
    private static final int INDEX_DURATION = 9;

    private static final String[] PROJECTION_VIDEOS = new String[] {
            VideoColumns._ID,
            VideoColumns.TITLE,
            VideoColumns.MIME_TYPE,
            VideoColumns.LATITUDE,
            VideoColumns.LONGITUDE,
            VideoColumns.DATE_TAKEN,
            VideoColumns.DATE_ADDED,
            VideoColumns.DATE_MODIFIED,
            VideoColumns.DATA,
            VideoColumns.DURATION};

    public int mDurationInSec;

    protected LocalVideo(ImageService imageService) {
        super(imageService);
    }

    @Override
    protected void cancelImageGeneration(ContentResolver resolver, int type) {
        Video.Thumbnails.cancelThumbnailRequest(resolver, mId);
    }

    @Override
    protected Bitmap generateImage(ContentResolver resolver, int type) {
        switch (type) {
            // Return a MINI_KIND bitmap in the cases of TYPE_FULL_IMAGE
            // and TYPE_THUMBNAIL.
            case TYPE_FULL_IMAGE:
            case TYPE_THUMBNAIL:
                return Video.Thumbnails.getThumbnail(
                        resolver, mId, Images.Thumbnails.MINI_KIND, null);
            case TYPE_MICROTHUMBNAIL:
                Bitmap bitmap = Video.Thumbnails.getThumbnail(
                        resolver, mId, Images.Thumbnails.MINI_KIND, null);
                return bitmap == null
                        ? null
                        : Utils.resize(bitmap, MICRO_TARGET_PIXELS);
            default:
                throw new IllegalArgumentException();
        }
    }

    public static LocalVideo load(ImageService imageService, Cursor cursor) {
        LocalVideo item = new LocalVideo(imageService);

        item.mId = cursor.getInt(INDEX_ID);
        item.mCaption = cursor.getString(INDEX_CAPTION);
        item.mMimeType = cursor.getString(INDEX_MIME_TYPE);
        item.mLatitude = cursor.getDouble(INDEX_LATITUDE);
        item.mLongitude = cursor.getDouble(INDEX_LONGITUDE);
        item.mDateTakenInMs = cursor.getLong(INDEX_DATE_TAKEN);
        item.mDateAddedInSec = cursor.getLong(INDEX_DATE_ADDED);
        item.mDateModifiedInSec = cursor.getLong(INDEX_DATE_MODIFIED);
        item.mFilePath = cursor.getString(INDEX_DATA);
        item.mDurationInSec = cursor.getInt(INDEX_DURATION);

        return item;
    }

    public static Cursor queryVideoInBucket(
            ContentResolver resolver, int bucketId) {

        // Build the where clause
        StringBuilder builder = new StringBuilder(ImageColumns.BUCKET_ID);
        builder.append(" = ").append(bucketId);
        String whereClause = builder.toString();

        return resolver.query(
                Video.Media.EXTERNAL_CONTENT_URI,
                PROJECTION_VIDEOS, whereClause, null, null);
    }


}
