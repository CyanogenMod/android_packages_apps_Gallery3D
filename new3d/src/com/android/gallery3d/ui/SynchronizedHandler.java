package com.android.gallery3d.ui;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class SynchronizedHandler extends Handler {

    private final Object mMonitor;

    public SynchronizedHandler(Object monitor) {
        mMonitor = Util.checkNotNull(monitor);
    }

    public SynchronizedHandler(Object monitor, Looper looper) {
        super(looper);
        mMonitor = Util.checkNotNull(monitor);
    }

    @Override
    public void dispatchMessage(Message message) {
        synchronized (mMonitor) {
            super.dispatchMessage(message);
        }
    }
}
