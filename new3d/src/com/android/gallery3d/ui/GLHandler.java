package com.android.gallery3d.ui;

import android.os.Handler;
import android.os.Message;

public class GLHandler extends Handler {

    private final GLRootView mRootView;

    public GLHandler(GLRootView rootView) {
        mRootView = rootView;
    }

    @Override
    public void dispatchMessage(Message message) {
        synchronized (mRootView) {
            super.dispatchMessage(message);
        }
    }
}
