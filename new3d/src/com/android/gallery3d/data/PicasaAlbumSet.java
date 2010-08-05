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

import android.database.Cursor;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.picasa.AlbumEntry;
import com.android.gallery3d.picasa.EntrySchema;
import com.android.gallery3d.picasa.PicasaContentProvider;

import java.util.ArrayList;

public class PicasaAlbumSet extends DatabaseMediaSet {
    private final EntrySchema SCHEMA = AlbumEntry.SCHEMA;

    private final ArrayList<PicasaAlbum> mAlbums = new ArrayList<PicasaAlbum>();
    private int mCachedTotalCount = -1;
    private final ArrayList<PicasaAlbum> mLoadBuffer = new ArrayList<PicasaAlbum>();

    public PicasaAlbumSet(GalleryContext context) {
        super(context);
    }

    public MediaItem[] getCoverMediaItems() {
        throw new UnsupportedOperationException();
    }

    public MediaItem getMediaItem(int index) {
        throw new IndexOutOfBoundsException();
    }

    public int getMediaItemCount() {
        return 0;
    }

    public MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    public int getSubMediaSetCount() {
        return mAlbums.size();
    }

    public String getTitle() {
        return null;
    }

    public int getTotalMediaItemCount() {
        if (mCachedTotalCount >= 0) return mCachedTotalCount;
        int totalCount = 0;
        for (PicasaAlbum album : mAlbums) {
            totalCount += album.getTotalMediaItemCount();
        }
        mCachedTotalCount = totalCount;
        return totalCount;
    }

    @Override
    protected void onLoadFromDatabase() {
        mLoadBuffer.clear();
        Cursor cursor = mContext.getContentResolver().query(
                PicasaContentProvider.ALBUMS_URI,
                SCHEMA.getProjection(), null, null, null);
        try {
            while (cursor.moveToNext()) {
                AlbumEntry entry = SCHEMA.cursorToObject(cursor, new AlbumEntry());
                mLoadBuffer.add(new PicasaAlbum(mContext, entry));
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    protected void onUpdateContent() {
        mAlbums.clear();
        mAlbums.addAll(mLoadBuffer);
        mLoadBuffer.clear();

        for (PicasaAlbum album : mAlbums) {
            album.invalidate();
        }
    }
}
