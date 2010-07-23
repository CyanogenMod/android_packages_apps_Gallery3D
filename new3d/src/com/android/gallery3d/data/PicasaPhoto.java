// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import android.graphics.Bitmap;

import com.android.gallery3d.picasa.PhotoEntry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class PicasaPhoto implements MediaItem {
    private final int mBitmapStatus[] = new int[MediaItem.TYPE_COUNT];
    private final Bitmap mBitmap[] = new Bitmap[MediaItem.TYPE_COUNT];

    private final Future<?>[] mTasks = new Future<?>[MediaItem.TYPE_COUNT];

    private final PhotoEntry mData;
    private MediaItemListener mListener;
    private int mFullSizeRequestCount = 0;

    public PicasaPhoto(PhotoEntry entry) {
        mData = entry;
    }

    public String getTitle() {
        return null;
    }

    public void cancelImageRequest(int type) {
        if (mBitmapStatus[type] == IMAGE_WAIT) {
            mBitmapStatus[type] = IMAGE_READY;
            if (type == MediaItem.TYPE_THUMBNAIL) type = MediaItem.TYPE_FULL_IMAGE;
            if (type == MediaItem.TYPE_FULL_IMAGE) {
                if (--mFullSizeRequestCount > 0) return;
            }
            Future<?> task = mTasks[type];
            if (task != null) task.cancel(true);
        }
    }

    public Bitmap getImage(int type) {
        return mBitmap[type];
    }

    public int requestImage(int type) {
        if (mBitmap[type] == null && mBitmapStatus[type] == IMAGE_READY) {
            //  Initial state: the image is not requested yet!
            DownloadService service = DownloadService.getInstance();
            mBitmapStatus[type] = IMAGE_WAIT;
            if (type == MediaItem.TYPE_THUMBNAIL) type = MediaItem.TYPE_FULL_IMAGE;

            try {
                switch (type) {
                    case MediaItem.TYPE_MICROTHUMBNAIL: {
                        mTasks[type] = new PicasaPhotoTask(
                                new URL(mData.thumbnailUrl),
                                new PhotoListener(type));
                        break;
                    }
                    case MediaItem.TYPE_FULL_IMAGE: {
                        ++ mFullSizeRequestCount;
                        if (mTasks[type] == null) {
                            mTasks[type] = new PicasaPhotoTask(
                                    new URL(mData.contentUrl),
                                    new PhotoListener(type));
                        }
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(String.valueOf(type));
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return mBitmapStatus[type];
    }

    private class PicasaPhotoTask extends UberFuture<Bitmap> {

        private final URL mUrl;

        public PicasaPhotoTask(URL url, FutureListener<Bitmap> listener) {
            super(listener);
            mUrl = url;
            super.execute();
        }

        @Override
        protected FutureTask<?> executeNextTask(int index, FutureTask<?> current)
                throws Exception {
            switch (index) {
                case 0: {
                    DownloadService service = DownloadService.getInstance();
                    return service.requestDownload(mUrl, this);
                }
                case 1: {
                    byte[] buffer = (byte[]) current.get();
                    DecodeService service = DecodeService.getInstance();
                    return service.requestDecode(buffer, null, this);
                }
            }
            return null;
        }

    }

    private class PhotoListener implements FutureListener<Bitmap> {
        private final int mType;

        public PhotoListener(int type) {
            mType = type;
        }

        public void onFutureDone(Future<? extends Bitmap> future) {
            if (future.isCancelled()) {
                if (mListener != null) {
                    mListener.onImageCanceled(PicasaPhoto.this, mType);
                    if (mType == MediaItem.TYPE_FULL_IMAGE) {
                        mListener.onImageCanceled(
                                PicasaPhoto.this, MediaItem.TYPE_THUMBNAIL);
                    }
                }
            } else {
                try {
                    Bitmap bitmap = future.get();
                    mBitmap[mType] = bitmap;
                    mBitmapStatus[mType] = MediaItem.IMAGE_READY;
                    if (mListener != null) {
                        mListener.onImageReady(PicasaPhoto.this, mType, bitmap);
                        if (mType == MediaItem.TYPE_FULL_IMAGE) {
                            mListener.onImageReady(PicasaPhoto.this,
                                    MediaItem.TYPE_THUMBNAIL, bitmap);
                        }
                    }
                } catch (Exception e) {
                    if (mListener != null) {
                        mListener.onImageError(PicasaPhoto.this, mType, e);
                        if (mType == MediaItem.TYPE_FULL_IMAGE) {
                            mListener.onImageError(PicasaPhoto.this,
                                    MediaItem.TYPE_THUMBNAIL, e);
                        }
                    }
                }
            }
        }
    }

    public void setListener(MediaItemListener listener) {
        mListener = listener;
    }
}
