package com.android.gallery3d.util;

public interface FutureListener<V> {
    public void onFutureDone(Future<? extends V> future);
}
