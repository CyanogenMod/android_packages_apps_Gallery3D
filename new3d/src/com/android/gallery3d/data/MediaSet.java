package com.android.gallery3d.data;

//
// MediaSet is a directory-like data structure.
// It contains MediaItems and sub-MediaSets.
//
// getTotalMediaItemCount() returns the number of all MediaItems, including
// those in sub-MediaSets.
//
// getCoverMediaItems() return a few representative MediaItems for this
// MediaSet.
//
public interface MediaSet {

    public interface MediaSetListener {
        public void onContentChanged();
    }

    public int getMediaItemCount();

    public MediaItem getMediaItem(int index);

    public int getSubMediaSetCount();

    public MediaSet getSubMediaSet(int index);

    public int getTotalMediaItemCount();

    public String getTitle();

    public MediaItem[] getCoverMediaItems();
}
