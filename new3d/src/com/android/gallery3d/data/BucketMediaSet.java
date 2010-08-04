     // Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.database.Cursor;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.ui.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class BucketMediaSet extends DatabaseMediaSet {
    private static final int MAX_NUM_COVER_ITEMS = 4;

    public static final Comparator<BucketMediaSet> sNameComparator = new MyComparator();

    private final int mBucketId;
    private final String mBucketTitle;

    private final ArrayList<DatabaseMediaItem> mMediaItems =
            new ArrayList<DatabaseMediaItem>();
    private ArrayList<DatabaseMediaItem> mLoadBuffer =
            new ArrayList<DatabaseMediaItem>();

    public BucketMediaSet(GalleryContext context, int id, String title) {
        super(context);
        mBucketId = id;
        mBucketTitle= title;
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

    @Override
    protected void onLoadFromDatabase() {
        ArrayList<DatabaseMediaItem> items = new ArrayList<DatabaseMediaItem>();
        mLoadBuffer = items;

        ContentResolver resolver = mContext.getContentResolver();
        ImageService imageService = mContext.getImageService();

        Cursor cursor = ImageMediaItem.queryImageInBucket(resolver, mBucketId);
        try {
            while (cursor.moveToNext()) {
                items.add(ImageMediaItem.load(imageService, cursor));
            }
        } finally {
            cursor.close();
        }

        cursor = VideoMediaItem.queryVideoInBucket(resolver, mBucketId);
        try {
            while (cursor.moveToNext()) {
                items.add(VideoMediaItem.load(imageService, cursor));
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
    }

    @Override
    protected void onUpdateContent() {
        Util.Assert(mLoadBuffer != null);
        mMediaItems.clear();
        mMediaItems.addAll(mLoadBuffer);
        mLoadBuffer = null;
    }

    private static class MyComparator implements Comparator<BucketMediaSet> {

        public int compare(BucketMediaSet s1, BucketMediaSet s2) {
            int result = s1.mBucketTitle.compareTo(s2.mBucketTitle);
            return result != 0 ? result : s1.mBucketId - s2.mBucketId;
        }
    }
}
