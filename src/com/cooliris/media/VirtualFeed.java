package com.cooliris.media;

public abstract class VirtualFeed<E> {
    protected int mLoadedBegin = 0;
    protected int mLoadedEnd = 0;
    protected int mLoadingBegin = 0;
    protected int mLoadingEnd = 0;
    protected int mLoadableBegin = Integer.MIN_VALUE;
    protected int mLoadableEnd = Integer.MAX_VALUE;
    protected final Deque<E> mElements = new Deque<E>();

    public VirtualFeed() {
    }

    public abstract void setLoadingRange(int begin, int end);

    public final E get(int index) {

        return null;

    }

    public final int getLoadedBegin() {
        return mLoadedBegin;
    }

    public final int getLoadedEnd() {
        return mLoadedEnd;
    }

    public final int getLoadingBegin() {
        return mLoadingBegin;
    }

    public final int getLoadingEnd() {
        return mLoadingEnd;
    }

    public final int getLoadableBegin() {
        return mLoadableBegin;
    }

    public final int getLoadableEnd() {
        return mLoadableEnd;
    }

    public interface RangeListener<E> {
        void onRangeUpdated(VirtualFeed<E> array);
    }
}
