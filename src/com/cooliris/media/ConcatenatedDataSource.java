package com.cooliris.media;

import java.util.ArrayList;

import android.util.Log;

public final class ConcatenatedDataSource implements DataSource {
    private static final String TAG = "ConcatenatedDataSource";
    private final DataSource mFirst;
    private final DataSource mSecond;

    public ConcatenatedDataSource(DataSource first, DataSource second) {
        mFirst = first;
        mSecond = second;
    }

    public void loadMediaSets(final MediaFeed feed) {
        mFirst.loadMediaSets(feed);
        mSecond.loadMediaSets(feed);
    }

    public void loadItemsForSet(final MediaFeed feed, final MediaSet parentSet, int rangeStart, int rangeEnd) {
        if (parentSet != null) {
            DataSource dataSource = parentSet.mDataSource;
            if (dataSource != null) {
                dataSource.loadItemsForSet(feed, parentSet, rangeStart, rangeEnd);
            } else {
                Log.e(TAG, "MediaSet was not added to the feed");
            }
        }
    }

    public boolean performOperation(int operation, final ArrayList<MediaBucket> mediaBuckets, Object data) {
        ArrayList<MediaBucket> singleBucket = new ArrayList<MediaBucket>(1);
        singleBucket.add(null);
        int numBuckets = mediaBuckets.size();
        boolean retVal = true;
        for (int i = 0; i < numBuckets; ++i) { // CR: iterator for
            MediaBucket bucket = mediaBuckets.get(i);
            MediaSet set = bucket.mediaSet;
            if (set != null) {
                DataSource dataSource = set.mDataSource;
                if (dataSource != null) {
                    singleBucket.set(0, bucket);
                    retVal &= dataSource.performOperation(operation, singleBucket, data);
                } else {
                    Log.e(TAG, "MediaSet was not added to the feed");
                }
            }
        }
        return retVal;
    }

    public DiskCache getThumbnailCache() {
        throw new UnsupportedOperationException("ConcatenatedDataSource should not create MediaItems");
    }

    public void shutdown() {
        mFirst.shutdown();
        mSecond.shutdown();
    }
}
