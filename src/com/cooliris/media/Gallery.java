package com.cooliris.media;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.TimeZone;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore.Images;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import android.media.MediaScannerConnection;

import com.cooliris.cache.CacheService;
import com.cooliris.wallpaper.RandomDataSource;
import com.cooliris.wallpaper.Slideshow;

public final class Gallery extends Activity {
    public static final TimeZone CURRENT_TIME_ZONE = TimeZone.getDefault();
    public static float PIXEL_DENSITY = 0.0f;
    public static final int CROP_MSG_INTERNAL = 100;

    private static final String TAG = "Gallery";
    private static final int CROP_MSG = 10;
    private RenderView mRenderView = null;
    private GridLayer mGridLayer;
    private final Handler mHandler = new Handler();
    private ReverseGeocoder mReverseGeocoder;
    private boolean mPause;
    private MediaScannerConnection mConnection;
    private WakeLock mWakeLock;
    private HashMap<String, Boolean> mAccountsEnabled = new HashMap<String, Boolean>();
    private boolean mDockSlideshow = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final boolean imageManagerHasStorage = ImageManager.hasStorage();
        boolean slideshowIntent = false;
        if (isViewIntent()) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                slideshowIntent = extras.getBoolean("slideshow", false);
            }
        }
        if (isViewIntent() && getIntent().getData().equals(Images.Media.EXTERNAL_CONTENT_URI)
                && slideshowIntent) {
            if (!imageManagerHasStorage) {
                Toast.makeText(this, getResources().getString(R.string.no_sd_card), Toast.LENGTH_LONG).show();
                finish();
            } else {
                Slideshow slideshow = new Slideshow(this);
                slideshow.setDataSource(new RandomDataSource());
                setContentView(slideshow);
                mDockSlideshow = true;
            }
            return;
        }
        if (PIXEL_DENSITY == 0.0f) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            PIXEL_DENSITY = metrics.density;
        }
        mReverseGeocoder = new ReverseGeocoder(this);
        mRenderView = new RenderView(this);
        mGridLayer = new GridLayer(this, (int) (96.0f * PIXEL_DENSITY), (int) (72.0f * PIXEL_DENSITY), new GridLayoutInterface(4),
                mRenderView);
        mRenderView.setRootLayer(mGridLayer);
        setContentView(mRenderView);
        
        Thread t = new Thread() {
            public void run() {
                int numRetries = 25;
                if (!imageManagerHasStorage) {
                    showToast(getResources().getString(R.string.no_sd_card), Toast.LENGTH_LONG);
                    do {
                        --numRetries;
                        try {
                        Thread.sleep(200);
                        } catch (InterruptedException e) {
                            ;
                        }
                    } while (numRetries > 0 && !ImageManager.hasStorage());
                }
                final boolean imageManagerHasStorageAfterDelay = ImageManager.hasStorage();
                CacheService.computeDirtySets(Gallery.this);
                CacheService.startCache(Gallery.this, false);
                final boolean isCacheReady = CacheService.isCacheReady(false);

                // Creating the DataSource objects.
                final PicasaDataSource picasaDataSource = new PicasaDataSource(Gallery.this);
                final LocalDataSource localDataSource = new LocalDataSource(Gallery.this);
                final ConcatenatedDataSource combinedDataSource = new ConcatenatedDataSource(localDataSource, picasaDataSource);

                // Depending upon the intent, we assign the right dataSource.
                if (!isPickIntent() && !isViewIntent()) {
                    if (imageManagerHasStorageAfterDelay) {
                        mGridLayer.setDataSource(combinedDataSource);
                    } else {
                        mGridLayer.setDataSource(picasaDataSource);
                    }
                    if (!isCacheReady && imageManagerHasStorageAfterDelay) {
                        showToast(getResources().getString(R.string.loading_new), Toast.LENGTH_LONG);
                    }
                } else if (!isViewIntent()) {
                    final Intent intent = getIntent();
                    if (intent != null) {
                        final String type = intent.resolveType(Gallery.this);
                        boolean includeImages = isImageType(type);
                        boolean includeVideos = isVideoType(type);
                        ((LocalDataSource) localDataSource).setMimeFilter(!includeImages, !includeVideos);
                        if (includeImages) {
                            if (imageManagerHasStorageAfterDelay) {
                                mGridLayer.setDataSource(combinedDataSource);
                            } else {
                                mGridLayer.setDataSource(picasaDataSource);
                            }
                        } else {
                            mGridLayer.setDataSource(localDataSource);
                        }
                        mGridLayer.setPickIntent(true);
                        if (!imageManagerHasStorageAfterDelay) {
                            showToast(getResources().getString(R.string.no_sd_card), Toast.LENGTH_LONG);
                        } else {
                            showToast(getResources().getString(R.string.pick_prompt), Toast.LENGTH_LONG);
                        }
                    }
                } else {
                    // View intent for images.
                    Uri uri = getIntent().getData();
                    boolean slideshow = getIntent().getBooleanExtra("slideshow", false);
                    final SingleDataSource singleDataSource = new SingleDataSource(Gallery.this, uri.toString(), slideshow);
                    final ConcatenatedDataSource singleCombinedDataSource = new ConcatenatedDataSource(singleDataSource, picasaDataSource);
                    mGridLayer.setDataSource(singleCombinedDataSource);
                    mGridLayer.setViewIntent(true, Utils.getBucketNameFromUri(uri));
                    if (singleDataSource.isSingleImage()) {
                        mGridLayer.setSingleImage(false);
                    } else if (slideshow) {
                        mGridLayer.setSingleImage(true);
                        mGridLayer.startSlideshow();
                    }
                }
            }
        };
        t.start();
        //We record the set of enabled accounts for picasa.
        mAccountsEnabled = PicasaDataSource.getAccountStatus(this);
        Log.i(TAG, "onCreate");
    }
    
    private void showToast(final String string, final int duration) {
        mHandler.post(new Runnable() {
            public void run() {
                Toast.makeText(Gallery.this, string, duration).show();
            }
        });
    }

    public ReverseGeocoder getReverseGeocoder() {
        return mReverseGeocoder;
    }

    public Handler getHandler() {
        return mHandler;
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
        if (ImageManager.hasStorage()) {
            CacheService.computeDirtySets(this);
            CacheService.startCache(this, false);
        }
        if (mRenderView != null) {
            mRenderView.onResume();
        }
        if (mPause) {
            // We check to see if the authenticated accounts have changed, and
            // if so, reload the datasource.
            HashMap<String, Boolean> accountsEnabled = PicasaDataSource.getAccountStatus(this);
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
            mPause = false;
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
        mPause = true;
    }

    public boolean isPaused() {
        return mPause;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGridLayer != null)
            mGridLayer.stop();
        if (mReverseGeocoder != null) {
            mReverseGeocoder.flushCache();
        }
        LocalDataSource.sThumbnailCache.flush();
        LocalDataSource.sThumbnailCacheVideo.flush();
        PicasaDataSource.sThumbnailCache.flush();
        CacheService.startCache(this, true);
    }

    @Override
    public void onDestroy() {
        // Force GLThread to exit.
        setContentView(R.layout.main);
        if (mGridLayer != null) {
            DataSource dataSource = mGridLayer.getDataSource();
            if (dataSource != null) {
                dataSource.shutdown();
            }
            mGridLayer.shutdown();
        }
        if (mReverseGeocoder != null)
            mReverseGeocoder.shutdown();
        if (mRenderView != null) {
            mRenderView.shutdown();
            mRenderView = null;
        }
        mGridLayer = null;
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

    private boolean isImageType(String type) {
        return type.equals("vnd.android.cursor.dir/image") || type.equals("image/*");
    }

    private boolean isVideoType(String type) {
        return type.equals("vnd.android.cursor.dir/video") || type.equals("video/*");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case CROP_MSG: {
            if (resultCode == RESULT_OK) {
                setResult(resultCode, data);
                finish();
            }
            break;
        }
        case CROP_MSG_INTERNAL: {
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

    public void launchCropperOrFinish(final MediaItem item) {
        final Bundle myExtras = getIntent().getExtras();
        String cropValue = myExtras != null ? myExtras.getString("crop") : null;
        final String contentUri = item.mContentUri;
        if (cropValue != null) {
            Bundle newExtras = new Bundle();
            if (cropValue.equals("circle")) {
                newExtras.putString("circleCrop", "true");
            }
            Intent cropIntent = new Intent();
            cropIntent.setData(Uri.parse(contentUri));
            cropIntent.setClass(this, CropImage.class);
            cropIntent.putExtras(newExtras);
            // Pass through any extras that were passed in.
            cropIntent.putExtras(myExtras);
            startActivityForResult(cropIntent, CROP_MSG);
        } else {
            if (contentUri.startsWith("http://")) {
                // This is a http uri, we must save it locally first and
                // generate a content uri from it.
                final ProgressDialog dialog = ProgressDialog.show(this, this.getResources().getString(R.string.initializing),
                        getResources().getString(R.string.running_face_detection), true, false);
                if (contentUri != null) {
                    MediaScannerConnection.MediaScannerConnectionClient client = new MediaScannerConnection.MediaScannerConnectionClient() {
                        public void onMediaScannerConnected() {
                            if (mConnection != null) {
                                try {
                                    final String path = UriTexture.writeHttpDataInDirectory(Gallery.this, contentUri,
                                            LocalDataSource.DOWNLOAD_BUCKET_NAME);
                                    if (path != null) {
                                        mConnection.scanFile(path, item.mMimeType);
                                    } else {
                                        shutdown("");
                                    }
                                } catch (Exception e) {
                                    shutdown("");
                                }
                            }
                        }

                        public void onScanCompleted(String path, Uri uri) {
                            shutdown(uri.toString());
                        }

                        public void shutdown(String uri) {
                            dialog.dismiss();
                            performReturn(myExtras, uri.toString());
                            if (mConnection != null) {
                                mConnection.disconnect();
                            }
                        }
                    };
                    MediaScannerConnection connection = new MediaScannerConnection(Gallery.this, client);
                    connection.connect();
                    mConnection = connection;
                }
            } else {
                performReturn(myExtras, contentUri);
            }
        }
    }

    private void performReturn(Bundle myExtras, String contentUri) {
        Intent result = new Intent(null, Uri.parse(contentUri));
        if (myExtras != null && myExtras.getBoolean("return-data")) {
            // The size of a transaction should be below 100K.
            Bitmap bitmap = null;
            try {
                bitmap = UriTexture.createFromUri(this, contentUri, 1024, 1024, 0, null);
            } catch (IOException e) {
                ;
            } catch (URISyntaxException e) {
                ;
            }
            if (bitmap != null) {
                result.putExtra("data", bitmap);
            }
        }
        setResult(RESULT_OK, result);
        finish();
    }

    public void refreshUIForSet(MediaSet set) {
        if (mGridLayer != null) {
            final MediaFeed feed = mGridLayer.getFeed();
            if (feed != null) {
                final MediaSet currentSet = feed.getMediaSet(set.mId);
                if (currentSet != null) {
                    // We need to refresh the UI with this set if the number of items have changed
                    if (currentSet.getNumItems() != set.getNumItems() || currentSet.mMaxAddedTimestamp != set.mMaxAddedTimestamp) {
                        final MediaSet newSet = feed.replaceMediaSet(set.mId, currentSet.mDataSource);
                        newSet.mName = currentSet.mName;
                        newSet.generateTitle(true);
                    }
                }
            }
        }
    }
}
