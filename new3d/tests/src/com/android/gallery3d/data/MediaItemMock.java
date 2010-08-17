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

import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;

public class MediaItemMock extends MediaItem {
    long mUniqueId;

    public MediaItemMock(long uniqueId) {
        mUniqueId = uniqueId;
    }

    public Future<Bitmap>
            requestImage(int type, FutureListener<? super Bitmap> listener) {
        return null;
    }

    public long getUniqueId() {
        return mUniqueId;
    }
}
