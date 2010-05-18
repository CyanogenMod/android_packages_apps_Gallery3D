package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.net.Uri;

public interface MediaItem {
    public static final int TYPE_FULL_IMAGE = 0;
    public static final int TYPE_SCREEN_NAIL = 1;
    public static final int TYPE_THUMBNAIL = 2;

    public interface MediaItemListener {
        public void onImageReady(MediaItem item, int type, Bitmap bitmap);
    }

    public Uri getMediaUri();

    public String getTitle();

    public Bitmap getImage(int type);

    public void setListener(MediaItemListener listener);
}
