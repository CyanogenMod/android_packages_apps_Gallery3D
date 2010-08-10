/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.graphics.Bitmap;

import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureHelper;
import com.android.gallery3d.util.FutureListener;

//
// LocalMediaItem is an abstract class captures those common fields
// in LocalImage and LocalVideo.
//
public abstract class LocalMediaItem extends MediaItem {

    private static final String TAG = "LocalMediaItem";

    // database fields
    protected int mId;
    protected String mCaption;
    protected String mMimeType;
    protected double mLatitude;
    protected double mLongitude;
    protected long mDateTakenInMs;
    protected long mDateAddedInSec;
    protected long mDateModifiedInSec;
    protected String mFilePath;

    protected int mRequestId[];
    private MyFuture mFutureBitmaps[];

    protected final ImageService mImageService;

    protected LocalMediaItem(ImageService imageService) {
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
