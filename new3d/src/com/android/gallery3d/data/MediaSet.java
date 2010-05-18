package com.android.gallery3d.data;

public interface MediaSet {

    public interface MediaSetListener {
        public void onContentChanged();
    }

    public int getSubMediaSetCount();

    public MediaSet getSubMediaSet(int index);

    public int getMediaCount();

    public MediaItem getMediaItem(int index);

    public int getTotalMediaItemCount();

    public String getTitle();

    public MediaItem[] getCoverMediaItems();
}
