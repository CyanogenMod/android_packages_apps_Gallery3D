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

import java.util.ArrayList;

// MediaSet is a directory-like data structure.
// It contains MediaItems and sub-MediaSets.
//
// The primary interface are:
// getMediaItemCount(), getMediaItem() and
// getSubMediaSetCount(), getSubMediaSet().
//
// getTotalMediaItemCount() returns the number of all MediaItems, including
// those in sub-MediaSets.
public abstract class MediaSet {

    public interface MediaSetListener {
        public void onContentChanged();
    }

    public int getMediaItemCount() {
        return 0;
    }

    // Returns the media items in the range [start, start + count).
    //
    // The number of media items returned may be less than the specified count
    // if there are not enough media items available. The number of
    // media items available may not be consistent with the return value of
    // getMediaItemCount() because the contents of database may have already
    // changed.
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        throw new IndexOutOfBoundsException();
    }

    // This is for compatibility only.
    public MediaItem getMediaItem(int index) {
        ArrayList<MediaItem> items = getMediaItem(index, 1);
        if (items.size() > 0) {
            return items.get(0);
        } else {
            return null;
        }
    }

    // This is for compatibility only.
    public MediaItem[] getCoverMediaItems() {
        if (getMediaItemCount() > 0) {
            ArrayList<MediaItem> items = getMediaItem(0, 4);
            MediaItem result[] = new MediaItem[items.size()];
            return items.toArray(result);
        } else if (getSubMediaSetCount() > 0) {
            return getSubMediaSet(0).getCoverMediaItems();
        } else {
            return new MediaItem[0];
        }
    }

    public int getSubMediaSetCount() {
        return 0;
    }

    public MediaSet getSubMediaSet(int index) {
        throw new IndexOutOfBoundsException();
    }

    public abstract int getTotalMediaItemCount();

    public abstract long getUniqueId();
    public abstract String getName();

    protected MediaSetListener mListener;

    public void setContentListener(MediaSetListener listener) {
        mListener = listener;
    }

    public abstract void reload();

    public abstract int getSupportedOperations();
    public abstract boolean supportOpeation(int operation);
    public abstract void delete();
}
