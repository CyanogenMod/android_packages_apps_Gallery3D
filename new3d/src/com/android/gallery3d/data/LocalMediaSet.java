package com.android.gallery3d.data;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LocalMediaSet implements MediaSet {
    public static final int ROOT_SET_ID = -1;
    private static final String TAG = "LocalMediaSet";

    private static final int MAX_NUM_COVERED_ITEMS = 4;

    private ArrayList<LocalMediaSet> mSubMediaSets =
            new ArrayList<LocalMediaSet>();
    private Map<Integer, Integer> mIdsToIndice =
            new HashMap<Integer, Integer>();

    private ArrayList<MediaItem> mMediaItems = new ArrayList<MediaItem>();

    private int mBucketId;
    private String mTitle;

    public LocalMediaSet(int bucketId, String title) {
        mBucketId = bucketId;
        mTitle = title;
    }

    public int getBucketId() {
        return mBucketId;
    }

    public MediaItem[] getCoverMediaItems() {
        int size = Math.min(getMediaItemCount(), MAX_NUM_COVERED_ITEMS);
        MediaItem[] coveredItems = new MediaItem[size];
        for (int i = 0; i < size; ++i) {
            coveredItems[i] = getMediaItem(i);
        }
        return coveredItems;
    }

    public MediaItem getMediaItem(int index) {
        return mMediaItems.get(index);
    }

    void addMediaItem(MediaItem item) {
        mMediaItems.add(item);
    }

    public MediaSet getSubMediaSetById(int setId) {
        Integer index = mIdsToIndice.get(setId);
        return index == null ? null
                : mSubMediaSets.get(index);
    }

    public int getSubMediaSetCount() {
        return mSubMediaSets.size();
    }

    public MediaSet getSubMediaSet(int index) {
        return mSubMediaSets.get(index);
    }

    public String getTitle() {
        return mTitle;
    }

    public int getMediaItemCount() {
        return mMediaItems.size();
    }

    public int getTotalMediaItemCount() {
        int totalItemCount = mMediaItems.size();
        for (LocalMediaSet set : mSubMediaSets) {
            totalItemCount += set.getMediaItemCount();
        }
        return totalItemCount;
    }

    public void addSubMediaSet(LocalMediaSet set) {
        if (mSubMediaSets.add(set)) {
            mIdsToIndice.put(set.mBucketId, mSubMediaSets.size() - 1);
        }
    }

    /*
     * Only for checking LocalMediaSet's content.
     */
    public void printOut() {
        Log.i(TAG, "Media Set, numItems: " + mTitle + ", " + getTotalMediaItemCount());

        for (MediaItem item : mMediaItems) {
            Log.i(TAG, "Media title: " + item.getTitle());
        }

        for (LocalMediaSet set: mSubMediaSets) {
            set.printOut();
        }
    }
}
