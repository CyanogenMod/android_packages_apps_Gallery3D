package com.android.gallery3d.data;

import android.graphics.Bitmap;

public interface MediaItem {
    public static final int IMAGE_TYPE_NUMBER = 3;
    public static final int TYPE_FULL_IMAGE = 0;
    public static final int TYPE_THUMBNAIL = 1;
    public static final int TYPE_MICROTHUMBNAIL = 2;

    public static final int IMAGE_READY = 0;
    public static final int IMAGE_WAIT = 1;
    public static final int IMAGE_ERROR = -1;

    public interface MediaItemListener {
        public void onImageReady(MediaItem item, int type, Bitmap bitmap);
        public void onImageError(MediaItem item, int type, Throwable error);
        public void onImageCanceled(MediaItem abstractMediaItem, int type);
    }

    public String getTitle();

    public Bitmap getImage(int type);

    public void setListener(MediaItemListener listener);

    // Request an image of a certain type. Return the status of the image.
    // A caller should check the returned status before calling getImage() to
    // get the image.
    public int requestImage(int type);

    public void cancelImageRequest(int type);

}
