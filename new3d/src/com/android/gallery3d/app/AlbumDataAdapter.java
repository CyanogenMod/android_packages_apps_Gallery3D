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

    private static final int MSG_UPDATE_DATA = 1;
    private static final int MSG_LOAD_DATA = 2;

    private static int UPDATE_LIMIT = 32;

    private final MediaItem[] mData;

    private int mActiveStart;
    private int mActiveEnd;

    private int mContentStart;
    private int mContentEnd;

    private final MediaSet mSource;
    private final Handler mMainHandler;
    private final Handler mDataHandler;
    private int mSize;

    private AlbumView.ModelListener mListener;

    public AlbumDataAdapter(GalleryContext context, MediaSet albumSet, int cacheSize) {
        albumSet.setContentListener(new MySourceListener());

        mSource = albumSet;
        mData = new MediaItem[cacheSize];
        mSize = albumSet.getMediaItemCount();

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
    }

    private void updateCacheData(ArrayList<MediaItem> update, int offset) {
        int start = Math.max(offset, mContentStart);
        int end = Math.min(offset + update.size(), mContentEnd);

        MediaItem data[] = mData;
        int size = data.length;
        int dataIndex = start % size;
        for (int i = start; i < end; ++i) {
            MediaItem updateItem = update.get(i - offset);
            MediaItem original = data[dataIndex];
            if (original != updateItem) {
                data[dataIndex] = updateItem;
                if (mListener != null) {
                    mListener.onWindowContentChanged(i, original, updateItem);
                }
            }
            if (++dataIndex == size) dataIndex = 0;
        }
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
            reloadData(contentStart, contentEnd, false);
        } else {
            for (int i = mContentStart; i < contentStart; ++i) {
                data[i % length] = null;
            }
            for (int i = contentEnd, n = mContentEnd; i < n; ++i) {
                data[i % length] = null;
            }
            reloadData(contentStart, mContentStart, false);
            reloadData(mContentEnd, contentEnd, false);
        }
        mContentStart = contentStart;
        mContentEnd = contentEnd;
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
                || Math.abs(contentStart - mContentStart) > UPDATE_LIMIT) {
            setContentWindow(contentStart, contentEnd);
        }
    }

    private void onSourceContentChanged() {
        reloadData(mContentStart, mContentEnd, true);
    }

    private void reloadData(int start, int end, boolean reloadSize) {
        if (start < end) {
            new ReloadTask(start, end, reloadSize).execute();
        }
    }

    private class MySourceListener implements MediaSet.MediaSetListener {
        public void onContentChanged() {
            onSourceContentChanged();
        }

        public void onContentDirty() {
        }
    }

    public void setListener(AlbumView.ModelListener listener) {
        mListener = listener;
    }

    private class ReloadTask {
        private final int mStart;
        private final int mEnd;
        private final boolean mReloadSize;
        private int mUpdateSize;
        private ArrayList<MediaItem> mUpdateItems;

        public ReloadTask(int start, int end, boolean reloadSize) {
            mStart = start;
            mEnd = end;
            mReloadSize = reloadSize;
        }

        public void execute() {
            mDataHandler.sendMessage(
                    mDataHandler.obtainMessage(MSG_LOAD_DATA, this));
        }

        public void loadFromDatabase() {
            if (mReloadSize) {
                mUpdateSize = mSource.getMediaItemCount();
            }
            mUpdateItems = mSource.getMediaItem(mStart, mEnd - mStart);
            mMainHandler.sendMessage(
                    mMainHandler.obtainMessage(MSG_UPDATE_DATA, this));
        }

        public void updateContent() {
            if (mReloadSize && mSize != mUpdateSize) {
                mSize = mUpdateSize;
                if (mListener != null) mListener.onSizeChanged(mSize);
            }
            updateCacheData(mUpdateItems, mStart);
        }
    }
}
