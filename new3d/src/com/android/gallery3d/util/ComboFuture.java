
package com.android.gallery3d.util;


// ComboFuture represents a sequence of Futures that will be executed one by one.
public abstract class ComboFuture<E> extends FutureHelper<E>
        implements FutureListener<Object> {

    private static final String TAG = "ComboFuture";

    private Future<?> mCurrentTask;
    private int mStep = 0;

    public ComboFuture(FutureListener<? super E> listener) {
        super(listener);
    }

    @Override
    protected synchronized void onCancel() {
        if (mCurrentTask != null) mCurrentTask.requestCancel();
    }

    public synchronized void execute() {
        try {
            mCurrentTask = executeNextTask(0, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Utils.Assert(mCurrentTask != null);
    }

    @SuppressWarnings("unchecked")
    public synchronized void onFutureDone(Future<?> currentTask) {
        try {
            mCurrentTask = executeNextTask(
                    ++mStep, currentTask);
            if (!isCancelled() && mCurrentTask == null) {
                setResult((E) currentTask.get());
            }
        } catch (Throwable t) {
            setException(t);
        }
    }

    // This function will be called when the current task has been done.
    abstract protected Future<?>
            executeNextTask(int index, Future<?> current) throws Exception;
}
