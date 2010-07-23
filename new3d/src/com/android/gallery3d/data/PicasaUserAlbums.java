// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.data;

import android.database.Cursor;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.picasa.AlbumEntry;
import com.android.gallery3d.picasa.EntrySchema;
import com.android.gallery3d.picasa.PicasaContentProvider;

import java.util.ArrayList;

public class PicasaUserAlbums extends DatabaseMediaSet {
    private final EntrySchema SCHEMA = AlbumEntry.SCHEMA;

    private final ArrayList<PicasaAlbum> mAlbums = new ArrayList<PicasaAlbum>();
    private int mCachedTotalCount = -1;
    private final ArrayList<PicasaAlbum> mLoadBuffer = new ArrayList<PicasaAlbum>();

    public PicasaUserAlbums(GalleryContext context) {
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
