// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ListenableFutureTask<T> extends FutureTask<T> {
    protected FutureListener<? super T> mListener;
    private Boolean mListenerCalled = false;

    protected ListenableFutureTask(
            Callable<T> callable, FutureListener<? super T> listener) {
        super(callable);
        mListener = listener;
    }

    /**
     * Sets a new listener for this future task. If the task has been done, the method
     * listener.onFutureDone() will be called immediately. Otherwise, the current listener
     * will be replaced with the new one.
     */
    public synchronized void setListener(FutureListener<? super T> listener) {
        if (mListenerCalled) {
            if (listener != null) listener.onFutureDone(this);
        } else {
            mListener = listener;
        }
    }

    @Override
    protected synchronized void done() {
        mListenerCalled = true;
        if (mListener != null) mListener.onFutureDone(this);
    }
}
