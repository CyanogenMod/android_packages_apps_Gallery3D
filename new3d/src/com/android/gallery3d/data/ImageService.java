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
import android.graphics.Bitmap;
import android.os.Process;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;

import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureHelper;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.Utils;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageService {
    private static final String TAG = "ImageService";

    private static final int MICRO_TARGET_PIXELS = 128 * 128;
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 1;
    private static final int KEEP_ALIVE_TIME = 10000; // 10 seconds

    private final Executor mExecutor = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
            new MyThreadFactory());

    private final ContentResolver mContentResolver;

    public ImageService(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
    }

    public Future<Bitmap> requestImageThumbnail(
            int id, int type, FutureListener<? super Bitmap> listener) {
        if (type != MediaItem.TYPE_MICROTHUMBNAIL
                && type != MediaItem.TYPE_THUMBNAIL) {
            throw new IllegalArgumentException(String.format("type = %s", type));
        }
        GetImageThumbnail task =
                new GetImageThumbnail(id, type, mContentResolver, listener);
        mExecutor.execute(task);
        return task;
    }

    public Future<Bitmap> requestVideoThumbnail(
            int id, int type, FutureListener<? super Bitmap> listener) {
        if (type != MediaItem.TYPE_MICROTHUMBNAIL
                && type != MediaItem.TYPE_THUMBNAIL
                && type != MediaItem.TYPE_FULL_IMAGE) {
            throw new IllegalArgumentException(String.format("type = %s", type));
        }
        GetVideoThumbnail task =
            new GetVideoThumbnail(id, type, mContentResolver, listener);
        mExecutor.execute(task);
        return task;
    }

    private static class GetImageThumbnail extends FutureHelper<Bitmap> implements Runnable {
        private final int STATE_READY = 0;
        private final int STATE_RUNNING = 1;
        private final int STATE_CANCELED = 2;
        private final int STATE_RAN = 4;

        // mState tries to guard that onCancel() is called only when
        // getThumbnail() is being executed.
        private AtomicInteger mState = new AtomicInteger(STATE_READY);
        private final int mId;
        private final int mType;
        private final ContentResolver mResolver;

        public GetImageThumbnail(int id, int type, ContentResolver resolver,
                FutureListener<? super Bitmap> listener) {
            super(listener);
            mId = id;
            mType = type;
            mResolver= resolver;
        }

        public void run() {
            Bitmap bitmap = null;
            try {
                bitmap = getThumbnail();
            } catch (Throwable throwable) {
                setException(throwable);
                return;
            }
            if (!isCancelled()) {
                if (bitmap == null && isCancelling()) {
                    cancelled();
                } else {
                    setResult(bitmap);
                }
            }
        }

        @Override
        protected void onCancel() {
            if (mState.compareAndSet(STATE_READY, STATE_CANCELED)) {
                cancelled();
            } else if (mState.compareAndSet(STATE_RUNNING, STATE_CANCELED)) {
                switch (mType) {
                    case MediaItem.TYPE_THUMBNAIL:
                    case MediaItem.TYPE_MICROTHUMBNAIL:
                        // TODO: MediaProvider doesn't provide a way to specify
                        //       which kind of thumbnail to be canceled. We should
                        //       try to fix the issue in MediaProvider or here.
                        Images.Thumbnails.cancelThumbnailRequest(mResolver, mId);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        }

        private Bitmap getThumbnail() {
            Bitmap result = null;
            int kind;

            switch (mType) {
                case MediaItem.TYPE_THUMBNAIL:
                    kind = Images.Thumbnails.MINI_KIND;
                    break;
                case MediaItem.TYPE_MICROTHUMBNAIL:
                    kind = Images.Thumbnails.MICRO_KIND;
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            if (mState.compareAndSet(STATE_READY, STATE_RUNNING)) {
                result = Images.Thumbnails.getThumbnail(
                        mResolver, mId, kind, null);
                mState.compareAndSet(STATE_RUNNING, STATE_RAN);
            }

            return result;
        }
    }

    private static class GetVideoThumbnail extends FutureHelper<Bitmap> implements Runnable {
        private final int STATE_READY = 0;
        private final int STATE_RUNNING = 1;
        private final int STATE_CANCELED = 2;
        private final int STATE_RAN = 4;

        private AtomicInteger mState = new AtomicInteger(STATE_READY);
        private final int mId;
        private final int mType;
        private final ContentResolver mResolver;

        public GetVideoThumbnail(int id, int type, ContentResolver resolver,
                FutureListener<? super Bitmap> listener) {
            super(listener);
            mId = id;
            mType = type;
            mResolver= resolver;
        }

        public void run() {
            Bitmap bitmap = null;
            try {
                bitmap = getThumbnail();
            } catch (Throwable throwable) {
                setException(throwable);
                return;
            }
            if (!isCancelled()) {
                if (bitmap == null && isCancelling()) {
                    cancelled();
                } else {
                    setResult(bitmap);
                }
            }
        }

        @Override
        protected void onCancel() {
            if (mState.compareAndSet(STATE_READY, STATE_CANCELED)) {
                cancelled();
            } else if (mState.compareAndSet(STATE_RUNNING, STATE_CANCELED)) {
                // TODO: fix the issue that we cannot cancel only one request
                Video.Thumbnails.cancelThumbnailRequest(mResolver, mId);
            }
        }

        private Bitmap getThumbnail() {
            Bitmap result = null;
            int kind;

            switch (mType) {
                case MediaItem.TYPE_FULL_IMAGE:
                case MediaItem.TYPE_THUMBNAIL:
                    kind = Video.Thumbnails.MINI_KIND;
                    break;
                case MediaItem.TYPE_MICROTHUMBNAIL:
                    kind = Video.Thumbnails.MICRO_KIND;
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            if (mState.compareAndSet(STATE_READY, STATE_RUNNING)) {
                result = Video.Thumbnails.getThumbnail(
                        mResolver, mId, kind, null);
                mState.compareAndSet(STATE_RUNNING, STATE_RAN);
            }

            return result;
        }
    }

    class MyThreadFactory implements ThreadFactory {
        public Thread newThread(final Runnable r) {
            return new Thread() {
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    r.run();
                }
            };
        }
    }
}
