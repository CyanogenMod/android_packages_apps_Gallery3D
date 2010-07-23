// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.ui;

import android.os.Bundle;

import com.android.gallery3d.app.GalleryContext;

abstract public class StateView extends GLView {
    protected GalleryContext mContext;

    public StateView() {
    }

    void setContext(GalleryContext context) {
        mContext = context;
    }

    abstract public void onStart(Bundle data);

    public void onBackPressed() {
        mContext.getStateManager().finish(this);
    }

    public void onPause() {}

    public void onResume() {}

    public void onDestroy() {}
}
