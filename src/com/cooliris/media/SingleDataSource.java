package com.cooliris.media;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

import com.cooliris.cache.CacheService;

// TODO: Merge SingleDataSource and LocalDataSource into one type if possible

public class SingleDataSource implements DataSource {
    private static final String TAG = "SingleDataSource";
    public final String mUri;
    public final String mBucketId;
    public boolean mDone;
    public final boolean mSlideshow;
    public final boolean mSingleUri;
    public final boolean mAllItems;
    public final DiskCache mDiskCache;
    private Context mContext;

    public SingleDataSource(final Context context, final String uri, final boolean slideshow) {
        this.mUri = uri;
        mContext = context;
        String bucketId = Uri.parse(uri).getQueryParameter("bucketId");
        if (bucketId != null && bucketId.length() > 0) {
            mBucketId = bucketId;
        } else {
            mBucketId = null;
        }
        if (mBucketId == null) {
            if (uri.equals(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())) {
                mAllItems = true;
            } else {
                mAllItems = false;
            }
        } else {
            mAllItems = false;
        }
        this.mSlideshow = slideshow;
        mSingleUri = isSingleImageMode(uri) && mBucketId == null;
        mDone = false;
        mDiskCache = mUri.startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString()) || mUri.startsWith("file://") ? LocalDataSource.sThumbnailCache
                : null;
    }

    public void shutdown() {

    }

    public boolean isSingleImage() {
        return mSingleUri;
    }

    private static boolean isSingleImageMode(String uriString) {
        return !uriString.equals(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())
                && !uriString.equals(MediaStore.Images.Media.INTERNAL_CONTENT_URI.toString());
    }

    public DiskCache getThumbnailCache() {
        return mDiskCache;
    }

    public void loadItemsForSet(MediaFeed feed, MediaSet parentSet, int rangeStart, int rangeEnd) {
        if (parentSet.mNumItemsLoaded > 0 && mDone) {
            return;
        }
        if (mSingleUri && !mDone) {
            MediaItem item = new MediaItem();
            item.mId = 0;
            item.mFilePath = "";
            item.setMediaType((isImage(mUri)) ? MediaItem.MEDIA_TYPE_IMAGE : MediaItem.MEDIA_TYPE_VIDEO);
            if (mUri.startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())) {
                MediaItem newItem = LocalDataSource.createMediaItemFromUri(mContext, Uri.parse(mUri));
                if (newItem != null) {
                    item = newItem;
                    String fileUri = new File(item.mFilePath).toURI().toString();
                    parentSet.mName = Utils.getBucketNameFromUri(Uri.parse(fileUri));
                    parentSet.mId = parseBucketIdFromFileUri(fileUri);
                    parentSet.generateTitle(true);
                }
            } else if (mUri.startsWith("file://")) {
                MediaItem newItem = null;
                int numRetries = 15;
                do {
                    newItem = LocalDataSource.createMediaItemFromFileUri(mContext, mUri);
                    if (newItem == null) {
                        --numRetries;
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            ;
                        }
                    }
                } while (newItem == null && numRetries >= 0);
                if (newItem != null) {
                    item = newItem;
                } else {
                    item.mContentUri = mUri;
                    item.mThumbnailUri = mUri;
                    item.mScreennailUri = mUri;
                    feed.setSingleImageMode(true);
                }
            } else {
                item.mContentUri = mUri;
                item.mThumbnailUri = mUri;
                item.mScreennailUri = mUri;
                feed.setSingleImageMode(true);
            }
            if (item != null) {
                feed.addItemToMediaSet(item, parentSet);
                // Parse EXIF orientation if a local file.
                if (mUri.startsWith("file://")) {
                    try {
                        ExifInterface exif = new ExifInterface(Uri.parse(mUri).getPath());
                        item.mRotation = Shared.exifOrientationToDegrees(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL));
                    } catch (IOException e) {
                        Log.i(TAG, "Error reading Exif information, probably not a jpeg.");
                    }
                }
                // Try and get the date taken for this item.
                long dateTaken = CacheService.fetchDateTaken(item);
                if (dateTaken != -1L) {
                    item.mDateTakenInMs = dateTaken;
                }
                CacheService.loadMediaItemsIntoMediaFeed(feed, parentSet, rangeStart, rangeEnd, true, false);
                ArrayList<MediaItem> items = parentSet.getItems();
                int numItems = items.size();
                if (numItems == 1 && parentSet.mNumItemsLoaded > 1) {
                    parentSet.mNumItemsLoaded = 1;
                }
                for (int i = 1; i < numItems; ++i) {
                    MediaItem thisItem = items.get(i);
                    try {
                    String filePath = Uri.fromFile(new File(thisItem.mFilePath)).toString();
                    if (item.mId == thisItem.mId
                            || ((item.mContentUri != null && thisItem.mContentUri != null) && (item.mContentUri
                                    .equals(thisItem.mContentUri) || item.mContentUri.equals(filePath)))) {
                        items.remove(thisItem);
                        --parentSet.mNumItemsLoaded;
                        break;
                    }
                    } catch (Exception e) {
                        // NullPointerException at java.io.File.fixSlashes(File.java:267)
                        continue;
                    }
                }
            }
            parentSet.updateNumExpectedItems();
            parentSet.generateTitle(true);
        } else if (mUri.equals(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())) {
            final Uri uriImages = Images.Media.EXTERNAL_CONTENT_URI;
            final ContentResolver cr = mContext.getContentResolver();
            String where = null;
            try {
                Cursor cursor = cr.query(uriImages, CacheService.PROJECTION_IMAGES, where, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    parentSet.setNumExpectedItems(cursor.getCount());
                    do {
                        if (Thread.interrupted()) {
                            return;
                        }
                        final MediaItem item = new MediaItem();
                        CacheService.populateMediaItemFromCursor(item, cr, cursor, CacheService.BASE_CONTENT_STRING_IMAGES);
                        feed.addItemToMediaSet(item, parentSet);
                    } while (cursor.moveToNext());
                    if (cursor != null) {
                        cursor.close();
                        cursor = null;
                    }
                    parentSet.updateNumExpectedItems();
                    parentSet.generateTitle(true);
                }
            } catch (Exception e) {
                // If the database operation failed for any reason.
                ;
            }
        } else {
            CacheService.loadMediaItemsIntoMediaFeed(feed, parentSet, rangeStart, rangeEnd, true, true);
        }
        mDone = true;
    }

    public static long parseBucketIdFromFileUri(String uriString) {
        // This is a local folder.
        final Uri uri = Uri.parse(uriString);
        final List<String> paths = uri.getPathSegments();
        final int numPaths = paths.size() - 1;
        StringBuffer pathBuilder = new StringBuffer(Environment.getExternalStorageDirectory().toString());
        if (numPaths > 1)
            pathBuilder.append("/");
        for (int i = 0; i < numPaths; ++i) {
            String path = paths.get(i);
            if (!"file".equals(path) && !"sdcard".equals(path)) {
                pathBuilder.append(path);
                if (i != numPaths - 1) {
                    pathBuilder.append("/");
                }
            }
        }
        return LocalDataSource.getBucketId(pathBuilder.toString());
    }

    private static boolean isImage(String uriString) {
        return !uriString.startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString());
    }

    private static long parseIdFromContentUri(String uri) {
        try {
            long id = ContentUris.parseId(Uri.parse(uri));
            return id;
        } catch (Exception e) {
            return 0;
        }
    }

    public void loadMediaSets(MediaFeed feed) {
        MediaSet set = null; // Dummy set.
        boolean loadOtherSets = true;
        if (mSingleUri) {
            String name = Utils.getBucketNameFromUri(Uri.parse(mUri));
            long id = getBucketId(mUri);
            set = feed.addMediaSet(id, this);
            set.mName = name;
            set.setNumExpectedItems(2);
            set.generateTitle(true);
            set.mPicasaAlbumId = Shared.INVALID;
            if (this.getThumbnailCache() != LocalDataSource.sThumbnailCache) {
                loadOtherSets = false;
            }
        } else if (mBucketId == null) {
            // All the buckets.
            set = feed.addMediaSet(0, this); // Create dummy set.
            set.mName = Utils.getBucketNameFromUri(Uri.parse(mUri));
            set.mId = LocalDataSource.getBucketId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/" + set.mName);
            set.setNumExpectedItems(1);
            set.generateTitle(true);
            set.mPicasaAlbumId = Shared.INVALID;
        } else {
            CacheService.loadMediaSet(feed, this, Long.parseLong(mBucketId));
            ArrayList<MediaSet> sets = feed.getMediaSets();
            if (sets.size() > 0)
                set = sets.get(0);
        }
        // We also load the other MediaSets
        if (!mAllItems && set != null && loadOtherSets) {
            if (!CacheService.isPresentInCache(set.mId)) {
                CacheService.markDirty(mContext);
            }
            CacheService.loadMediaSets(feed, this, true, false);
        }
    }

    private long getBucketId(String uriString) {
        if (uriString.startsWith("content://.")) {
            return parseIdFromContentUri(uriString);
        } else {
            return parseBucketIdFromFileUri(uriString);
        }
    }

    public boolean performOperation(int operation, ArrayList<MediaBucket> mediaBuckets, Object data) {
        int numBuckets = mediaBuckets.size();
        ContentResolver cr = mContext.getContentResolver();
        switch (operation) {
        case MediaFeed.OPERATION_DELETE:
            // TODO: Refactor this against LocalDataSource.performOperation.
            for (int i = 0; i < numBuckets; ++i) {
                MediaBucket bucket = mediaBuckets.get(i);
                MediaSet set = bucket.mediaSet;
                ArrayList<MediaItem> items = bucket.mediaItems;
                if (set != null && items == null) {
                    // TODO bulk delete
                    // remove the entire bucket
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
                    for (int j = 0; j < numItems; ++j) {
                        MediaItem item = items.get(j);
                        cr.delete(Uri.parse(item.mContentUri), null, null);
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
        try {
            int currentOrientation = (int) item.mRotation;
            angleToRotate += currentOrientation;
            float rotation = Shared.normalizePositive(angleToRotate);

            // Update the file EXIF information.
            Uri uri = Uri.parse(item.mContentUri);
            String uriScheme = uri.getScheme();
            if (uriScheme.equals("file")) {
                ExifInterface exif = new ExifInterface(uri.getPath());
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(Shared.degreesToExifOrientation(rotation)));
                exif.saveAttributes();
            }

            // Update the object representation of the item.
            item.mRotation = rotation;
        } catch (Exception e) {
        }
    }

}
