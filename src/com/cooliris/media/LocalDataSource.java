package com.cooliris.media;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

import com.cooliris.cache.CacheService;

public final class LocalDataSource implements DataSource {
    private static final String TAG = "LocalDataSource";

    public static final DiskCache sThumbnailCache = new DiskCache("local-image-thumbs");
    public static final DiskCache sThumbnailCacheVideo = new DiskCache("local-video-thumbs");

    public static final String CAMERA_STRING = "Camera";
    public static final String DOWNLOAD_STRING = "download";
    public static final String CAMERA_BUCKET_NAME = Environment.getExternalStorageDirectory().toString() + "/DCIM/" + CAMERA_STRING;
    public static final String DOWNLOAD_BUCKET_NAME = Environment.getExternalStorageDirectory().toString() + "/" + DOWNLOAD_STRING;
    public static final int CAMERA_BUCKET_ID = getBucketId(CAMERA_BUCKET_NAME);
    public static final int DOWNLOAD_BUCKET_ID = getBucketId(DOWNLOAD_BUCKET_NAME);

    public static boolean sObserverActive = false;
    private boolean mDisableImages;
    private boolean mDisableVideos;

    /**
     * Matches code in MediaProvider.computeBucketValues. Should be a common
     * function.
     */
    public static int getBucketId(String path) {
        return (path.toLowerCase().hashCode());
    }

    private Context mContext;
    private ContentObserver mObserver;

    public LocalDataSource(Context context) {
        mContext = context;
    }

    public void setMimeFilter(boolean disableImages, boolean disableVideos) {
        mDisableImages = disableImages;
        mDisableVideos = disableVideos;
    }

    public void loadMediaSets(final MediaFeed feed) {
        if (mContext == null) {
            return;
        }
        stopListeners();
        CacheService.loadMediaSets(feed, this, !mDisableImages, !mDisableVideos);
        Handler handler = ((Gallery) mContext).getHandler();
        ContentObserver observer = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                final boolean isPaused = ((Gallery) mContext).isPaused();
                if (isPaused) {
                    MediaSet mediaSet = feed.getCurrentSet();
                    if (mediaSet != null) {
                        CacheService.markDirtyImmediate(mediaSet.mId);
                        refreshUI(feed, mediaSet.mId);
                    }
                }
                CacheService.senseDirty(mContext, new CacheService.Observer() {
                    public void onChange(long[] ids) {
                        if (!isPaused)
                            return;
                        if (ids != null) {
                            int numLongs = ids.length;
                            for (int i = 0; i < numLongs; ++i) {
                                refreshUI(feed, ids[i]);
                            }
                        }
                    }
                });
            }
        };

        // Start listening.
        Uri uriImages = Images.Media.EXTERNAL_CONTENT_URI;
        Uri uriVideos = Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver cr = mContext.getContentResolver();
        mObserver = observer;
        cr.registerContentObserver(uriImages, true, observer);
        cr.registerContentObserver(uriVideos, true, observer);
        sObserverActive = true;
    }

    public void shutdown() {
        stopListeners();
    }

    private void stopListeners() {
        ContentResolver cr = mContext.getContentResolver();
        if (mObserver != null) {
            cr.unregisterContentObserver(mObserver);
            cr.unregisterContentObserver(mObserver);
        }
        sObserverActive = false;
    }

    protected void refreshUI(MediaFeed feed, long setIdToUse) {
        if (setIdToUse == Shared.INVALID) {
            return;
        }
        if (feed.getMediaSet(setIdToUse) == null) {
            MediaSet mediaSet = feed.addMediaSet(setIdToUse, this);
            if (setIdToUse == CAMERA_BUCKET_ID) {
                mediaSet.mName = CAMERA_STRING;
            } else if (setIdToUse == DOWNLOAD_BUCKET_ID) {
                mediaSet.mName = DOWNLOAD_STRING;
            }
            mediaSet.generateTitle(true);
        } else {
            MediaSet mediaSet = feed.replaceMediaSet(setIdToUse, this);
            Log.i(TAG, "Replacing mediaset " + mediaSet.mName + " id " + setIdToUse + " current Id " + mediaSet.mId);
            if (setIdToUse == CAMERA_BUCKET_ID) {
                mediaSet.mName = CAMERA_STRING;
            } else if (setIdToUse == DOWNLOAD_BUCKET_ID) {
                mediaSet.mName = DOWNLOAD_STRING;
            }
            mediaSet.generateTitle(true);
        }
    }

    public void loadItemsForSet(final MediaFeed feed, final MediaSet parentSet, int rangeStart, int rangeEnd) {
        // Quick load from the cache.
        if (mContext == null || parentSet == null) {
            return;
        }
        loadMediaItemsIntoMediaFeed(feed, parentSet, rangeStart, rangeEnd);
    }

    private void loadMediaItemsIntoMediaFeed(final MediaFeed mediaFeed, final MediaSet set, int rangeStart, int rangeEnd) {
        if (rangeEnd - rangeStart < 0) {
            return;
        }
        CacheService.loadMediaItemsIntoMediaFeed(mediaFeed, set, rangeStart, rangeEnd, !mDisableImages, !mDisableVideos);
        if (set.mId == CAMERA_BUCKET_ID) {
            mediaFeed.moveSetToFront(set);
        }
    }

    public boolean performOperation(final int operation, final ArrayList<MediaBucket> mediaBuckets, final Object data) {
        int numBuckets = mediaBuckets.size();
        ContentResolver cr = mContext.getContentResolver();
        switch (operation) {
        case MediaFeed.OPERATION_DELETE:
            for (int i = 0; i < numBuckets; ++i) {
                MediaBucket bucket = mediaBuckets.get(i);
                MediaSet set = bucket.mediaSet;
                ArrayList<MediaItem> items = bucket.mediaItems;
                if (set != null && items == null) {
                    // Remove the entire bucket.
                    final Uri uriImages = Images.Media.EXTERNAL_CONTENT_URI;
                    final Uri uriVideos = Video.Media.EXTERNAL_CONTENT_URI;
                    final String whereImages = Images.ImageColumns.BUCKET_ID + "=" + Long.toString(set.mId);
                    final String whereVideos = Video.VideoColumns.BUCKET_ID + "=" + Long.toString(set.mId);
                    cr.delete(uriImages, whereImages, null);
                    cr.delete(uriVideos, whereVideos, null);
                }
                if (set != null && items != null) {
                    // We need to remove these items from the set.
                    int numItems = items.size();
                    try {
                        for (int j = 0; j < numItems; ++j) {
                            MediaItem item = items.get(j);
                            cr.delete(Uri.parse(item.mContentUri), null, null);
                        }
                    } catch (Exception e) {
                        // If the database operation failed for any reason.
                        ;
                    }
                    set.updateNumExpectedItems();
                    set.generateTitle(true);
                }
            }
            break;
        case MediaFeed.OPERATION_ROTATE:
            for (int i = 0; i < numBuckets; ++i) {
                MediaBucket bucket = mediaBuckets.get(i);
                ArrayList<MediaItem> items = bucket.mediaItems;
                if (items == null) {
                    continue;
                }
                float angleToRotate = ((Float) data).floatValue();
                if (angleToRotate == 0) {
                    return true;
                }
                int numItems = items.size();
                for (int j = 0; j < numItems; ++j) {
                    rotateItem(items.get(j), angleToRotate);
                }
            }
            break;
        }
        return true;
    }

    private void rotateItem(final MediaItem item, float angleToRotate) {
        ContentResolver cr = mContext.getContentResolver();
        try {
            int currentOrientation = (int) item.mRotation;
            angleToRotate += currentOrientation;
            float rotation = Shared.normalizePositive(angleToRotate);
            String rotationString = Integer.toString((int) rotation);

            // Update the database entry.
            ContentValues values = new ContentValues();
            values.put(Images.ImageColumns.ORIENTATION, rotationString);
            try {
                cr.update(Uri.parse(item.mContentUri), values, null, null);
            } catch (Exception e) {
                // If the database operation fails for any reason.
                ;
            }

            // Update the file EXIF information.
            Uri uri = Uri.parse(item.mContentUri);
            String uriScheme = uri.getScheme();
            if (uriScheme.equals("file")) {
                ExifInterface exif = new ExifInterface(uri.getPath());
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(Shared.degreesToExifOrientation(rotation)));
                exif.saveAttributes();
            }

            // Invalidate the cache entry.
            CacheService.markDirty(mContext, item.mParentMediaSet.mId);

            // Update the object representation of the item.
            item.mRotation = rotation;
        } catch (Exception e) {
            // System.out.println("Apparently not a JPEG");
        }
    }

    public DiskCache getThumbnailCache() {
        return sThumbnailCache;
    }

    public static MediaItem createMediaItemFromUri(Context context, Uri target) {
        MediaItem item = null;
        long id = ContentUris.parseId(target);
        ContentResolver cr = context.getContentResolver();
        String whereClause = Images.ImageColumns._ID + "=" + Long.toString(id);
        try {
            Cursor cursor = cr.query(Images.Media.EXTERNAL_CONTENT_URI, CacheService.PROJECTION_IMAGES, whereClause, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    item = new MediaItem();
                    CacheService.populateMediaItemFromCursor(item, cr, cursor, Images.Media.EXTERNAL_CONTENT_URI.toString() + "/");
                }
                cursor.close();
                cursor = null;
            }
        } catch (Exception e) {
            // If the database operation failed for any reason.
            ;
        }
        return item;
    }

    public static MediaItem createMediaItemFromFileUri(Context context, String fileUri) {
        MediaItem item = null;
        String filepath = new File(URI.create(fileUri)).toString();
        ContentResolver cr = context.getContentResolver();
        long bucketId = SingleDataSource.parseBucketIdFromFileUri(fileUri);
        String whereClause = Images.ImageColumns.BUCKET_ID + "=" + bucketId + " AND " + Images.ImageColumns.DATA + "='" + filepath
                + "'";
        try {
            Cursor cursor = cr.query(Images.Media.EXTERNAL_CONTENT_URI, CacheService.PROJECTION_IMAGES, whereClause, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    item = new MediaItem();
                    CacheService.populateMediaItemFromCursor(item, cr, cursor, Images.Media.EXTERNAL_CONTENT_URI.toString() + "/");
                }
                cursor.close();
                cursor = null;
            }
        } catch (Exception e) {
            // If the database operation failed for any reason.
            ;
        }
        return item;
    }
}
