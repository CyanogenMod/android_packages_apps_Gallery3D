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

import android.os.Environment;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.util.IdentityCache;

import java.io.File;
import java.io.IOException;

public class DataManager {
    private static final String TAG = "DataManager";
    private static int PICASA_CACHE_MAX_ENTRIES = 5000;
    private static int PICASA_CACHE_MAX_BYTES = 200 * 1024 * 1024;
    private static String PICASA_CACHE_FILE = "/picasaweb";

    // Below are constants for categories.
    public static final int ID_LOCAL_IMAGE = 1;
    public static final int ID_LOCAL_VIDEO = 2;
    public static final int ID_PICASA_IMAGE = 3;

    public static final int ID_LOCAL_IMAGE_ALBUM = 4;
    public static final int ID_LOCAL_VIDEO_ALBUM = 5;
    public static final int ID_PICASA_ALBUM = 6;

    public static final int ID_LOCAL_IMAGE_ALBUM_SET = 7;
    public static final int ID_LOCAL_VIDEO_ALBUM_SET = 8;
    public static final int ID_PICASA_ALBUM_SET = 9;

    public static final int ID_COMBO_ALBUM_SET = 10;
    public static final int ID_MERGE_LOCAL_ALBUM_SET = 11;
    public static final int ID_MERGE_LOCAL_ALBUM = 12;

    private GalleryContext mContext;
    private MediaSet mRootSet;
    private HandlerThread mDataThread;
    private IdentityCache<Long, MediaItem> mMediaItemCache;
    private BlobCache mPicasaCache = null;

    public DataManager(GalleryContext context) {
        mContext = context;
        mMediaItemCache = new IdentityCache<Long, MediaItem>();
    }

    public static long makeId(int category, int item) {
        long result = category;
        return (result << 32) | item;
    }

    public static int extractItemId(long id) {
        return (int) id;
    }

    // Return null when we cannot instantiate a BlobCache, e.g.:
    // there is no SD card found.
    public BlobCache getPicasaCache() {
        if (mPicasaCache == null) {
            String path = getPicasaCachePath();
            if (path == null) {
                return null;
            } else {
                try {
                    mPicasaCache = new BlobCache(path, PICASA_CACHE_MAX_ENTRIES,
                            PICASA_CACHE_MAX_BYTES, true);
                } catch (IOException e) {
                    Log.e(TAG, "Cannot instantiate Picasaweb Cache!", e);
                }
            }
        }
        return mPicasaCache;
    }

    private String getPicasaCachePath() {
        File cacheDir = mContext.getAndroidContext().getExternalCacheDir();
        return cacheDir == null
                ? null
                : cacheDir.getAbsolutePath() + PICASA_CACHE_FILE;
    }

    public MediaSet getRootSet() {
        if (mRootSet == null) {
            PicasaAlbumSet picasaSet = new PicasaAlbumSet(mContext);
            LocalAlbumSet localImageSet = new LocalAlbumSet(mContext, true);
            LocalAlbumSet localVideoSet = new LocalAlbumSet(mContext, false);
            MediaSet localSet = new MergeAlbumSet(
                    makeId(ID_MERGE_LOCAL_ALBUM_SET, 0),
                    LocalAlbum.sDateTakenComparator,
                    localImageSet, localVideoSet);

            mRootSet = new ComboAlbumSet(
                    makeId(ID_COMBO_ALBUM_SET, 0),
                    localSet, picasaSet);
            mRootSet.reload();
        }
        return mRootSet;
    }

    public MediaSet getSubMediaSet(int subSetIndex) {
        return getRootSet().getSubMediaSet(subSetIndex);
    }

    public MediaItem getFromCache(Long key) {
        return mMediaItemCache.get(key);
    }

    public MediaItem putToCache(long key, MediaItem item) {
        return mMediaItemCache.put(Long.valueOf(key), item);
    }

    public synchronized Looper getDataLooper() {
        if (mDataThread == null ) {
            mDataThread = new HandlerThread(
                    "DataThread", Process.THREAD_PRIORITY_BACKGROUND);
            mDataThread.start();
        }
        return mDataThread.getLooper();
    }
}
