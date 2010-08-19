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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.FutureTask;
import com.android.gallery3d.util.Utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DecodeService {
    private static final String TAG = "DecodeService";

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 1;
    private static final int KEEP_ALIVE_TIME = 10000; // 10 seconds
    private static final int JPEG_MARK_POSITION = 60 * 1024;

    private final Executor mExecutor = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    public Future<Bitmap> requestDecode(
            File file, Options options, FutureListener<? super Bitmap> listener) {
        if (options == null) options = new Options();
        FutureTask<Bitmap> task = new DecodeFutureTask(
                new DecodeFile(file, options), options, listener);
        mExecutor.execute(task);
        return task;
    }

    public FutureTask<Bitmap> requestDecode(
            byte[] bytes, Options options, FutureListener<? super Bitmap> listener) {
        return requestDecode(bytes, 0, bytes.length, options, listener);
    }

    public FutureTask<Bitmap> requestDecode(
            byte[] bytes, int offset, int length,
            Options options, FutureListener<? super Bitmap> listener) {
        if (options == null) options = new Options();
        if (offset < 0 || length <= 0 || offset + length > bytes.length) {
            throw new IllegalArgumentException(String.format(
                    "offset = %s, length = %s, bytes = %s",
                    offset, length, bytes.length));
        }
        FutureTask<Bitmap> task = new DecodeFutureTask(
                new DecodeByteArray(bytes, offset, length, options),
                options, listener);
        mExecutor.execute(task);
        return task;
    }

    public FutureTask<Bitmap> requestDecode(
            File file, Options options, int targetLength, int maxPixelCount,
            FutureListener<? super Bitmap> listener) {
        if (options == null) options = new Options();
        FutureTask<Bitmap> task = new DecodeFutureTask(
                new DecodeAndSampleFile(file, options, targetLength, maxPixelCount),
                options, listener);
        mExecutor.execute(task);
        return task;
    }

    private static class DecodeFutureTask extends FutureTask<Bitmap> {

        private final Options mOptions;

        public DecodeFutureTask(Callable<Bitmap> callable,
                Options options, FutureListener<? super Bitmap> listener) {
            super(callable, listener);
            mOptions = options;
        }

        @Override
        public void cancelTask() {
            mOptions.requestCancelDecode();
        }
    }

    private static class DecodeFile implements Callable<Bitmap> {

        private final File mFile;
        private final Options mOptions;

        public DecodeFile(File file, Options options) {
            mFile = file;
            mOptions = options;
        }

        public Bitmap call() throws Exception {
            return BitmapFactory.decodeFile(mFile.getAbsolutePath(), mOptions);
        }
    }

    private static class DecodeByteArray implements Callable<Bitmap> {
        private final byte[] mBytes;
        private final Options mOptions;
        private final int mOffset;
        private final int mLength;

        public DecodeByteArray(
                byte bytes[], int offset, int length, Options options) {
            mBytes = bytes;
            mOffset = offset;
            mLength = length;
            mOptions = options;
        }

        public Bitmap call() throws Exception {
            return BitmapFactory.decodeByteArray(mBytes, mOffset, mLength, mOptions);
        }
    }

    private static class DecodeAndSampleFile implements Callable<Bitmap> {

        private final int mTargetLength;
        private final int mMaxPixelCount;
        private final File mFile;
        private final Options mOptions;

        public DecodeAndSampleFile(
                File file, Options options, int targetLength, int maxPixelCount) {
            mFile = file;
            mOptions = options;
            mTargetLength = targetLength;
            mMaxPixelCount = maxPixelCount;
        }

        public Bitmap call() throws IOException {
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(mFile), JPEG_MARK_POSITION);
            try {
                // Decode bufferedInput for calculating a sample size.
                final BitmapFactory.Options options = mOptions;
                options.inJustDecodeBounds = true;
                bis.mark(JPEG_MARK_POSITION);
                BitmapFactory.decodeStream(bis, null, options);
                if (options.mCancel) return null;

                try {
                    bis.reset();
                } catch (IOException e) {
                    Log.w(TAG, "failed in resetting the buffer after reading the jpeg header", e);
                    bis.close();
                    bis = new BufferedInputStream(new FileInputStream(mFile));
                }

                options.inSampleSize =  Utils.computeSampleSize(
                        options, mTargetLength, mMaxPixelCount);
                options.inJustDecodeBounds = false;
                return BitmapFactory.decodeStream(bis, null, options);
            } finally {
                bis.close();
            }
        }

    }

}
