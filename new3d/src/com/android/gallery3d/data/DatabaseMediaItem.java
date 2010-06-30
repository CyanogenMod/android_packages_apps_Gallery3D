package com.android.gallery3d.data;


public abstract class DatabaseMediaItem extends AbstractMediaItem {
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
