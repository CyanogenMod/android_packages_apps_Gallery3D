     // Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;

import com.android.gallery3d.ui.SynchronizedHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class BucketMediaSet implements MediaSet {
    private static final int MAX_NUM_COVER_ITEMS = 4;

    private static final int MSG_LOAD_DATABASE = 1;
    private static final int MSG_UPDATE_BUCKET = 2;

    public static final Comparator<BucketMediaSet> sNameComparator = new MyComparator();

    private final MediaDbAccessor mAccessor;
    private final int mBucketId;
    private final String mBucketTitle;
    private final ArrayList<DatabaseMediaItem> mMediaItems =
            new ArrayList<DatabaseMediaItem>();
    private ArrayList<DatabaseMediaItem> mLoadBuffer =
            new ArrayList<DatabaseMediaItem>();

    private final Handler mHandler;
    private final Handler mMainHandler;

    private MediaSetListener mListener;

    protected void invalidate() {
        mHandler.sendEmptyMessage(MSG_LOAD_DATABASE);
    }

    public BucketMediaSet(MediaDbAccessor accessor, int id, String title) {
        mAccessor = accessor;
        mBucketId = id;
        mBucketTitle= title;

        mHandler = new Handler(accessor.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_LOAD_DATABASE:
                        loadMediaItemsFromDatabase();
                        break;
                    default: throw new IllegalArgumentException();
                }
            }
        };

        mMainHandler = new SynchronizedHandler(
                accessor.getUiMonitor(), accessor.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_BUCKET:
                        updateContent();
                        break;
                    default: throw new IllegalArgumentException();
                }
            }
        };

    }

    public MediaItem[] getCoverMediaItems() {
        int size = Math.min(MAX_NUM_COVER_ITEMS, mMediaItems.size());
        MediaItem items[] = new MediaItem[size];
        for (int i = 0; i < size; ++i) {
            items[i] = mMediaItems.get(i);
        }
        return items;
    }

    public MediaItem getMediaItem(int index) {
        return mMediaItems.get(index);
    }

    public int getMediaItemCount() {
        return mMediaItems.size();
    }

    public MediaSet getSubMediaSet(int index) {
        throw new IndexOutOfBoundsException();
    }

    public int getSubMediaSetCount() {
        return 0;
    }

    public String getTitle() {
        return mBucketTitle;
    }

    public int getTotalMediaItemCount() {
        return mMediaItems.size();
    }

    public void setContentListener(MediaSetListener listener) {
        mListener = listener;
    }

    private void loadMediaItemsFromDatabase() {
        ArrayList<DatabaseMediaItem> items = new ArrayList<DatabaseMediaItem>();
        mLoadBuffer = items;

        ContentResolver resolver = mAccessor.getContentResolver();

        Cursor cursor = ImageMediaItem.queryImageInBucket(resolver, mBucketId);
        try {
            while (cursor.moveToNext()) {
                items.add(ImageMediaItem.load(cursor));
            }
        } finally {
            cursor.close();
        }

        cursor = VideoMediaItem.queryVideoInBucket(resolver, mBucketId);
        try {
            while (cursor.moveToNext()) {
                items.add(VideoMediaItem.load(cursor));
            }
        } finally {
            cursor.close();
        }

        Collections.sort(items, new Comparator<DatabaseMediaItem>() {

            public int compare(DatabaseMediaItem o1, DatabaseMediaItem o2) {
                // sort items in descending order based on their taken time.
                long result = -(o1.mDateTakenInMs - o2.mDateTakenInMs);
                return result == 0
                        ? o1.mId - o2.mId
                        : result > 0 ? 1 : -1;
            }
        });

        mMainHandler.sendEmptyMessage(MSG_UPDATE_BUCKET);
    }

    private void updateContent() {
        if (mLoadBuffer == null) throw new IllegalArgumentException();

        mMediaItems.clear();
        mMediaItems.addAll(mLoadBuffer);
        mLoadBuffer = null;

        if (mListener != null) mListener.onContentChanged();
    }

    private static class MyComparator implements Comparator<BucketMediaSet> {

        public int compare(BucketMediaSet s1, BucketMediaSet s2) {
            int result = s1.mBucketTitle.compareTo(s2.mBucketTitle);
            return result != 0 ? result : s1.mBucketId - s2.mBucketId;
        }
    }
}
