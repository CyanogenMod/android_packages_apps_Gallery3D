/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.cooliris.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.net.Uri;

import com.cooliris.app.App;
import com.cooliris.app.Res;

/**
 * This activity plays a video from a specified URI.
 */
public class MovieView extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "MovieView";

    // IDs of the main menu items.
    public static final int MENU_OPEN_URL          = 0;

    private App mApp = null; 
    private MovieViewControl mControl;
    private boolean mFinishOnCompletion;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mApp = new App(MovieView.this);
        setContentView(Res.layout.movie_view);
        View rootView = findViewById(Res.id.root);
        Intent intent = getIntent();
        mControl = new MovieViewControl(rootView, this, intent.getData()) {
            @Override
            public void onCompletion() {
                if (mFinishOnCompletion) {
                    finish();
                }
            }
        };
        if (intent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
            int orientation = intent.getIntExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (orientation != getRequestedOrientation()) {
                setRequestedOrientation(orientation);
            }
        }
        mFinishOnCompletion = intent.getBooleanExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (getResources().getBoolean(R.bool.button_brightness_override)) {
            winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        }
        win.setAttributes(winParams);
    }

    @Override
    public void onPause() {
        mControl.onPause();
        super.onPause();
        mApp.onPause();
    }

    @Override
    public void onResume() {
        mControl.onResume();
        super.onResume();
        mApp.onResume();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        MenuItem item1 = menu.add(0, MENU_OPEN_URL, 0, R.string.menu_open_url);
        item1.setIcon(android.R.drawable.ic_menu_preferences);

        return true;
    }

    private void restart(String uri) {
        Intent i = new Intent(this, MovieView.class);
        i.setData(Uri.parse(uri));
        startActivity(i);
    }

    private void showSetUrlDialog() {
        final EditText editText = new EditText(this);
        //editText.setText("rtsp://192.168.1.1/sample_300kbit.mp4");
        editText.setHint(R.string.video_set_url_hint);

        new AlertDialog.Builder(this)
        .setView(editText)
        .setTitle(R.string.video_set_url_title)
        .setMessage(R.string.video_set_url_msg)
        .setPositiveButton(R.string.video_set_url_pos, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String uri = editText.getText().toString();
                finish();
                restart(uri);
            }
        })
        .setNegativeButton(R.string.video_set_url_neg, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        }).create().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = true;
        switch(item.getItemId()) {
            case MENU_OPEN_URL:
                showSetUrlDialog();
                break;
            default:
                ret = false;
        }
        return ret;
    }
    
    @Override
    public void onDestroy() {
        mControl.onDestroy();
        mApp.shutdown();
        super.onDestroy();
    }
}
