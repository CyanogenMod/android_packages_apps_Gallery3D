package com.android.gallery3d.data;

import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootStub;

import android.content.ContentResolver;

class GalleryContextMock extends GalleryContextStub {
    GLRoot mGLRoot = new GLRootStub();
    DataManager mDataManager = new DataManager(this);
    ContentResolver mResolver;

    GalleryContextMock(ContentResolver resolver) {
        mResolver = resolver;
    }

    public GLRoot getGLRoot() { return mGLRoot; }
    public DataManager getDataManager() { return mDataManager; }
    public ContentResolver getContentResolver() { return mResolver; }
}
