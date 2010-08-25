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

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore.Images.ImageColumns;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;

import java.io.File;

// LocalImage represents an image in the local storage.
public class LocalImage extends LocalMediaItem {

    private static final int MICRO_TARGET_PIXELS = 128 * 128;

    private static final int FULLIMAGE_TARGET_SIZE = 2048;
    private static final int FULLIMAGE_MAX_NUM_PIXELS = 3 * 1024 * 1024;
    private static final String TAG = "LocalImage";

    // Must preserve order between these indices and the order of the terms in
    // the following PROJECTION array.
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

    static final String[] PROJECTION =  {
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

    private long mUniqueId;
    private int mRotation;
    private final GalleryContext mContext;

    protected LocalImage(GalleryContext context) {
        mContext = context;
    }

    @Override
    public long getUniqueId() {
        return mUniqueId;
    }

    @Override
    public synchronized Future<Bitmap>
            requestImage(int type, FutureListener<? super Bitmap> listener) {
        if (type == TYPE_FULL_IMAGE) {
            return mContext.getDecodeService().requestDecode(
                    new File(mFilePath), null, FULLIMAGE_TARGET_SIZE,
                    FULLIMAGE_MAX_NUM_PIXELS, listener);
        } else {
            return mContext.getImageService()
                    .requestImageThumbnail(mId, type, listener);
        }
    }

    public static LocalImage load(int parentId, GalleryContext context,
            Cursor cursor, DataManager dataManager) {
        int itemId = cursor.getInt(INDEX_ID);
        LocalImage item = new LocalImage(context);
        item.mId = itemId;
        item.mCaption = cursor.getString(INDEX_CAPTION);
        item.mMimeType = cursor.getString(INDEX_MIME_TYPE);
        item.mLatitude = cursor.getDouble(INDEX_LATITUDE);
        item.mLongitude = cursor.getDouble(INDEX_LONGITUDE);
        item.mDateTakenInMs = cursor.getLong(INDEX_DATE_TAKEN);
        item.mDateAddedInSec = cursor.getLong(INDEX_DATE_ADDED);
        item.mDateModifiedInSec = cursor.getLong(INDEX_DATE_MODIFIED);
        item.mFilePath = cursor.getString(INDEX_DATA);
        item.mRotation = cursor.getInt(INDEX_ORIENTATION);
        item.mUniqueId = DataManager.makeId(parentId, itemId);

        return item;
    }
}
