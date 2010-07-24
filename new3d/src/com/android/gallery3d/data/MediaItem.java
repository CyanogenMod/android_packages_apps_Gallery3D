package com.android.gallery3d.data;

import android.graphics.Bitmap;

import java.util.concurrent.Future;

public interface MediaItem {
    public static final int TYPE_COUNT = 3;
    public static final int TYPE_FULL_IMAGE = 0;
    public static final int TYPE_THUMBNAIL = 1;
    public static final int TYPE_MICROTHUMBNAIL = 2;

    public static final int IMAGE_READY = 0;
    public static final int IMAGE_WAIT = 1;
    public static final int IMAGE_ERROR = -1;

    public String getTitle();

    public Future<Bitmap> requestImage(int type, FutureListener<? super Bitmap> listener);
}
