// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import android.content.Context;

public class DataManager {
    private static DataManager mInstance;
    private MediaSet mRootSet;
    private Context mContext;

    public DataManager(Context context) {
        mContext = context;
    }

    public static void initialize(Context context) {
        mInstance = new DataManager(context);
    }

    public static synchronized DataManager getInstance() {
        if (mInstance == null) throw new IllegalStateException();
        return mInstance;
    }

    public MediaSet getRootSet() {
        if (mRootSet == null) {
            mRootSet = MediaDbAccessor.getInstance().getRootMediaSets();
        }
        return mRootSet;
    }

    public MediaSet getSubMediaSet(int subSetIndex) {
        if (mRootSet == null) {
            mRootSet = MediaDbAccessor.getInstance().getRootMediaSets();
        }
        return mRootSet.getSubMediaSet(subSetIndex);
    }
}
