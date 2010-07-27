// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.FutureTask;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DecodeService {
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 1;
    private static final int KEEP_ALIVE_TIME = 10000; // 10 seconds

    private static DecodeService sInstance;

    private final Executor mExecutor = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    public static synchronized DecodeService getInstance() {
        if (sInstance == null) sInstance = new DecodeService();
        return sInstance;
    }

    public Future<Bitmap> requestDecode(
            File file, Options options, FutureListener<? super Bitmap> listener) {
        if (options == null) options = new Options();
        FutureTask<Bitmap> task = new DecodeFutureTask(
                new DecodeFile(file, options), listener);
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
                new DecodeByteArray(bytes, offset, length, options), listener);
        mExecutor.execute(task);
        return task;
    }

    private static class DecodeFutureTask extends FutureTask<Bitmap> {

        private Options mOptions;

        public DecodeFutureTask(
                Callable<Bitmap> callable, FutureListener<? super Bitmap> listener) {
            super(callable, listener);
        }

        @Override
        public void onCancel() {
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

}
