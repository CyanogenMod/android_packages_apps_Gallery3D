//package com.cooliris.media;
//
//// Deprecated class. Need to remove from perforce
//
//import java.util.ArrayList;
//
//public class FlatLocalDataSource implements MediaFeed.DataSource {
//    private final boolean mIncludeImages;
//    private final boolean mIncludeVideos;
//    
//    public FlatLocalDataSource(boolean includeImages, boolean includeVideos) {
//        mIncludeImages = includeImages;
//        mIncludeVideos = includeVideos;
//    }
//    
//    public DiskCache getThumbnailCache() {
//        return LocalDataSource.sThumbnailCache;
//    }
//
//    public void loadItemsForSet(MediaFeed feed, MediaSet parentSet, int rangeStart, int rangeEnd) {
//        // TODO Auto-generated method stub
//        
//    }
//
//    public void loadMediaSets(MediaFeed feed) {
//        MediaSet set = feed.addMediaSet(0, this);
//        set.name = "Local Media";
//    }
//
//    public boolean performOperation(int operation, ArrayList<MediaBucket> mediaBuckets, Object data) {
//        // TODO Auto-generated method stub
//        return false;
//    }
//
//}
