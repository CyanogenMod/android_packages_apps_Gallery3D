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

import android.test.AndroidTestCase;

import java.util.ArrayList;

public class MediaSetMock extends MediaSet {
    ArrayList<MediaItem> mItems = new ArrayList<MediaItem>();
    ArrayList<MediaSet> mSets = new ArrayList<MediaSet>();
    long mUniqueId;

    public MediaSetMock(long id) {
        mUniqueId = id;
    }

    public MediaSetMock(long id, int items, long item_id_start) {
        mUniqueId = id;
        for (int i = 0; i < items; i++) {
            mItems.add(new MediaItemMock(item_id_start + i));
        }
    }

    public void addMediaSet(MediaSet sub) {
        mSets.add(sub);
    }

    public int getMediaItemCount() {
        return mItems.size();
    }

    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        ArrayList<MediaItem> result = new ArrayList<MediaItem>();
        int end = Math.min(start + count, mItems.size());

        for (int i = start; i < end; i++) {
            result.add(mItems.get(i));
        }
        return result;
    }

    public int getSubMediaSetCount() {
        return mSets.size();
    }

    public MediaSet getSubMediaSet(int index) {
        return mSets.get(index);
    }

    public int getTotalMediaItemCount() {
        int result = mItems.size();
        for (MediaSet s : mSets) {
            result += s.getTotalMediaItemCount();
        }
        return result;
    }

    public long getUniqueId() {
        return mUniqueId;
    }

    public String getName() {
        return "Set " + mUniqueId;
    }

    public void reload() {
        mListener.onContentChanged();
    }
}
