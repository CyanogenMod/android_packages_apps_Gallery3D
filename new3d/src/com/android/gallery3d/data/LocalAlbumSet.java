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
import java.util.concurrent.atomic.AtomicBoolean;

// LocalAlbumSet lists all image or video albums in the local storage.
public class LocalAlbumSet extends MediaSet {
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

    private GalleryContext mContext;
    private boolean mIsImage;
    private long mUniqueId;
    private ArrayList<LocalAlbum> mAlbums = new ArrayList<LocalAlbum>();
    private AtomicBoolean mContentDirty = new AtomicBoolean(true);
    private final MyContentObserver mContentObserver;

    public LocalAlbumSet(int parentId, int childKey, GalleryContext context,
            boolean isImage) {
        mContext = context;
        mIsImage = isImage;
        if (isImage) {
            mProjection = PROJECTION_IMAGE_BUCKETS;
            mBaseUri = Images.Media.EXTERNAL_CONTENT_URI;
        } else {
            mProjection = PROJECTION_VIDEO_BUCKETS;
            mBaseUri = Video.Media.EXTERNAL_CONTENT_URI;
        }

        mUniqueId = context.getDataManager().obtainSetId(parentId, childKey, this);
        mContentObserver = new MyContentObserver();
        context.getContentResolver().registerContentObserver(
                mBaseUri, true, mContentObserver);
    }

    @Override
    public long getUniqueId() {
        return mUniqueId;
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    @Override
    public int getSubMediaSetCount() {
        return mAlbums.size();
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public int getTotalMediaItemCount() {
        int total = 0;
        for (MediaSet album : mAlbums) {
            total += album.getTotalMediaItemCount();
        }
        return total;
    }

    protected ArrayList<LocalAlbum> loadSubMediaSets() {
        Uri uri = mBaseUri.buildUpon().
                appendQueryParameter("distinct", "true").build();
        Utils.assertNotInRenderThread();
        Cursor cursor = mContext.getContentResolver().query(
                uri, mProjection, null, null, null);
        if (cursor == null) throw new NullPointerException();
        HashMap<Integer, String> buffer = new HashMap<Integer, String>();
        try {
            while (cursor.moveToNext()) {
                buffer.put(cursor.getInt(BUCKET_ID_INDEX),
                        cursor.getString(BUCKET_NAME_INDEX));
            }
        } finally {
            cursor.close();
        }

        ArrayList<LocalAlbum> albums = new ArrayList<LocalAlbum>();
        DataManager dataManager = mContext.getDataManager();
        int parentId = getMyId();
        for (Map.Entry<Integer, String> entry : buffer.entrySet()) {
            int childKey = entry.getKey();
            LocalAlbum album = (LocalAlbum) dataManager.getMediaSet(parentId, childKey);
            if (album == null) {
                album = new LocalAlbum(parentId, mContext,
                        childKey, entry.getValue(), mIsImage);
            }
            albums.add(album);
        }
        for (int i = 0, n = albums.size(); i < n; ++i) {
            albums.get(i).reload();
        }
        Collections.sort(albums, LocalAlbum.sBucketNameComparator);
        return albums;
    }

    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(new Handler(mContext.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mContentDirty.compareAndSet(false, true)) {
                if (mListener != null) mListener.onContentDirty();
            }
        }
    }

    @Override
    public int getSupportedOperations(long uniqueId) {
        return SUPPORT_DELETE;
    }

    @Override
    public void delete(long uniqueId) {
        Utils.Assert(DataManager.extractParentId(uniqueId) == getMyId());

        int childId = DataManager.extractSelfId(uniqueId);
        LocalAlbum child = (LocalAlbum) mContext.getDataManager().getMediaSet(childId);
        child.deleteSelf();
    }

    @Override
    public boolean reload() {
        if (!mContentDirty.compareAndSet(true, false)) return false;
        ArrayList<LocalAlbum> album = loadSubMediaSets();
        if (album.equals(mAlbums)) return false;
        mAlbums = album;
        return true;
    }

    // For debug only. Fake there is a ContentObserver.onChange() event.
    void fakeChange() {
        mContentObserver.dispatchChange(true);
    }
}
