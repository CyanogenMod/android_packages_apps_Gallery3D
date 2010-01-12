package com.cooliris.media;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class ConcurrentPool<E extends Object> {
    private AtomicReferenceArray<E> mFreeList;
    private AtomicInteger mFreeListIndex;

    public ConcurrentPool(E[] objects) {
        mFreeList = new AtomicReferenceArray<E>(objects);
        mFreeListIndex = new AtomicInteger(objects.length);
    }

    public E create() {
        final int index = mFreeListIndex.decrementAndGet();
        E object = mFreeList.get(index);
        mFreeList.set(index, null);
        return object;
    }

    public void delete(E object) {
        final int index = mFreeListIndex.getAndIncrement();
        while (!mFreeList.compareAndSet(index, null, object)) {
            Thread.yield();
        }
    }
}
