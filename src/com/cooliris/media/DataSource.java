package com.cooliris.media;

import java.util.ArrayList;

public interface DataSource {
    // Load the sets to be displayed.
    void loadMediaSets(final MediaFeed feed);

    // Pass in Shared.INFINITY for the rangeEnd to load all items.
    void loadItemsForSet(final MediaFeed feed, final MediaSet parentSet, int rangeStart, int rangeEnd);

    // Called when the data source will no longer be used.
    void shutdown();

    boolean performOperation(int operation, ArrayList<MediaBucket> mediaBuckets, Object data);

    DiskCache getThumbnailCache();
}
