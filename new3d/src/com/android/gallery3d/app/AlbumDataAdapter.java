// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.app;

import android.os.Handler;
import android.os.Message;

import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.ui.AlbumView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Utils;

import java.util.ArrayList;

public class AlbumDataAdapter implements AlbumView.Model {
    private static final String TAG = "AlbumDataAdapter";

    private static final long RELOAD_DELAY = 100; // 100ms

    private static final int MSG_UPDATE_DATA = 1;
    private static final int MSG_LOAD_DATA = 2;

    private static final int MIN_LOAD_COUNT = 32;
    private static final int MAX_LOAD_COUNT = 64;

    // Load the data because the source has been changed
    private static final int LOAD_SOURCE = 1;
    // Load the size for initial content
    private static final int LOAD_SIZE = 2;
    // Load the data for new content range
    private static final int LOAD_RANGE = 4;

    private final MediaItem[] mData;

    private int mActiveStart;
    private int mActiveEnd;

    private int mContentStart;
    private int mContentEnd;

    private final MediaSet mSource;
    private final Handler mMainHandler;
    private final Handler mDataHandler;
    private int mSize = 0;

    private AlbumView.ModelListener mListener;

    public AlbumDataAdapter(GalleryContext context, MediaSet albumSet, int cacheSize) {
        albumSet.setContentListener(new MySourceListener());

        mSource = albumSet;
        mData = new MediaItem[cacheSize];

        mDataHandler = new Handler(context.getDataManager().getDataLooper()) {
            @Override
            public void handleMessage(Message message) {
                Utils.Assert(message.what == MSG_LOAD_DATA);
                ((ReloadTask) message.obj).loadFromDatabase();
            }
        };

        mMainHandler = new SynchronizedHandler(context.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.Assert(message.what == MSG_UPDATE_DATA);
                ((ReloadTask) message.obj).updateContent();
            }
        };

        reloadData(LOAD_SIZE, 0, 0, 0);
    }

    public MediaItem get(int index) {
        if (!isActive(index)) {
            throw new IllegalArgumentException(String.format(
                    "%s not in (%s, %s)", index, mActiveStart, mActiveEnd));
        }
        return mData[index % mData.length];
    }

    public boolean isActive(int index) {
        return index >= mActiveStart && index < mActiveEnd;
    }

    public int size() {
        return mSize;
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart == mContentStart && contentEnd == mContentEnd) return;
        MediaItem[] data = mData;
        int length = data.length;
        if (contentStart >= mContentEnd || mContentStart >= contentEnd) {
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                data[i % length] = null;
            }
            reloadData(LOAD_RANGE, contentStart, contentEnd, 0);
        } else {
            for (int i = mContentStart; i < contentStart; ++i) {
                data[i % length] = null;
            }
            for (int i = contentEnd, n = mContentEnd; i < n; ++i) {
                data[i % length] = null;
            }
            reloadData(LOAD_RANGE, contentStart, mContentStart, 0);
            reloadData(LOAD_RANGE, mContentEnd, contentEnd, 0);
        }
        synchronized (this) {
            mContentStart = contentStart;
            mContentEnd = contentEnd;
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

    private void reloadData(int loadBits, int start, int end, long delay) {
        if (start >= end) loadBits &= ~LOAD_RANGE;
        if (loadBits != 0) {
            new ReloadTask(loadBits, start, end).execute(delay);
        }
    }

    private class MySourceListener implements MediaSet.MediaSetListener {
        public void onContentDirty() {
            reloadData(LOAD_SOURCE, mContentStart, mContentEnd, RELOAD_DELAY);
        }
    }

    public void setListener(AlbumView.ModelListener listener) {
        mListener = listener;
    }

    // TODO: use only one ReloadTask to update content
    private class ReloadTask {
        private int mStart;
        private int mEnd;
        private int mLoadBits;
        private int mUpdateSize = -1;
        private ArrayList<MediaItem> mUpdateItems;

        public ReloadTask(int loadBits, int start, int end) {
            mStart = start;
            mEnd = end;
            mLoadBits = loadBits;
        }

        public void execute(long delayed) {
            mDataHandler.sendMessageDelayed(
                    mDataHandler.obtainMessage(MSG_LOAD_DATA, this), delayed);
        }

        public void loadFromDatabase() {
            int loadBits = mLoadBits;

            if ((loadBits & LOAD_SOURCE) != 0) {
                if (!mSource.reload()) loadBits &= ~LOAD_SOURCE;
            }

            if ((loadBits & (LOAD_SOURCE | LOAD_SIZE)) != 0) {
                mUpdateSize = mSource.getMediaItemCount();
            }

            if ((loadBits & LOAD_RANGE) != 0) {
                synchronized (AlbumDataAdapter.this) {
                    mStart = Math.max(mContentStart, mStart);
                    mEnd = Math.min(mContentEnd, mEnd);
                }

                int count = Math.min(MAX_LOAD_COUNT, mEnd - mStart);
                if (count > 0) {
                    mUpdateItems = mSource.getMediaItem(mStart, count);
                } else {
                    loadBits &= ~LOAD_RANGE;
                    mUpdateItems = null;
                }
            }

            if (loadBits != 0) {
                mMainHandler.sendMessage(
                        mMainHandler.obtainMessage(MSG_UPDATE_DATA, this));
            }
        }

        public void updateContent() {
            if (mUpdateSize >= 0 && mSize != mUpdateSize) {
                mSize = mUpdateSize;
                if (mListener != null) mListener.onSizeChanged(mSize);
            }
            if (mUpdateItems == null) return;

            int start = Math.max(mStart, mContentStart);
            int end = Math.min(mStart + mUpdateItems.size(), mContentEnd);
            MediaItem data[] = mData;
            int size = data.length;
            int dataIndex = start % size;
            for (int i = start; i < end; ++i) {
                MediaItem updateItem = mUpdateItems.get(i - mStart);
                MediaItem original = data[dataIndex];
                // TODO: we should implement equals() for MediaItem
                //       to see if the item has been changed.
                if (original != updateItem) {
                    data[dataIndex] = updateItem;
                    if (mListener != null && i >= mActiveStart && i < mActiveEnd) {
                        mListener.onWindowContentChanged(i, original, updateItem);
                    }
                }
                if (++dataIndex == size) dataIndex = 0;
            }

            mStart = Math.max(mContentStart, mStart + mUpdateItems.size());
            mEnd = Math.min(mEnd, mContentEnd);
            if (mStart < mEnd && mUpdateItems.size() == MAX_LOAD_COUNT) {
                mLoadBits &= ~(LOAD_SOURCE | LOAD_SIZE);
                mDataHandler.sendMessage(
                        mDataHandler.obtainMessage(MSG_LOAD_DATA, this));
            }
        }
    }
}
