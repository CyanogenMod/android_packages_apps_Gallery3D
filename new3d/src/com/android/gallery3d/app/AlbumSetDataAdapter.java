// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.app;

import android.os.Handler;
import android.os.Message;

import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.ui.AlbumSetView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Utils;

import java.util.ArrayList;

public class AlbumSetDataAdapter implements AlbumSetView.Model {
    private static final String TAG = "AlbumSetDataAdapter";

    private static final long RELOAD_DELAY = 100; // 100ms

    private static final int MIN_LOAD_COUNT = 4;
    private static final int MAX_COVER_COUNT = 4;

    // Load the data because the source has been changed
    private static final int LOAD_SOURCE = 1;
    // Load the initial content
    private static final int LOAD_SIZE = 2;
    // Load the data for new content range
    private static final int LOAD_RANGE = 4;

    private static final int MSG_UPDATE_CONTENT = 1;
    private static final int MSG_LOAD_CONTENT = 2;

    private static final MediaItem[] EMPTY_MEDIA_ITEMS = new MediaItem[0];

    private final MediaItem[][] mData;

    private int mActiveStart;
    private int mActiveEnd;

    private int mContentStart;
    private int mContentEnd;

    private final MediaSet mSource;
    private int mSize;

    private AlbumSetView.ModelListener mListener;

    private Handler mMainHandler;
    private Handler mDataHandler;

    public AlbumSetDataAdapter(GalleryContext context, MediaSet albumSet, int cacheSize) {
        albumSet.setContentListener(new MySourceListener());

        mSource = albumSet;
        mData = new MediaItem[cacheSize][];

        mMainHandler = new SynchronizedHandler(context.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.Assert(message.what == MSG_UPDATE_CONTENT);
                ((ReloadTask) message.obj).updateContent();
            }
        };

        mDataHandler = new Handler(context.getDataManager().getDataLooper()) {
            @Override
            public void handleMessage(Message message) {
                Utils.Assert(message.what == MSG_LOAD_CONTENT);
                ((ReloadTask) message.obj).loadFromDatabase();
            }
        };

        reloadData(LOAD_SIZE, 0, 0, 0);
    }

    public MediaItem[] get(int index) {
        if (index < mActiveStart && index >= mActiveEnd) {
            throw new IllegalArgumentException(String.format(
                    "%s not in (%s, %s)", index, mActiveStart, mActiveEnd));
        }
        MediaItem[] result = mData[index % mData.length];

        // If the result is not ready yet, return an empty array
        return result == null ? EMPTY_MEDIA_ITEMS : result;
    }

    public int size() {
        return mSize;
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart == mContentStart && contentEnd == mContentEnd) return;
        MediaItem[][] data = mData;
        int length = data.length;
        if (contentStart >= mContentEnd || mContentStart >= contentEnd) {
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                mData[i % length] = null;
            }
            reloadData(LOAD_RANGE, contentStart, contentEnd, 0);
        } else {
            for (int i = mContentStart; i < contentStart; ++i) {
                mData[i % length] = null;
            }
            for (int i = contentEnd, n = mContentEnd; i < n; ++i) {
                mData[i % length] = null;
            }
            reloadData(LOAD_RANGE, contentStart, mContentStart, 0);
            reloadData(LOAD_RANGE, mContentEnd, contentEnd, 0);
        }
        mContentStart = contentStart;
        mContentEnd = contentEnd;
    }

    private void reloadData(int loadBits, int start, int end, long delay) {
        if (start >= end) loadBits &= ~LOAD_RANGE;
        if (loadBits != 0) {
            new ReloadTask(loadBits, start, end).execute(delay);
        }
    }

    public void setActiveWindow(int start, int end) {
        if (start == mActiveStart && end == mActiveEnd) return;

        mActiveStart = start;
        mActiveEnd = end;

        Utils.Assert(start <= end
                && end - start <= mData.length && end <= mSize);

        int length = mData.length;
        mActiveStart = start;
        mActiveEnd = end;

        // If no data is visible, keep the cache content
        if (start == end) return;

        int contentStart = Utils.clamp((start + end) / 2 - length / 2,
                0, Math.max(0, mSize - length));
        int contentEnd = Math.min(contentStart + length, mSize);
        if (mContentStart > start || mContentEnd < end
                || Math.abs(contentStart - mContentStart) > MIN_LOAD_COUNT) {
            setContentWindow(contentStart, contentEnd);
        }
    }

    private class MySourceListener implements MediaSet.MediaSetListener {
        public void onContentDirty() {
            reloadData(LOAD_SOURCE, mContentStart, mContentEnd, RELOAD_DELAY);
        }
    }

    public void setListener(AlbumSetView.ModelListener listener) {
        mListener = listener;
    }

    // TODO: using only one task to update the content
    private class ReloadTask {
        private int mStart;
        private int mEnd;
        private final int mLoadBits;

        private MediaItem[] mLoadData = null;
        private int mUpdateSize = -1;

        public ReloadTask(int loadBits, int start, int end) {
            mStart = start;
            mEnd = end;
            mLoadBits = loadBits;
        }

        public void execute(long delay) {
            mDataHandler.sendMessageDelayed(
                    mDataHandler.obtainMessage(MSG_LOAD_CONTENT, this), delay);
        }

        public void loadFromDatabase() {
            int loadBits = mLoadBits;
            mLoadData = null;

            if ((loadBits & LOAD_SOURCE) != 0) {
                if (!mSource.reload()) loadBits &= ~LOAD_SOURCE;
            }

            if ((loadBits & (LOAD_SOURCE | LOAD_SIZE)) != 0) {
                mUpdateSize = mSource.getSubMediaSetCount();
            }

            if ((loadBits & LOAD_RANGE) != 0) {
                int size = mSource.getSubMediaSetCount();
                MediaSet subset = mSource.getSubMediaSet(mStart);
                if (subset != null) {
                    ArrayList<MediaItem> items =
                            subset.getMediaItem(0, MAX_COVER_COUNT);
                    mLoadData = items.toArray(new MediaItem[items.size()]);
                }
            }
            if (loadBits != 0) {
                mMainHandler.sendMessage(
                        mMainHandler.obtainMessage(MSG_UPDATE_CONTENT, this));
            }
        }

        public void updateContent() {
            if (mUpdateSize >= 0 && mUpdateSize != mSize) {
                mSize = mUpdateSize;
                if (mListener != null) mListener.onSizeChanged(mSize);
            }
            int index = mStart;
            if (mLoadData != null
                    && index >= mContentStart && index < mContentEnd) {
                MediaItem[] update = mLoadData;
                MediaItem[] original = mData[index];
                // TODO: Fix it. Find a way to judge if we need to update content
                if (!update.equals(original)) {
                    mData[index] = update;
                    if (mListener != null
                            && index >= mActiveStart && index < mActiveEnd) {
                        mListener.onWindowContentChanged(index, original, update);
                    }
                }
            }
            mStart = index + 1;
            mStart = Math.max(mContentStart, mStart);
            mEnd = Math.min(mContentEnd, mEnd);
            if (mStart < mEnd) {
                mDataHandler.sendMessage(
                        mDataHandler.obtainMessage(MSG_LOAD_CONTENT, this));
            }
        }
    }
}
