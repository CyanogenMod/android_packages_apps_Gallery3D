/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.cooliris.wallpaper;

import java.io.IOException;
import java.net.URISyntaxException;

import com.cooliris.cache.CacheService;
import com.cooliris.cache.ImageList;
import com.cooliris.media.UriTexture;
import com.cooliris.media.Util;

import android.content.Context;
import android.graphics.Bitmap;

public class RandomDataSource implements Slideshow.DataSource {

    public Bitmap getBitmapForIndex(Context context, int currentSlideshowCounter) {
        ImageList list = CacheService.getImageList(context);
        // Once we have the id and the thumbid, we can return a bitmap
        // First we select a random numbers
        if (list.ids == null)
            return null;
        double random = Math.random();
        random *= list.ids.length;
        int index = (int) random;
        long cacheId = list.thumbids[index];
        final String uri = CacheService.BASE_CONTENT_STRING_IMAGES + list.ids[index];
        Bitmap retVal = null;
        try {
            int max_resolution =
                    context.getResources().getInteger(com.cooliris.media.R.integer.max_resolution);
            retVal = UriTexture.createFromUri(context, uri,
                    max_resolution, max_resolution, cacheId, null);
            if (retVal != null) {
                retVal = Util.rotate(retVal, list.orientation[index]);
            }
        } catch (OutOfMemoryError e) {
            ;
        } catch (IOException e) {
            ;
        } catch (URISyntaxException e) {
            ;
        }
        return retVal;
    }

}
