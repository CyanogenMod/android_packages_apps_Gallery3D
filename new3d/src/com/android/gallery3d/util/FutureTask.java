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

	public FutureTask(Callable<V> callable, FutureListener<? super V> listener) {
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

		try {
			V result = mCallable.call();
			mCallable = null;
			if (result == null && mHelper.isCancelling()) {
				mHelper.cancelled();
			} else {
				mHelper.setResult(result);
			}
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
		}

		if (!mState.compareAndSet(STATE_RUNNING, STATE_RAN)) {
			// STATE_INTERRUPTED
			synchronized (this) {
				Thread.interrupted(); // consume the interrupted signal
			}
		}
		mRunner = null;
	}

	protected synchronized void onCancel() {
        if (mState.compareAndSet(STATE_RUNNING, STATE_INTERRUPTED)){
            mRunner.interrupt();
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
	            FutureTask.this.onCancel();
	        } // else mState == STATE_DONE;
		}
	}
}
