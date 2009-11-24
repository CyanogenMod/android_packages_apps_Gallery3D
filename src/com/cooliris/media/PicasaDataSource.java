package com.cooliris.media;

import java.util.ArrayList;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.cooliris.picasa.AlbumEntry;
import com.cooliris.picasa.Entry;
import com.cooliris.picasa.EntrySchema;
import com.cooliris.picasa.PicasaApi;
import com.cooliris.picasa.PicasaContentProvider;
import com.cooliris.picasa.PicasaService;

public final class PicasaDataSource implements DataSource {
    private static final String TAG = "PicasaDataSource";
    public static final DiskCache sThumbnailCache = new DiskCache("picasa-thumbs");
    private static final String DEFAULT_BUCKET_SORT_ORDER = AlbumEntry.Columns.USER + ", " + AlbumEntry.Columns.DATE_PUBLISHED
            + " DESC";

    private ContentProviderClient mProviderClient;
    private final Context mContext;
    private ContentObserver mAlbumObserver;

    public PicasaDataSource(Context context) {
        mContext = context;
    }

    public void loadMediaSets(final MediaFeed feed) {
        if (mProviderClient == null) {
            mProviderClient = mContext.getContentResolver().acquireContentProviderClient(PicasaContentProvider.AUTHORITY);
        }
        // Force permission dialog to be displayed if necessary. TODO: remove this after signed by Google.
        PicasaApi.getAccounts(mContext);
        
        // Ensure that users are up to date. TODO: also listen for accounts changed broadcast.
        PicasaService.requestSync(mContext, PicasaService.TYPE_USERS_ALBUMS, 0);
        Handler handler = ((Gallery)mContext).getHandler();
        ContentObserver albumObserver = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                loadMediaSetsIntoFeed(feed, true);
            }
        };
        mAlbumObserver = albumObserver;
        loadMediaSetsIntoFeed(feed, true);
        
        // Start listening.
        ContentResolver cr = mContext.getContentResolver();
        cr.registerContentObserver(PicasaContentProvider.ALBUMS_URI, false, mAlbumObserver);
        cr.registerContentObserver(PicasaContentProvider.PHOTOS_URI, false, mAlbumObserver);
    }
 
    public void shutdown() {
        if (mAlbumObserver != null) {
            ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(mAlbumObserver);
        }
    }

    public void loadItemsForSet(final MediaFeed feed, final MediaSet parentSet, int rangeStart, int rangeEnd) {
        if (parentSet == null) {
            return;
        } else {
            // Return a list of items within an album.
            addItemsToFeed(feed, parentSet, rangeStart, rangeEnd);
        }
    }

    protected void loadMediaSetsIntoFeed(final MediaFeed feed, boolean sync) {
        ContentProviderClient client = mProviderClient;
        if (client == null)
            return;
        try {
            EntrySchema albumSchema = AlbumEntry.SCHEMA;
            Cursor cursor = client.query(PicasaContentProvider.ALBUMS_URI, albumSchema.getProjection(), null, null,
                    DEFAULT_BUCKET_SORT_ORDER);
            AlbumEntry album = new AlbumEntry();
            MediaSet mediaSet;
            if (cursor.moveToFirst()) {
                int numAlbums = cursor.getCount();
                ArrayList<MediaSet> picasaSets = new ArrayList<MediaSet>(numAlbums);
                do {
                    albumSchema.cursorToObject(cursor, album);
                    mediaSet = feed.getMediaSet(album.id);
                    if (mediaSet == null) {
                        mediaSet = feed.addMediaSet(album.id, this);
                        mediaSet.mName = album.title;
                        mediaSet.mEditUri = album.editUri;
                        mediaSet.generateTitle(true);
                    } else {
                        mediaSet.setNumExpectedItems(album.numPhotos);
                    }
                    mediaSet.mPicasaAlbumId = album.id;
                    mediaSet.mSyncPending = album.photosDirty;
                    picasaSets.add(mediaSet);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (RemoteException e) {
            Log.e(TAG, "Error occurred loading albums");
        }
    }

    private void addItemsToFeed(MediaFeed feed, MediaSet set, int start, int end) {
        ContentProviderClient client = mProviderClient;
        Cursor cursor = null;
        try {
            // Query photos in the album.
            EntrySchema photosSchema = PhotoProjection.SCHEMA;
            String whereInAlbum = "album_id = " + Long.toString(set.mId);
            cursor = client.query(PicasaContentProvider.PHOTOS_URI, photosSchema.getProjection(), whereInAlbum, null, null);
            PhotoProjection photo = new PhotoProjection();
            int count = cursor.getCount();
            if (count < end) {
                end = count;
            }
            set.setNumExpectedItems(count);
            set.generateTitle(true);
            // Move to the next unread item.
            int newIndex = start + 1;
            if (newIndex > count || !cursor.move(newIndex)) {
                end = 0;
                cursor.close();
                set.updateNumExpectedItems();
                set.generateTitle(true);
                return;
            }
            if (set.mNumItemsLoaded == 0) {
                photosSchema.cursorToObject(cursor, photo);
                set.mMinTimestamp = photo.dateTaken;
                cursor.moveToLast();
                photosSchema.cursorToObject(cursor, photo);
                set.mMinTimestamp = photo.dateTaken;
                cursor.moveToFirst();
            }
            for (int i = 0; i < end; ++i) {
                photosSchema.cursorToObject(cursor, photo);
                MediaItem item = new MediaItem();
                item.mId = photo.id;
                item.mEditUri = photo.editUri;
                item.mMimeType = photo.contentType;
                item.mDateTakenInMs = photo.dateTaken;
                item.mLatitude = photo.latitude;
                item.mLongitude = photo.longitude;
                item.mThumbnailUri = photo.thumbnailUrl;
                item.mScreennailUri = photo.screennailUrl;
                item.mContentUri = photo.contentUrl;
                item.mCaption = photo.title;
                item.mWeblink = photo.htmlPageUrl;
                item.mDescription = photo.summary;
                item.mFilePath = item.mContentUri;
                feed.addItemToMediaSet(item, set);
                if (!cursor.moveToNext()) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error occurred loading photos for album " + set.mId);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void prime(final MediaItem item) {
    }

    public boolean performOperation(final int operation, final ArrayList<MediaBucket> mediaBuckets, final Object data) {
        try {
            if (operation == MediaFeed.OPERATION_DELETE) {
                ContentProviderClient client = mProviderClient;
                for (int i = 0, numBuckets = mediaBuckets.size(); i != numBuckets; ++i) {
                    MediaBucket bucket = mediaBuckets.get(i);
                    ArrayList<MediaItem> items = bucket.mediaItems;
                    if (items == null) {
                        // Delete an album.
                        String albumUri = PicasaContentProvider.ALBUMS_URI + "/" + bucket.mediaSet.mId;
                        client.delete(Uri.parse(albumUri), null, null);
                    } else {
                        // Delete a set of photos.
                        for (int j = 0, numItems = items.size(); j != numItems; ++j) {
                            MediaItem item = items.get(j);
                            if (item != null) {
                                Log.i(TAG, "Deleting picasa photo " + item.mContentUri);
                                String itemUri = PicasaContentProvider.PHOTOS_URI + "/" + item.mId; 
                                client.delete(Uri.parse(itemUri), null, null);
                            }
                        }
                    }
                }
            }
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public DiskCache getThumbnailCache() {
        return sThumbnailCache;
    }

    /**
     * The projection of PhotoEntry needed by the data source.
     */
    private static final class PhotoProjection extends Entry {
        public static final EntrySchema SCHEMA = new EntrySchema(PhotoProjection.class);
        @Column("edit_uri")
        public String editUri;
        @Column("title")
        public String title;
        @Column("summary")
        public String summary;
        @Column("date_taken")
        public long dateTaken;
        @Column("latitude")
        public double latitude;
        @Column("longitude")
        public double longitude;
        @Column("thumbnail_url")
        public String thumbnailUrl;
        @Column("screennail_url")
        public String screennailUrl;
        @Column("content_url")
        public String contentUrl;
        @Column("content_type")
        public String contentType;
        @Column("html_page_url")
        public String htmlPageUrl;
    }
}
