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
import android.os.Handler;
import android.os.Message;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Utils;

// DatabaseMediaSet is used as a base class for media sets loaded from a
// database. Subclasses need to implement the following two methods:
//
// onLoadFromDatabase(): Runs in the database thread. It can do blocking
// operations like reading from databases.
//
// onUpdateContent(): Runs in the main thread. It should not do blocking
// operations. It can update the media set using the data read in
// onLoadFromDatabase().
public abstract class DatabaseMediaSet extends MediaSet {

    private static final int MSG_LOAD_DATABASE = 0;
    private static final int MSG_UPDATE_CONTENT = 1;

    protected final Handler mMainHandler;
    protected final Handler mDbHandler;
    protected final GalleryContext mContext;
    protected final ContentResolver mResolver;

    // How many times do we need to reload: 1 means we are reloading,
    // 2 means after current reloading, we need to do another one.
    private int mReloadCount;

    protected DatabaseMediaSet(GalleryContext context) {
        mContext = context;
        mResolver = mContext.getContentResolver();

        mMainHandler = new SynchronizedHandler(context.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.Assert(message.what == MSG_UPDATE_CONTENT);
                onUpdateContent();
                if (mListener != null) mListener.onContentChanged();

                synchronized (DatabaseMediaSet.this) {
                    // If we still have pending reload, do it now.
                    if (--mReloadCount > 0) {
                        mDbHandler.sendEmptyMessage(MSG_LOAD_DATABASE);
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

    public synchronized void reload() {
        // If we already have reload pending, just return.
        if (mReloadCount >= 2) return;
        // If this is the first reload, start it.
        if (++mReloadCount == 1) {
            mDbHandler.sendEmptyMessage(MSG_LOAD_DATABASE);
        }
    }

    abstract protected void onLoadFromDatabase();
    abstract protected void onUpdateContent();
}
