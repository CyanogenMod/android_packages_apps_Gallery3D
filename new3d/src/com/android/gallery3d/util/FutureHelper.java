package com.android.gallery3d.util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/*
 * The state diagram:
 *
 *     ready --> setResult() or setException() --> ran (done)
 *     ready --> requestCancel() --> cancelling --> call onCancel()
 *     cancelling --> setResult() or setException() --> ran (done)
 *     cancelling --> cancelled() --> cancelled (done)
 */
public class FutureHelper<V> implements Future<V> {

    private final Sync mSync = new Sync();
    private FutureListener<? super V> mListener;

    protected FutureHelper(FutureListener<? super V> listener) {
        mListener = listener;
    }

    public V get(long duration, TimeUnit unit)
            throws ExecutionException, InterruptedException, TimeoutException {
        return mSync.innerGet(duration, unit);
    }

    public V get() throws ExecutionException, InterruptedException {
        return mSync.innerGet();
    }

    public void setResult(V result) {
        mSync.innerSetResult(result);
    }

    public void setException(Throwable throwable) {
        mSync.innerSetException(throwable);
    }

    public void cancelled() {
        mSync.innerCancelled();
    }

    public void requestCancel() {
        mSync.innerRequestCancel();
    }

    public boolean isCancelled() {
        return mSync.innerIsCanclled();
    }

    public boolean isDone() {
        return mSync.innerIsDone();
    }

    protected boolean isCancelling() {
        return mSync.innerIsCanclling();
    }

    protected void onCancel() {
        throw new UnsupportedOperationException();
    }

    private void done() {
        if (mListener != null) {
            mListener.onFutureDone(this);
            mListener = null;
        }
    }

    private class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = -3545729446289780870L;

        private static final int STATE_READY = 0;
        private static final int STATE_CANCELLING = 1;
        private static final int STATE_CANCELLED = 2;
        private static final int STATE_RAN = 4;

        private AtomicInteger mState = new AtomicInteger(STATE_READY);
        private V mResult;
        private ExecutionException mException;

        @Override
        protected int tryAcquireShared(int ignore) {
            return (mState.get() & (STATE_RAN | STATE_CANCELLED)) != 0 ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int ignore) {
            return true;
        }

        public V innerGet(long duration, TimeUnit unit)
                throws ExecutionException, InterruptedException, TimeoutException {
            if (!tryAcquireSharedNanos(0, unit.toNanos(duration))) {
                throw new TimeoutException();
            }
            int s = mState.get();
            if (s == STATE_CANCELLED) throw new CancellationException();
            if (mException != null) throw mException;
            return mResult;
        }

        public V innerGet() throws ExecutionException, InterruptedException {
            acquireSharedInterruptibly(0);
            int s = mState.get();
            if (s == STATE_CANCELLED) throw new CancellationException();
            if (mException != null) throw mException;
            return mResult;
        }

        public void innerCancelled() {
            if (mState.compareAndSet(STATE_CANCELLING, STATE_CANCELLED)) {
                releaseShared(0);
                done();
            } else {
                throw new IllegalStateException();
            }
        }

        public void innerSetResult(V result) {
            while (true) {
                int s = mState.get();
                if ((s & (STATE_CANCELLED | STATE_RAN)) != 0) {
                    throw new IllegalStateException();
                }
                if (mState.compareAndSet(s, STATE_RAN)) break;
            }
            mResult = result;
            releaseShared(0);
            done();
        }

        public void innerSetException(Throwable throwable) {
            while (true) {
                int s = mState.get();
                if ((s & (STATE_CANCELLED | STATE_RAN)) != 0) {
                    throw new IllegalStateException();
                }
                if (mState.compareAndSet(s, STATE_RAN)) break;
            }
            mException = new ExecutionException(throwable);
            releaseShared(0);
            done();
        }

        public void innerRequestCancel() {
            if (mState.compareAndSet(STATE_READY, STATE_CANCELLING)) {
                FutureHelper.this.onCancel();
            }
        }

        public boolean innerIsCanclled() {
            return mState.get() == STATE_CANCELLED;
        }

        public boolean innerIsDone() {
            return (mState.get() & (STATE_CANCELLED | STATE_RAN)) != 0;
        }

        public boolean innerIsCanclling() {
            return mState.get() == STATE_CANCELLING;
        }
    }
}
