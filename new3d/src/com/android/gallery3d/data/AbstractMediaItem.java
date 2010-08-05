// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.graphics.Bitmap;

import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureHelper;
import com.android.gallery3d.util.FutureListener;

//
//AbstractMediaItem is an abstract class captures those common fields
//in ImageMediaItem and VideoMediaItem.
//
public abstract class AbstractMediaItem implements MediaItem {

    private static final String TAG = AbstractMediaItem.class.getSimpleName();
    protected int mRequestId[];
    private MyFuture mFutureBitmaps[];

    protected final ImageService mImageService;

    @SuppressWarnings("unchecked")
    protected AbstractMediaItem(ImageService imageService) {
        mImageService = imageService;
        mFutureBitmaps = new MyFuture[TYPE_COUNT];
        mRequestId = new int[TYPE_COUNT];
    }

    public synchronized Future<Bitmap>
            requestImage(int type, FutureListener<? super Bitmap> listener) {
        if (mFutureBitmaps[type] != null) {
            // TODO: we should not allow overlapped requests
            return null;
        } else {
            mFutureBitmaps[type] = new MyFuture(type, listener);
            mRequestId[type] = mImageService.requestImage(this, type);
            return mFutureBitmaps[type];
        }
    }

    private synchronized void cancelImageRequest(int type) {
        mImageService.cancelRequest(mRequestId[type]);
        mFutureBitmaps[type] = null;
    }

    protected synchronized void onImageReady(int type, Bitmap bitmap) {
        FutureHelper<Bitmap> helper = mFutureBitmaps[type];
        mFutureBitmaps[type] = null;
        if (helper != null) helper.setResult(bitmap);
    }

    protected synchronized void onImageError(int type, Throwable e) {
        FutureHelper<Bitmap> helper = mFutureBitmaps[type];
        mFutureBitmaps[type] = null;
        if (helper != null) helper.setException(e);
    }

    protected synchronized void onImageCanceled(int type) {
        FutureHelper<Bitmap> helper = mFutureBitmaps[type];
        if (helper != null) helper.cancelled();
    }

    abstract protected Bitmap generateImage(
            ContentResolver resolver, int type) throws Exception;

    abstract protected void cancelImageGeneration(
            ContentResolver resolver, int type);

    private class MyFuture extends FutureHelper<Bitmap> {
        private final int mSizeType;

        public MyFuture(int sizeType, FutureListener<? super Bitmap> listener) {
            super(listener);
            mSizeType = sizeType;
        }

        @Override
        public void onCancel() {
            cancelImageRequest(mSizeType);
        }
    }

}
