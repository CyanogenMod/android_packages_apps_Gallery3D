package com.cooliris.media;

import java.util.ArrayList;

public interface DataSource {
    // Load the sets to be displayed.
    void loadMediaSets(final MediaFeed feed);

    // rangeStart->rangeEnd is inclusive
    // Pass in Shared.INFINITY for the rangeEnd to load all items.
    void loadItemsForSet(final MediaFeed feed, final MediaSet parentSet, int rangeStart, int rangeEnd);

    // Called when the data source will no longer be used.
    void shutdown();

    boolean performOperation(int operation, ArrayList<MediaBucket> mediaBuckets, Object data);

    DiskCache getThumbnailCache();
    
    // This method is called so that we can setup listeners for any databases that the datasource uses
    String[] getDatabaseUris();

    // Called when the user explicitly requests a refresh, or when the application is brought to the foreground.
    // Alternatively, when one or more of the database's data changes, this method will be called.
    void refresh(final MediaFeed feed, final String[] databaseUris);
    
}
