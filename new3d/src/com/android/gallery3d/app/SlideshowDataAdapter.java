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

package com.android.gallery3d.app;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.gallery3d.app.SlideshowPage.ModelListener;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MediaSet.MediaSetListener;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.Utils;

import java.util.ArrayList;
import java.util.LinkedList;

public class SlideshowDataAdapter implements SlideshowPage.Model {
    private static final String TAG = "SlideshowDataAdapter";

    private static final int IMAGE_QUEUE_CAPACITY = 3;

    private static final int MSG_LOAD_MEDIA_ITEM = 0;
    private static final int MSG_FILL_BITMAP = 1;

    private static final int MSG_LOAD_DATA = 2;
    private static final int MSG_UPDATE_DATA = 3;

    private final MediaSet mSource;

    private int mIndex = 0;
    private int mSize = 0;

    private LinkedList<Bitmap> mImageQueue = new LinkedList<Bitmap>();
    private int mImageRequestCount = IMAGE_QUEUE_CAPACITY;

    private Handler mMainHandler;
    private Handler mDataHandler;

    private SlideshowPage.ModelListener mListener;

    public SlideshowDataAdapter(GalleryContext context, MediaSet source) {
        mSource = source;
        mSource.setContentListener(new SourceListener());

        mMainHandler = new SynchronizedHandler(context.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_DATA:
                        ((ReloadTask) message.obj).updateContent();
                        break;
                    case MSG_FILL_BITMAP:
                        ((LoadNextImageTask) message.obj).fillBitmap();
                        break;
                    default: throw new AssertionError(message.what);
                }
            }
        };

        mDataHandler = new Handler(context.getDataManager().getDataLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_LOAD_DATA:
                        ((ReloadTask) message.obj).reloadData();
                        break;
                    case MSG_LOAD_MEDIA_ITEM:
                        ((LoadNextImageTask) message.obj).loadMediaItem();
                        break;
                    default:
                        throw new AssertionError(message.what);
                }
            }
        };

        new ReloadTask().execute();
    }

    public boolean hasNext() {
        return !mImageQueue.isEmpty();
    }

    public Bitmap nextSlideBitmap() {
        new LoadNextImageTask(mIndex++).execute();
        return mImageQueue.removeFirst();
    }

    private class ReloadTask {
        private int mUpdateSize = -1;

        public void execute() {
            mDataHandler.sendMessage(
                    mDataHandler.obtainMessage(MSG_LOAD_DATA, this));
        }

        public void reloadData() {
            mSource.reload();
            mUpdateSize = mSource.getMediaItemCount();
            mMainHandler.sendMessage(
                    mMainHandler.obtainMessage(MSG_UPDATE_DATA, this));
        }

        public void updateContent() {
            int size = mSize;
            mSize = mUpdateSize;
            if (size == mIndex && size < mUpdateSize && mImageRequestCount > 0) {
                --mImageRequestCount;
                new LoadNextImageTask(mIndex++).execute();
            }
        }
    }

    private class LoadNextImageTask implements FutureListener<Bitmap>{
        private int mItemIndex;
        private MediaItem mItem;
        private Bitmap mBitmap;

        public LoadNextImageTask(int index) {
            mItemIndex = index;
        }

        public void execute() {
            mDataHandler.sendMessage(
                    mDataHandler.obtainMessage(MSG_LOAD_MEDIA_ITEM, this));
        }

        public void loadMediaItem() {
            ArrayList<MediaItem> list = mSource.getMediaItem(mItemIndex, 1);
            // If list is empty, assume it is the end of the list
            if (!list.isEmpty()) {
                mItem = list.get(0);
                mItem.requestImage(MediaItem.TYPE_FULL_IMAGE, this);
            }
        }

        public void onFutureDone(Future<? extends Bitmap> future) {
            try {
                mBitmap = future.get();
                if (mBitmap != null) {
                    mBitmap = Utils.resizeBitmap(mBitmap, 640);
                }
            } catch (Throwable e) {
                Log.w(TAG, "fail to get bitmap", e);
            }
            mMainHandler.sendMessage(
                    mMainHandler.obtainMessage(MSG_FILL_BITMAP, this));
        }

        public void fillBitmap() {
            if (mBitmap != null) {
                mImageQueue.addLast(mBitmap);
                if (mImageQueue.size() == 1 && mListener != null) {
                    mListener.onContentChanged();
                }
            }
            if (mImageRequestCount > 0 && mIndex < mSize) {
                --mImageRequestCount;
                new LoadNextImageTask(mIndex++).execute();
            }
        }
    }

    private class SourceListener implements MediaSetListener {
        public void onContentDirty() {
            new ReloadTask().execute();
        }
    }

    public void setListener(ModelListener listener) {
        mListener = listener;
    }
}
