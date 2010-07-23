// Copyright 2010 Google Inc. All Rights Reserved.

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
            PicasaUserAlbums picasaRoot = new PicasaUserAlbums(mContext);
            RootMediaSet mediaRoot = new RootMediaSet(mContext);
            picasaRoot.invalidate();

            mRootSet = new ComboMediaSet(mediaRoot, picasaRoot);
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
