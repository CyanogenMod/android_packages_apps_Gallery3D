package com.cooliris.media;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.content.res.Resources;

;

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
            strings.add("1 " + resources.getString(R.string.album_selected));
        } else {
            strings.add(Integer.toString(numOriginalSets) + " " + resources.getString(R.string.albums_selected));
        }

        // Number of items selected.
        int numItems = selectedItemsSet.mNumItemsLoaded;
        if (numItems == 1) {
            strings.add("1 " + resources.getString(R.string.item_selected));
        } else {
            strings.add(Integer.toString(numItems) + " " + resources.getString(R.string.items_selected));
        }

        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

        // Start and end times of the selected items.
        if (selectedItemsSet.areTimestampsAvailable()) {
            long minTimestamp = selectedItemsSet.mMinTimestamp;
            long maxTimestamp = selectedItemsSet.mMaxTimestamp;
            if (selectedItemsSet.isPicassaSet()) {
                minTimestamp -= Gallery.CURRENT_TIME_ZONE.getOffset(minTimestamp);
                maxTimestamp -= Gallery.CURRENT_TIME_ZONE.getOffset(maxTimestamp);
            }
            strings.add(resources.getString(R.string.start) + ": " + dateTimeFormat.format(new Date(minTimestamp)));
            strings.add(resources.getString(R.string.end) + ": " + dateTimeFormat.format(new Date(maxTimestamp)));
        } else if (selectedItemsSet.areAddedTimestampsAvailable()) {
            long minTimestamp = selectedItemsSet.mMinAddedTimestamp;
            long maxTimestamp = selectedItemsSet.mMaxAddedTimestamp;
            if (selectedItemsSet.isPicassaSet()) {
                minTimestamp -= Gallery.CURRENT_TIME_ZONE.getOffset(minTimestamp);
                maxTimestamp -= Gallery.CURRENT_TIME_ZONE.getOffset(maxTimestamp);
            }
            strings.add(resources.getString(R.string.start) + ": " + dateTimeFormat.format(new Date(minTimestamp)));
            strings.add(resources.getString(R.string.end) + ": " + dateTimeFormat.format(new Date(maxTimestamp)));
        } else {
            strings.add(resources.getString(R.string.start) + ": " + resources.getString(R.string.date_unknown));
            strings.add(resources.getString(R.string.end) + ": " + resources.getString(R.string.date_unknown));
        }

        // The location of the selected items.
        String locationString = null;
        if (selectedItemsSet.mLatLongDetermined) {
            locationString = selectedItemsSet.mReverseGeocodedLocation;
            if (locationString == null) {
                // Try computing the location if it does not exist.
                ReverseGeocoder reverseGeocoder = ((Gallery) context).getReverseGeocoder();
                locationString = reverseGeocoder.computeMostGranularCommonLocation(selectedItemsSet);
            }
        }
        if (locationString != null && locationString.length() > 0) {
            strings.add(resources.getString(R.string.location) + ": " + locationString);
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
        CharSequence[] strings = new CharSequence[5];
        strings[0] = resources.getString(R.string.title) + ": " + item.mCaption;
        strings[1] = resources.getString(R.string.type) + ": " + item.getDisplayMimeType();

        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

        if (item.isDateTakenValid()) {
            long dateTaken = item.mDateTakenInMs;
            if (item.isPicassaItem()) {
                dateTaken -= Gallery.CURRENT_TIME_ZONE.getOffset(dateTaken);
            }
            strings[2] = resources.getString(R.string.taken_on) + ": " + dateTimeFormat.format(new Date(dateTaken));
        } else if (item.isDateAddedValid()) {
            long dateAdded = item.mDateAddedInSec * 1000;
            if (item.isPicassaItem()) {
                dateAdded -= Gallery.CURRENT_TIME_ZONE.getOffset(dateAdded);
            }
            // TODO: Make this added_on as soon as translations are ready.
            // strings[2] = resources.getString(R.string.added_on) + ": " +
            // DateFormat.format("h:mmaa MMM dd yyyy", dateAdded);
            strings[2] = resources.getString(R.string.taken_on) + ": " + dateTimeFormat.format(new Date(dateAdded));
        } else {
            strings[2] = resources.getString(R.string.taken_on) + ": " + resources.getString(R.string.date_unknown);
        }
        MediaSet parentMediaSet = item.mParentMediaSet;
        if (parentMediaSet == null) {
            strings[3] = resources.getString(R.string.album) + ":";
        } else {
            strings[3] = resources.getString(R.string.album) + ": " + parentMediaSet.mName;
        }
        ReverseGeocoder reverseGeocoder = ((Gallery) context).getReverseGeocoder();
        String locationString = item.getReverseGeocodedLocation(reverseGeocoder);
        if (locationString == null || locationString.length() == 0) {
            locationString = context.getResources().getString(R.string.location_unknown);
        }
        strings[4] = resources.getString(R.string.location) + ": " + locationString;
        return strings;
    }
}