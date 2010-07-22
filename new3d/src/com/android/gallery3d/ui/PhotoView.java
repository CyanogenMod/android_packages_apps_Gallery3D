// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.ui;

import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.os.Message;

public class PhotoView extends StateView implements SlotView.SlotTapListener {
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
                                mContext, mScaledBitmaps,
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
        MediaSet set = DataManager.getInstance().getSubMediaSet(mSetIndex);
        MediaItem item = set.getMediaItem(mPhotoIndex);
        item.setListener(new MyMediaItemListener());
        item.requestImage(MediaItem.TYPE_FULL_IMAGE);
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

    private class MyMediaItemListener implements MediaItem.MediaItemListener {
        private static final int BACKUP_SIZE = 512;
        public MyMediaItemListener() {}

        public void onImageCanceled(MediaItem abstractMediaItem, int type) {
            // Do nothing
        }

        public void onImageError(MediaItem item, int type, Throwable error) {
            // Do nothing
        }

        public void onImageReady(MediaItem item, int type, Bitmap bitmap) {
            mScaledBitmaps = getScaledBitmaps(bitmap);
            mBackupBitmap = getBackupBitmap(bitmap);
            mHandler.sendEmptyMessage(ON_IMAGE_READY);
        }

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
    }

    @Override
    protected void renderBackground(GLCanvas view) {
        view.clearBuffer();
    }
}
