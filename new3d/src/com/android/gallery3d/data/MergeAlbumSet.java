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

import com.android.gallery3d.util.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

// MergeAlbumSet merges two or more media sets into one.
// If the the input media sets have sub media sets with the item id,
// they are merged into one media set.
public class MergeAlbumSet extends MediaSet implements MediaSet.MediaSetListener {
    private static final String TAG = "MergeAlbumSet";
    private static final int PAGE_SIZE = 100;
    private final long mUniqueId;
    private final DataManager mDataManager;
    private Comparator<MediaItem> mComparator;
    private final MediaSet[] mSets;
    private ArrayList<MergeAlbum> mAlbums = new ArrayList<MergeAlbum>();
    private TreeSet<Integer> mIds = new TreeSet<Integer>();

    public MergeAlbumSet(DataManager dataManager, int parentId, int childKey,
            Comparator<MediaItem> comparator, MediaSet ... mediaSets) {
        mUniqueId = dataManager.obtainSetId(parentId, childKey, this);
        mDataManager = dataManager;
        mComparator = comparator;
        mSets = mediaSets;
        updateIds();
        for (MediaSet set : mediaSets) {
            set.setContentListener(this);
        }
    }

    @Override
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
                int subId = subset.getMergeId();
                allIds.add(subId);
            }
        }
        // Update all existing albums.
        for (MergeAlbum album : mAlbums) {
            album.updateData();
        }

        // If there are new ids, create new albums for them.
        for (int subId : allIds) {
            if (mIds.contains(subId)) continue;
            mIds.add(subId);
            mAlbums.add(new MergeAlbum(mDataManager,
                    getMyId(), PAGE_SIZE, mComparator, mSets, subId));
        }
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    @Override
    public int getSubMediaSetCount() {
        return mAlbums.size();
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public int getTotalMediaItemCount() {
        int count = 0;
        for (MediaSet set : mSets) {
            count += set.getTotalMediaItemCount();
        }
        return count;
    }

    @Override
    public boolean reload() {
        boolean changed = false;
        for (MediaSet set : mSets) {
            changed |= set.reload();
        }
        if (changed) updateIds();
        return changed;
    }

    public void onContentDirty() {
        if (mListener != null) mListener.onContentDirty();
    }

    @Override
    public int getSupportedOperations(long uniqueId) {
        return SUPPORT_DELETE;
    }

    @Override
    public void delete(long uniqueId) {
        Utils.Assert(DataManager.extractParentId(uniqueId) == getMyId());

        int childId = DataManager.extractSelfId(uniqueId);
        MergeAlbum child = (MergeAlbum) mDataManager.getMediaSet(childId);
        child.deleteSelf();
    }
}
