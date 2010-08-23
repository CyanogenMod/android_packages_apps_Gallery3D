
package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.util.Log;

import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;

public abstract class AbstractDisplayItem
        extends DisplayItem implements FutureListener<Bitmap> {

    private static final String TAG = "AbstractDisplayItem";

    private static final int STATE_INVALID = 0x01;
    private static final int STATE_VALID = 0x02;
    private static final int STATE_UPDATING = 0x04;
    private static final int STATE_CANCELING = 0x08;
    private static final int STATE_ERROR = 0x10;

    private int mState = STATE_INVALID;
    private Future<Bitmap> mFuture;
    private boolean mImageRequested = false;
    private boolean mRecycling = false;
    private Bitmap mBitmap;

    protected final MediaItem mMediaItem;

    public AbstractDisplayItem(MediaItem item) {
        mMediaItem = item;
        if (item == null) mState = STATE_ERROR;
    }

    public void updateImage() {
        Future<Bitmap> future = mFuture;
        mFuture = null;

        if (future.isCancelled()) {
            mState = STATE_INVALID;
            if (mRecycling) {
                onRecycled();
            } else if (mImageRequested) {
                Log.v(TAG, String.format("request image again %s", toString()));
                requestImage();
            }
            return;
        }

        Bitmap bitmap = null;
        try {
            bitmap = future.get();
        } catch (Throwable t) {
            Log.e(TAG, "cannot get bitmap", t);
        }

        if (mRecycling) {
            if (bitmap != null) bitmap.recycle();
            onRecycled();
            return;
        }

        mBitmap = bitmap;
        mState = bitmap == null ? STATE_ERROR : STATE_VALID ;
        onBitmapAvailable(mBitmap);
    }

    @Override
    public long getIdentity() {
        return mMediaItem != null
                ? mMediaItem.getUniqueId()
                : System.identityHashCode(this);
    }

    public void requestImage() {
        if (mState == STATE_INVALID) {
            mState = STATE_UPDATING;
            mFuture = mMediaItem.requestImage(MediaItem.TYPE_MICROTHUMBNAIL, this);
        }
        mImageRequested = true;
    }

    public void cancelImageRequest() {
        if (mState == STATE_UPDATING) {
            mState = STATE_CANCELING;
            mFuture.requestCancel();
        }
        mImageRequested = false;
    }

    private boolean inState(int states) {
        return (mState & states) != 0;
    }

    public void recycle() {
        if (!inState(STATE_UPDATING | STATE_CANCELING)) {
            if (mBitmap != null) {
                mBitmap.recycle();
                mBitmap = null;
            }
            onRecycled();
        } else {
            mRecycling = true;
            cancelImageRequest();
        }
    }

    public boolean isRequestInProgress() {
        return mImageRequested && inState(STATE_UPDATING | STATE_CANCELING);
    }

    protected void onRecycled() {
    }

    abstract protected void onBitmapAvailable(Bitmap bitmap);

    abstract public void onFutureDone(Future<? extends Bitmap> future);
}
