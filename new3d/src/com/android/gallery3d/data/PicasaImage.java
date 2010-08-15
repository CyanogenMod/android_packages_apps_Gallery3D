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

import android.graphics.Bitmap;
import android.util.Log;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.data.BlobCache.LookupRequest;
import com.android.gallery3d.picasa.PhotoEntry;
import com.android.gallery3d.util.ComboFuture;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

// PicasaImage is an image in the Picasa account.
public class PicasaImage extends MediaItem {
    private static final String TAG = "PicasaImage";

    private final GalleryContext mContext;
    private final PhotoEntry mData;
    private final BlobCache mPicasaCache;
    private final long mUniqueId;

    public PicasaImage(GalleryContext context, PhotoEntry entry) {
        mContext = context;
        mData = entry;
        mPicasaCache = mContext.getDataManager().getPicasaCache();
        mUniqueId = DataManager.makeId(
                DataManager.ID_PICASA_IMAGE, (int) entry.id);
    }

    @Override
    public long getUniqueId() {
        return mUniqueId;
    }

    @Override
    public synchronized Future<Bitmap>
            requestImage(int type, FutureListener<? super Bitmap> listener) {
        URL photoUrl = getPhotoUrl(type);
        if (mPicasaCache != null) {

            // Try to get the image from cache.
            LookupRequest request = new LookupRequest();
            request.key = Utils.crc64Long(photoUrl.toString());
            boolean isCached = false;
            try {
                isCached = mPicasaCache.lookup(request);
            } catch (IOException e) {
                Log.w(TAG, "IOException in getting an image from " +
                        "PicasaCache", e);
            }

            if (isCached) {
                byte[] uri = Utils.getBytesInUtf8(photoUrl.toString());
                if (isSameUri(uri, request.buffer)) {
                    Log.i(TAG, "Get Image from Cache (type, url): " + type +
                            " " + photoUrl.toString());
                    DecodeService service = mContext.getDecodeService();
                    return service.requestDecode(request.buffer, uri.length,
                            request.length - uri.length, null, listener);
                }
            }
        }

        // Get the image from Picasaweb instead.
        return new PicasaTask(type, photoUrl, listener);
    }

    private boolean isSameUri(byte[] uri, byte[] buffer) {
        int uriLength = uri.length;
        if (uriLength > buffer.length) {
            return false;
        }
        for (int i = 0; i < uriLength; ++i) {
            if (uri[i] != buffer[i]) {
                return false;
            }
        }
        return true;
    }

    private URL getPhotoUrl(int type) {
        URL url = null;
        try {
            switch (type) {
                case MediaItem.TYPE_MICROTHUMBNAIL:
                     url = new URL(mData.thumbnailUrl);
                    break;
                case MediaItem.TYPE_THUMBNAIL:
                    url = new URL(mData.screennailUrl);
                    break;
                case MediaItem.TYPE_FULL_IMAGE:
                    url = new URL(mData.contentUrl);
                    break;
                default:
                    throw new AssertionError();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }

    private class PicasaTask extends ComboFuture<Bitmap> {
        private final URL mUrl;
        private final int mType;

        public PicasaTask(
                int type, URL url, FutureListener<? super Bitmap> listener) {
            super(listener);
            mType = type;
            mUrl = url;
            execute();
        }

        @Override
        protected Future<?> executeNextTask(int step, Future<?> current)
                throws Exception {
            switch (step) {
                case 0: {
                    DownloadService service = mContext.getDownloadService();
                    return service.requestDownload(mUrl, this);
                }
                case 1: {
                    byte[] downloadedImage = (byte[]) current.get();

                    // Insert the downloaded image to the cache if the image is
                    // not a full size one.
                    if ((mPicasaCache != null)
                            && (mType != MediaItem.TYPE_FULL_IMAGE)) {
                        long cacheKey = Utils.crc64Long(mUrl.toString());
                        byte[] uri = Utils.getBytesInUtf8(mUrl.toString());
                        ByteBuffer buffer = ByteBuffer.allocate(
                                downloadedImage.length + uri.length);
                        buffer.put(uri);
                        buffer.put(downloadedImage);
                        mPicasaCache.insert(cacheKey, buffer.array());
                    }

                    // Decode the downloaded image.
                    DecodeService service = mContext.getDecodeService();
                    return service.requestDecode(downloadedImage, null, this);
                }
            }
            return null;
        }
    }
}
