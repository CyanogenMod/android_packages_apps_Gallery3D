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
import com.android.gallery3d.picasa.PhotoEntry;
import com.android.gallery3d.picasa.PicasaContentProvider;

import java.util.ArrayList;

public class PicasaAlbum extends DatabaseMediaSet {
    private static final int MAX_COVER_COUNT = 4;
    private static final EntrySchema SCHEMA = PhotoEntry.SCHEMA;

    private final AlbumEntry mData;
    private final ArrayList<PicasaImage> mPhotos = new ArrayList<PicasaImage>();
    private final ArrayList<PicasaImage> mLoadBuffer = new ArrayList<PicasaImage>();

    public PicasaAlbum(GalleryContext context, AlbumEntry entry) {
        super(context);
        mData = entry;
    }

    public MediaItem[] getCoverMediaItems() {
        int size = Math.min(MAX_COVER_COUNT, mPhotos.size());
        MediaItem items[] = new MediaItem[size];
        for (int i = 0; i < size; ++i) {
            items[i] = mPhotos.get(i);
        }
        return items;
    }

    public MediaItem getMediaItem(int index) {
        return mPhotos.get(index);
    }

    public int getMediaItemCount() {
        return mPhotos.size();
    }

    public MediaSet getSubMediaSet(int index) {
        throw new IndexOutOfBoundsException();
    }

    public int getSubMediaSetCount() {
        return 0;
    }

    public String getTitle() {
        return null;
    }

    public int getTotalMediaItemCount() {
        return mPhotos.size();
    }

    @Override
    protected void onLoadFromDatabase() {
        StringBuilder whereClause = new StringBuilder(PhotoEntry.Columns.ALBUM_ID);
        whereClause.append(" = ").append(mData.id);

        Cursor cursor = mContext.getContentResolver().query(
                PicasaContentProvider.PHOTOS_URI,
                SCHEMA.getProjection(), whereClause.toString(), null, null);
        try {
            while (cursor.moveToNext()) {
                PhotoEntry entry = SCHEMA.cursorToObject(cursor, new PhotoEntry());
                mLoadBuffer.add(new PicasaImage(entry));
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    protected void onUpdateContent() {
        mPhotos.clear();
        mPhotos.addAll(mLoadBuffer);
        mLoadBuffer.clear();
    }
}
