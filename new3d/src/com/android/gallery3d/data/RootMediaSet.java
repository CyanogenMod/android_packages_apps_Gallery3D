// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video.VideoColumns;
import android.util.Log;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.ui.SynchronizedHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RootMediaSet implements MediaSet{
    private static final String TITLE = "RootSet";

    private static final int MSG_LOAD_DATA = 0;
    private static final int MSG_UPDATE_CONTENT = 0;

    // Must preserve order between these indices and the order of the terms in
    // BUCKET_PROJECTION_IMAGES, BUCKET_PROJECTION_VIDEOS.
    // Not using SortedHashMap for efficiency reasons.
    private static final int BUCKET_ID_INDEX = 0;
    private static final int BUCKET_NAME_INDEX = 1;

    private static final String[] PROJECTION_IMAGE_BUCKETS = {
            ImageColumns.BUCKET_ID,
            ImageColumns.BUCKET_DISPLAY_NAME };

    private static final String[] PROJECTION_VIDEO_BUCKETS = {
            VideoColumns.BUCKET_ID,
            VideoColumns.BUCKET_DISPLAY_NAME };

    private int mTotalCountCached = -1;

    private final ArrayList<BucketMediaSet>
            mSubsets = new ArrayList<BucketMediaSet>();

    private HashMap<Integer, String> mLoadBuffer;

    private final Handler mDataHandler;
    private final Handler mMainHandler;

    private final GalleryContext mContext;
    private MediaSetListener mListener;

    public RootMediaSet(GalleryContext context) {

        mContext = context;

        mDataHandler = new Handler(context.getDataManager().getDataLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_LOAD_DATA:
                        loadBucketsFromDatabase();
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        };

        mMainHandler = new SynchronizedHandler(
                context.getUIMonitor(), context.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_CONTENT:
                        updateContent();
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        };

        mDataHandler.sendEmptyMessage(MSG_LOAD_DATA);
    }

    public MediaItem[] getCoverMediaItems() {
        return new MediaItem[0];
    }

    public MediaItem getMediaItem(int index) {
        throw new IndexOutOfBoundsException();
    }

    public synchronized int getMediaItemCount() {
        return 0;
    }

    public synchronized MediaSet getSubMediaSet(int index) {
        return mSubsets.get(index);
    }

    public synchronized int getSubMediaSetCount() {
        return mSubsets.size();
    }

    public String getTitle() {
        return TITLE;
    }

    public int getTotalMediaItemCount() {
        if (mTotalCountCached >= 0) return mTotalCountCached;
        int total = 0;
        for (MediaSet subset : mSubsets) {
            total += subset.getTotalMediaItemCount();
        }
        mTotalCountCached = total;
        return total;
    }

    public void setContentListener(MediaSetListener listener) {
        mListener = listener;
    }

    private void loadBucketsFromDatabase() {
        ContentResolver resolver = mContext.getContentResolver();
        HashMap<Integer, String> map = new HashMap<Integer, String>();
        mLoadBuffer = map;

        Uri uriImages = Images.Media.EXTERNAL_CONTENT_URI.buildUpon().
                appendQueryParameter("distinct", "true").build();
        Cursor cursor = resolver.query(
                uriImages, PROJECTION_IMAGE_BUCKETS, null, null, null);
        if (cursor == null) throw new NullPointerException();
        try {
            while (cursor.moveToNext()) {
                Log.v("Image", cursor.getString(BUCKET_NAME_INDEX));
                map.put(cursor.getInt(BUCKET_ID_INDEX),
                        cursor.getString(BUCKET_NAME_INDEX));
            }
        } finally {
            cursor.close();
        }

        Uri uriVideos = Video.Media.EXTERNAL_CONTENT_URI.buildUpon().
                appendQueryParameter("distinct", "true").build();
        cursor = resolver.query(
                uriVideos, PROJECTION_VIDEO_BUCKETS, null, null, null);
        if (cursor == null) throw new NullPointerException();
        try {
            while (cursor.moveToNext()) {
                Log.v("Video", cursor.getString(BUCKET_ID_INDEX));
                Log.v("Video", cursor.getString(BUCKET_NAME_INDEX));
                map.put(cursor.getInt(BUCKET_ID_INDEX),
                        cursor.getString(BUCKET_NAME_INDEX));
            }
        } finally {
            cursor.close();
        }

        mMainHandler.sendEmptyMessage(MSG_UPDATE_CONTENT);
    }

    private void updateContent() {
        HashMap<Integer, String> map = mLoadBuffer;
        if (map == null) throw new IllegalStateException();

        GalleryContext context = mContext;
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            mSubsets.add(new BucketMediaSet(
                    context, entry.getKey(), entry.getValue()));
        }
        mLoadBuffer = null;

        Collections.sort(mSubsets, BucketMediaSet.sNameComparator);

        for (BucketMediaSet mediaset : mSubsets) {
            mediaset.invalidate();
        }
        if (mListener != null) mListener.onContentChanged();
    }
}
