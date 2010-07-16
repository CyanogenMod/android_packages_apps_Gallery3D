package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.content.Context;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import com.android.gallery3d.ui.Util;

public class MediaDbAccessor {

    private static final String TAG = "MediaDbAccessor";

    private static MediaDbAccessor sInstance;

    private final HandlerThread mThread =
            new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);

    private final Context mContext;
    private final Looper mMainLooper;
    private final Object mUiMonitor;
    private final MediaSet mRoot;

    public MediaDbAccessor(Context context, Object uiMonitor) {
        mThread.start();
        mMainLooper = Looper.getMainLooper();
        mContext = context;
        mUiMonitor = Util.checkNotNull(uiMonitor);

        RootMediaSet mediaRoot = new RootMediaSet(this);
        PicasaUserAlbums picasaRoot = new PicasaUserAlbums(this);
        picasaRoot.invalidate();

        mRoot = new ComboMediaSet(mediaRoot, picasaRoot);
    }

    public static void initialize(Context context, Object uiMonitor) {
        sInstance = new MediaDbAccessor(context, uiMonitor);
    }

    public static MediaDbAccessor getInstance() {
        if (sInstance == null) throw new IllegalStateException();
        return sInstance;
    }

    public MediaSet getRootMediaSets() {
        return mRoot;
    }

    public ContentResolver getContentResolver() {
        return mContext.getContentResolver();
    }

    public Looper getLooper() {
        return mThread.getLooper();
    }

    public Looper getMainLooper() {
        return mMainLooper;
    }

    public Object getUiMonitor() {
        return mUiMonitor;
    }
}