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
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.picasa.AlbumEntry;
import com.android.gallery3d.picasa.EntrySchema;
import com.android.gallery3d.picasa.PhotoEntry;
import com.android.gallery3d.picasa.PicasaContentProvider;
import com.android.gallery3d.util.Utils;

import java.util.ArrayList;

// PicasaAlbum lists all images in a Picasa album.
public class PicasaAlbum extends MediaSet {
    private static final String TAG = "PicasaAlbum";
    private static final EntrySchema SCHEMA = PhotoEntry.SCHEMA;
    private static final String[] COUNT_PROJECTION = { "count(*)" };
    private static final String WHERE_CLAUSE = PhotoEntry.Columns.ALBUM_ID
            + "=?";

    private final AlbumEntry mData;
    private final ContentResolver mResolver;
    private long mUniqueId;
    private GalleryContext mContext;
    private boolean mIsDirty = true;

    public PicasaAlbum(GalleryContext context, AlbumEntry entry) {
        mContext = context;
        mResolver = context.getContentResolver();
        mData = entry;
        mUniqueId = DataManager.makeId(
                DataManager.ID_PICASA_ALBUM, (int) entry.id);
        mResolver.registerContentObserver(
                PicasaContentProvider.PHOTOS_URI, true, new MyContentObserver());
    }

    @Override
    public long getUniqueId() {
        return mUniqueId;
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        Uri uri = PicasaContentProvider.PHOTOS_URI.buildUpon()
                .appendQueryParameter("limit", start + "," + count).build();

        ArrayList<MediaItem> list = new ArrayList<MediaItem>();
        Cursor cursor = mResolver.query(uri,
                SCHEMA.getProjection(), WHERE_CLAUSE,
                new String[]{String.valueOf(mData.id)},
                PhotoEntry.Columns.DISPLAY_INDEX);

        try {
            while (cursor.moveToNext()) {
                PhotoEntry entry = SCHEMA.cursorToObject(cursor, new PhotoEntry());
                DataManager dataManager = mContext.getDataManager();
                long uniqueId = DataManager.makeId(
                        DataManager.ID_PICASA_IMAGE, (int) entry.id);
                MediaItem item = dataManager.getFromCache(uniqueId);
                if (item == null) {
                    item = new PicasaImage(mContext, entry);
                    dataManager.putToCache(uniqueId, item);
                }
                list.add(item);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    @Override
    public int getMediaItemCount() {
        Cursor cursor = mResolver.query(
                PicasaContentProvider.PHOTOS_URI,
                COUNT_PROJECTION, WHERE_CLAUSE,
                new String[]{String.valueOf(mData.id)}, null);
        try {
            Utils.Assert(cursor.moveToNext());
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public int getTotalMediaItemCount() {
        return getMediaItemCount();
    }

    @Override
    public void reload() {
        if (mIsDirty) {
            mIsDirty = false;
            if (mListener != null) mListener.onContentChanged();
        }
    }

    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(new Handler(mContext.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange) {
            mIsDirty = true;
            if (mListener != null) mListener.onContentDirty();
        }
    }
}
