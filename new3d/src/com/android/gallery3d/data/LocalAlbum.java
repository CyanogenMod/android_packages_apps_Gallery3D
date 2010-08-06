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
import android.database.Cursor;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class LocalAlbum extends DatabaseMediaSet {
    private static final int MAX_NUM_COVER_ITEMS = 4;

    public static final Comparator<LocalAlbum> sNameComparator = new MyComparator();

    private final int mBucketId;
    private final String mBucketTitle;

    private final ArrayList<LocalMediaItem> mMediaItems =
            new ArrayList<LocalMediaItem>();
    private ArrayList<LocalMediaItem> mLoadBuffer =
            new ArrayList<LocalMediaItem>();

    public LocalAlbum(GalleryContext context, int id, String title) {
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
        ArrayList<LocalMediaItem> items = new ArrayList<LocalMediaItem>();
        mLoadBuffer = items;

        ContentResolver resolver = mContext.getContentResolver();
        ImageService imageService = mContext.getImageService();

        Cursor cursor = LocalImage.queryImageInBucket(resolver, mBucketId);
        try {
            while (cursor.moveToNext()) {
                items.add(LocalImage.load(imageService, cursor));
            }
        } finally {
            cursor.close();
        }

        cursor = LocalVideo.queryVideoInBucket(resolver, mBucketId);
        try {
            while (cursor.moveToNext()) {
                items.add(LocalVideo.load(imageService, cursor));
            }
        } finally {
            cursor.close();
        }

        Collections.sort(items, new Comparator<LocalMediaItem>() {

            public int compare(LocalMediaItem o1, LocalMediaItem o2) {
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
        Utils.Assert(mLoadBuffer != null);
        mMediaItems.clear();
        mMediaItems.addAll(mLoadBuffer);
        mLoadBuffer = null;
    }

    private static class MyComparator implements Comparator<LocalAlbum> {

        public int compare(LocalAlbum s1, LocalAlbum s2) {
            int result = s1.mBucketTitle.compareTo(s2.mBucketTitle);
            return result != 0 ? result : s1.mBucketId - s2.mBucketId;
        }
    }
}
