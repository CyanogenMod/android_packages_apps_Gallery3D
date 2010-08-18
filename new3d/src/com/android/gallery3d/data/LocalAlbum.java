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

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video.VideoColumns;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.util.Utils;

import java.util.ArrayList;
import java.util.Comparator;

// LocalAlbumSet lists all media items in one bucket on local storage.
// The media items need to be all images or all videos, but not both.
public class LocalAlbum extends MediaSet {
    private static final String TAG = "LocalAlbum";
    private static final String[] COUNT_PROJECTION = { "count(*)" };
    private static final String DELETE_ITEM_WHERE_CLAUSE = "_id=?";
    private final String mWhereClause;
    private final String mOrderClause;
    private final Uri mBaseUri;
    private final String[] mProjection;
    public static final Comparator<LocalAlbum> sBucketNameComparator =
            new BucketNameComparator();
    public static final Comparator<MediaItem> sDateTakenComparator =
            new DateTakenComparator();

    private final GalleryContext mContext;
    private final ContentResolver mResolver;
    private final int mBucketId;
    private final String mBucketName;
    private boolean mIsImage;
    private long mUniqueId;
    private boolean mIsDirty = true;

    public LocalAlbum(int parentId, GalleryContext context, int bucketId,
            String name, boolean isImage) {
        mContext = context;
        mResolver = context.getContentResolver();
        mBucketId = bucketId;
        mBucketName = name;
        mIsImage = isImage;

        if (isImage) {
            mWhereClause = ImageColumns.BUCKET_ID + "=?";
            mOrderClause = ImageColumns.DATE_TAKEN + " DESC, "
                    + ImageColumns._ID + " ASC";
            mBaseUri = Images.Media.EXTERNAL_CONTENT_URI;
            mProjection = LocalImage.PROJECTION;
        } else {
            mWhereClause = VideoColumns.BUCKET_ID + "=?";
            mOrderClause = VideoColumns.DATE_TAKEN + " DESC, "
                    + VideoColumns._ID + " ASC";
            mBaseUri = Video.Media.EXTERNAL_CONTENT_URI;
            mProjection = LocalVideo.PROJECTION;
        }

        mUniqueId = context.getDataManager().obtainSetId(parentId, bucketId, this);
        mResolver.registerContentObserver(mBaseUri, true, new MyContentObserver());
    }

    @Override
    public long getUniqueId() {
        return mUniqueId;
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        ImageService imageService = mContext.getImageService();
        DataManager dataManager = mContext.getDataManager();

        Uri uri = mBaseUri.buildUpon()
                .appendQueryParameter("limit", start + "," + count).build();
        ArrayList<MediaItem> list = new ArrayList<MediaItem>();
        Cursor cursor = mResolver.query(
                uri, mProjection, mWhereClause,
                new String[]{String.valueOf(mBucketId)},
                mOrderClause);

        try {
            while (cursor.moveToNext()) {
                int myId = getMyId();
                if (mIsImage) {
                    list.add(LocalImage.load(myId, mContext, cursor, dataManager));
                } else {
                    list.add(LocalVideo.load(myId, imageService, cursor, dataManager));
                }
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    @Override
    public int getMediaItemCount() {
        Cursor cursor = mResolver.query(
                mBaseUri, COUNT_PROJECTION, mWhereClause,
                new String[]{String.valueOf(mBucketId)}, null);
        try {
            Utils.Assert(cursor.moveToNext());
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
    }

    @Override
    public String getName() {
        return mBucketName;
    }

    @Override
    public int getTotalMediaItemCount() {
        return getMediaItemCount();
    }

    private static class BucketNameComparator implements Comparator<LocalAlbum> {
        public int compare(LocalAlbum s1, LocalAlbum s2) {
            int result = s1.mBucketName.compareTo(s2.mBucketName);
            if (result != 0) return result;
            if (s1.mBucketId > s2.mBucketId) {
                return 1;
            } else if (s1.mBucketId < s2.mBucketId) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private static class DateTakenComparator implements Comparator<MediaItem> {
        public int compare(MediaItem item1, MediaItem item2) {
            LocalMediaItem s1 = (LocalMediaItem) item1;
            LocalMediaItem s2 = (LocalMediaItem) item2;
            if (s1.mDateTakenInMs > s2.mDateTakenInMs) {
                return -1;
            } else if (s1.mDateTakenInMs < s2.mDateTakenInMs) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    @Override
    public void reload() {
        if (mIsDirty) {
            mIsDirty = false;
            if (mListener != null) mListener.onContentChanged();
        }
    }

    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(new Handler(mContext.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange) {
            mIsDirty = true;
            if (mListener != null) mListener.onContentDirty();
        }
    }

    public int getMergeId() {
        return mBucketId;
    }

    public int getSupportedOperations(long uniqueId) {
        return SUPPORT_DELETE | SUPPORT_ROTATE;
    }

    public void delete(long uniqueId) {
        Utils.Assert(DataManager.extractParentId(uniqueId) == getMyId());
        int itemId = DataManager.extractSelfId(uniqueId);
        mResolver.delete(mBaseUri, DELETE_ITEM_WHERE_CLAUSE,
                new String[] {String.valueOf(itemId)});
    }

    public void rotate(long uniqueId, int degrees) {
        // TODO
    }

    public void deleteSelf() {
        mResolver.delete(mBaseUri, mWhereClause,
                new String[]{String.valueOf(mBucketId)});
    }
}
