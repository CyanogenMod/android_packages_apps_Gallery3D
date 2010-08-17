/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.data;

import android.util.Log;

import java.util.Comparator;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.SortedMap;

// MergeAlbum merges items from two or more MediaSets. It uses a Comparator to
// determine the order of items. The items are assumed to be sorted in the input
// media sets (with the same order that the Comparator uses).
//
// This only handles MediaItems, not SubMediaSets.
public class MergeAlbum extends MediaSet implements MediaSet.MediaSetListener {
    private static final String TAG = "MergeAlbum";
    private final long mUniqueId;
    private final int mPageSize;
    private final Comparator<MediaItem> mComparator;
    private final MediaSet[] mSets;
    private final int mSize;  // caches mSets.length
    private FetchCache[] mFetcher;

    // mIndex maps global position to the position of each underlying media sets.
    private TreeMap<Integer, int[]> mIndex;

    public MergeAlbum(long uniqueId, int pageSize, Comparator<MediaItem> comparator,
            MediaSet[] mediaSets) {
        mUniqueId = uniqueId;
        mPageSize = pageSize;
        mComparator = comparator;
        mSets = mediaSets;
        mSize = mSets.length;
        mFetcher = new FetchCache[mSize];
        for (int i = 0; i < mSize; i++) {
            MediaSet s = mSets[i];
            s.setContentListener(this);
            mFetcher[i] = new FetchCache(s, mPageSize);
        }
        mIndex = new TreeMap<Integer, int[]>();
        mIndex.put(0, new int[mSize]);
    }

    private void invalidateCache() {
        for (int i = 0; i < mSize; i++) {
            mFetcher[i].invalidate();
        }
        mIndex.clear();
        mIndex.put(0, new int[mSize]);
    }

    public long getUniqueId() {
        return mUniqueId;
    }

    public String getName() {
        return TAG;
    }

    public int getMediaItemCount() {
        return getTotalMediaItemCount();
    }

    public ArrayList<MediaItem> getMediaItem(int start, int count) {

        // First find the nearest mark position <= start.
        SortedMap<Integer, int[]> head = mIndex.headMap(start + 1);
        int markPos = head.lastKey();
        int[] subPos = (int []) head.get(markPos).clone();
        MediaItem[] slot = new MediaItem[mSize];

        // fill all slots
        for (int i = 0; i < mSize; i++) {
            slot[i] = mFetcher[i].getItem(subPos[i]);
        }

        ArrayList<MediaItem> result = new ArrayList<MediaItem>();

        for (int i = markPos; i < start + count; i++) {
            int k = -1;  // k points to the best slot up to now.
            for (int j = 0; j < mSize; j++) {
                if (slot[j] != null) {
                    if (k == -1 || mComparator.compare(slot[j], slot[k]) < 0) {
                        k = j;
                    }
                }
            }

            // If we don't have anything, all streams are exhausted.
            if (k == -1) break;

            // Pick the best slot and refill it.
            subPos[k]++;
            if (i >= start) {
                result.add(slot[k]);
            }
            slot[k] = mFetcher[k].getItem(subPos[k]);

            // Periodically leave a mark in the index, so we can come back later.
            if ((i + 1) % mPageSize == 0) {
                mIndex.put(i + 1, (int[]) subPos.clone());
            }
        }

        return result;
    }

    public int getTotalMediaItemCount() {
        int count = 0;
        for (MediaSet set : mSets) {
            count += set.getTotalMediaItemCount();
        }
        return count;
    }

    public void reload() {
        for (MediaSet set : mSets) {
            set.reload();
        }
    }

    public void onContentChanged() {
        invalidateCache();
        if (mListener != null) {
            mListener.onContentChanged();
        }
    }
}

class FetchCache {
    private static final String TAG = "FetchCache";
    private MediaSet mBaseSet;
    private int mPageSize;
    private ArrayList<MediaItem> mCache;
    private int mStartPos;

    FetchCache(MediaSet baseSet, int pageSize) {
        mBaseSet = baseSet;
        mPageSize = pageSize;
    }

    void invalidate() {
        mCache = null;
    }

    MediaItem getItem(int index) {
        boolean needLoading = false;
        if (mCache == null) {
            needLoading = true;
        } else if (index < mStartPos || index >= mStartPos + mPageSize) {
            needLoading = true;
        }

        if (needLoading) {
            mCache = mBaseSet.getMediaItem(index, mPageSize);
            mStartPos = index;
        }

        if (index < mStartPos || index >= mStartPos + mCache.size()) {
            return null;
        }

        return mCache.get(index - mStartPos);
    }
}
