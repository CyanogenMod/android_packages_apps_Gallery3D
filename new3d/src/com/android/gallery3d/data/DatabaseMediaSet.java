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

import android.os.Handler;
import android.os.Message;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Utils;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class DatabaseMediaSet extends MediaSet {

    private static final int MSG_LOAD_DATABASE = 0;
    private static final int MSG_UPDATE_CONTENT = 1;

    private static final int BIT_INVALIDATING = 1;
    private static final int BIT_PENDING = 2;

    protected final Handler mMainHandler;
    protected final Handler mDbHandler;
    protected final GalleryContext mContext;

    protected MediaSetListener mListener;
    private AtomicInteger mState = new AtomicInteger();

    protected DatabaseMediaSet(GalleryContext context) {
        mContext = context;

        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Utils.Assert(message.what == MSG_UPDATE_CONTENT);
                onUpdateContent();
                if (mListener != null) mListener.onContentChanged();

                while (true) {
                    int s = mState.get();

                    // Either (1) resets the the pending bit and sets the
                    //            invalidating bit, or
                    //        (2) resets the state to 0 if the pending bit was
                    //            originally cleared.
                    int t = (s & BIT_PENDING) == 0 ? 0 : BIT_INVALIDATING;
                    if (mState.compareAndSet(s, t)) {
                        if (t == BIT_INVALIDATING) {
                            // Case 1: clear the pending bit by loading data
                            //         from database.
                            mDbHandler.sendEmptyMessage(MSG_LOAD_DATABASE);
                        }
                        break;
                    }
                }
            }
        };

        mDbHandler = new Handler(context.getDataManager().getDataLooper()) {
            @Override
            public void handleMessage(Message message) {
                Utils.Assert(message.what == MSG_LOAD_DATABASE);
                onLoadFromDatabase();
                mMainHandler.sendEmptyMessage(MSG_UPDATE_CONTENT);
            }
        };
    }

    public void invalidate() {
        while (true) {
            int s = mState.get();

            // State is moved either to (1) invalidating, or (2) invalidating and pending.
            int t = (s & BIT_INVALIDATING) == 0
                    ? BIT_INVALIDATING
                    : BIT_INVALIDATING | BIT_PENDING;
            if (mState.compareAndSet(s, t)) {
                if (t == BIT_INVALIDATING) {
                    // Case 1: loading data from database.
                    mDbHandler.sendEmptyMessage(MSG_LOAD_DATABASE);
                }
                break;
            }
        }
    }

    public void setContentListener(MediaSetListener listener) {
        mListener = listener;
    }

    abstract protected void onLoadFromDatabase();
    abstract protected void onUpdateContent();
}
