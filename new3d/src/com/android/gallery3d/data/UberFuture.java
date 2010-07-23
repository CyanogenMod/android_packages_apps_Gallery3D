// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import com.android.gallery3d.ui.Util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

//UberFuture represents a sequence of Futures that will be executed one by one.
public abstract class UberFuture<E> implements Future<E>, FutureListener<Object> {

    private static final String TAG = "UberFuture";

    private final ReentrantLock mLock = new ReentrantLock();
    private final Condition mDone = mLock.newCondition();

    private boolean mIsDone = false;
    private boolean mIsCancelled = false;
    private FutureTask<?> mCurrentTask;
    private ExecutionException mException = null;

    private FutureListener<? super E> mListener;

    private int mStep = 0;

    public UberFuture(FutureListener<? super E> listener) {
        try {
            mListener = listener;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setListener(FutureListener<? super E> listener) {
        mListener = listener;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        mLock.lock();
        try {
            if (mIsDone) return false;
            mIsCancelled = true;
            mCurrentTask.cancel(mayInterruptIfRunning);
            done();
            return true;
        } finally {
            mLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public E get() throws InterruptedException, ExecutionException {
        mLock.lock();
        try {
            if (!mIsDone) mDone.await();
            if (mException != null) throw mException;
            if (mIsCancelled) throw new CancellationException();
            return (E) mCurrentTask.get();
        } finally {
            mLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public E get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        mLock.lock();
        try {
            if (!mIsDone) {
                if (!mDone.await(timeout, unit)) throw new TimeoutException();
            }
            if (mException != null) throw mException;
            if (mIsCancelled) throw new CancellationException();
            return (E) mCurrentTask.get();
        } finally {
            mLock.unlock();
        }
    }

    public boolean isCancelled() {
        mLock.lock();
        try {
            return mIsCancelled;
        } finally {
            mLock.unlock();
        }
    }

    public boolean isDone() {
        mLock.lock();
        try {
            return mIsDone || mIsCancelled;
        } finally {
            mLock.unlock();
        }
    }

    private void done() {
        mIsDone = true;
        mDone.signalAll();
        if (mListener != null) mListener.onFutureDone(this);
    }

    public void execute() {
        try {
            mCurrentTask = executeNextTask(0, null);
            Util.Assert(mCurrentTask != null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void onFutureDone(Future<?> currentTask) {
        mLock.lock();
        try {
            if (mIsCancelled) return;
            try {
                FutureTask<?> next =
                        executeNextTask(++mStep, (FutureTask<?>) currentTask);
                if (next != null) {
                    mCurrentTask = next;
                } else {
                    done();
                }
            } catch (Throwable t) {
                if (t instanceof ExecutionException) {
                    mException = (ExecutionException) t;
                } else {
                    mException = new ExecutionException(t);
                }
                done();
            }
        } finally {
            mLock.unlock();
        }
    }

    abstract protected FutureTask<?>
            executeNextTask(int index, FutureTask<?> current) throws Exception;
}
