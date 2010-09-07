/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.ui;

import android.content.Context;
import android.os.Vibrator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;

public class SelectionManager {
    private static final String TAG = "SelectionManager";
    private Set<Long> mClickedSet;
    private MediaSet mSourceMediaSet;
    private final Vibrator mVibrator;
    private final SelectionDrawer mDrawer;
    private SelectionListener mListener;
    private DataManager mDataManager;
    private boolean mInverseSelection;
    private boolean mIsAlbumSet;
    private int mTotal;

    public interface SelectionListener {
        public void onSelectionModeChange(boolean inSelectionMode);
    }

    public SelectionManager(GalleryContext galleryContext, boolean isAlbumSet) {
        Context context = galleryContext.getAndroidContext();
        mDrawer = new SelectionDrawer(context);
        mDataManager = galleryContext.getDataManager();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mClickedSet = new HashSet<Long>();
        mIsAlbumSet = isAlbumSet;
        mTotal = -1;
    }

    public void setSelectionListener(SelectionListener listener) {
        mListener = listener;
    }

    public void selectAll() {
        enterSelectionMode();
        mInverseSelection = true;
        mClickedSet.clear();
    }

    public void deSelectAll() {
        leaveSelectionMode();
        mInverseSelection = false;
        mClickedSet.clear();
    }

    public SelectionDrawer getSelectionDrawer() {
        return mDrawer;
    }

    public boolean inSelectionMode() {
        return mInverseSelection || (mClickedSet.size() > 0);
    }

    private void enterSelectionMode() {
        if (inSelectionMode()) return;

        mVibrator.vibrate(100);
        mDrawer.setSelectionMode(true);
        if (mListener != null) mListener.onSelectionModeChange(true);
    }

    public void leaveSelectionMode() {
        if (!inSelectionMode()) return;

        mInverseSelection = false;
        mDrawer.setSelectionMode(false);
        mClickedSet.clear();
        if (mListener != null) mListener.onSelectionModeChange(false);
    }

    public boolean isItemSelected(long itemId) {
        return mInverseSelection ^ mClickedSet.contains(itemId);
    }

    public void toggle(long itemId) {
        if (mClickedSet.contains(itemId)) {
            mClickedSet.remove(itemId);
        } else {
            enterSelectionMode();
            mClickedSet.add(itemId);
        }

        if (mInverseSelection) {
            if (mTotal < 0) {
                mTotal = mIsAlbumSet
                        ? mSourceMediaSet.getSubMediaSetCount()
                        : mSourceMediaSet.getMediaItemCount();
            }
            if (mClickedSet.size() == mTotal) leaveSelectionMode();
        } else {
            if (mClickedSet.size() == 0) leaveSelectionMode();
        }
    }

    private static void expandMediaSet(ArrayList<Long> items, MediaSet set) {
        int subCount = set.getSubMediaSetCount();
        for (int i = 0; i < subCount; i++) {
            expandMediaSet(items, set.getSubMediaSet(i));
        }
        int total = set.getMediaItemCount();
        int batch = 50;
        int index = 0;

        while (index < total) {
            int count = index + batch < total
                    ? batch
                    : total - index;
            ArrayList<MediaItem> list = set.getMediaItem(index, count);
            for (MediaItem item : list) {
                items.add(item.getUniqueId());
            }
            index += batch;
        }
    }

    public ArrayList<Long> getSelected(boolean expandSet) {
        ArrayList<Long> selected = new ArrayList<Long>();
        if (mIsAlbumSet) {
            if (mInverseSelection) {
                int max = mSourceMediaSet.getSubMediaSetCount();
                for (int i = 0; i < max; i++) {
                    MediaSet set = mSourceMediaSet.getSubMediaSet(i);
                    long id = set.getUniqueId();
                    if (!mClickedSet.contains(id)) {
                        if (expandSet) {
                            expandMediaSet(selected, set);
                        } else {
                            selected.add(id);
                        }
                    }
                }
            } else {
                for (long id : mClickedSet) {
                    if (expandSet) {
                        expandMediaSet(selected,
                                mDataManager.getMediaSet(DataManager.extractSelfId(id)));
                    } else {
                        selected.add(id);
                    }
                }
            }
        } else {
            if (mInverseSelection) {
                int batch = 50;
                int total = mSourceMediaSet.getMediaItemCount();
                int index = 0;

                while (index < total) {
                    int count = index + batch < total
                            ? batch
                            : total - index;
                    ArrayList<MediaItem> list = mSourceMediaSet.getMediaItem(index, count);
                    for (MediaItem item : list) {
                        long id = item.getUniqueId();
                        if (!mClickedSet.contains(id)) selected.add(id);
                    }
                    index += batch;
                }
            } else {
                for (long id : mClickedSet) {
                    selected.add(id);
                }
            }
        }
        return selected;
    }

    public void setSourceMediaSet(MediaSet set) {
        mSourceMediaSet = set;
        mTotal = -1;
    }

    public MediaSet getSourceMediaSet() {
        return mSourceMediaSet;
    }
}
