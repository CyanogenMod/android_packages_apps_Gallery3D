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

package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.android.gallery3d.data.FutureListener;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.ui.ImageViewer.ImageData;

import java.util.ArrayList;
import java.util.concurrent.Future;

public class PhotoView extends StateView implements SlotView.SlotTapListener {
    private static final String TAG = PhotoView.class.getSimpleName();

    public static final String KEY_SET_INDEX = "keySetIndex";
    public static final String KEY_PHOTO_INDEX = "keyPhotoIndex";

    private static final int MSG_UPDATE_SCREENNAIL = 1;
    private static final int MSG_UPDATE_FULLIMAGE = 2;

    private static final int TARGET_LENGTH = 1600;
    private static final int MIPMAPS_MIN_LENGTH = 480;

    private SynchronizedHandler mHandler;

    private ImageViewer mImageViewer;
    private final MyImageViewerModel mModel = new MyImageViewerModel();
    private int mSetIndex;
    private int mPhotoIndex;

    private MediaSet mMediaSet;

    @Override
    public void onStart(Bundle data) {
        mHandler = new SynchronizedHandler(getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_SCREENNAIL: {
                        mModel.updateScreenNail(message.arg1, (Bitmap) message.obj);
                        break;
                    }
                    case MSG_UPDATE_FULLIMAGE: {
                        mModel.updateFullImage(message.arg1, (Bitmap) message.obj);
                        break;
                    }
                    default: throw new AssertionError();
                }
            }
        };

        mSetIndex = data.getInt(KEY_SET_INDEX);
        mPhotoIndex = data.getInt(KEY_PHOTO_INDEX);

        mMediaSet = mContext.getDataManager().getSubMediaSet(mSetIndex);

        mImageViewer = new ImageViewer(mContext);
        mImageViewer.setModel(mModel);
        addComponent(mImageViewer);
        mModel.requestNextImage();
    }


    @Override
    public void onPause() {
        lockRendering();
        try {
            mImageViewer.close();
        } finally {
            unlockRendering();
        }
    }

    @Override
    protected void onLayout(
            boolean changed, int left, int top, int right, int bottom) {
        if (mImageViewer != null) {
            mImageViewer.layout(0, 0, right - left, bottom - top);
        }
    }

    public void onSingleTapUp(int slotIndex) {
    }

    private class MyImageViewerModel implements ImageViewer.Model {

        private Bitmap mScaledBitmaps[];
        private Bitmap mScreenNails[] = new Bitmap[3]; // prev, curr, next

        public Bitmap[] getMipmaps() {
            return mScaledBitmaps;
        }

        public ImageData getImageData(int which) {
            Bitmap screennail = mScreenNails[which];
            if (screennail == null) return null;

            int width = 0;
            int height = 0;

            if (which == INDEX_CURRENT && mScaledBitmaps != null) {
                width = mScaledBitmaps[0].getWidth();
                height = mScaledBitmaps[0].getHeight();
            } else {
                // We cannot get the size of image before getting the
                // full-size image. In the future, we should add the data to
                // database or get it from the header in runtime. Now, we
                // just use the thumb-nail image to estimate the size
                float scale = (float) TARGET_LENGTH / Math.max(
                        screennail.getWidth(), screennail.getHeight());
                width = Math.round(screennail.getWidth() * scale);
                height = Math.round(screennail.getHeight() * scale);
            }
            return new ImageData(width, height, screennail);
        }

        public void next() {
            ++mPhotoIndex;
            Bitmap[] screenNails = mScreenNails;

            if (screenNails[INDEX_PREVIOUS] != null) {
                screenNails[INDEX_PREVIOUS].recycle();
            }
            screenNails[INDEX_PREVIOUS] = screenNails[INDEX_CURRENT];
            screenNails[INDEX_CURRENT] = screenNails[INDEX_NEXT];
            screenNails[INDEX_NEXT] = null;


            if (mScaledBitmaps != null) {
                for (Bitmap bitmap : mScaledBitmaps) {
                    bitmap.recycle();
                }
                mScaledBitmaps = null;
            }

            requestNextImage();
        }

        public void previous() {
            --mPhotoIndex;
            Bitmap[] screenNails = mScreenNails;

            if (screenNails[INDEX_NEXT] != null) {
                screenNails[INDEX_NEXT].recycle();
            }
            screenNails[INDEX_NEXT] = screenNails[INDEX_CURRENT];
            screenNails[INDEX_CURRENT] = screenNails[INDEX_PREVIOUS];
            screenNails[INDEX_PREVIOUS] = null;

            if (mScaledBitmaps != null) {
                for (Bitmap bitmap : mScaledBitmaps) {
                    bitmap.recycle();
                }
                mScaledBitmaps = null;
            }

            requestNextImage();
        }

        public void updateScreenNail(int index, Bitmap screenNail) {
            int offset = (index - mPhotoIndex) + 1;
            if (offset < 0 || offset > 2) {
                screenNail.recycle();
                return;
            }

            if (screenNail != null) {
                mScreenNails[offset] = screenNail;
                mImageViewer.notifyScreenNailInvalidated(offset);
            }
            requestNextImage();
        }

        public void updateFullImage(int index, Bitmap fullImage) {
            int offset = (index - mPhotoIndex) + 1;
            if (offset != INDEX_CURRENT) {
                fullImage.recycle();
                return;
            }
            Log.v(TAG, String.format("full image %d available: %s %s",
                    index, fullImage.getWidth(), fullImage.getHeight()));

            if (fullImage != null) {
                mScaledBitmaps = getScaledBitmaps(fullImage, MIPMAPS_MIN_LENGTH);
                mImageViewer.notifyMipmapsInvalidated();
                // We need to update the estimated width and height
                mImageViewer.notifyScreenNailInvalidated(INDEX_CURRENT);
            }
            requestNextImage();
        }

        public void requestNextImage() {
            int setSize = mMediaSet.getMediaItemCount();

            if (setSize == 0) return;

            // First request the current screen nail
            if (mScreenNails[INDEX_CURRENT] == null) {
                MediaItem current = mMediaSet.getMediaItem(mPhotoIndex);
                current.requestImage(MediaItem.TYPE_THUMBNAIL,
                        new ScreenNailListener(mPhotoIndex));
                return;
            }

            // Next, the next screen nail
            if (mScreenNails[INDEX_NEXT] == null && mPhotoIndex + 1 < setSize) {
                MediaItem next = mMediaSet.getMediaItem(mPhotoIndex + 1);
                next.requestImage(MediaItem.TYPE_THUMBNAIL,
                        new ScreenNailListener(mPhotoIndex + 1));
                return;
            }

            // Next, the previous screen nail
            if (mScreenNails[INDEX_PREVIOUS] == null && mPhotoIndex > 0) {
                MediaItem previous = mMediaSet.getMediaItem(mPhotoIndex - 1);
                previous.requestImage(MediaItem.TYPE_THUMBNAIL,
                        new ScreenNailListener(mPhotoIndex - 1));
                return;
            }

            // Next, the full size image
            if (mScaledBitmaps == null) {
                MediaItem current = mMediaSet.getMediaItem(mPhotoIndex);
                current.requestImage(MediaItem.TYPE_FULL_IMAGE,
                        new FullImageListener(mPhotoIndex));
                return;
            }
        }
    }

    public void onLongTap(int slotIndex) {
        // TODO
    }

    public void setImageViewer(ImageViewer imageViewer) {
        // TODO modify ImageViewer to accepting a data model
        removeComponent(mImageViewer);
        mImageViewer = imageViewer;
        addComponent(mImageViewer);
        requestLayout();
    }

    @Override
    protected void renderBackground(GLCanvas view) {
        view.clearBuffer();
    }

    private static Bitmap[] getScaledBitmaps(Bitmap bitmap, int minLength) {
        Config config = bitmap.hasAlpha()
                ? Config.ARGB_8888 : Config.RGB_565;

        int width = bitmap.getWidth() / 2;
        int height = bitmap.getHeight() / 2;

        ArrayList<Bitmap> list = new ArrayList<Bitmap>();
        list.add(bitmap);
        while (width > minLength || height > minLength) {
            Bitmap half = Bitmap.createBitmap(width, height, config);
            Canvas canvas = new Canvas(half);
            canvas.scale(0.5f, 0.5f);
            canvas.drawBitmap(bitmap, 0, 0, null);
            width /= 2;
            height /= 2;
            bitmap = half;
            list.add(bitmap);
        }
        return list.toArray(new Bitmap[list.size()]);
    }

    private class ScreenNailListener implements FutureListener<Bitmap> {

        private final int mIndex;

        public ScreenNailListener(int index) {
            mIndex = index;
        }

        public void onFutureDone(Future<? extends Bitmap> future) {
            Bitmap bitmap = null;
            try {
                bitmap = future.get();
            } catch (Exception e) {
                Log.v(TAG, "fail to get image", e);
            }
            mHandler.sendMessage(mHandler.obtainMessage(
                    MSG_UPDATE_SCREENNAIL, mIndex, 0, bitmap));
        }
    }

    private class FullImageListener implements FutureListener<Bitmap> {

        private final int mIndex;

        public FullImageListener(int index) {
            mIndex = index;
        }

        public void onFutureDone(Future<? extends Bitmap> future) {
            Bitmap bitmap = null;
            try {
                bitmap = future.get();
            } catch (Exception e) {
                Log.v(TAG, "fail to get image", e);
            }
            mHandler.sendMessage(mHandler.obtainMessage(
                    MSG_UPDATE_FULLIMAGE, mIndex, 0, bitmap));
        }
    }
}
