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

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaOperation;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.util.ComboFuture;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.FutureTask;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class MenuHandler extends Handler {
    private static final String TAG = "MenuHandler";
    private ProgressDialog mDialog;
    private MenuTask mCurrentTask;
    private Handler mMainHandler;

    public MenuHandler(GalleryContext context) {
        super(context.getDataManager().getDataLooper());
        mDialog = new ProgressDialog(context.getAndroidContext()) {
            public void onBackPressed() {
                mCurrentTask.requestCancel();
            }
        };
        mMainHandler = new Handler(context.getMainLooper());
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    }

    private class MenuTask extends ComboFuture<Void> {
        private class CallableMediaOperation implements Callable<Void> {
            MediaItem mItem;
            int mOperation;

            public CallableMediaOperation(MediaItem item, int operation) {
                mItem = item;
                mOperation = operation;
            }

            public Void call() throws Exception {
                switch (mOperation) {
                    case MediaOperation.DELETE:
                        mItem.delete();
                        break;
                }
                return null;
            }
        }

        ArrayList<MediaItem> mItems;
        int mOperation;
        int mIndex;
        boolean mCancelled;

        public MenuTask(int operation, final int title, MediaSet parent, Set<Integer> selectedSet,
                boolean isSubSet, FutureListener<Void> listener) {
            super(listener);
            mOperation = operation;

            // calculate how many items to operate
            int total = 0;
            if (isSubSet) {
                for (int index : selectedSet) {
                    MediaSet subset = parent.getSubMediaSet(index);
                    total += subset.getMediaItemCount();
                }
            } else {
                total = selectedSet.size();
            }

            // add media item to process queue
            mItems = new ArrayList<MediaItem>(total);
            mIndex = 0;
            for (int index : selectedSet) {
                if (isSubSet) {
                    MediaSet subset = parent.getSubMediaSet(index);
                    int setCount = subset.getMediaItemCount();
                    for (int i = 0; i < setCount; i++) {
                        mItems.add(subset.getMediaItem(i));
                    }
                } else {
                    mItems.add(parent.getMediaItem(index));
                }
            }

            final int max = total;
            mMainHandler.post(new Runnable() {
                public void run() {
                    mDialog.setTitle(title);
                    mDialog.setMax(max);
                    mDialog.show();
                    mDialog.setProgress(0);
                }
            });

//          start the first operation
            execute();
        }

        @Override
        protected Future<?> executeNextTask(int index, Future<?> current) throws Exception {
            if (mCancelled || index >= mItems.size()) return null;
            FutureTask task = new FutureTask<Void>(new CallableMediaOperation(mItems.get(index),
                    mOperation), this);
            post(task);
            return task;
        }

        public synchronized void onFutureDone(Future<?> currentTask) {
            mMainHandler.post(new Runnable() {
                public void run() {
                    mDialog.setProgress(++mIndex);
                }
            });
            // move on to next item
            super.onFutureDone(currentTask);
        }

        @Override
        protected synchronized void onCancel() {
            super.onCancel();
            mCancelled = true;
            mMainHandler.post(new Runnable() {
                public void run() {
                    mDialog.dismiss();
                }
            });
        }
    }

    public void handleMediaItemOperation(int operation, int title, MediaSet parent,
            Set<Integer> selectedSet, boolean isSubSet) {
        mCurrentTask = new MenuTask(operation, title, parent, selectedSet, isSubSet,
                new FutureListener<Void>() {
                    public void onFutureDone(Future<? extends Void> future) {
                        mMainHandler.post(new Runnable() {
                            public void run() {
                                mDialog.dismiss();
                            }
                        });
                    }
                });
    }

    public void onPause() {
        if (mCurrentTask != null) {
            mCurrentTask.requestCancel();
            try {
                mCurrentTask.get();
            } catch (CancellationException ce) {
                // ignore it.
            } catch (ExecutionException ee) {
                // ignore it.
            } catch (InterruptedException ie) {
                // ignore it.
            }
            mCurrentTask = null;
        }
        mDialog.dismiss();
    }
}
