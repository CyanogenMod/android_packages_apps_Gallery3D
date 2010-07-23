package com.android.gallery3d.data;


public abstract class DatabaseMediaItem extends AbstractMediaItem {

    protected int mId;
    protected String mCaption;
    protected String mMimeType;
    protected double mLatitude;
    protected double mLongitude;
    protected long mDateTakenInMs;
    protected long mDateAddedInSec;
    protected long mDateModifiedInSec;
    protected String mFilePath;

    public String getTitle() {
        return mCaption;
    }
}
