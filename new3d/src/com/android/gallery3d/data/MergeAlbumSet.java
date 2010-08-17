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
import java.util.TreeSet;

// MergeAlbumSet merges two or more media sets into one.
// If the the input media sets have sub media sets with the item id,
// they are merged into one media set.
public class MergeAlbumSet extends MediaSet implements MediaSet.MediaSetListener {
    private static final String TAG = "MergeAlbumSet";
    private static final int PAGE_SIZE = 100;
    private final long mUniqueId;
    private Comparator<MediaItem> mComparator;
    private final MediaSet[] mSets;
    private ArrayList<MergeAlbum> mAlbums = new ArrayList<MergeAlbum>();
    private TreeSet<Integer> mIds = new TreeSet<Integer>();

    public MergeAlbumSet(long uniqueId, Comparator<MediaItem> comparator,
            MediaSet ... mediaSets) {
        mUniqueId = uniqueId;
        mComparator = comparator;
        mSets = mediaSets;
        updateIds();
        for (MediaSet set : mediaSets) {
            set.setContentListener(this);
        }
    }

    public long getUniqueId() {
        return mUniqueId;
    }

    private void updateIds() {

        // allIds keeps all item id of the (sub) media sets in the album sets
        // we want to merge.
        TreeSet<Integer> allIds = new TreeSet<Integer>();
        for (MediaSet set : mSets) {
            for (int i = 0, n = set.getSubMediaSetCount(); i < n; i++) {
                MediaSet subset = set.getSubMediaSet(i);
                int itemId = DataManager.extractItemId(subset.getUniqueId());
                allIds.add(itemId);
            }
        }

        // Update all existing albums.
        for (MergeAlbum album : mAlbums) {
            album.updateData();
        }

        // If there are new ids, create new albums for them.
        for (int itemId : allIds) {
            if (mIds.contains(itemId)) continue;
            mIds.add(itemId);
            long newId = DataManager.makeId(DataManager.ID_MERGE_LOCAL_ALBUM,
                    itemId);
            mAlbums.add(new MergeAlbum(newId, PAGE_SIZE, mComparator, mSets, itemId));
        }
    }

    public MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    public int getSubMediaSetCount() {
        return mAlbums.size();
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
        updateIds();
        if (mListener != null) {
            mListener.onContentChanged();
        }
    }
}
