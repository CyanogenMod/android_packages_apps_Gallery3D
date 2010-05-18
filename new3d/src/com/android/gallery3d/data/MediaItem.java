package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.graphics.Bitmap;

public interface MediaItem {
    public static final int TYPE_FULL_IMAGE = 0;
    public static final int TYPE_THUMBNAIL = 1;
    public static final int TYPE_MICROTHUMBNAIL = 2;

    public interface MediaItemListener {
        public void onImageReady(MediaItem item, int type, Bitmap bitmap);
    }

    public String getMediaUri();

    public String getTitle();

    public Bitmap getImage(ContentResolver cr, int type);

    public void setListener(MediaItemListener listener);
}
