package com.cooliris.media;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.cooliris.app.App;
import com.cooliris.app.Res;
import com.cooliris.cache.CacheService;
import com.cooliris.wallpaper.RandomDataSource;
import com.cooliris.wallpaper.Slideshow;

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

    private static final int CHECK_STORAGE = 0;
    private static final int HANDLE_INTENT = 1;
    private static final int NUM_STORAGE_CHECKS = 25;

    private Handler handler = new Handler() {
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
                mApp.showToast(getResources().getString(Res.string.no_sd_card), Toast.LENGTH_LONG);
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
                Toast.makeText(this, getResources().getString(Res.string.no_sd_card), Toast.LENGTH_LONG).show();
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

        sendInitialMessage();

        Log.i(TAG, "onCreate");
    }

    private void sendInitialMessage() {
        mNumRetries = 0;
        Message checkStorage = new Message();
        checkStorage.what = CHECK_STORAGE;
        handler.sendMessage(checkStorage);
    }

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
            mApp.getHandler().post(new Runnable() {
                public void run() {
                    // We check to see if the authenticated accounts have
                    // changed, and
                    // if so, reload the datasource.
                    HashMap<String, Boolean> accountsEnabled = PicasaDataSource.getAccountStatus(Gallery.this);
                    String[] keys = new String[accountsEnabled.size()];
                    keys = accountsEnabled.keySet().toArray(keys);
                    int numKeys = keys.length;
                    for (int i = 0; i < numKeys; ++i) {
                        String key = keys[i];
                        boolean newValue = accountsEnabled.get(key).booleanValue();
                        boolean oldValue = false;
                        Boolean oldValObj = mAccountsEnabled.get(key);
                        if (oldValObj != null) {
                            oldValue = oldValObj.booleanValue();
                        }
                        if (oldValue != newValue) {
                            // Reload the datasource.
                            if (mGridLayer != null)
                                mGridLayer.setDataSource(mGridLayer.getDataSource());
                            break;
                        }
                    }
                    mAccountsEnabled = accountsEnabled;
                }
            });
        	mApp.onResume();
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
                if (mGridLayer != null) {
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
        mAccountsEnabled = PicasaDataSource.getAccountStatus(Gallery.this);
    }

}
