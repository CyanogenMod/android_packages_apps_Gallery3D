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

package com.cooliris.media;

import com.cooliris.app.App;
import com.cooliris.app.Res;
import com.cooliris.cache.CacheService;
import com.cooliris.wallpaper.RandomDataSource;
import com.cooliris.wallpaper.Slideshow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.HashMap;

public final class Gallery extends Activity {
    public static final String REVIEW_ACTION = "com.cooliris.media.action.REVIEW";
    private static final String TAG = "Gallery";

    private App mApp = null;
    private RenderView mRenderView = null;
    private GridLayer mGridLayer;
    private WakeLock mWakeLock;
    private HashMap<String, Boolean> mAccountsEnabled = new HashMap<String, Boolean>();
    private boolean mDockSlideshow = false;
    private int mNumRetries;
    private boolean mImageManagerHasStorageAfterDelay = false;
    private HandlerThread mPicasaAccountThread = new HandlerThread("PicasaAccountMonitor");
    private Handler mPicasaHandler = null;

    private static final int GET_PICASA_ACCOUNT_STATUS = 1;
    private static final int UPDATE_PICASA_ACCOUNT_STATUS = 2;

    private static final int CHECK_STORAGE = 0;
    private static final int HANDLE_INTENT = 1;
    private static final int NUM_STORAGE_CHECKS = 25;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CHECK_STORAGE:
                    checkStorage();
                    break;
                case HANDLE_INTENT:
                    initializeDataSource();
                    break;
            }
        }
    };

    private void checkStorage() {
        mNumRetries++;
        mImageManagerHasStorageAfterDelay = ImageManager.hasStorage();
        if (!mImageManagerHasStorageAfterDelay && mNumRetries < NUM_STORAGE_CHECKS) {
            if (mNumRetries == 1) {
                int res;

                if (Environment.isExternalStorageRemovable()) {
                    res = Res.string.no_sd_card;
                } else {
                    res = Res.string.no_usb_storage;
                }

                mApp.showToast(getResources().getString(res), Toast.LENGTH_LONG);
            }
            handler.sendEmptyMessageDelayed(CHECK_STORAGE, 200);
        } else {
            handler.sendEmptyMessage(HANDLE_INTENT);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp = new App(Gallery.this);
        final boolean imageManagerHasStorage = ImageManager.hasStorage();
        boolean slideshowIntent = false;
        if (isViewIntent()) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                slideshowIntent = extras.getBoolean("slideshow", false);
            }
        }
        if (isViewIntent() && getIntent().getData().equals(Images.Media.EXTERNAL_CONTENT_URI) && slideshowIntent) {
            if (!imageManagerHasStorage) {
                int res;

                if (Environment.isExternalStorageRemovable()) {
                    res = Res.string.no_sd_card;
                } else {
                    res = Res.string.no_usb_storage;
                }

                Toast.makeText(this, getResources().getString(res), Toast.LENGTH_LONG).show();
                finish();
            } else {
                Slideshow slideshow = new Slideshow(this);
                slideshow.setDataSource(new RandomDataSource());
                setContentView(slideshow);
                mDockSlideshow = true;
            }
            return;
        }
        mRenderView = new RenderView(this);
        mGridLayer = new GridLayer(this, (int) (96.0f * App.PIXEL_DENSITY), (int) (72.0f * App.PIXEL_DENSITY), new GridLayoutInterface(4),
                mRenderView);
        mRenderView.setRootLayer(mGridLayer);
        setContentView(mRenderView);

        mPicasaAccountThread.start();
        mPicasaHandler = new Handler(mPicasaAccountThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case GET_PICASA_ACCOUNT_STATUS:
                        mAccountsEnabled = PicasaDataSource.getAccountStatus(Gallery.this);
                        break;
                    case UPDATE_PICASA_ACCOUNT_STATUS:
                        updatePicasaAccountStatus();
                        break;
                }
            }
        };

        sendInitialMessage();

        Log.i(TAG, "onCreate");
    }

    private void sendInitialMessage() {
        mNumRetries = 0;
        Message checkStorage = new Message();
        checkStorage.what = CHECK_STORAGE;
        handler.sendMessage(checkStorage);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handler.removeMessages(CHECK_STORAGE);
        handler.removeMessages(HANDLE_INTENT);

        sendInitialMessage();
    }

    @Override
    public void onRestart() {
        super.onRestart();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mDockSlideshow) {
            if (mWakeLock != null) {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "GridView.Slideshow.All");
            mWakeLock.acquire();
            return;
        }
        if (mRenderView != null) {
            mRenderView.onResume();
        }
        if (mApp.isPaused()) {
            if (mPicasaHandler != null) {
                mPicasaHandler.removeMessages(GET_PICASA_ACCOUNT_STATUS);
                mPicasaHandler.sendEmptyMessage(UPDATE_PICASA_ACCOUNT_STATUS);
            }
        	mApp.onResume();
        }
    }

    void updatePicasaAccountStatus() {
        // We check to see if the authenticated accounts have
        // changed, if so, reload the datasource.

        // TODO: This should be done in PicasaDataFeed
        if (mGridLayer != null) {
            HashMap<String, Boolean> accountsEnabled = PicasaDataSource.getAccountStatus(this);
            if (!accountsEnabled.equals(mAccountsEnabled)) {
                mGridLayer.setDataSource(mGridLayer.getDataSource());
                mAccountsEnabled = accountsEnabled;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRenderView != null)
            mRenderView.onPause();
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mWakeLock = null;
        }

        LocalDataSource.sThumbnailCache.flush();
        LocalDataSource.sThumbnailCacheVideo.flush();
        PicasaDataSource.sThumbnailCache.flush();

        if (mPicasaHandler != null) {
            mPicasaHandler.removeMessages(GET_PICASA_ACCOUNT_STATUS);
            mPicasaHandler.removeMessages(UPDATE_PICASA_ACCOUNT_STATUS);
        }
    	mApp.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGridLayer != null)
            mGridLayer.stop();

        // Start the thumbnailer.
        CacheService.startCache(this, true);
    }

    @Override
    public void onDestroy() {
        // Force GLThread to exit.
        setContentView(Res.layout.main);

        // Remove any post messages.
        handler.removeMessages(CHECK_STORAGE);
        handler.removeMessages(HANDLE_INTENT);

        mPicasaAccountThread.quit();
        mPicasaAccountThread = null;
        mPicasaHandler = null;

        if (mGridLayer != null) {
            DataSource dataSource = mGridLayer.getDataSource();
            if (dataSource != null) {
                dataSource.shutdown();
            }
            mGridLayer.shutdown();
        }
        if (mRenderView != null) {
            mRenderView.shutdown();
            mRenderView = null;
        }
        mGridLayer = null;
        mApp.shutdown();
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mGridLayer != null) {
            mGridLayer.markDirty(30);
        }
        if (mRenderView != null)
            mRenderView.requestRender();
        Log.i(TAG, "onConfigurationChanged");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mRenderView != null) {
            return mRenderView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private boolean isPickIntent() {
        String action = getIntent().getAction();
        return (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action));
    }

    private boolean isViewIntent() {
        String action = getIntent().getAction();
        return Intent.ACTION_VIEW.equals(action);
    }

    private boolean isReviewIntent() {
        String action = getIntent().getAction();
        return REVIEW_ACTION.equals(action);
    }

    private boolean isImageType(String type) {
        return type.contains("*/") || type.equals("vnd.android.cursor.dir/image") || type.equals("image/*");
    }

    private boolean isVideoType(String type) {
        return type.contains("*/") || type.equals("vnd.android.cursor.dir/video") || type.equals("video/*");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case CropImage.CROP_MSG: {
            if (resultCode == RESULT_OK) {
                setResult(resultCode, data);
                finish();
            }
            break;
        }
        case CropImage.CROP_MSG_INTERNAL: {
            // We cropped an image, we must try to set the focus of the camera
            // to that image.
            if (resultCode == RESULT_OK) {
                String contentUri = data.getAction();
                if (mGridLayer != null && contentUri != null) {
                    mGridLayer.focusItem(contentUri);
                }
            }
            break;
        }
        }
    }

    @Override
    public void onLowMemory() {
        if (mRenderView != null) {
            mRenderView.handleLowMemory();
        }
    }

    private void initializeDataSource() {
        final boolean hasStorage = mImageManagerHasStorageAfterDelay;
        // Creating the DataSource objects.
        final PicasaDataSource picasaDataSource = new PicasaDataSource(Gallery.this);
        final LocalDataSource localDataSource = new LocalDataSource(Gallery.this, LocalDataSource.URI_ALL_MEDIA, false);
        final ConcatenatedDataSource combinedDataSource = new ConcatenatedDataSource(localDataSource, picasaDataSource);

        // Depending upon the intent, we assign the right dataSource.
        if (!isPickIntent() && !isViewIntent() && !isReviewIntent()) {
            localDataSource.setMimeFilter(true, true);
            if (hasStorage) {
                mGridLayer.setDataSource(combinedDataSource);
            } else {
                mGridLayer.setDataSource(picasaDataSource);
            }
        } else if (isPickIntent()) {
            final Intent intent = getIntent();
            if (intent != null) {
                String type = intent.resolveType(Gallery.this);
                if (type == null) {
                    // By default, we include images
                    type = "image/*";
                }
                boolean includeImages = isImageType(type);
                boolean includeVideos = isVideoType(type);
                localDataSource.setMimeFilter(includeImages, includeVideos);
                if (includeImages) {
                    if (hasStorage) {
                        mGridLayer.setDataSource(combinedDataSource);
                    } else {
                        mGridLayer.setDataSource(picasaDataSource);
                    }
                } else {
                    mGridLayer.setDataSource(localDataSource);
                }
                mGridLayer.setPickIntent(true);
                if (hasStorage) {
                    mApp.showToast(getResources().getString(Res.string.pick_prompt), Toast.LENGTH_LONG);
                }
            }
        } else { // view intent for images and review intent for images and videos
            final Intent intent = getIntent();
            Uri uri = intent.getData();
            boolean slideshow = intent.getBooleanExtra("slideshow", false);
            final LocalDataSource singleDataSource = new LocalDataSource(Gallery.this, uri.toString(), true);
            // Display both image and video.
            singleDataSource.setMimeFilter(true, true);

            if (hasStorage) {
                ConcatenatedDataSource singleCombinedDataSource = new ConcatenatedDataSource(singleDataSource,
                        picasaDataSource);
                mGridLayer.setDataSource(singleCombinedDataSource);
            } else {
                mGridLayer.setDataSource(picasaDataSource);
            }
            mGridLayer.setViewIntent(true, Utils.getBucketNameFromUri(getContentResolver(), uri));

            if (isReviewIntent()) {
                mGridLayer.setEnterSelectionMode(true);
            }

            if (singleDataSource.isSingleImage()) {
                mGridLayer.setSingleImage(false);
            } else if (slideshow) {
                mGridLayer.setSingleImage(true);
                mGridLayer.startSlideshow();
            }
        }
        // We record the set of enabled accounts for picasa.
        mPicasaHandler.sendEmptyMessage(GET_PICASA_ACCOUNT_STATUS);
    }
}
