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

import android.os.Handler;

import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.FutureTask;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class MenuExecutor {
    private static final String TAG = "MenuExecutor";

    public static final int ACTION_DELETE = 1;
    public static final int ACTION_DETAILS = 2;
    public static final int ACTION_ROTATE_CW = 3;
    public static final int ACTION_ROTATE_CCW = 4;

    private Handler mHandler;
    private DataManager mDataManager;

    public MenuExecutor(DataManager manager) {
        mDataManager = manager;
        mHandler = new Handler(manager.getDataLooper());
    }

    public static interface OnProgressUpdateListener {
        public void onProgressUpdate(int index, Object result);
        public void onProgressComplete();
    }

    public class MediaOperation implements Callable<Void>, FutureListener<Void> {
        ArrayList<Long> mItems;
        int mOperation;
        int mIndex;
        FutureTask<Void> mTask;
        OnProgressUpdateListener mProgressUpdater;

        public MediaOperation(int operation, ArrayList<Long> items,
                OnProgressUpdateListener progressUpdater) {
            mItems = items;
            mIndex = 0;
            mOperation = operation;
            mProgressUpdater = progressUpdater;
        }

        public Void call() throws Exception {
            for (long id : mItems) {
                if (mTask.isCancelled()) return null;

                Object result = null;
                switch (mOperation) {
                    case ACTION_DELETE:
                        mDataManager.delete(id);
                        break;
                    case ACTION_ROTATE_CW:
                        mDataManager.rotate(id, -90);
                        break;
                    case ACTION_ROTATE_CCW:
                        mDataManager.rotate(id, 90);
                        break;
                    case ACTION_DETAILS:
                        result = mDataManager.getDetails(id);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                mProgressUpdater.onProgressUpdate(mIndex++, result);
            }
            return null;
        }

        public void setTask(FutureTask<Void> task) {
            mTask = task;
        }

        public void onFutureDone(Future<? extends Void> future) {
            mProgressUpdater.onProgressComplete();
        }

        public void cancel() {
            mTask.requestCancel();
            try {
                mTask.get();
            } catch (CancellationException ce) {
                // ignore it.
            } catch (ExecutionException ee) {
                // ignore it.
            } catch (InterruptedException ie) {
                // ignore it.
            }
        }
    }

    public MediaOperation startMediaOperation(int opcode, ArrayList<Long> items,
            final OnProgressUpdateListener listener) {
        MediaOperation operation = new MediaOperation(opcode, items, listener);
        FutureTask<Void> task = new FutureTask<Void>(operation, operation);
        operation.setTask(task);
        mHandler.post(task);
        return operation;
    }
}
