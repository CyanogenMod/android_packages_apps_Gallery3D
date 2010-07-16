// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import android.os.Handler;
import android.os.Message;

import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.ui.Util;

public abstract class DatabaseMediaSet implements MediaSet {

    private static final int MSG_LOAD_DATABASE = 0;
    private static final int MSG_UPDATE_CONTENT = 1;

    protected Handler mMainHandler;
    protected Handler mDbHandler;
    protected MediaDbAccessor mAccessor;

    protected MediaSetListener mListener;

    protected DatabaseMediaSet(MediaDbAccessor accessor) {
        mAccessor = accessor;

        mMainHandler = new SynchronizedHandler(
                accessor.getUiMonitor(), accessor.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                Util.Assert(message.what == MSG_UPDATE_CONTENT);
                onUpdateContent();
                if (mListener != null) mListener.onContentChanged();
            }
        };

        mDbHandler = new Handler(accessor.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                Util.Assert(message.what == MSG_LOAD_DATABASE);
                onLoadFromDatabase();
                mMainHandler.sendEmptyMessage(MSG_UPDATE_CONTENT);
            }
        };
    }

    public void invalidate() {
        mDbHandler.sendEmptyMessage(MSG_LOAD_DATABASE);
    }

    public void setContentListener(MediaSetListener listener) {
        mListener = listener;
    }

    abstract protected void onLoadFromDatabase();
    abstract protected void onUpdateContent();
}
