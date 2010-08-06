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

import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import com.android.gallery3d.app.GalleryContext;

public class DataManager {
    private GalleryContext mContext;
    private MediaSet mRootSet;
    private HandlerThread mDataThread;

    public DataManager(GalleryContext context) {
        mContext = context;
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
