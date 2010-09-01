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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

// PicasaAlbumSet lists all albums in a Picasa account.
public class PicasaAlbumSet extends MediaSet {
    private static final String TAG = "PicasaAlbumSet";
    private final EntrySchema SCHEMA = AlbumEntry.SCHEMA;

    private ArrayList<PicasaAlbum> mAlbums = new ArrayList<PicasaAlbum>();
    private final ArrayList<AlbumEntry> mLoadBuffer = new ArrayList<AlbumEntry>();
    private final long mUniqueId;
    private GalleryContext mContext;
    private AtomicBoolean mContentDirty = new AtomicBoolean(true);

    public PicasaAlbumSet(int parentId, int childKey, GalleryContext context) {
        mContext = context;
        mUniqueId = context.getDataManager().obtainSetId(parentId, childKey, this);
        context.getContentResolver().registerContentObserver(
                PicasaContentProvider.ALBUMS_URI, true, new MyContentObserver());
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    @Override
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

    @Override
    public int getTotalMediaItemCount() {
        int totalCount = 0;
        for (PicasaAlbum album : mAlbums) {
            totalCount += album.getTotalMediaItemCount();
        }
        return totalCount;
    }

    private ArrayList<PicasaAlbum> loadSubMediaSets() {
        Utils.assertNotInRenderThread();
        Cursor cursor = mContext.getContentResolver().query(
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

        ArrayList<PicasaAlbum> newAlbums = new ArrayList<PicasaAlbum>();
        DataManager dataManager = mContext.getDataManager();

        int parentId = getMyId();
        for (int i = 0, n = mLoadBuffer.size(); i < n; i++) {
            AlbumEntry entry = mLoadBuffer.get(i);
            int childKey = (int) entry.id;
            PicasaAlbum album = (PicasaAlbum) dataManager.getMediaSet(parentId, childKey);
            if (album == null) {
                album = new PicasaAlbum(parentId, mContext, entry);
            }
            newAlbums.add(album);
        }

        for (int i = 0, n = mAlbums.size(); i < n; ++i) {
            mAlbums.get(i).reload();
        }
        Collections.sort(newAlbums, PicasaAlbum.sEditDateComparator);
        return newAlbums;
    }

    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(new Handler(mContext.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mContentDirty.compareAndSet(false, true)) {
                if (mListener != null) mListener.onContentDirty();
            }
        }
    }

    @Override
    public boolean reload() {
        if (!mContentDirty.compareAndSet(true, false)) return false;
        ArrayList<PicasaAlbum> album = loadSubMediaSets();
        if (album.equals(mAlbums)) return false;
        mAlbums = album;
        return true;
    }
}
