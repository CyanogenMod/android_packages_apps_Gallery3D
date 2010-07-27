package com.android.gallery3d.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 * This is adapted from the java.util.concurrent.Future, but differs in how
 * "cancel" is handled. In java version, cancel() is completed only if the 
 * result hasn't be set. If the answer arrives later than cancel(), the answer 
 * is ignored and would be GC later. But this would be a problem if the result 
 * holds an amount of resource. In this case, we have to free the resource of 
 * the result before we can continue. To solve this, in the Future here, 
 * requestCancel() is only a request. The implementation could ignore it or 
 * try its best to cancel the computation. However, once the result has been 
 * returned, the state of the future is "done" instead of "canceled".
 */
public interface Future<V> {
    public V get() throws ExecutionException, InterruptedException;

    public V get(long duration, TimeUnit unit)
            throws ExecutionException, InterruptedException, TimeoutException;

    public void requestCancel();

    public boolean isCancelled();
    public boolean isDone();
}
