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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// LocalAlbumSet lists all image or video albums in the local storage.
public class LocalAlbumSet extends DatabaseMediaSet {
    private static final String TAG = "LocalAlbumSet";

    // The indices should match the following projections.
    private static final int BUCKET_ID_INDEX = 0;
    private static final int BUCKET_NAME_INDEX = 1;

    private static final String[] PROJECTION_IMAGE_BUCKETS = {
            ImageColumns.BUCKET_ID,
            ImageColumns.BUCKET_DISPLAY_NAME };

    private static final String[] PROJECTION_VIDEO_BUCKETS = {
            VideoColumns.BUCKET_ID,
            VideoColumns.BUCKET_DISPLAY_NAME };

    private final String[] mProjection;
    private final Uri mBaseUri;

    private boolean mIsImage;
    private long mUniqueId;
    private ArrayList<LocalAlbum> mAlbums = new ArrayList<LocalAlbum>();
    private final HashMap<Integer, String> mLoadBuffer = new HashMap<Integer, String>();

    public LocalAlbumSet(int parentId, int childKey, GalleryContext context,
            boolean isImage) {
        super(context);
        mIsImage = isImage;
        if (isImage) {
            mProjection = PROJECTION_IMAGE_BUCKETS;
            mBaseUri = Images.Media.EXTERNAL_CONTENT_URI;
        } else {
            mProjection = PROJECTION_VIDEO_BUCKETS;
            mBaseUri = Video.Media.EXTERNAL_CONTENT_URI;
        }

        mUniqueId = context.getDataManager().obtainSetId(parentId, childKey, this);
        context.getContentResolver().registerContentObserver(
                mBaseUri, true, new MyContentObserver());
    }

    @Override
    public long getUniqueId() {
        return mUniqueId;
    }

    public synchronized MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    public synchronized int getSubMediaSetCount() {
        return mAlbums.size();
    }

    @Override
    public String getName() {
        return TAG;
    }

    public int getTotalMediaItemCount() {
        int total = 0;
        for (MediaSet album : mAlbums) {
            total += album.getTotalMediaItemCount();
        }
        return total;
    }

    @Override
    protected void onLoadFromDatabase() {
        Uri uri = mBaseUri.buildUpon().
                appendQueryParameter("distinct", "true").build();
        Cursor cursor = mResolver.query(
                uri, mProjection, null, null, null);
        if (cursor == null) throw new NullPointerException();
        try {
            while (cursor.moveToNext()) {
                mLoadBuffer.put(cursor.getInt(BUCKET_ID_INDEX),
                        cursor.getString(BUCKET_NAME_INDEX));
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    protected void onUpdateContent() {
        HashMap<Integer, String> map = mLoadBuffer;
        ArrayList<LocalAlbum> newAlbums = new ArrayList<LocalAlbum>();
        DataManager dataManager = mContext.getDataManager();

        int parentId = getMyId();
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            int childKey = entry.getKey();
            LocalAlbum album = (LocalAlbum) dataManager.getMediaSet(parentId, childKey);
            if (album == null) {
                album = new LocalAlbum(parentId, mContext,
                        childKey, entry.getValue(), mIsImage);
            }
            newAlbums.add(album);
        }

        mAlbums = newAlbums;
        mLoadBuffer.clear();

        Collections.sort(mAlbums, LocalAlbum.sBucketNameComparator);

        for (int i = 0, n = mAlbums.size(); i < n; i++) {
            mAlbums.get(i).reload();
        }
    }

    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(new Handler(mContext.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange) {
            notifyContentDirty();
        }
    }

    public int getSupportedOperations(long uniqueId) {
        return SUPPORT_DELETE;
    }

    public void delete(long uniqueId) {
        Utils.Assert(DataManager.extractParentId(uniqueId) == getMyId());

        int childId = DataManager.extractSelfId(uniqueId);
        LocalAlbum child = (LocalAlbum) mContext.getDataManager().getMediaSet(childId);
        child.deleteSelf();
    }
}
