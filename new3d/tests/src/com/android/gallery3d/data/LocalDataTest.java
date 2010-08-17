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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import java.util.ArrayList;

public class LocalDataTest extends AndroidTestCase {
    private static final String TAG = "LocalDataTest";

    @MediumTest
    public void testLocalAlbum() {
        run(new TestZeroImage());
        run(new TestOneImage());
        run(new TestMoreImages());
        run(new TestZeroVideo());
        run(new TestOneVideo());
        run(new TestMoreVideos());
    }

    static void run(Thread t) {
        t.start();
        try {
            t.join();
        } catch (InterruptedException ex) {
            fail();
        }
    }

    abstract class TestLocalAlbumBase extends Thread {
        private boolean mIsImage;

        TestLocalAlbumBase(boolean isImage) {
            mIsImage = isImage;
        }

        public void run() {
            Looper.prepare();
            SQLiteDatabase db = SQLiteDatabase.create(null);

            prepareData(db);

            GalleryContextStub context = newGalleryContext(db);
            LocalAlbumSet albumSet = new LocalAlbumSet(context, mIsImage);
            MyListener listener = new MyListener();
            albumSet.setContentListener(listener);
            albumSet.reload();
            Looper.loop();

            assertEquals(1, listener.count);
            verifyResult(albumSet);
        }

        abstract void prepareData(SQLiteDatabase db);
        abstract void verifyResult(LocalAlbumSet albumSet);
    }

    abstract class TestLocalImageAlbum extends TestLocalAlbumBase {
        TestLocalImageAlbum() {
            super(true);
        }
    }

    abstract class TestLocalVideoAlbum extends TestLocalAlbumBase {
        TestLocalVideoAlbum() {
            super(false);
        }
    }

    class TestZeroImage extends TestLocalImageAlbum {
        public void prepareData(SQLiteDatabase db) {
            createImageTable(db);
        }

        public void verifyResult(LocalAlbumSet albumSet) {
            assertEquals(0, albumSet.getMediaItemCount());
            assertEquals(0, albumSet.getSubMediaSetCount());
            assertEquals(0, albumSet.getTotalMediaItemCount());
            assertEquals(DataManager.makeId(DataManager.ID_LOCAL_IMAGE_ALBUM_SET, 0),
                    albumSet.getUniqueId());
        }
    }

    class TestOneImage extends TestLocalImageAlbum {
        public void prepareData(SQLiteDatabase db) {
            createImageTable(db);
            insertImageData(db);
        }

        public void verifyResult(LocalAlbumSet albumSet) {
            assertEquals(0, albumSet.getMediaItemCount());
            assertEquals(1, albumSet.getSubMediaSetCount());
            assertEquals(1, albumSet.getTotalMediaItemCount());
            MediaSet sub = albumSet.getSubMediaSet(0);
            assertEquals(1, sub.getMediaItemCount());
            assertEquals(0, sub.getSubMediaSetCount());
            LocalMediaItem item = (LocalMediaItem) sub.getMediaItem(0);
            assertEquals(1, item.mId);
            assertEquals("IMG_0072", item.mCaption);
            assertEquals("image/jpeg", item.mMimeType);
            assertEquals(12.0, item.mLatitude);
            assertEquals(34.0, item.mLongitude);
            assertEquals(0xD000, item.mDateTakenInMs);
            assertEquals(1280395646L, item.mDateAddedInSec);
            assertEquals(1275934796L, item.mDateModifiedInSec);
            assertEquals("/mnt/sdcard/DCIM/100CANON/IMG_0072.JPG", item.mFilePath);
            assertEquals(DataManager.makeId(DataManager.ID_LOCAL_IMAGE_ALBUM, 0xB000),
                    sub.getUniqueId());
            assertEquals(DataManager.makeId(DataManager.ID_LOCAL_IMAGE, 1),
                    item.getUniqueId());
        }
    }

    class TestMoreImages extends TestLocalImageAlbum {
        public void prepareData(SQLiteDatabase db) {
            // Albums are sorted by names, and items are sorted by
            // dateTimeTaken (descending)
            createImageTable(db);
            // bucket 0xB002
            insertImageData(db, 1000, 0xB000, "second");  // id 1
            insertImageData(db, 2000, 0xB000, "second");  // id 2
            // bucket 0xB001
            insertImageData(db, 3000, 0xB001, "first");   // id 3
        }

        public void verifyResult(LocalAlbumSet albumSet) {
            assertEquals(0, albumSet.getMediaItemCount());
            assertEquals(2, albumSet.getSubMediaSetCount());
            assertEquals(3, albumSet.getTotalMediaItemCount());

            MediaSet first = albumSet.getSubMediaSet(0);
            assertEquals(1, first.getMediaItemCount());
            LocalMediaItem item = (LocalMediaItem) first.getMediaItem(0);
            assertEquals(3, item.mId);
            assertEquals(3000L, item.mDateTakenInMs);

            MediaSet second = albumSet.getSubMediaSet(1);
            assertEquals(2, second.getMediaItemCount());
            item = (LocalMediaItem) second.getMediaItem(0);
            assertEquals(2, item.mId);
            assertEquals(2000L, item.mDateTakenInMs);
            item = (LocalMediaItem) second.getMediaItem(1);
            assertEquals(1, item.mId);
            assertEquals(1000L, item.mDateTakenInMs);

            assertEquals(DataManager.makeId(DataManager.ID_LOCAL_IMAGE_ALBUM, 0xB001),
                    first.getUniqueId());
            assertEquals(DataManager.makeId(DataManager.ID_LOCAL_IMAGE_ALBUM, 0xB000),
                    second.getUniqueId());
        }
    }

    static void createImageTable(SQLiteDatabase db) {
        // This is copied from MediaProvider
        db.execSQL("CREATE TABLE IF NOT EXISTS images (" +
                "_id INTEGER PRIMARY KEY," +
                "_data TEXT," +
                "_size INTEGER," +
                "_display_name TEXT," +
                "mime_type TEXT," +
                "title TEXT," +
                "date_added INTEGER," +
                "date_modified INTEGER," +
                "description TEXT," +
                "picasa_id TEXT," +
                "isprivate INTEGER," +
                "latitude DOUBLE," +
                "longitude DOUBLE," +
                "datetaken INTEGER," +
                "orientation INTEGER," +
                "mini_thumb_magic INTEGER," +
                "bucket_id TEXT," +
                "bucket_display_name TEXT" +
               ");");
    }

    static void insertImageData(SQLiteDatabase db) {
        insertImageData(db, 0xD000, 0xB000, "name");
    }

    static void insertImageData(SQLiteDatabase db, long dateTaken,
            int bucketId, String bucketName) {
        db.execSQL("INSERT INTO images (title, mime_type, latitude, longitude, "
                + "datetaken, date_added, date_modified, bucket_id, "
                + "bucket_display_name, _data, orientation) "
                + "VALUES ('IMG_0072', 'image/jpeg', 12, 34, "
                + dateTaken + ", 1280395646, 1275934796, '" + bucketId + "', "
                + "'" + bucketName + "', "
                + "'/mnt/sdcard/DCIM/100CANON/IMG_0072.JPG', 0)");
    }

    class TestZeroVideo extends TestLocalVideoAlbum {
        public void prepareData(SQLiteDatabase db) {
            createVideoTable(db);
        }

        public void verifyResult(LocalAlbumSet albumSet) {
            assertEquals(0, albumSet.getMediaItemCount());
            assertEquals(0, albumSet.getSubMediaSetCount());
            assertEquals(0, albumSet.getTotalMediaItemCount());
            assertEquals(DataManager.makeId(DataManager.ID_LOCAL_VIDEO_ALBUM_SET, 0),
                    albumSet.getUniqueId());
        }
    }

    class TestOneVideo extends TestLocalVideoAlbum {
        public void prepareData(SQLiteDatabase db) {
            createVideoTable(db);
            insertVideoData(db);
        }

        public void verifyResult(LocalAlbumSet albumSet) {
            assertEquals(0, albumSet.getMediaItemCount());
            assertEquals(1, albumSet.getSubMediaSetCount());
            assertEquals(1, albumSet.getTotalMediaItemCount());
            MediaSet sub = albumSet.getSubMediaSet(0);
            assertEquals(1, sub.getMediaItemCount());
            assertEquals(0, sub.getSubMediaSetCount());
            LocalMediaItem item = (LocalMediaItem) sub.getMediaItem(0);
            assertEquals(1, item.mId);
            assertEquals("VID_20100811_051413", item.mCaption);
            assertEquals("video/mp4", item.mMimeType);
            assertEquals(11.0, item.mLatitude);
            assertEquals(22.0, item.mLongitude);
            assertEquals(0xD000, item.mDateTakenInMs);
            assertEquals(1281503663L, item.mDateAddedInSec);
            assertEquals(1281503662L, item.mDateModifiedInSec);
            assertEquals("/mnt/sdcard/DCIM/Camera/VID_20100811_051413.3gp",
                    item.mFilePath);
            assertEquals(DataManager.makeId(DataManager.ID_LOCAL_VIDEO_ALBUM, 0xB000),
                    sub.getUniqueId());
            assertEquals(DataManager.makeId(DataManager.ID_LOCAL_VIDEO, 1),
                    item.getUniqueId());
        }
    }

    class TestMoreVideos extends TestLocalVideoAlbum {
        public void prepareData(SQLiteDatabase db) {
            // Albums are sorted by names, and items are sorted by
            // dateTimeTaken (descending)
            createVideoTable(db);
            // bucket 0xB002
            insertVideoData(db, 1000, 0xB000, "second");  // id 1
            insertVideoData(db, 2000, 0xB000, "second");  // id 2
            // bucket 0xB001
            insertVideoData(db, 3000, 0xB001, "first");   // id 3
        }

        public void verifyResult(LocalAlbumSet albumSet) {
            assertEquals(0, albumSet.getMediaItemCount());
            assertEquals(2, albumSet.getSubMediaSetCount());
            assertEquals(3, albumSet.getTotalMediaItemCount());

            MediaSet first = albumSet.getSubMediaSet(0);
            assertEquals(1, first.getMediaItemCount());
            LocalMediaItem item = (LocalMediaItem) first.getMediaItem(0);
            assertEquals(3, item.mId);
            assertEquals(3000L, item.mDateTakenInMs);

            MediaSet second = albumSet.getSubMediaSet(1);
            assertEquals(2, second.getMediaItemCount());
            item = (LocalMediaItem) second.getMediaItem(0);
            assertEquals(2, item.mId);
            assertEquals(2000L, item.mDateTakenInMs);
            item = (LocalMediaItem) second.getMediaItem(1);
            assertEquals(1, item.mId);
            assertEquals(1000L, item.mDateTakenInMs);

            assertEquals(DataManager.makeId(DataManager.ID_LOCAL_VIDEO_ALBUM, 0xB001),
                    first.getUniqueId());
            assertEquals(DataManager.makeId(DataManager.ID_LOCAL_VIDEO_ALBUM, 0xB000),
                    second.getUniqueId());
        }
    }

    static void createVideoTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS video (" +
                   "_id INTEGER PRIMARY KEY," +
                   "_data TEXT NOT NULL," +
                   "_display_name TEXT," +
                   "_size INTEGER," +
                   "mime_type TEXT," +
                   "date_added INTEGER," +
                   "date_modified INTEGER," +
                   "title TEXT," +
                   "duration INTEGER," +
                   "artist TEXT," +
                   "album TEXT," +
                   "resolution TEXT," +
                   "description TEXT," +
                   "isprivate INTEGER," +   // for YouTube videos
                   "tags TEXT," +           // for YouTube videos
                   "category TEXT," +       // for YouTube videos
                   "language TEXT," +       // for YouTube videos
                   "mini_thumb_data TEXT," +
                   "latitude DOUBLE," +
                   "longitude DOUBLE," +
                   "datetaken INTEGER," +
                   "mini_thumb_magic INTEGER" +
                   ");");
        db.execSQL("ALTER TABLE video ADD COLUMN bucket_id TEXT;");
        db.execSQL("ALTER TABLE video ADD COLUMN bucket_display_name TEXT");
    }

    static void insertVideoData(SQLiteDatabase db) {
        insertVideoData(db, 0xD000, 0xB000, "name");
    }

    static void insertVideoData(SQLiteDatabase db, long dateTaken,
            int bucketId, String bucketName) {
        db.execSQL("INSERT INTO video (title, mime_type, latitude, longitude, "
                + "datetaken, date_added, date_modified, bucket_id, "
                + "bucket_display_name, _data, duration) "
                + "VALUES ('VID_20100811_051413', 'video/mp4', 11, 22, "
                + dateTaken + ", 1281503663, 1281503662, '" + bucketId + "', "
                + "'" + bucketName + "', "
                + "'/mnt/sdcard/DCIM/Camera/VID_20100811_051413.3gp', 2964)");
    }

    static GalleryContextStub newGalleryContext(SQLiteDatabase db) {
        ContentProvider cp = new DbContentProvider(db);
        MockContentResolver cr = new MockContentResolver();
        cr.addProvider("media", cp);
        return new GalleryContextMock(null, cr);
    }

    static class MyListener implements MediaSet.MediaSetListener {
        int count;
        public void onContentChanged() {
            count++;
            Looper.myLooper().quit();
        }
    }
}

class DbContentProvider extends MockContentProvider {
    private static final String TAG = "DbContentProvider";
    private SQLiteDatabase mDatabase;

    DbContentProvider(SQLiteDatabase db) {
        mDatabase = db;
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        // This is a simplified version extracted from MediaProvider.

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String uriString = uri.toString();
        if (uriString.startsWith("content://media/external/images/media")) {
            qb.setTables("images");
        } else if (uriString.startsWith("content://media/external/video/media")) {
            qb.setTables("video");
        } else {
            return null;
        }

        String groupBy = null;
        String limit = uri.getQueryParameter("limit");

        if (uri.getQueryParameter("distinct") != null) {
            qb.setDistinct(true);
        }

        Log.v(TAG, "query = " + qb.buildQuery(projection, selection,
                selectionArgs, groupBy, null, sortOrder, limit));

        if (selectionArgs != null) {
            for (String s : selectionArgs) {
                Log.v(TAG, "  selectionArgs = " + s);
            }
        }

        Cursor c = qb.query(mDatabase, projection, selection,
                selectionArgs, groupBy, null, sortOrder, limit);

        return c;
    }
}
