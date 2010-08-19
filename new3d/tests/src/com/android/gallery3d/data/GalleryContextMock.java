package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Looper;

import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootStub;

class GalleryContextMock extends GalleryContextStub {
    GLRoot mGLRoot = new GLRootStub();
    DataManager mDataManager = new DataManager(this);
    ContentResolver mResolver;
    Context mContext;
    Looper mMainLooper;

    GalleryContextMock(Context context,
            ContentResolver resolver, Looper mainLooper) {
        mContext = context;
        mResolver = resolver;
        mMainLooper = mainLooper;
    }

    @Override
    public GLRoot getGLRoot() { return mGLRoot; }
    @Override
    public DataManager getDataManager() { return mDataManager; }
    @Override
    public Context getAndroidContext() { return mContext; }
    @Override
    public ContentResolver getContentResolver() { return mResolver; }
    @Override
    public Looper getMainLooper() { return mMainLooper; }
}
