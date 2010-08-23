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

import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.picasa.AlbumEntry;
import com.android.gallery3d.picasa.EntrySchema;
import com.android.gallery3d.picasa.PicasaContentProvider;
import com.android.gallery3d.util.Utils;

import java.util.ArrayList;

// PicasaAlbumSet lists all albums in a Picasa account.
public class PicasaAlbumSet extends DatabaseMediaSet {
    private static final String TAG = "PicasaAlbumSet";
    private final EntrySchema SCHEMA = AlbumEntry.SCHEMA;

    private ArrayList<PicasaAlbum> mAlbums = new ArrayList<PicasaAlbum>();
    private final ArrayList<AlbumEntry> mLoadBuffer = new ArrayList<AlbumEntry>();
    private final long mUniqueId;

    public PicasaAlbumSet(int parentId, int childKey, GalleryContext context) {
        super(context);
        mUniqueId = context.getDataManager().obtainSetId(parentId, childKey, this);
        context.getContentResolver().registerContentObserver(
                PicasaContentProvider.ALBUMS_URI, true, new MyContentObserver());
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    public int getSubMediaSetCount() {
        return mAlbums.size();
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public long getUniqueId() {
        return mUniqueId;
    }

    public int getTotalMediaItemCount() {
        int totalCount = 0;
        for (PicasaAlbum album : mAlbums) {
            totalCount += album.getTotalMediaItemCount();
        }
        return totalCount;
    }

    @Override
    protected void onLoadFromDatabase() {
        Utils.assertNotInRenderThread();
        Cursor cursor = mResolver.query(
                PicasaContentProvider.ALBUMS_URI,
                SCHEMA.getProjection(), null, null, null);
        try {
            while (cursor.moveToNext()) {
                AlbumEntry entry = SCHEMA.cursorToObject(cursor, new AlbumEntry());
                mLoadBuffer.add(entry);
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    protected void onUpdateContent() {
        ArrayList<PicasaAlbum> newAlbums = new ArrayList<PicasaAlbum>();
        int parentId = getMyId();
        DataManager dataManager = mContext.getDataManager();

        for (int i = 0, n = mLoadBuffer.size(); i < n; i++) {
            AlbumEntry entry = mLoadBuffer.get(i);
            int childKey = (int) entry.id;
            PicasaAlbum album = (PicasaAlbum) dataManager.getMediaSet(parentId, childKey);
            if (album == null) {
                album = new PicasaAlbum(parentId, mContext, entry);
            }
            newAlbums.add(album);
        }
        mAlbums = newAlbums;
        mLoadBuffer.clear();

        for (int i = 0, n = mAlbums.size(); i < n; i++) {
            mAlbums.get(i).reload();
        }
    }

    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(new Handler(mContext.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange) {
            notifyContentDirty();
        }
    }
}
