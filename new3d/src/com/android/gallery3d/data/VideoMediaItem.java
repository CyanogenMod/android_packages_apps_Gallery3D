package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;

public class VideoMediaItem extends DatabaseMediaItem {
    private static final int MICRO_TARGET_PIXELS = 128 * 128;
    public int mDurationInSec;

    @Override
    protected void cancelImageGeneration(ContentResolver resolver, int type) {
        Video.Thumbnails.cancelThumbnailRequest(resolver, mId);
    }

    @Override
    protected Bitmap generateImage(ContentResolver resolver, int type) {
        switch (type) {
            // Return a MINI_KIND bitmap in the cases of TYPE_FULL_IMAGE
            // and TYPE_THUMBNAIL.
            case TYPE_FULL_IMAGE:
            case TYPE_THUMBNAIL:
                return Video.Thumbnails.getThumbnail(
                        resolver, mId, Images.Thumbnails.MINI_KIND, null);
            case TYPE_MICROTHUMBNAIL:
                Bitmap bitmap = Video.Thumbnails.getThumbnail(
                        resolver, mId, Images.Thumbnails.MINI_KIND, null);
                return bitmap == null
                        ? null
                        : Utils.resize(bitmap, MICRO_TARGET_PIXELS);
            default:
                throw new IllegalArgumentException();
        }
    }
}
