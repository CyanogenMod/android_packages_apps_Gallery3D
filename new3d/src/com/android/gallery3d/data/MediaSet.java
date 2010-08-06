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

// This is currently implemented MediaSet and MediaItem:
//
//           | Local | Picasa
// ----------+----------------
//  AlbumSet |   1   |    2
//  Album    |   3   |    4
//  Image    |   5   |    6
//  Video    |   7   | (unimplemented)
//
//  Inheritance relation:
//
//  MediaSet -- DatabaseMediaSet -- {1,2,3,4}
//  MediaItem -- LocalMediaItem -- {5, 7}
//            -- {6}
//
//  root = ComboMediaSet (LocalAlbumSet, PicasaAlbumSet);


// MediaSet is a directory-like data structure.
// It contains MediaItems and sub-MediaSets.
//
// getTotalMediaItemCount() returns the number of all MediaItems, including
// those in sub-MediaSets.
//
// getCoverMediaItems() return a few representative MediaItems for this
// MediaSet.
//
public abstract class MediaSet {

    public interface MediaSetListener {
        public void onContentChanged();
    }

    public abstract int getMediaItemCount();

    public abstract MediaItem getMediaItem(int index);

    public abstract int getSubMediaSetCount();

    public abstract MediaSet getSubMediaSet(int index);

    public abstract int getTotalMediaItemCount();

    public abstract String getTitle();

    public abstract MediaItem[] getCoverMediaItems();

    public abstract void setContentListener(MediaSetListener listener);
}
