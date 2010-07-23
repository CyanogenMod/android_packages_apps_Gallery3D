// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import java.util.concurrent.Future;

public interface FutureListener<E> {
    public void onFutureDone(Future<? extends E> future);
}
