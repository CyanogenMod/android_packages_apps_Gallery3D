// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.app;

import android.os.Handler;
import android.os.Message;

import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.ui.GalleryView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Utils;

import java.util.ArrayList;

public class GalleryDataAdapter implements GalleryView.Model {
    private static final String TAG = "GalleryDataAdapter";

    private static final int UPDATE_LIMIT = 32;
    private static final int MAX_COVER_COUNT = 4;

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

    private GalleryView.ModelListener mListener;

    private Handler mMainHandler;
    private Handler mDataHandler;

    public GalleryDataAdapter(GalleryContext context, MediaSet albumSet, int cacheSize) {
        albumSet.setContentListener(new MySourceListener());

        mSource = albumSet;
        mData = new MediaItem[cacheSize][];
        mSize = albumSet.getMediaItemCount();

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
            reloadData(contentStart, contentEnd);
        } else {
            for (int i = mContentStart; i < contentStart; ++i) {
                mData[i % length] = null;
            }
            for (int i = contentEnd, n = mContentEnd; i < n; ++i) {
                mData[i % length] = null;
            }
            reloadData(contentStart, mContentStart);
            reloadData(mContentEnd, contentEnd);
        }
        mContentStart = contentStart;
        mContentEnd = contentEnd;
    }

    private void reloadData(int start, int end) {
        if (end <= start) return;

        MediaSet[] sourceSets = new MediaSet[end - start];
        for (int i = start; i < end; ++i) {
            sourceSets[i - start] = mSource.getSubMediaSet(i);
        }
        new ReloadTask(sourceSets, start).execute();
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
        int size = mSource.getSubMediaSetCount();
        if (mSize != size) {
            mSize = size;
            if (mListener != null) mListener.onSizeChanged(size);
        }
        reloadData(mContentStart, mContentEnd);
    }

    private class MySourceListener implements MediaSet.MediaSetListener {

        public void onContentDirty() {
            mSource.reload();
        }

        public void onContentChanged() {
            onSourceContentChanged();
        }
    }

    public void setListener(GalleryView.ModelListener listener) {
        mListener = listener;
    }

    private class ReloadTask {
        private final int mOffset;
        private final MediaSet mSourceSets[];
        private ArrayList<MediaItem[]> mLoadData = new ArrayList<MediaItem[]>();

        public ReloadTask(MediaSet[] sourceSets, int offset) {
            mSourceSets = sourceSets;
            mOffset = offset;
        }

        public void execute() {
            mDataHandler.sendMessage(
                    mDataHandler.obtainMessage(MSG_LOAD_CONTENT, this));
        }

        public void loadFromDatabase() {
            for (MediaSet set : mSourceSets) {
                ArrayList<MediaItem> items = set.getMediaItem(0, MAX_COVER_COUNT);
                mLoadData.add(items.toArray(new MediaItem[items.size()]));
            }
            mMainHandler.sendMessage(
                    mMainHandler.obtainMessage(MSG_UPDATE_CONTENT, this));
        }

        public void updateContent() {
            int start = Math.max(mOffset, mContentStart);
            int end = Math.min(mOffset + mLoadData.size(), mContentEnd);
            int offset = mOffset;
            for (int i = start; i < end; ++i) {
                MediaItem[] update = mLoadData.get(i - offset);
                MediaItem[] original = mData[i];
                if (!update.equals(original)) {
                    mData[i] = update;
                    if (mListener != null) {
                        mListener.onWindowContentChanged(i, original, update);
                    }
                }
            }
        }
    }
}
