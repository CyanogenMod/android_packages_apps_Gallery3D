// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

// Merge multiple media sets into one.
public class ComboMediaSet implements MediaSet {

    private final MediaSet[] mSets;

    public ComboMediaSet(MediaSet ... mediaSets) {
        mSets = mediaSets;
    }

    public MediaItem[] getCoverMediaItems() {
        throw new UnsupportedOperationException();
    }

    public MediaItem getMediaItem(int index) {
        for (MediaSet set : mSets) {
            int size = set.getMediaItemCount();
            if (index < size) {
                return set.getMediaItem(index);
            }
            index -= size;
        }
        throw new IndexOutOfBoundsException();
    }

    public int getMediaItemCount() {
        int count = 0;
        for (MediaSet set : mSets) {
            count += set.getMediaItemCount();
        }
        return count;
    }

    public MediaSet getSubMediaSet(int index) {
        for (MediaSet set : mSets) {
            int size = set.getSubMediaSetCount();
            if (index < size) {
                return set.getSubMediaSet(index);
            }
            index -= size;
        }
        throw new IndexOutOfBoundsException();
    }

    public int getSubMediaSetCount() {
        int count = 0;
        for (MediaSet set : mSets) {
            count += set.getSubMediaSetCount();
        }
        return count;
    }

    public String getTitle() {
        return null;
    }

    public int getTotalMediaItemCount() {
        int count = 0;
        for (MediaSet set : mSets) {
            count += set.getTotalMediaItemCount();
        }
        return count;
    }

    public void setContentListener(MediaSetListener listener) {
        for (MediaSet set : mSets) {
            set.setContentListener(listener);
        }
    }
}
