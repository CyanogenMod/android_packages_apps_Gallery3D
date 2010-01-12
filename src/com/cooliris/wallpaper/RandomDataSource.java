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
            retVal = UriTexture.createFromUri(context, uri, UriTexture.MAX_RESOLUTION, UriTexture.MAX_RESOLUTION, cacheId, null);
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
