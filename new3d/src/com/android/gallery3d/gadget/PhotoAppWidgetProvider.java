/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.gallery3d.gadget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.gallery3d.R;

/**
 * Simple widget to show a user-selected picture.
 */
public class PhotoAppWidgetProvider extends AppWidgetProvider {
    static final String TAG = "PhotoAppWidgetProvider";
    static final boolean LOGD = true;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update each requested appWidgetId with its unique photo
        PhotoDatabaseHelper helper = new PhotoDatabaseHelper(context);
        for (int appWidgetId : appWidgetIds) {
            int[] specificAppWidget = new int[] {appWidgetId};
            RemoteViews views = buildUpdate(context, appWidgetId, helper);
            if (LOGD) {
                Log.d(TAG, "sending out views=" + views + " for id=" + appWidgetId);
            }
            appWidgetManager.updateAppWidget(specificAppWidget, views);
        }
        helper.close();
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // Clean deleted photos out of our database
        PhotoDatabaseHelper helper = new PhotoDatabaseHelper(context);
        for (int appWidgetId : appWidgetIds) {
            helper.deletePhoto(appWidgetId);
        }
        helper.close();
    }

    /**
     * Load photo for given widget and build {@link RemoteViews} for it.
     */
    static RemoteViews buildUpdate(Context context, int appWidgetId, PhotoDatabaseHelper helper) {
        RemoteViews views = null;
        Bitmap bitmap = helper.getPhoto(appWidgetId);
        if (bitmap != null) {
            views = new RemoteViews(context.getPackageName(), R.layout.photo_frame);
            views.setImageViewBitmap(R.id.photo, bitmap);
        }
        return views;
    }

}
