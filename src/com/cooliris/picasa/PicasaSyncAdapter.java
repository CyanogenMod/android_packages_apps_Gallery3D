package com.cooliris.picasa;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;

public class PicasaSyncAdapter extends AbstractThreadedSyncAdapter {
    private final Context mContext;
    public final static String TAG = "PicasaSyncAdapter";

    public PicasaSyncAdapter(Context applicationContext) {
        super(applicationContext, false);
        mContext = applicationContext;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient providerClient,
            SyncResult syncResult) {
        if (extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false)) {
            ContentResolver.setIsSyncable(account, authority, 1);
            ContentResolver.setSyncAutomatically(account, authority, true);
            return;
        }
        PicasaService.performSync(mContext, account, extras, syncResult);
    }

    public static final class AccountChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: Need to get account list change broadcast.
        }

    }
}
