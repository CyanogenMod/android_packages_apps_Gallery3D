package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.provider.MediaStore.Images;

public class ImageMediaItem extends AbstractMediaItem {
    public int mRotation;

    public Bitmap getImage(ContentResolver cr, int type) {
        Bitmap bitmap = null;
        switch (type) {
            case TYPE_FULL_IMAGE:
                bitmap = Utils.generateBitmap(mFilePath, FULLIMAGE_TARGET_SIZE,
                        FULLIMAGE_MAX_NUM_PIXELS);
                break;
            case TYPE_THUMBNAIL:
                bitmap = Utils.getImageThumbnail(cr, mId,
                        Images.Thumbnails.MINI_KIND);
                break;
            case TYPE_MICROTHUMBNAIL:
                bitmap = Utils.getImageThumbnail(cr, mId,
                        Images.Thumbnails.MICRO_KIND);
                break;
        }
        return bitmap;
    }

    public void setListener(MediaItemListener listener) {
        // TODO Auto-generated method stub

    }
}
