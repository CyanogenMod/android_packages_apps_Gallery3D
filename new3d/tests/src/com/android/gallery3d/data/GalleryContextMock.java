package com.android.gallery3d.data;

import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootStub;

import android.content.ContentResolver;
import android.content.Context;

class GalleryContextMock extends GalleryContextStub {
    GLRoot mGLRoot = new GLRootStub();
    DataManager mDataManager = new DataManager(this);
    ContentResolver mResolver;
    Context mContext;

    GalleryContextMock(Context context, ContentResolver resolver) {
        mContext = context;
        mResolver = resolver;
    }

    public GLRoot getGLRoot() { return mGLRoot; }
    public DataManager getDataManager() { return mDataManager; }
    public Context getAndroidContext() { return mContext; }
    public ContentResolver getContentResolver() { return mResolver; }
}
