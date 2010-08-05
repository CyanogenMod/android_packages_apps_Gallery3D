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

import java.io.File;
import java.io.IOException;

public class DataManager {
    private static final String TAG = "DataManager";
    private static int PICASA_CACHE_MAX_ENTRIES = 5000;
    private static int PICASA_CACHE_MAX_BYTES = 200 * 1024 * 1024;
    private static String PICASA_CACHE_FILE = "/picasaweb";
    private GalleryContext mContext;
    private MediaSet mRootSet;
    private HandlerThread mDataThread;
    private BlobCache mPicasaCache = null;

    public DataManager(GalleryContext context) {
        mContext = context;
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
            LocalAlbumSet localSet = new LocalAlbumSet(mContext);
            picasaSet.invalidate();

            mRootSet = new ComboMediaSet(localSet, picasaSet);
        }
        return mRootSet;
    }

    public MediaSet getSubMediaSet(int subSetIndex) {
        return getRootSet().getSubMediaSet(subSetIndex);
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
