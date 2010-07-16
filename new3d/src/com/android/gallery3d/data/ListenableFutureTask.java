// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ListenableFutureTask<T> extends FutureTask<T> {
    protected FutureListener<? super T> mListener;

    protected ListenableFutureTask(
            Callable<T> callable, FutureListener<? super T> listener) {
        super(callable);
        mListener = listener;
    }

    @Override
    protected void done() {
        super.done();
        if (mListener != null) mListener.onFutureDone(this);
    }
}
