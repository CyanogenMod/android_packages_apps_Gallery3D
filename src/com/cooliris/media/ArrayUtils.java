package com.cooliris.media;

import java.util.ArrayList;
import java.util.Arrays;

public final class ArrayUtils {
    public static final void computeSortedIntersection(ArrayList<MediaItem> firstList, final ArrayList<MediaItem> secondList,
            int maxSize, ArrayList<MediaItem> intersectionList, MediaItem[] hash) {
        // Assumes that firstList is generally larger than the second list.
        // Build a simple filter to speed up containment testing.
        int mask = hash.length - 1;
        int numItemsToHash = Math.min(secondList.size(), 2 * hash.length);
        for (int i = 0; i < numItemsToHash; ++i) {
            MediaItem item = secondList.get(i);
            if (item != null) {
                hash[item.hashCode() & mask] = item;
            }
        }

        // Build the intersection array.
        int firstListSize = firstList.size();
        for (int i = 0; i < firstListSize; ++i) {
            MediaItem firstListItem = firstList.get(i);
            if (firstListItem == null)
                continue;
            MediaItem hashItem = (hash != null) ? hash[firstListItem.hashCode() & mask] : null;
            if (hashItem != null
                    && ((hashItem.mId != Shared.INVALID && hashItem.mId == firstListItem.mId) || contains(secondList, firstListItem))) {
                intersectionList.add(firstListItem);
                if (--maxSize == 0) {
                    break;
                }
            }
        }

        // Clear the hash table.
        Arrays.fill(hash, null);
    }

    public static final boolean contains(Object[] array, Object object) {
        if (object == null) {
            return false;
        }
        int length = array.length;
        for (int i = 0; i < length; ++i) {
            if (object.equals(array[i])) {
                return true;
            }
        }
        return false;
    }

    public static void clear(Object[] array) {
        int length = array.length;
        for (int i = 0; i < length; i++) {
            array[i] = null;
        }
    }

    public static final boolean contains(ArrayList<MediaItem> items, MediaItem item) {
        final int numItems = items.size();
        if (item.mId == Shared.INVALID)
            return false;
        for (int i = 0; i < numItems; ++i) {
            MediaItem thisItem = items.get(i);
            if (item.mId == thisItem.mId)
                return true;
        }
        return false;
    }
}
