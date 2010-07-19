// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import android.graphics.Bitmap;

import com.android.gallery3d.picasa.PhotoEntry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class PicasaPhoto implements MediaItem {
    private final PicasaTask[] mTasks = new PicasaTask[MediaItem.TYPE_COUNT];

    private final PhotoEntry mData;

    public PicasaPhoto(PhotoEntry entry) {
        mData = entry;
    }

    public String getTitle() {
        return null;
    }

    private PicasaTask newPicasaTask(
                int type, FutureListener<? super Bitmap> listener) {
        URL url = null;
        try {
            if (type == MediaItem.TYPE_MICROTHUMBNAIL) {
                url = new URL(mData.thumbnailUrl);
            } else if (type == MediaItem.TYPE_THUMBNAIL) {
                url = new URL(mData.screennailUrl);
            } else if (type == MediaItem.TYPE_FULL_IMAGE) {
                url = new URL(mData.contentUrl);
            } else {
                throw new AssertionError();
            }
            return new PicasaTask(type, url, listener);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized Future<Bitmap>
            requestImage(int type, FutureListener<? super Bitmap> listener) {
        if (mTasks[type] != null) {
            throw new IllegalStateException();
        } else {
            mTasks[type] = newPicasaTask(type, listener);
        }
        return mTasks[type];
    }

    private class PicasaTask extends UberFuture<Bitmap> {
        private final URL mUrl;
        private final int mType;

        public PicasaTask(
                int type, URL url, FutureListener<? super Bitmap> listener) {
            super(listener);
            mType = type;
            mUrl = url;
            execute();
        }

        @Override
        protected void onDone() {
            super.onDone();
            synchronized (PicasaPhoto.this) {
                mTasks[mType] = null;
            }
        }

        @Override
        protected FutureTask<?> executeNextTask(int step, FutureTask<?> current)
                throws Exception {
            switch (step) {
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
}
