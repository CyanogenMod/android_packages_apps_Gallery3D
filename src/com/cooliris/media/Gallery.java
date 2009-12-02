package com.cooliris.media;

import java.io.IOException;
import java.net.URISyntaxException;
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
    public static boolean NEEDS_REFRESH = true;

    private static final String TAG = "Gallery";
    public static final int CROP_MSG_INTERNAL = 100;
    private static final int CROP_MSG = 10;
    private RenderView mRenderView = null;
    private GridLayer mGridLayer;
    private final Handler mHandler = new Handler();
    private ReverseGeocoder mReverseGeocoder;
    private boolean mPause;
    private MediaScannerConnection mConnection;
    private WakeLock mWakeLock;
    private static final boolean TEST_WALLPAPER = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final boolean imageManagerHasStorage = ImageManager.quickHasStorage();
        if (TEST_WALLPAPER || (isViewIntent() && getIntent().getData().equals(Images.Media.EXTERNAL_CONTENT_URI))) {
            if (!imageManagerHasStorage) {
                Toast.makeText(this, getResources().getString(R.string.no_sd_card), Toast.LENGTH_LONG).show();
                finish();
            } else {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "GridView.Slideshow.All");
                mWakeLock.acquire();
                Slideshow slideshow = new Slideshow(this);
                slideshow.setDataSource(new RandomDataSource());
                setContentView(slideshow);
            }
            return;
        }
        boolean isCacheReady = CacheService.isCacheReady(false);
        CacheService.startCache(this, false);
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
        if (!isPickIntent() && !isViewIntent()) {
            PicasaDataSource picasaDataSource = new PicasaDataSource(this);
            if (imageManagerHasStorage) {
                LocalDataSource localDataSource = new LocalDataSource(this);
                ConcatenatedDataSource combinedDataSource = new ConcatenatedDataSource(localDataSource, picasaDataSource);
                mGridLayer.setDataSource(combinedDataSource);
            } else {
                mGridLayer.setDataSource(picasaDataSource);
            }
            if (!imageManagerHasStorage) {
                Toast.makeText(this, getResources().getString(R.string.no_sd_card), Toast.LENGTH_LONG).show();
            } else {
                if (!isCacheReady) {
                    Toast.makeText(this, getResources().getString(R.string.loading_new), Toast.LENGTH_LONG).show();
                } else {
                    // Toast.makeText(this, getResources().getString(R.string.initializing), Toast.LENGTH_SHORT).show();
                }
            }
        } else if (!isViewIntent()) {
            Intent intent = getIntent();
            if (intent != null) {
                String type = intent.resolveType(this);
                boolean includeImages = isImageType(type);
                boolean includeVideos = isVideoType(type);
                LocalDataSource localDataSource = new LocalDataSource(this);
                ((LocalDataSource) localDataSource).setMimeFilter(!includeImages, !includeVideos);
                if (includeImages) {
                    PicasaDataSource picasaDataSource = new PicasaDataSource(this);
                    if (imageManagerHasStorage) {
                        ConcatenatedDataSource combinedDataSource = new ConcatenatedDataSource(localDataSource, picasaDataSource);
                        mGridLayer.setDataSource(combinedDataSource);
                    } else {
                        mGridLayer.setDataSource(picasaDataSource);
                    }
                } else {
                    mGridLayer.setDataSource(localDataSource);
                }
                mGridLayer.setPickIntent(true);
                if (!imageManagerHasStorage) {
                    Toast.makeText(this, getResources().getString(R.string.no_sd_card), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, getResources().getString(R.string.pick_prompt), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            // View intent for images.
            Uri uri = getIntent().getData();
            boolean slideshow = getIntent().getBooleanExtra("slideshow", false);
            SingleDataSource localDataSource = new SingleDataSource(this, uri.toString(), slideshow);
            PicasaDataSource picasaDataSource = new PicasaDataSource(this);
            ConcatenatedDataSource combinedDataSource = new ConcatenatedDataSource(localDataSource, picasaDataSource);
            mGridLayer.setDataSource(combinedDataSource);
            mGridLayer.setViewIntent(true, Utils.getBucketNameFromUri(uri));
            if (SingleDataSource.isSingleImageMode(uri.toString())) {
                mGridLayer.setSingleImage(false);
            } else if (slideshow) {
                mGridLayer.setSingleImage(true);
                mGridLayer.startSlideshow();
            }
            // Toast.makeText(this, getResources().getString(R.string.initializing), Toast.LENGTH_SHORT).show();
        }
        Log.i(TAG, "onCreate");
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
        if (mRenderView != null)
            mRenderView.onResume();
        if (NEEDS_REFRESH) {
            NEEDS_REFRESH = false;
            CacheService.markDirtyImmediate(LocalDataSource.CAMERA_BUCKET_ID);
            CacheService.markDirtyImmediate(LocalDataSource.DOWNLOAD_BUCKET_ID);
            CacheService.startCache(this, false);
        }
        mPause = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRenderView != null)
            mRenderView.onPause();
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
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mWakeLock = null;
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
            // We cropped an image, we must try to set the focus of the camera to that image.
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
                // This is a http uri, we must save it locally first and generate a content uri from it.
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
}
