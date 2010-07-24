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

import java.util.ArrayList;
import java.util.concurrent.Future;

public class PhotoView extends StateView implements SlotView.SlotTapListener {
    private static final String TAG = PhotoView.class.getSimpleName();

    public static final String KEY_SET_INDEX = "keySetIndex";
    public static final String KEY_PHOTO_INDEX = "keyPhotoIndex";

    private static final int ON_IMAGE_READY = 1;

    private SynchronizedHandler mHandler;
    private ImageViewer mImageViewer;
    private Bitmap mScaledBitmaps[];
    private Bitmap mBackupBitmap;

    private int mSetIndex;
    private int mPhotoIndex;

    public PhotoView() {}

    @Override
    public void onStart(Bundle data) {
        mHandler = new SynchronizedHandler(getGLRootView()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case ON_IMAGE_READY:
                        setImageViewer(new ImageViewer(
                                mContext.getAndroidContext(), mScaledBitmaps,
                                mBackupBitmap));
                        break;
                }
            }
        };
        initializeData(data);
    }

    @Override
    public void onPause() {
        synchronized (getGLRootView()) {
            mImageViewer.close();
        }
    }

    private void initializeData(Bundle data) {
        mSetIndex = data.getInt(KEY_SET_INDEX);
        mPhotoIndex = data.getInt(KEY_PHOTO_INDEX);
        MediaSet set = mContext.getDataManager().getSubMediaSet(mSetIndex);
        MediaItem item = set.getMediaItem(mPhotoIndex);
        item.requestImage(MediaItem.TYPE_FULL_IMAGE, new MyMediaItemListener());
    }

    @Override
    protected void onLayout(
            boolean changed, int left, int top, int right, int bottom) {
        if (mImageViewer != null) {
            mImageViewer.layout(0, 0, right - left, bottom - top);
        }
    }

    public void onSingleTapUp(int slotIndex) {
        // TODO Auto-generated method stub

    }

    public void setImageViewer(ImageViewer imageViewer) {
        // TODO modify ImageViewer to accepting a data model
        removeComponent(mImageViewer);
        mImageViewer = imageViewer;
        addComponent(mImageViewer);
        requestLayout();
    }

    private class MyMediaItemListener implements FutureListener<Bitmap> {
        private static final int BACKUP_SIZE = 512;

        private Bitmap getBackupBitmap(Bitmap bitmap) {
            Config config = bitmap.hasAlpha()
                    ? Config.ARGB_8888 : Config.RGB_565;

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale = (float) BACKUP_SIZE / (width > height ? width : height);
            width = (int) (width * scale + 0.5f);
            height = (int) (height * scale + 0.5f);
            Bitmap result = Bitmap.createBitmap(width, height, config);
            Canvas canvas = new Canvas(result);
            canvas.scale(scale, scale);
            canvas.drawBitmap(bitmap, 0, 0, null);
            return result;
        }

        private Bitmap[] getScaledBitmaps(Bitmap bitmap) {
            Config config = bitmap.hasAlpha()
                    ? Config.ARGB_8888 : Config.RGB_565;

            int width = bitmap.getWidth() / 2;
            int height = bitmap.getHeight() / 2;

            ArrayList<Bitmap> list = new ArrayList<Bitmap>();
            list.add(bitmap);
            while (width > BACKUP_SIZE || height > BACKUP_SIZE) {
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

        public void onFutureDone(Future<? extends Bitmap> future) {
            try {
                Bitmap bitmap = future.get();
                mScaledBitmaps = getScaledBitmaps(bitmap);
                mBackupBitmap = getBackupBitmap(bitmap);
                mHandler.sendEmptyMessage(ON_IMAGE_READY);
            } catch (Exception e) {
                Log.e(TAG, "failt to get image", e);
            }
        }
    }

    @Override
    protected void renderBackground(GLCanvas view) {
        view.clearBuffer();
    }
}
