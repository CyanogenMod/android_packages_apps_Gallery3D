// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.gadget;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class PhotoDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "launcher.db";

    private static final int DATABASE_VERSION = 2;

    static final String TABLE_PHOTOS = "photos";
    static final String FIELD_APPWIDGET_ID = "appWidgetId";
    static final String FIELD_PHOTO_BLOB = "photoBlob";

    PhotoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PHOTOS + " ("
                + FIELD_APPWIDGET_ID + " INTEGER PRIMARY KEY,"
                + FIELD_PHOTO_BLOB + " BLOB" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int version = oldVersion;

        if (version != DATABASE_VERSION) {
            Log.w(PhotoAppWidgetProvider.TAG, "Destroying all old data.");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHOTOS);
            onCreate(db);
        }
    }

    /**
     * Store the given bitmap in this database for the given appWidgetId.
     */
    public boolean setPhoto(int appWidgetId, Bitmap bitmap) {
        boolean success = false;
        try {
            // Try go guesstimate how much space the icon will take when
            // serialized to avoid unnecessary allocations/copies during
            // the write.
            int size = bitmap.getWidth() * bitmap.getHeight() * 4;
            ByteArrayOutputStream out = new ByteArrayOutputStream(size);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            ContentValues values = new ContentValues();
            values.put(PhotoDatabaseHelper.FIELD_APPWIDGET_ID, appWidgetId);
            values.put(PhotoDatabaseHelper.FIELD_PHOTO_BLOB, out.toByteArray());

            SQLiteDatabase db = getWritableDatabase();
            db.insertOrThrow(PhotoDatabaseHelper.TABLE_PHOTOS, null, values);

            success = true;
        } catch (SQLiteException e) {
            Log.e(PhotoAppWidgetProvider.TAG, "Could not open database", e);
        } catch (IOException e) {
            Log.e(PhotoAppWidgetProvider.TAG, "Could not serialize photo", e);
        }
        if (PhotoAppWidgetProvider.LOGD) {
            Log.d(PhotoAppWidgetProvider.TAG, "setPhoto success=" + success);
        }
        return success;
    }

    private static final String[] PHOTOS_PROJECTION = {FIELD_PHOTO_BLOB};
    private static final int INDEX_PHOTO_BLOB = 0;
    private static final String WHERE_CLAUSE = FIELD_APPWIDGET_ID + " = ?";

    /**
     * Inflate and return a bitmap for the given appWidgetId.
     */
    public Bitmap getPhoto(int appWidgetId) {
        Cursor c = null;
        Bitmap bitmap = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String selection = String.format("%s=%d", FIELD_APPWIDGET_ID, appWidgetId);
            c = db.query(TABLE_PHOTOS, PHOTOS_PROJECTION,
                    WHERE_CLAUSE, new String[]{String.valueOf(appWidgetId)},
                    null, null, null, null);

            if (c != null && PhotoAppWidgetProvider.LOGD) {
                Log.d(PhotoAppWidgetProvider.TAG, "getPhoto query count=" + c.getCount());
            }

            if (c != null && c.moveToFirst()) {
                byte[] data = c.getBlob(INDEX_PHOTO_BLOB);
                if (data != null) {
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                }
            }
        } catch (SQLiteException e) {
            Log.e(PhotoAppWidgetProvider.TAG, "Could not load photo from database", e);
        } finally {
            if (c != null) c.close();
        }
        return bitmap;
    }

    /**
     * Remove any bitmap associated with the given appWidgetId.
     */
    public void deletePhoto(int appWidgetId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            String whereClause = String.format("%s=%d", FIELD_APPWIDGET_ID, appWidgetId);
            db.delete(TABLE_PHOTOS, whereClause, null);
        } catch (SQLiteException e) {
            Log.e(PhotoAppWidgetProvider.TAG, "Could not delete photo from database", e);
        }
    }
}