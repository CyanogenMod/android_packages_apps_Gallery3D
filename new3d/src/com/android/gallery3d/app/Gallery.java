/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.android.gallery3d.R;
import com.android.gallery3d.data.LocalMediaSet;
import com.android.gallery3d.data.MediaDbAccessor;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.ui.Compositor;
import com.android.gallery3d.ui.GLHandler;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.ui.GridSlotAdapter;
import com.android.gallery3d.ui.MediaSetSlotAdapter;
import com.android.gallery3d.ui.SlotView;

public final class Gallery extends Activity implements SlotView.SlotTapListener {
    public static final String REVIEW_ACTION = "com.android.gallery3d.app.REVIEW";

    private static final int CHANGE_BACKGROUND = 1;

    private static final String TAG = "Gallery";
    private GLRootView mGLRootView;
    private GLHandler mHandler;
    private Compositor mCompositor;
    private MediaSet mRootSet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);
        mHandler = new GLHandler(mGLRootView) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case CHANGE_BACKGROUND:
                        mCompositor.changeBackground();
                        mHandler.sendEmptyMessageDelayed(CHANGE_BACKGROUND, 3000);
                        break;
                }
            }
        };

        mRootSet = MediaDbAccessor.getMediaSets(this);
        mCompositor = new Compositor(this);
        SlotView slotView = mCompositor.getSlotView();
        slotView.setModel(new MediaSetSlotAdapter(this, mRootSet));
        slotView.setSlotTapListener(this);
        mGLRootView.setContentPane(mCompositor);
    }

    public void onSingleTapUp(int slotIndex) {
        mCompositor.getSlotView().setModel(
                new GridSlotAdapter(this, mRootSet.getSubMediaSet(slotIndex)));
    }

    @Override
    public void onRestart() {
        super.onRestart();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mGLRootView.onResume();
        mHandler.sendEmptyMessageDelayed(CHANGE_BACKGROUND, 3000);
    }

    @Override
    public void onPause() {
        super.onPause();
        mGLRootView.onPause();
        mHandler.removeMessages(CHANGE_BACKGROUND);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    private boolean isPickIntent() {
        String action = getIntent().getAction();
        return (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action));
    }

    private boolean isViewIntent() {
        String action = getIntent().getAction();
        return Intent.ACTION_VIEW.equals(action);
    }

    private boolean isReviewIntent() {
        String action = getIntent().getAction();
        return REVIEW_ACTION.equals(action);
    }

    private boolean isImageType(String type) {
        return type.contains("*/") || type.equals("vnd.android.cursor.dir/image") || type.equals("image/*");
    }

    private boolean isVideoType(String type) {
        return type.contains("*/") || type.equals("vnd.android.cursor.dir/video") || type.equals("video/*");
    }
}
