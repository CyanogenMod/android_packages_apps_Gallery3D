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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.app.StateManager;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.PositionRepository;

class GalleryContextStub implements GalleryContext {
    public ImageService getImageService() { return null; }
    public StateManager getStateManager() { return null; }
    public DataManager getDataManager() { return null; }
    public DownloadService getDownloadService() { return null; }
    public DecodeService getDecodeService() { return null; }

    public GLRoot getGLRoot() { return null; }
    public PositionRepository getPositionRepository() { return null; }

    public Context getAndroidContext() { return null; }

    public Looper getMainLooper() { return null; }
    public Resources getResources() { return null; }
    public ContentResolver getContentResolver() { return null; }
}
