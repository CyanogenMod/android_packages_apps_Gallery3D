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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

// MergeAlbumSet merges two or more media sets into one.
// If the the input media sets have sub media sets with the item id,
// they are merged into one media set.
public class MergeAlbumSet extends MediaSet implements MediaSet.MediaSetListener {
    private static final String TAG = "MergeAlbumSet";
    private static final int PAGE_SIZE = 100;
    private final long mUniqueId;
    private Comparator<MediaItem> mComparator;
    private final MediaSet[] mSets;
    private MediaSet[] mAlbums;

    public MergeAlbumSet(long uniqueId, Comparator<MediaItem> comparator,
            MediaSet ... mediaSets) {
        mUniqueId = uniqueId;
        mComparator = comparator;
        mSets = mediaSets;
        updateNames();
        for (MediaSet set : mediaSets) {
            set.setContentListener(this);
        }
    }

    public long getUniqueId() {
        return mUniqueId;
    }

    private void updateNames() {

        // This map maps from a the item id to a list of media sets.
        // The list of media sets are the media set with that item id.
        TreeMap<Integer, ArrayList<MediaSet>> map =
                new TreeMap<Integer, ArrayList<MediaSet>>();
        for (MediaSet set : mSets) {
            for (int i = 0, n = set.getSubMediaSetCount(); i < n; i++) {
                MediaSet subset = set.getSubMediaSet(i);
                int itemId = DataManager.extractItemId(subset.getUniqueId());
                ArrayList<MediaSet> list = map.get(itemId);
                if (list == null) {
                    list = new ArrayList<MediaSet>();
                    map.put(itemId, list);
                }
                list.add(subset);
            }
        }

        int size = map.size();
        mAlbums = new MediaSet[size];

        int i = 0;
        for (Map.Entry<Integer, ArrayList<MediaSet>> entry : map.entrySet()) {
            ArrayList<MediaSet> list = entry.getValue();
            if (list.size() == 1) {
                mAlbums[i] = list.get(0);
            } else {
                int itemId = entry.getKey();
                long id = DataManager.makeId(DataManager.ID_MERGE_LOCAL_ALBUM,
                        itemId);
                MediaSet[] sets = new MediaSet[list.size()];
                mAlbums[i] = new MergeAlbum(id, PAGE_SIZE, mComparator,
                        list.toArray(sets));
            }
            i = i + 1;
        }
    }

    public MediaSet getSubMediaSet(int index) {
        return mAlbums[index];
    }

    public int getSubMediaSetCount() {
        return mAlbums.length;
    }

    public String getName() {
        return TAG;
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
        updateNames();
        if (mListener != null) {
            mListener.onContentChanged();
        }
    }

    @Override
    public void delete() {
    }

    @Override
    public int getSupportedOperations() {
        return 0;
    }

    @Override
    public boolean supportOpeation(int operation) {
        return false;
    }
}
