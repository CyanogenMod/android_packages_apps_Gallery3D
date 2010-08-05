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

package com.android.gallery3d.app;

import android.os.Bundle;

import com.android.gallery3d.ui.GLView;

abstract public class ActivityState {
    protected GalleryContext mContext;

    protected ActivityState() {
    }

    protected void setContentPane(GLView content) {
        mContext.getGLRootView().setContentPane(content);
    }

    void setContext(GalleryContext context) {
        mContext = context;
    }

    protected void onBackPressed() {
        mContext.getStateManager().finishState(this);
    }

    protected void onSaveState(Bundle outState) {
    }

    protected void onCreate(Bundle data, Bundle storedState){}

    protected void onPause() {}

    protected void onResume() {}

    protected void onDestroy() {}
}
