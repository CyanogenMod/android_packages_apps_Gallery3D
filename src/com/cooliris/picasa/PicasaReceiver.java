package com.cooliris.picasa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PicasaReceiver extends BroadcastReceiver {

    private static final String TAG = "PicasaRecevier";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "Accounts changed: " + intent);
    }

}
