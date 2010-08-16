// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;

import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DecodeService;
import com.android.gallery3d.data.DownloadService;
import com.android.gallery3d.data.ImageService;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.PositionRepository;

public interface GalleryContext {
    public ImageService getImageService();
    public StateManager getStateManager();
    public DataManager getDataManager();
    public DownloadService getDownloadService();
    public DecodeService getDecodeService();

    public GLRoot getGLRoot();
    public PositionRepository getPositionRepository();

    public Context getAndroidContext();

    public Looper getMainLooper();
    public Resources getResources();
    public ContentResolver getContentResolver();

}
