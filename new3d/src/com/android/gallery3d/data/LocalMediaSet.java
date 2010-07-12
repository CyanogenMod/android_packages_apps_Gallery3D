package com.android.gallery3d.data;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//
// LocalMediaSet is a MediaSet which is obtained from MediaProvider.
// Each LocalMediaSet is identified by a bucket id.
// It is populated by addMediaItem(MediaItem) and addSubMediaSet(LocalMediaSet).
//
// getSubMediaSetById(int setId) returns a sub-MediaSet given a bucket id.
//
public class LocalMediaSet implements MediaSet {
    public static final int ROOT_SET_ID = -1;
    private static final String TAG = "LocalMediaSet";

    private static final int MAX_NUM_COVERED_ITEMS = 4;

    private final ArrayList<LocalMediaSet> mSubMediaSets =
            new ArrayList<LocalMediaSet>();
    private final Map<Integer, Integer> mIdsToIndice =
            new HashMap<Integer, Integer>();

    private final ArrayList<MediaItem> mMediaItems = new ArrayList<MediaItem>();

    private final int mBucketId;
    private final String mTitle;

    public LocalMediaSet(int bucketId, String title) {
        mBucketId = bucketId;
        mTitle = title;
    }

    public int getBucketId() {
        return mBucketId;
    }

    public MediaItem[] getCoverMediaItems() {
        MediaItem[] coverItems = new MediaItem[MAX_NUM_COVERED_ITEMS];
        int filled = fillCoverMediaItems(coverItems, 0);
        if (filled < MAX_NUM_COVERED_ITEMS) {
            MediaItem[] result = new MediaItem[filled];
            System.arraycopy(coverItems, 0, result, 0, filled);
            return result;
        } else {
            return coverItems;
        }
    }

    private int fillCoverMediaItems(MediaItem[] items, int offset) {
        // Fill from my MediaItems.
        int size = Math.min(getMediaItemCount(), items.length - offset);
        for (int i = 0; i < size; ++i) {
            items[offset + i] = getMediaItem(i);
        }
        if (offset + size == items.length) return size;

        // Fill from sub-MediaSets.
        int n = getSubMediaSetCount();
        for (int i = 0; i < n; ++i) {
            size += mSubMediaSets.get(i).fillCoverMediaItems(items, offset + size);
            if (offset + size == items.length) break;
        }

        return size;
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
            totalItemCount += set.getTotalMediaItemCount();
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

    public void setContentListener(MediaSetListener listener) {
    }
}
