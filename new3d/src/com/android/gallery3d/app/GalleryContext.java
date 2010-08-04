// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;

import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.ImageService;
import com.android.gallery3d.ui.StateManager;

public interface GalleryContext {
    public ImageService getImageService();
    public StateManager getStateManager();
    public DataManager getDataManager();

    public Context getAndroidContext();

    public Looper getMainLooper();
    public Resources getResources();
    public ContentResolver getContentResolver();
}
