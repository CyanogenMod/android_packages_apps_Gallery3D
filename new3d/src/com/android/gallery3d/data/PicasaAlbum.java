// Copyright 2010 Google Inc. All Rights Reserved.

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
    private final ArrayList<PicasaPhoto> mPhotos = new ArrayList<PicasaPhoto>();
    private final ArrayList<PicasaPhoto> mLoadBuffer = new ArrayList<PicasaPhoto>();

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
                mLoadBuffer.add(new PicasaPhoto(entry));
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
