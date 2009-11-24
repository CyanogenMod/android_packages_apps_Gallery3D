package com.cooliris.cache;

import com.cooliris.media.Gallery;
import com.cooliris.media.SingleDataSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private final String TAG = "BootReceiver"; 

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Got intent with action " + action);
        if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
            CacheService.markDirty(context);
            CacheService.startCache(context, true);
        } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            // Do nothing, wait for the mediascanner to be done after mounting.
            ;
        } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)) {
            Uri fileUri = intent.getData();
            long bucketId = SingleDataSource.parseBucketIdFromFileUri(fileUri.toString());
            if (!CacheService.isPresentInCache(bucketId)) {
                CacheService.markDirty(context);
            }
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Gallery.NEEDS_REFRESH = true;
        }
    }
}
