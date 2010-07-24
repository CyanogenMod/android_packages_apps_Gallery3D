// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import com.android.gallery3d.ui.Util;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

//UberFuture represents a sequence of Futures that will be executed one by one.
public abstract class UberFuture<E> extends FutureHelper<E>
        implements FutureListener<Object> {

    private static final String TAG = "UberFuture";

    private FutureTask<?> mCurrentTask;
    private int mStep = 0;

    public UberFuture(FutureListener<? super E> listener) {
        super(listener);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        mLock.lock();
        try {
            if (super.cancel(mayInterruptIfRunning)) {
                FutureTask<?> task = mCurrentTask;
                if (task != null) task.cancel(mayInterruptIfRunning);
                return true;
            } else {
                return false;
            }
        } finally {
            mLock.unlock();
        }
    }

    public void execute() {
        mLock.lock();
        try {
            mCurrentTask = executeNextTask(0, null);
            Util.Assert(mCurrentTask != null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            mLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public void onFutureDone(Future<?> currentTask) {
        if (isDone()) return;
        mLock.lock();
        try {
            mCurrentTask = executeNextTask(
                    ++mStep, (FutureTask<?>) currentTask);
            if (mCurrentTask == null) {
                setResult((E) currentTask.get());
            }
        } catch (Throwable t) {
            setException(t);
        } finally {
            mLock.unlock();
        }
    }

    abstract protected FutureTask<?>
            executeNextTask(int index, FutureTask<?> current) throws Exception;
}
