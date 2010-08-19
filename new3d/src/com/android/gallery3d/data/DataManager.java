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
import java.util.HashMap;

// DataManager manages all media sets and media items in the system.
//
// Current organization:
// ComboMediaSet
//   -- MergeAlbumSet
//       -- LocalAlbumSet(isImage=true) .. LocalAlbum(isImage=true) .. LocalImage
//       -- LocalAlbumSet(isImage=false) .. LocalAlbum(isImage=false) .. LocalVideo
//   -- PicasaAlbumSet .. PicasaAlbum .. PicasaImage
//
// Each MediaSet and MediaItem has a unique 64 bits id. The most significant
// 32 bits represents its parent, and the least significant 32 bits represents
// the self id. For MediaSet the self id is is globally unique, but for
// MediaItem it's unique only relative to its parent.
//
// To make sure the id is the same when the MediaSet is re-created, a key
// is provided to obtainSetId() to make sure the same self id will be used as
// when the parent and key are the same.

public class DataManager {
    private static final String TAG = "DataManager";
    private static int PICASA_CACHE_MAX_ENTRIES = 5000;
    private static int PICASA_CACHE_MAX_BYTES = 200 * 1024 * 1024;
    private static String PICASA_CACHE_FILE = "/picasaweb";

    // This is a predefined parent id for sets created directly by DataManager.
    public static final int ID_ROOT = 0;

    // This is predefined child key for sets.
    public static final int KEY_COMBO = 0;
    public static final int KEY_MERGE = 1;
    public static final int KEY_LOCAL_IMAGE = 2;
    public static final int KEY_LOCAL_VIDEO = 3;
    public static final int KEY_PICASA = 4;

    private GalleryContext mContext;
    private MediaSet mRootSet;
    private HandlerThread mDataThread;
    private IdentityCache<Integer, MediaSet> mMediaSetCache;
    private HashMap<Long, Integer> mKeyToSelfId;
    private BlobCache mPicasaCache = null;

    public DataManager(GalleryContext context) {
        mContext = context;
        mMediaSetCache = new IdentityCache<Integer, MediaSet>();
        mKeyToSelfId = new HashMap<Long, Integer>();
    }

    public static long makeId(int parent, int self) {
        long result = parent;
        return (result << 32) | (self & 0xffffffffL);
    }

    public static int extractSelfId(long id) {
        return (int) id;
    }

    public static int extractParentId(long id) {
        return (int) (id >> 32);
    }

    private int mNextSelfId = 1;
    public synchronized long obtainSetId(int parentId, int childKey, MediaSet self) {
        long key = parentId;
        key = (key << 32) | (childKey & 0xffffffffL);

        int selfId;
        Integer value = mKeyToSelfId.get(key);

        if (value != null) {
            selfId = value;
        } else {
            while (mMediaSetCache.get(mNextSelfId) != null
                    || mNextSelfId == ID_ROOT) {
                ++mNextSelfId;
            }
            selfId = mNextSelfId++;
            mKeyToSelfId.put(key, selfId);
        }

        mMediaSetCache.put(selfId, self);
        return makeId(parentId, selfId);
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
            PicasaAlbumSet picasaSet = new PicasaAlbumSet(
                    ID_ROOT, KEY_PICASA, mContext);
            LocalAlbumSet localImageSet = new LocalAlbumSet(
                    ID_ROOT, KEY_LOCAL_IMAGE, mContext, true);
            LocalAlbumSet localVideoSet = new LocalAlbumSet(
                    ID_ROOT, KEY_LOCAL_VIDEO, mContext, false);

            MediaSet localSet = new MergeAlbumSet(
                    this, ID_ROOT, KEY_MERGE,
                    LocalAlbum.sDateTakenComparator,
                    localImageSet, localVideoSet);

            mRootSet = new ComboAlbumSet(
                    this, ID_ROOT, KEY_COMBO,
                    localSet, picasaSet);
            mRootSet.reload();
        }
        return mRootSet;
    }

    public synchronized Looper getDataLooper() {
        if (mDataThread == null ) {
            mDataThread = new HandlerThread(
                    "DataThread", Process.THREAD_PRIORITY_BACKGROUND);
            mDataThread.start();
        }
        return mDataThread.getLooper();
    }

    public MediaSet getMediaSet(int id) {
        return mMediaSetCache.get(id);
    }

    public int getSupportedOperations(long uniqueId) {
        int parentId = DataManager.extractParentId(uniqueId);
        MediaSet parent = getMediaSet(parentId);
        return parent.getSupportedOperations(uniqueId);
    }

    public void delete(long uniqueId) {
        int parentId = DataManager.extractParentId(uniqueId);
        MediaSet parent = getMediaSet(parentId);
        parent.delete(uniqueId);
    }

    public void rotate(long uniqueId, int degrees) {
        int parentId = DataManager.extractParentId(uniqueId);
        MediaSet parent = getMediaSet(parentId);
        parent.rotate(uniqueId, degrees);
    }
}
