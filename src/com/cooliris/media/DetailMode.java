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

package com.cooliris.media;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.io.File;

import android.content.Context;
import android.content.res.Resources;
import android.media.ExifInterface;

import com.cooliris.app.App;
import com.cooliris.app.Res;

public final class DetailMode {
    public static CharSequence[] populateDetailModeStrings(Context context, ArrayList<MediaBucket> buckets) {
        int numBuckets = buckets.size();
        if (MediaBucketList.isSetSelection(buckets) && numBuckets == 1) {
            // If just 1 set was selected, save the trouble of processing the
            // items in the set again.
            // We have already processed details for that set.
            return populateSetViewDetailModeStrings(context, MediaBucketList.getFirstSetSelection(buckets), 1);
        } else if (MediaBucketList.isSetSelection(buckets) || MediaBucketList.isMultipleItemSelection(buckets)) {
            // Cycle through the items and add them to the selection items set.
            MediaSet selectedItemsSet = new MediaSet();
            for (int i = 0; i < numBuckets; i++) {
                MediaBucket bucket = buckets.get(i);
                ArrayList<MediaItem> currItems = null;
                int numCurrItems = 0;
                if (MediaBucketList.isSetSelection(bucket)) {
                    MediaSet currSet = bucket.mediaSet;
                    if (currSet != null) {
                        currItems = currSet.getItems();
                        numCurrItems = currSet.getNumItems();
                    }
                } else {
                    currItems = bucket.mediaItems;
                    numCurrItems = currItems.size();
                }
                if (currItems != null) {
                    for (int j = 0; j < numCurrItems; j++) {
                        selectedItemsSet.addItem(currItems.get(j));
                    }
                }
            }
            return populateSetViewDetailModeStrings(context, selectedItemsSet, numBuckets);
        } else {
            return populateItemViewDetailModeStrings(context, MediaBucketList.getFirstItemSelection(buckets));
        }
    }

    private static CharSequence[] populateSetViewDetailModeStrings(Context context, MediaSet selectedItemsSet, int numOriginalSets) {
        if (selectedItemsSet == null) {
            return null;
        }
        Resources resources = context.getResources();
        ArrayList<CharSequence> strings = new ArrayList<CharSequence>();

        // Number of albums selected.
        if (numOriginalSets == 1) {
            strings.add("1 " + resources.getString(Res.string.album_selected));
        } else {
            strings.add(Integer.toString(numOriginalSets) + " " + resources.getString(Res.string.albums_selected));
        }

        // Number of items selected.
        int numItems = selectedItemsSet.mNumItemsLoaded;
        if (numItems == 1) {
            strings.add("1 " + resources.getString(Res.string.item_selected));
        } else {
            strings.add(Integer.toString(numItems) + " " + resources.getString(Res.string.items_selected));
        }

        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

        // Start and end times of the selected items.
        if (selectedItemsSet.areTimestampsAvailable()) {
            long minTimestamp = selectedItemsSet.mMinTimestamp;
            long maxTimestamp = selectedItemsSet.mMaxTimestamp;
            if (selectedItemsSet.isPicassaSet()) {
                minTimestamp -= App.CURRENT_TIME_ZONE.getOffset(minTimestamp);
                maxTimestamp -= App.CURRENT_TIME_ZONE.getOffset(maxTimestamp);
            }
            strings.add(resources.getString(Res.string.start) + ": " + dateTimeFormat.format(new Date(minTimestamp)));
            strings.add(resources.getString(Res.string.end) + ": " + dateTimeFormat.format(new Date(maxTimestamp)));
        } else if (selectedItemsSet.areAddedTimestampsAvailable()) {
            long minTimestamp = selectedItemsSet.mMinAddedTimestamp;
            long maxTimestamp = selectedItemsSet.mMaxAddedTimestamp;
            if (selectedItemsSet.isPicassaSet()) {
                minTimestamp -= App.CURRENT_TIME_ZONE.getOffset(minTimestamp);
                maxTimestamp -= App.CURRENT_TIME_ZONE.getOffset(maxTimestamp);
            }
            strings.add(resources.getString(Res.string.start) + ": " + dateTimeFormat.format(new Date(minTimestamp)));
            strings.add(resources.getString(Res.string.end) + ": " + dateTimeFormat.format(new Date(maxTimestamp)));
        } else {
            strings.add(resources.getString(Res.string.start) + ": " + resources.getString(Res.string.date_unknown));
            strings.add(resources.getString(Res.string.end) + ": " + resources.getString(Res.string.date_unknown));
        }

        // The location of the selected items.
        String locationString = null;
        if (selectedItemsSet.mLatLongDetermined) {
            locationString = selectedItemsSet.mReverseGeocodedLocation;
            if (locationString == null) {
                // Try computing the location if it does not exist.
                ReverseGeocoder reverseGeocoder = App.get(context).getReverseGeocoder();
                locationString = reverseGeocoder.computeMostGranularCommonLocation(selectedItemsSet);
            }
        }
        if (locationString != null && locationString.length() > 0) {
            strings.add(resources.getString(Res.string.location) + ": " + locationString);
        }
        int numStrings = strings.size();
        CharSequence[] stringsArr = new CharSequence[numStrings];
        for (int i = 0; i < numStrings; ++i) {
            stringsArr[i] = strings.get(i);
        }
        return stringsArr;
    }

    private static CharSequence[] populateItemViewDetailModeStrings(Context context, MediaItem item) {
        if (item == null) {
            return null;
        }
        Resources resources = context.getResources();
        CharSequence[] strings = new CharSequence[8];
        strings[0] = resources.getString(Res.string.title) + ": " + item.mCaption;
        strings[1] = resources.getString(Res.string.type) + ": " + item.getDisplayMimeType();

        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

        if (item.mLocaltime == null) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            try {
                ExifInterface exif = new ExifInterface(item.mFilePath);
                String localtime = exif.getAttribute(ExifInterface.TAG_DATETIME);
                if (localtime != null) {
                    item.mLocaltime = formatter.parse(localtime, new ParsePosition(0));
                }
            } catch (IOException ex) {
                // ignore it.
            }
            if (item.mLocaltime == null && item.mCaption != null) {
                formatter = new SimpleDateFormat("yyyyMMdd'_'HHmmss");
                // skip initial IMG_ or VND_
                item.mLocaltime = formatter.parse(item.mCaption, new ParsePosition(4));
            }
        }

        if (item.mLocaltime != null) {
            strings[2] = resources.getString(Res.string.taken_on) + ": " + dateTimeFormat.format(item.mLocaltime);
        } else if (item.isDateTakenValid()) {
            long dateTaken = item.mDateTakenInMs;
            if (item.isPicassaItem()) {
                dateTaken -= App.CURRENT_TIME_ZONE.getOffset(dateTaken);
            }
            strings[2] = resources.getString(Res.string.taken_on) + ": " + dateTimeFormat.format(new Date(dateTaken));
        } else if (item.isDateAddedValid()) {
            long dateAdded = item.mDateAddedInSec * 1000;
            if (item.isPicassaItem()) {
                dateAdded -= App.CURRENT_TIME_ZONE.getOffset(dateAdded);
            }
            // TODO: Make this added_on as soon as translations are ready.
            // strings[2] = resources.getString(Res.string.added_on) + ": " +
            // DateFormat.format("h:mmaa MMM dd yyyy", dateAdded);
            strings[2] = resources.getString(Res.string.taken_on) + ": " + dateTimeFormat.format(new Date(dateAdded));
        } else {
            strings[2] = resources.getString(Res.string.taken_on) + ": " + resources.getString(Res.string.date_unknown);
        }
        MediaSet parentMediaSet = item.mParentMediaSet;
        if (parentMediaSet == null) {
            strings[3] = resources.getString(Res.string.album) + ":";
        } else {
            strings[3] = resources.getString(Res.string.album) + ": " + parentMediaSet.mName;
        }
        ReverseGeocoder reverseGeocoder = App.get(context).getReverseGeocoder();
        String locationString = item.getReverseGeocodedLocation(reverseGeocoder);
        if (locationString == null || locationString.length() == 0) {
            locationString = context.getResources().getString(Res.string.location_unknown);
        }
        strings[4] = resources.getString(Res.string.location) + ": " + locationString;

        String fileSize;
        if (item.mFilePath == null) {
            fileSize = context.getResources().getString(Res.string.file_size_unknown);
        } else {
            File file = new File(item.mFilePath);
            long lenght = file.length()/1024;
            fileSize = Long.toString(lenght) + " KB";
        }

        strings[5] = resources.getString(Res.string.file_size) + ": " + fileSize;
        String imageSize;
        String camera;
        if (item.mFilePath == null) {
            imageSize = context.getResources().getString(Res.string.size_unknown);
            camera = context.getResources().getString(Res.string.cam_unknown);
        } else {
            try {
                ExifInterface exif = new ExifInterface(item.mFilePath);
                String length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
                String width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
                String maker = exif.getAttribute(ExifInterface.TAG_MAKE);
                String model = exif.getAttribute(ExifInterface.TAG_MODEL);
                camera = exif.getAttribute(ExifInterface.TAG_MAKE) + " " + exif.getAttribute(ExifInterface.TAG_MODEL);
                if (width.contentEquals("0") || length.contentEquals("0")) {
                    imageSize = context.getResources().getString(Res.string.size_unknown);
                } else {
                    imageSize = width + " x " + length;
                }
                if (maker == null || model == null)
                {
                    camera = context.getResources().getString(Res.string.cam_unknown);
                } else {
                    camera = maker + " " + model;
                }

            } catch (IOException ex) {
                // ignore it.
                imageSize = context.getResources().getString(Res.string.size_unknown);
                camera = context.getResources().getString(Res.string.cam_unknown);
            }
        }
        strings[6] = resources.getString(Res.string.size) + ": " + imageSize;
        strings[7] = resources.getString(Res.string.cam) + ": " + camera;


        return strings;
    }
}
