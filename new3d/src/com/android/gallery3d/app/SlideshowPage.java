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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.SlideshowView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Utils;

public class SlideshowPage extends ActivityState {
    private static final String TAG = "SlideshowPage";

    public static final String KEY_BUCKET_INDEX = "keyBucketIndex";

    private static final long SLIDESHOW_DELAY = 3000; // 2 seconds
    private static final int MSG_SHOW_NEXT_SLIDE = 1;

    public static interface Model {
        public boolean hasNext();
        public Bitmap nextSlideBitmap();
        public void setListener(ModelListener listener);
    }

    public static interface ModelListener {
        public void onContentChanged();
    }

    private Handler mHandler;
    private Model mModel;
    private SlideshowView mSlideshowView;
    private boolean mSlideshowActive = false;

    private GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            mSlideshowView.layout(0, 0, right - left, bottom - top);
        }

        @Override
        protected void renderBackground(GLCanvas canvas) {
            canvas.clearBuffer();
        }
    };

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        mHandler = new SynchronizedHandler(mContext.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.Assert(message.what == MSG_SHOW_NEXT_SLIDE);
                showNextSlide();
            }
        };
        initializeViews();
        intializeData(data);
    }

    private void showNextSlide() {
        if (!mModel.hasNext()) {
            mSlideshowActive = false;
            return;
        }
        mSlideshowActive = true;
        mSlideshowView.next(mModel.nextSlideBitmap());
        mHandler.sendEmptyMessageDelayed(MSG_SHOW_NEXT_SLIDE, SLIDESHOW_DELAY);
    }

    @Override
    public void onPause() {
        mHandler.removeMessages(MSG_SHOW_NEXT_SLIDE);
    }

    @Override
    public void onResume() {
        mHandler.sendEmptyMessage(MSG_SHOW_NEXT_SLIDE);
    }

    private void initializeViews() {
        mSlideshowView = new SlideshowView();
        mRootPane.addComponent(mSlideshowView);
        setContentPane(mRootPane);
    }

    private void intializeData(Bundle data) {
        int bucketIndex = data.getInt(KEY_BUCKET_INDEX);
        MediaSet mediaSet = mContext.getDataManager()
                .getRootSet().getSubMediaSet(bucketIndex);
        SlideshowDataAdapter adapter =
                new SlideshowDataAdapter(mContext, mediaSet);
        setModel(adapter);
    }

    public void setModel(Model source) {
        mHandler.removeMessages(MSG_SHOW_NEXT_SLIDE);
        mModel = source;
        mModel.setListener(new MyModelListener());
        showNextSlide();
    }

    private class MyModelListener implements ModelListener {
        public void onContentChanged() {
            if (!mSlideshowActive) showNextSlide();
        }
    }
}
