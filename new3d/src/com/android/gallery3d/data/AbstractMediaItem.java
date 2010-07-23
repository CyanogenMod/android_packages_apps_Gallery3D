// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.graphics.Bitmap;

//
//AbstractMediaItem is an abstract class captures those common fields
//in ImageMediaItem and VideoMediaItem.
//
public abstract class AbstractMediaItem implements MediaItem {

    protected Bitmap mBitmap[];
    protected int mBitmapStatus[];
    protected int mRequestId[];

    private MediaItemListener mListener;
    protected final ImageService mImageService;

    protected AbstractMediaItem(ImageService imageService) {
        mImageService = imageService;
        mBitmap = new Bitmap[TYPE_COUNT];
        mBitmapStatus = new int[TYPE_COUNT];
        mRequestId = new int[TYPE_COUNT];
    }

    public Bitmap getImage(int type) {
        return mBitmap[type];
    }

    public synchronized int requestImage(int type) {
        if (mBitmap[type] == null && mBitmapStatus[type] == IMAGE_READY) {
            //  Initial state: the image is not requested yet!
            mBitmapStatus[type] = IMAGE_WAIT;
            mRequestId[type] = mImageService.requestImage(this, type);
        }
        return mBitmapStatus[type];
    }

    public synchronized void cancelImageRequest(int type) {
        if (mBitmapStatus[type] != IMAGE_WAIT) return;
        mImageService.cancelRequest(mRequestId[type]);
    }

    protected synchronized void onImageReady(int type, Bitmap bitmap) {
        mBitmap[type] = bitmap;
        mBitmapStatus[type] = IMAGE_READY;
        if (mListener != null) mListener.onImageReady(this, type, bitmap);
    }

    protected synchronized void onImageError(int type, Throwable e) {
        mBitmapStatus[type] = IMAGE_ERROR;
        if (mListener != null) mListener.onImageError(this, type, e);
    }

    protected synchronized void onImageCanceled(int type) {
        mBitmap[type] = null;
        mBitmapStatus[type] = IMAGE_READY;
        if (mListener != null) mListener.onImageCanceled(this, type);
    }

    public void setListener(MediaItemListener listener) {
        mListener = listener;
    }

    abstract protected Bitmap generateImage(
            ContentResolver resolver, int type) throws Exception;
    abstract protected void cancelImageGeneration(
            ContentResolver resolver, int type);

}
