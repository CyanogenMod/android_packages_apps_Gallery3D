package com.android.gallery3d.data;

public abstract class AbstractMediaItem implements MediaItem {
    static final int FULLIMAGE_TARGET_SIZE = 1024;
    static final int FULLIMAGE_MAX_NUM_PIXELS = 3 * 1024 * 1024;
    static final int UNCONSTRAINED = -1;

    int mId;
    String mCaption;
    String mMimeType;
    double mLatitude;
    double mLongitude;
    long mDateTakenInMs;
    long mDateAddedInSec;
    long mDateModifiedInSec;
    String mFilePath;
    String mContentUri;

    public String getMediaUri() {
        return mContentUri;
    }

    public String getTitle() {
        return mCaption;
    }
}
