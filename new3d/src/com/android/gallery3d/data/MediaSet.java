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

package com.android.gallery3d.data;

import android.net.Uri;

import java.util.ArrayList;

// MediaSet is a directory-like data structure.
// It contains MediaItems and sub-MediaSets.
//
// The primary interface are:
// getMediaItemCount(), getMediaItem() and
// getSubMediaSetCount(), getSubMediaSet().
//
// getTotalMediaItemCount() returns the number of all MediaItems, including
// those in sub-MediaSets.
public abstract class MediaSet {

    // Below are the bits returned from getSupportedOperations():
    public static final int SUPPORT_DELETE = 1;
    public static final int SUPPORT_ROTATE = 2;
    public static final int SUPPORT_SHARE = 4;

    public static final int MEDIA_TYPE_UNKNOWN = 1;
    public static final int MEDIA_TYPE_IMAGE = 2;
    public static final int MEDIA_TYPE_VIDEO = 4;
    public static final int MEDIA_TYPE_ALL = MEDIA_TYPE_IMAGE | MEDIA_TYPE_VIDEO;

    public interface MediaSetListener {
        public void onContentDirty();
    }

    public int getMediaItemCount() {
        return 0;
    }

    // Returns the media items in the range [start, start + count).
    //
    // The number of media items returned may be less than the specified count
    // if there are not enough media items available. The number of
    // media items available may not be consistent with the return value of
    // getMediaItemCount() because the contents of database may have already
    // changed.
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        throw new IndexOutOfBoundsException();
    }

    public int getSubMediaSetCount() {
        return 0;
    }

    public MediaSet getSubMediaSet(int index) {
        throw new IndexOutOfBoundsException();
    }

    public abstract int getTotalMediaItemCount();

    public abstract long getUniqueId();
    public abstract String getName();

    protected MediaSetListener mListener;

    public void setContentListener(MediaSetListener listener) {
        mListener = listener;
    }

    // Reload the content. Return true if the content is changed; false otherwise.
    // reload should be called in the same thread as getMediaItem(int, int) and
    // getSubMediaSet(int).
    public abstract boolean reload();

    public int getSupportedOperations(long uniqueId) {
        return 0;
    }

    public void delete(long uniqueId) {
        throw new UnsupportedOperationException();
    }

    public void rotate(long uniqueId, int degrees) {
        throw new UnsupportedOperationException();
    }

    public Uri getMediaItemUri(long uniqueId) {
        throw new UnsupportedOperationException();
    }

    public int getMediaType(long uniqueId) {
        return MEDIA_TYPE_UNKNOWN;
    }

    public int getMergeId() {
        return 0;
    }

    int getMyId() {
        return DataManager.extractSelfId(getUniqueId());
    }
}
