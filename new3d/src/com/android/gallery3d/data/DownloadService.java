// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadService {
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int KEEP_ALIVE_TIME = 10000;

    private static DownloadService sInstance;

    private final ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    public static synchronized DownloadService getInstance() {
        if (sInstance == null) sInstance = new DownloadService();
        return sInstance;
    }

    public DownloadService() {
    }

    public FutureTask<Void> requestDownload(
            URL url, File file, FutureListener<Void> listener) {
        FutureTask<Void> task = new ListenableFutureTask<Void>(
                new DownloadToFile(url, file), listener);
        mExecutor.execute(task);
        return task;
    }

    public FutureTask<byte[]> requestDownload(
            URL url, FutureListener<? super byte[]> listener) {
        FutureTask<byte[]> task = new ListenableFutureTask<byte[]>(
                new DownloadToByteArray(url), listener);
        mExecutor.execute(task);
        return task;
    }

    private static void download(URL url, OutputStream output) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException(String.format("connection error: %s - %s",
                        conn.getResponseCode(), conn.getResponseMessage()));
            }
            InputStream input = conn.getInputStream();
            byte buffer[] = new byte[4096];
            int rc = input.read(buffer, 0, buffer.length);
            while (rc > 0) {
                output.write(buffer, 0, rc);
                rc = input.read(buffer, 0, buffer.length);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static class DownloadToFile implements Callable<Void> {

        private final URL mUrl;
        private final File mFile;

        public DownloadToFile(URL url, File file) {
            mUrl = url;
            mFile = file;
        }

        public Void call() throws Exception {
            BufferedOutputStream bos =
                    new BufferedOutputStream(new FileOutputStream(mFile));
            try {
                download(mUrl, bos);
                return null;
            } finally {
                bos.close();
            }
        }
    }

    private static class DownloadToByteArray implements Callable<byte[]> {
        private final URL mUrl;

        public DownloadToByteArray(URL url) {
            mUrl = url;
        }

        public byte[] call() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                download(mUrl, baos);
                return baos.toByteArray();
            } finally {
                baos.close();
            }
        }
    }

}
