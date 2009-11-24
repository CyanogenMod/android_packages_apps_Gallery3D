package com.cooliris.media;

public final class IndexRange {
    public IndexRange(int beginRange, int endRange) {
        begin = beginRange;
        end = endRange;
    }

    public IndexRange() {
        begin = 0;
        end = 0;
    }

    public void set(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    public boolean isEmpty() {
        return begin == end;
    }

    public int size() {
        return end - begin;
    }

    public int begin;
    public int end;
}
