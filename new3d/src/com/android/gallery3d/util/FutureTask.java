package com.android.gallery3d.util;

import java.io.InterruptedIOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * State transition diagram:
 *
 *    ready --> running
 *    ready --> cancelled
 *    running --> interrupted
 *    running --> ran
 */
public class FutureTask<V> implements Future<V>, Runnable {

    private static final int STATE_READY = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_CANCELLED = 2;
    private static final int STATE_INTERRUPTED = 4;
    private static final int STATE_RAN = 8;

    private Callable<V> mCallable;
    private final MyHelper mHelper;
    private volatile Thread mRunner;
    private AtomicInteger mState = new AtomicInteger(STATE_READY);
    private final boolean mInterruptible;

    public FutureTask(Callable<V> callable, FutureListener<? super V> listener) {
        this(callable, false, listener);
    }

    // @param interruptible Sets whether this task is interruptible, if true,
    //         the thread running the task will be interrupted when canceling.
    public FutureTask(Callable<V> callable,
            boolean interruptible, FutureListener<? super V> listener) {
        mInterruptible = interruptible;
        mCallable = callable;
        mHelper = new MyHelper(listener);
    }

    public void requestCancel() {
        mHelper.requestCancel();
    }

    public V get() throws ExecutionException, InterruptedException {
        return mHelper.get();
    }

    public V get(long duration, TimeUnit unit) throws ExecutionException,
            InterruptedException, TimeoutException {
        return mHelper.get(duration, unit);
    }

    public boolean isCancelled() {
        return mHelper.isCancelled();
    }

    public boolean isDone() {
        return mHelper.isDone();
    }

    public void run() {
        mRunner = Thread.currentThread();
        if (!mState.compareAndSet(STATE_READY, STATE_RUNNING)) return;

        boolean noException = false;
        V result = null;
        try {
            result = mCallable.call();
            noException = true;
        } catch (InterruptedException e) {
            if (mHelper.isCancelling()) {
                mHelper.cancelled();
            } else {
                mHelper.setException(e);
            }
        } catch (InterruptedIOException e) {
            if (mHelper.isCancelling()) {
                mHelper.cancelled();
            } else {
                mHelper.setException(e);
            }
        } catch (Throwable t) {
            mHelper.setException(t);
        } finally {
            mCallable = null;
        }

        if (noException) {
            if (result == null && mHelper.isCancelling()) {
                mHelper.cancelled();
            } else {
                mHelper.setResult(result);
            }
        }

        if (mInterruptible &&
                !mState.compareAndSet(STATE_RUNNING, STATE_RAN)) {
            // STATE_INTERRUPTED
            synchronized (this) {
                Thread.interrupted(); // consume the interrupted signal
            }
        }
        mRunner = null;
    }

    protected void onRequestCancel() {
    }

    protected synchronized void cancelTask() {
        if (mState.compareAndSet(STATE_RUNNING, STATE_INTERRUPTED)){
            if (mInterruptible) mRunner.interrupt();
            onRequestCancel();
        }
    }

    private class MyHelper extends FutureHelper<V> {

        MyHelper(FutureListener<? super V> listener) {
            super(listener);
        }

        @Override
        protected void onCancel() {
            if (mState.compareAndSet(STATE_READY, STATE_CANCELLED)) {
                cancelled();
            } else if (mState.get() == STATE_RUNNING) {
                FutureTask.this.cancelTask();
            } // else mState == STATE_DONE;
        }
    }
}
