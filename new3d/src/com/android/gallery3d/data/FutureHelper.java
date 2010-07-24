// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// A helper class to let user implement Future more easily.

public class FutureHelper<T> implements Future<T> {
    protected ReentrantLock mLock = new ReentrantLock();

    private Condition mDone = mLock.newCondition();
    private Condition mCancelled = mLock.newCondition();

    private boolean mIsDone;
    private boolean mIsCancelled;

    private FutureListener<? super T> mListener;
    private T mResult;
    private Throwable mException;

    public FutureHelper(FutureListener<? super T> listener) {
        mListener = listener;
    }

    public void setListener(FutureListener<? super T> listener) {
        if (listener == mListener) return;
        mLock.lock();
        try {
            if (mIsDone && listener != null) {
                listener.onFutureDone(this);
            } else {
                mListener = listener;
            }
        } finally {
            mLock.unlock();
        }
    }

    private void done() {
        mIsDone = true;
        FutureListener<? super T> listener = mListener;
        if (listener != null) mListener.onFutureDone(this);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        mLock.lock();
        try {
            if (mIsDone) return false;
            try {
                if (!mIsCancelled) mCancelled.await();
            } catch (InterruptedException e) {
                // We should add this to the interface
                throw new RuntimeException(e);
            }
            done();
            return true;
        } finally {
            mLock.unlock();
        }
    }

    public T get() throws InterruptedException, ExecutionException {
        mLock.lock();
        try {
            if (!mIsDone) mDone.await();
            if (mIsCancelled) throw new CancellationException();
            if (mException != null) throw new ExecutionException(mException);
            return mResult;
        } finally {
            mLock.unlock();
        }
    }

    public T get(long timeout, TimeUnit unit) throws
            InterruptedException, ExecutionException, TimeoutException {
        mLock.lock();
        try {
            if (!mIsDone) {
                if (!mDone.await(timeout, unit)) throw new TimeoutException();
            }
            if (mIsCancelled) throw new CancellationException();
            if (mException != null) throw new ExecutionException(mException);
            return mResult;
        } finally {
            mLock.unlock();
        }
    }

    public boolean isCancelled() {
        return mIsCancelled;
    }

    public boolean isDone() {
        return mIsDone;
    }

    public void setResult(T result) {
        mLock.lock();
        try {
            if (mIsDone) return;
            mResult = result;
            done();
        } finally {
            mLock.unlock();
        }
    }

    public void setException(Throwable throwable) {
        mLock.lock();
        try {
            if (mIsDone) return;
            mException = throwable;
            done();
        } finally {
            mLock.unlock();
        }
    }

    public void cancelled() {
        mLock.lock();
        try {
            mIsCancelled = true;
            mCancelled.signalAll();
        } finally {
            mLock.unlock();
        }
    }
}
