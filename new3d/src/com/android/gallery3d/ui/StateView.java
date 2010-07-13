// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.ui;

import android.content.Context;
import android.os.Bundle;

abstract public class StateView extends GLView {
    protected Context mContext;

    public StateView() {
    }

    void setContext(Context context) {
        mContext = context;
    }

    abstract public void onStart(Bundle data);

    public void onBackPressed() {
        StateManager.getInstance().finish(this);
    }

    public void onPause() {}

    public void onResume() {}

    public void onDestroy() {}
}
