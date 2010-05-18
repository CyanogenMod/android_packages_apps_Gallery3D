package com.android.gallery3d.data;

public interface MediaSet {

    public interface MediaSetListener {
        public void onContentChanged();
    }

    public int getSubMediaSetCount();

    public MediaItem getMediaItem(int index);

    public int getMediaItemCount();

    public int getTotalMediaItemCount();

    public String getTitle();

    public MediaItem[] getCoverMediaItems();

    public MediaSet getSubMediaSet(int index);
}
