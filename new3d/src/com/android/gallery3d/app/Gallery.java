/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.android.gallery3d.R;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.ImageService;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.ui.GalleryView;
import com.android.gallery3d.ui.StateManager;
import com.android.gallery3d.ui.StateView;

public final class Gallery extends Activity implements GalleryContext {
    public static final String REVIEW_ACTION = "com.android.gallery3d.app.REVIEW";

    private static final String TAG = "Gallery";
    private GLRootView mGLRootView;

    private StateManager mStateManager;
    private ImageService mImageService;
    private DataManager mDataManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);

        getStateManager().startStateView(GalleryView.class, new Bundle());
    }

    @Override
    public void onResume() {
        super.onResume();
        mGLRootView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGLRootView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        synchronized (this) {
            if (mImageService != null) mImageService.close();
        }
    }

    @Override
    public void onBackPressed() {
        StateView stateView = getStateManager().peekState();
        if (stateView != null) {
            synchronized (mGLRootView) {
                stateView.onBackPressed();
            }
        } else {
            finish();
        }
    }

    public Context getAndroidContext() {
        return this;
    }

    public synchronized ImageService getImageService() {
        if (mImageService == null) {
            mImageService = new ImageService(getContentResolver());
        }
        return mImageService;
    }

    public synchronized DataManager getDataManager() {
        if (mDataManager == null) {
            mDataManager = new DataManager(this);
        }
        return mDataManager;
    }

    public synchronized StateManager getStateManager() {
        if (mStateManager == null) {
            mStateManager = new StateManager(this, mGLRootView);
        }
        return mStateManager;
    }

    public Object getUIMonitor() {
        return mGLRootView;
    }
}
