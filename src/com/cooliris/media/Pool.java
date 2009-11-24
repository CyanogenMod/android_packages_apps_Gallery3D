package com.cooliris.media;

public final class Pool<E extends Object> {
    private final E[] mFreeList;
    private int mFreeListIndex;

    public Pool(E[] objects) {
        mFreeList = objects;
        mFreeListIndex = objects.length;
    }

    public E create() {
        int index = --mFreeListIndex;
        if (index >= 0 && index < mFreeList.length) {
            E object = mFreeList[index];
            mFreeList[index] = null;
            return object;
        }
        return null;
    }

    public void delete(E object) {
        int index = mFreeListIndex;
        if (index >= 0 && index < mFreeList.length) {
            mFreeList[index] = object;
            mFreeListIndex++;
        }
    }
}
