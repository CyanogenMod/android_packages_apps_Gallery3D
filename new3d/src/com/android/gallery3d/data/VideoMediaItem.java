package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.provider.MediaStore.Images;

public class VideoMediaItem extends AbstractMediaItem {
    public int mDurationInSec;

    public Bitmap getImage(ContentResolver cr, int type) {
        Bitmap bitmap = null;
        switch (type) {
            // Return a MINI_KIND bitmap in the cases of TYPE_FULL_IMAGE and TYPE_THUMBNAIL.
            case TYPE_FULL_IMAGE:
            case TYPE_THUMBNAIL:
                bitmap = Utils.getVideoThumbnail(cr, mId, Images.Thumbnails.MINI_KIND);
                break;

            case TYPE_MICROTHUMBNAIL:
                bitmap = Utils.getVideoThumbnail(cr, mId, Images.Thumbnails.MICRO_KIND);
                break;
        }
        return bitmap;
    }

    public void setListener(MediaItemListener listener) {
        // TODO Auto-generated method stub

    }
}
