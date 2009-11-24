package com.cooliris.media;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;
import android.os.Process;

import com.cooliris.media.MediaClustering.Cluster;

public final class MediaFeed implements Runnable {
    public static final int OPERATION_DELETE = 0;
    public static final int OPERATION_ROTATE = 1;
    public static final int OPERATION_CROP = 2;

    private static final int NUM_ITEMS_LOOKAHEAD = 60;

    private IndexRange mVisibleRange = new IndexRange();
    private IndexRange mBufferedRange = new IndexRange();
    private ArrayList<MediaSet> mMediaSets = new ArrayList<MediaSet>();
    private Listener mListener;
    private DataSource mDataSource;
    private boolean mListenerNeedsUpdate = false;
    private MediaSet mSingleWrapper = new MediaSet();
    private boolean mInClusteringMode = false;
    private HashMap<MediaSet, MediaClustering> mClusterSets = new HashMap<MediaSet, MediaClustering>(32);
    private int mExpandedMediaSetIndex = Shared.INVALID;
    private MediaFilter mMediaFilter;
    private MediaSet mMediaFilteredSet;
    private Context mContext;
    private Thread mDataSourceThread = null;
    private Thread mAlbumSourceThread = null;
    private boolean mListenerNeedsLayout;
    private boolean mWaitingForMediaScanner;
    private boolean mSingleImageMode;
    private boolean mLoading;

    public interface Listener {
        public abstract void onFeedAboutToChange(MediaFeed feed);

        public abstract void onFeedChanged(MediaFeed feed, boolean needsLayout);
    }

    public MediaFeed(Context context, DataSource dataSource, Listener listener) {
        mContext = context;
        mListener = listener;
        mDataSource = dataSource;
        mSingleWrapper.setNumExpectedItems(1);
        mLoading = true;
    }
    
    public void shutdown() {
        if (mDataSourceThread != null) {
            mDataSource.shutdown();
            mDataSourceThread.interrupt();
            mDataSourceThread = null;
        }
        if (mAlbumSourceThread != null) {
            mAlbumSourceThread.interrupt();
            mAlbumSourceThread = null;
        }
        int numSets = mMediaSets.size();
        for (int i = 0; i < numSets; ++i) {
            MediaSet set = mMediaSets.get(i);
            set.clear();
        }
        synchronized (mMediaSets) {
            mMediaSets.clear();
        }
        int numClusters = mClusterSets.size();
        for (int i = 0; i < numClusters; ++i) {
            MediaClustering mc = mClusterSets.get(i);
            if (mc != null) {
                mc.clear();
            }
        }
        mClusterSets.clear();
        mListener = null;
        mDataSource = null;
        mSingleWrapper = null;
    }

    public void setVisibleRange(int begin, int end) {
        mVisibleRange.begin = begin;
        mVisibleRange.end = end;
        int numItems = 96;
        int numItemsBy2 = numItems / 2;
        int numItemsBy4 = numItems / 4;
        mBufferedRange.begin = (begin / numItemsBy2) * numItemsBy2 - numItemsBy4;
        mBufferedRange.end = mBufferedRange.begin + numItems;
    }

    public void setFilter(MediaFilter filter) {
        mMediaFilter = filter;
        mMediaFilteredSet = null;
        if (mListener != null) {
            mListener.onFeedAboutToChange(this);
        }
    }

    public void removeFilter() {
        mMediaFilter = null;
        mMediaFilteredSet = null;
        if (mListener != null) {
            mListener.onFeedAboutToChange(this);
            updateListener(true);
        }
    }

    public ArrayList<MediaSet> getMediaSets() {
        return mMediaSets;
    }

    public synchronized MediaSet getMediaSet(final long setId) {
        if (setId != Shared.INVALID) {
            int mMediaSetsSize = mMediaSets.size();
            for (int i = 0; i < mMediaSetsSize; i++) {
                if (mMediaSets.get(i).mId == setId) {
                    return mMediaSets.get(i);
                }
            }
        }
        return null;
    }

    public MediaSet getFilteredSet() {
        return mMediaFilteredSet;
    }

    public MediaSet addMediaSet(final long setId, DataSource dataSource) {
        MediaSet mediaSet = new MediaSet(dataSource);
        mediaSet.mId = setId;
        mMediaSets.add(mediaSet);
        return mediaSet;
    }

    public DataSource getDataSource() {
        return mDataSource;
    }

    public MediaClustering getClustering() {
        if (mExpandedMediaSetIndex != Shared.INVALID && mExpandedMediaSetIndex < mMediaSets.size()) {
            return mClusterSets.get(mMediaSets.get(mExpandedMediaSetIndex));
        }
        return null;
    }

    public ArrayList<Cluster> getClustersForSet(final MediaSet set) {
        ArrayList<Cluster> clusters = null;
        if (mClusterSets != null && mClusterSets.containsKey(set)) {
            MediaClustering mediaClustering = mClusterSets.get(set);
            if (mediaClustering != null) {
                clusters = mediaClustering.getClusters();
            }
        }
        return clusters;
    }

    public void addItemToMediaSet(MediaItem item, MediaSet mediaSet) {
        item.mParentMediaSet = mediaSet;
        mediaSet.addItem(item);
        synchronized (this) {
            if (item.mClusteringState == MediaItem.NOT_CLUSTERED) {
                MediaClustering clustering = mClusterSets.get(mediaSet);
                if (clustering == null) {
                    clustering = new MediaClustering(mediaSet.isPicassaAlbum());
                    mClusterSets.put(mediaSet, clustering);
                }
                clustering.setTimeRange(mediaSet.mMaxTimestamp - mediaSet.mMinTimestamp, mediaSet.getNumExpectedItems());
                clustering.addItemForClustering(item);
                item.mClusteringState = MediaItem.CLUSTERED;
            }
        }
    }

    public void performOperation(final int operation, final ArrayList<MediaBucket> mediaBuckets, final Object data) {
        int numBuckets = mediaBuckets.size();
        final ArrayList<MediaBucket> copyMediaBuckets = new ArrayList<MediaBucket>(numBuckets);
        for (int i = 0; i < numBuckets; ++i) {
            copyMediaBuckets.add(mediaBuckets.get(i));
        }
        if (operation == OPERATION_DELETE && mListener != null) {
            mListener.onFeedAboutToChange(this);

        }
        Thread operationThread = new Thread(new Runnable() {
            public void run() {
                ArrayList<MediaBucket> mediaBuckets = copyMediaBuckets;
                if (operation == OPERATION_DELETE) {
                    int numBuckets = mediaBuckets.size();
                    for (int i = 0; i < numBuckets; ++i) {
                        MediaBucket bucket = mediaBuckets.get(i);
                        MediaSet set = bucket.mediaSet;
                        ArrayList<MediaItem> items = bucket.mediaItems;
                        if (set != null && items == null) {
                            // Remove the entire bucket.
                            removeMediaSet(set);
                        } else if (set != null && items != null) {
                            // We need to remove these items from the set.
                            int numItems = items.size();
                            // We also need to delete the items from the cluster.
                            MediaClustering clustering = mClusterSets.get(set);
                            for (int j = 0; j < numItems; ++j) {
                                MediaItem item = items.get(j);
                                removeItemFromMediaSet(item, set);
                                if (clustering != null) {
                                    clustering.removeItemFromClustering(item);
                                }
                            }
                            set.updateNumExpectedItems();
                            set.generateTitle(true);
                        }
                    }
                    updateListener(true);
                    if (mDataSource != null) {
                        mDataSource.performOperation(OPERATION_DELETE, mediaBuckets, null);
                    }
                } else {
                    mDataSource.performOperation(operation, mediaBuckets, data);
                }
            }
        });
        operationThread.setName("Operation " + operation);
        operationThread.start();
    }

    public void removeMediaSet(MediaSet set) {
        mMediaSets.remove(set);
    }

    private void removeItemFromMediaSet(MediaItem item, MediaSet mediaSet) {
        mediaSet.removeItem(item);
        synchronized (this) {
            MediaClustering clustering = mClusterSets.get(mediaSet);
            if (clustering != null) {
                clustering.removeItemFromClustering(item);
            }
        }
    }

    public void updateListener(boolean needsLayout) {
        mListenerNeedsUpdate = true;
        mListenerNeedsLayout = needsLayout;
    }

    public int getNumSlots() {
        int currentMediaSetIndex = mExpandedMediaSetIndex;
        ArrayList<MediaSet> mediaSets = mMediaSets;
        int mediaSetsSize = mediaSets.size();

        if (mInClusteringMode == false) {
            if (currentMediaSetIndex == Shared.INVALID || currentMediaSetIndex >= mediaSetsSize) {
                return mediaSetsSize;
            } else {
                MediaSet setToUse = (mMediaFilteredSet == null) ? mediaSets.get(currentMediaSetIndex) : mMediaFilteredSet;
                return setToUse.getNumItems();
            }
        } else if (currentMediaSetIndex != Shared.INVALID && currentMediaSetIndex < mediaSetsSize) {
            MediaSet set = mediaSets.get(currentMediaSetIndex);
            MediaClustering clustering = mClusterSets.get(set);
            if (clustering != null) {
                return clustering.getClustersForDisplay().size();
            }
        }
        return 0;
    }

    public MediaSet getSetForSlot(int slotIndex) {
        if (slotIndex < 0) {
            return null;
        }

        ArrayList<MediaSet> mediaSets = mMediaSets;
        int mediaSetsSize = mediaSets.size();
        int currentMediaSetIndex = mExpandedMediaSetIndex;

        if (mInClusteringMode == false) {
            if (currentMediaSetIndex == Shared.INVALID || currentMediaSetIndex >= mediaSetsSize) {
                if (slotIndex >= mediaSetsSize) {
                    return null;
                }
                return mMediaSets.get(slotIndex);
            }
            if (mSingleWrapper.getNumItems() == 0) {
                mSingleWrapper.addItem(null);
            }
            MediaSet setToUse = (mMediaFilteredSet == null) ? mMediaSets.get(currentMediaSetIndex) : mMediaFilteredSet;
            ArrayList<MediaItem> items = setToUse.getItems();
            if (slotIndex >= setToUse.getNumItems()) {
                return null;
            }
            mSingleWrapper.getItems().set(0, items.get(slotIndex));
            return mSingleWrapper;
        } else if (currentMediaSetIndex != Shared.INVALID && currentMediaSetIndex < mediaSetsSize) {
            MediaSet set = mediaSets.get(currentMediaSetIndex);
            MediaClustering clustering = mClusterSets.get(set);
            if (clustering != null) {
                ArrayList<MediaClustering.Cluster> clusters = clustering.getClustersForDisplay();
                if (clusters.size() > slotIndex) {
                    MediaClustering.Cluster cluster = clusters.get(slotIndex);
                    cluster.generateCaption(mContext);
                    return cluster;
                }
            }
        }
        return null;
    }

    public boolean getWaitingForMediaScanner() {
        return mWaitingForMediaScanner;
    }
    
    public boolean isLoading() {
        return mLoading;
    }

    public void start() {
        final MediaFeed feed = this;
        mLoading = true;
        mAlbumSourceThread = new Thread(new Runnable() {
            public void run() {
                if (mContext == null)
                    return;
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                DataSource dataSource = mDataSource;
                // We must wait while the SD card is mounted or the MediaScanner is running.
                mWaitingForMediaScanner = false;
                while (ImageManager.isMediaScannerScanning(mContext.getContentResolver())) {
                    // MediaScanner is still running, wait
                    mWaitingForMediaScanner = true;
                    try {
                        if (mContext == null)
                            return;
                        showToast(mContext.getResources().getString(R.string.initializing), Toast.LENGTH_LONG);
                        Thread.sleep(6000);
                    } catch (InterruptedException e) {

                    }
                }
                if (mWaitingForMediaScanner) {
                    showToast(mContext.getResources().getString(R.string.loading_new), Toast.LENGTH_LONG);
                }
                mWaitingForMediaScanner = false;
                if (dataSource != null) {
                    dataSource.loadMediaSets(feed);
                }
                mLoading = false;
            }
        });
        mAlbumSourceThread.setName("MediaSets");
        mAlbumSourceThread.start();
        mDataSourceThread = new Thread(this);
        mDataSourceThread.setName("MediaFeed");
        mDataSourceThread.start();
    }

    private void showToast(final String string, final int duration) {
        showToast(string, duration, false);
    }

    private void showToast(final String string, final int duration, final boolean centered) {
        if (mContext != null && !((Gallery)mContext).isPaused()) {
            ((Gallery) mContext).getHandler().post(new Runnable() {
                public void run() {
                    if (mContext != null) {
                        Toast toast = Toast.makeText(mContext, string, duration);
                        if (centered) {
                            toast.setGravity(Gravity.CENTER, 0, 0);
                        }
                        toast.show();
                    }
                }
            });
        }
    }

    public void run() {
        DataSource dataSource = mDataSource;
        int sleepMs = 100;
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        if (dataSource != null) {
            while (Thread.interrupted() == false) {
                if (mListenerNeedsUpdate) {
                    mListenerNeedsUpdate = false;
                    if (mListener != null)
                        mListener.onFeedChanged(this, mListenerNeedsLayout);
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException e) {
                        return;
                    }
                } else {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                sleepMs = 100;
                ArrayList<MediaSet> mediaSets = mMediaSets;
                synchronized (mediaSets) {
                    int expandedSetIndex = mExpandedMediaSetIndex;
                    if (expandedSetIndex >= mMediaSets.size()) {
                        expandedSetIndex = Shared.INVALID;
                    }
                    if (expandedSetIndex == Shared.INVALID) {
                        // We purge the sets outside this visibleRange.
                        int numSets = mMediaSets.size();
                        IndexRange visibleRange = mVisibleRange;
                        IndexRange bufferedRange = mBufferedRange;
                        boolean scanMediaSets = true;
                        for (int i = 0; i < numSets; ++i) {
                            if (i >= visibleRange.begin && i <= visibleRange.end && scanMediaSets) {
                                MediaSet set = mediaSets.get(i);
                                int numItemsLoaded = set.mNumItemsLoaded;
                                if (!set.setContainsValidItems()) {
                                    mediaSets.remove(set);
                                    if (mListener != null) {
                                        mListener.onFeedChanged(this, false);
                                    }
                                    break;
                                }
                                if (numItemsLoaded < set.getNumExpectedItems() && numItemsLoaded < 8) {
                                    dataSource.loadItemsForSet(this, set, numItemsLoaded, 8);
                                    if (set.getNumExpectedItems() == 0) {
                                        mediaSets.remove(set);
                                        break;
                                    }
                                    if (mListener != null) {
                                        mListener.onFeedChanged(this, false);
                                    }
                                    sleepMs = 100;
                                    scanMediaSets = false;
                                }
                            }
                        }
                        numSets = mMediaSets.size();
                        for (int i = 0; i < numSets; ++i) {
                            MediaSet set = mediaSets.get(i);
                            if (i >= bufferedRange.begin && i <= bufferedRange.end) {
                                if (scanMediaSets) {
                                    int numItemsLoaded = set.mNumItemsLoaded;
                                    if (numItemsLoaded < set.getNumExpectedItems() && numItemsLoaded < 8) {
                                        dataSource.loadItemsForSet(this, set, numItemsLoaded, 8);
                                        if (set.getNumExpectedItems() == 0) {
                                            mediaSets.remove(set);
                                            break;
                                        }
                                        if (mListener != null) {
                                            mListener.onFeedChanged(this, false);
                                        }
                                        sleepMs = 100;
                                        scanMediaSets = false;
                                    }
                                }
                            } else if (i < bufferedRange.begin || i > bufferedRange.end){
                                // Purge this set to its initial status.
                                MediaClustering clustering = mClusterSets.get(set);
                                if (clustering != null) {
                                    clustering.clear();
                                    mClusterSets.remove(set);
                                }
                                if (set.getNumItems() != 0)
                                    set.clear();
                            }
                        }
                    }
                    if (expandedSetIndex != Shared.INVALID) {
                        int numSets = mMediaSets.size();
                        for (int i = 0; i < numSets; ++i) {
                            // Purge other sets.
                            if (i != expandedSetIndex) {
                                MediaSet set = mediaSets.get(i);
                                MediaClustering clustering = mClusterSets.get(set);
                                if (clustering != null) {
                                    clustering.clear();
                                    mClusterSets.remove(set);
                                }
                                if (set.getNumItems() != 0)
                                    set.clear();
                            }
                        }
                        // Make sure all the items are loaded for the album.
                        int numItemsLoaded = mediaSets.get(expandedSetIndex).mNumItemsLoaded;
                        int requestedItems = mVisibleRange.end;
                        // requestedItems count changes in clustering mode.
                        if (mInClusteringMode && mClusterSets != null) {
                            requestedItems = 0;
                            MediaClustering clustering = mClusterSets.get(mediaSets.get(expandedSetIndex));
                            ArrayList<Cluster> clusters = clustering.getClustersForDisplay();
                            int numClusters = clusters.size();
                            for (int i = 0; i < numClusters; i++) {
                                requestedItems += clusters.get(i).getNumExpectedItems();
                            }
                        }
                        MediaSet set = mediaSets.get(expandedSetIndex);
                        if (numItemsLoaded < set.getNumExpectedItems()) {
                            // TODO(Venkat) Why are we doing 4th param calculations like this?
                            dataSource.loadItemsForSet(this, set, numItemsLoaded, (requestedItems / NUM_ITEMS_LOOKAHEAD)
                                    * NUM_ITEMS_LOOKAHEAD + NUM_ITEMS_LOOKAHEAD);
                            if (set.getNumExpectedItems() == 0) {
                                mediaSets.remove(set);
                                mListener.onFeedChanged(this, false);
                            }
                            if (numItemsLoaded != set.mNumItemsLoaded && mListener != null) {
                                mListener.onFeedChanged(this, false);
                            }
                        }
                    }
                    MediaFilter filter = mMediaFilter;
                    if (filter != null && mMediaFilteredSet == null) {
                        if (expandedSetIndex != Shared.INVALID) {
                            MediaSet set = mediaSets.get(expandedSetIndex);
                            ArrayList<MediaItem> items = set.getItems();
                            int numItems = set.getNumItems();
                            MediaSet filteredSet = new MediaSet();
                            filteredSet.setNumExpectedItems(numItems);
                            mMediaFilteredSet = filteredSet;
                            for (int i = 0; i < numItems; ++i) {
                                MediaItem item = items.get(i);
                                if (filter.pass(item)) {
                                    filteredSet.addItem(item);
                                }
                            }
                            filteredSet.updateNumExpectedItems();
                            filteredSet.generateTitle(true);
                        }
                        updateListener(true);
                    }
                }
            }
        }
    }

    public void expandMediaSet(int mediaSetIndex) {
        // We need to check if this slot can be focused or not.
        if (mListener != null) {
            mListener.onFeedAboutToChange(this);
        }
        if (mExpandedMediaSetIndex > 0 && mediaSetIndex == Shared.INVALID) {
            // We are collapsing a previously expanded media set
            if (mediaSetIndex < mMediaSets.size() && mExpandedMediaSetIndex >= 0) {
                MediaSet set = mMediaSets.get(mExpandedMediaSetIndex);
                if (set.getNumItems() == 0) {
                    set.clear();
                }
            }
        }
        mExpandedMediaSetIndex = mediaSetIndex;
        if (mediaSetIndex < mMediaSets.size() && mediaSetIndex >= 0) {
            // Notify Picasa that the user entered the album.
            // MediaSet set = mMediaSets.get(mediaSetIndex);
            // PicasaService.requestSync(mContext, PicasaService.TYPE_ALBUM_PHOTOS, set.mPicasaAlbumId);
        }
        updateListener(true);
    }

    public boolean canExpandSet(int slotIndex) {
        int mediaSetIndex = slotIndex;
        if (mediaSetIndex < mMediaSets.size() && mediaSetIndex >= 0) {
            MediaSet set = mMediaSets.get(mediaSetIndex);
            if (set.getNumItems() > 0) {
                MediaItem item = set.getItems().get(0);
                if (item.mId == Shared.INVALID) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public boolean hasExpandedMediaSet() {
        return (mExpandedMediaSetIndex != Shared.INVALID);
    }

    public boolean restorePreviousClusteringState() {
        boolean retVal = disableClusteringIfNecessary();
        if (retVal) {
            if (mListener != null) {
                mListener.onFeedAboutToChange(this);
            }
            updateListener(true);
        }
        return retVal;
    }

    private boolean disableClusteringIfNecessary() {
        if (mInClusteringMode) {
            // Disable clustering.
            mInClusteringMode = false;
            return true;
        }
        return false;
    }

    public boolean isClustered() {
        return mInClusteringMode;
    }

    public MediaSet getCurrentSet() {
        if (mExpandedMediaSetIndex != Shared.INVALID && mExpandedMediaSetIndex < mMediaSets.size()) {
            return mMediaSets.get(mExpandedMediaSetIndex);
        }
        return null;
    }

    public void performClustering() {
        if (mListener != null) {
            mListener.onFeedAboutToChange(this);
        }
        MediaSet setToUse = null;
        if (mExpandedMediaSetIndex != Shared.INVALID || mExpandedMediaSetIndex < mMediaSets.size()) {
            setToUse = mMediaSets.get(mExpandedMediaSetIndex);
        }
        if (setToUse != null) {
            MediaClustering clustering = null;
            synchronized (this) {
                // Make sure the computation is completed to the end.
                clustering = mClusterSets.get(setToUse);
                if (clustering != null) {
                    clustering.compute(null, true);
                } else {
                    return;
                }
            }
            mInClusteringMode = true;
            updateListener(true);
        }
    }

    public void moveSetToFront(MediaSet mediaSet) {
        ArrayList<MediaSet> mediaSets = mMediaSets;
        int numSets = mediaSets.size();
        if (numSets == 0) {
            mediaSets.add(mediaSet);
            return;
        }
        MediaSet setToFind = mediaSets.get(0);
        if (setToFind == mediaSet) {
            return;
        }
        mediaSets.set(0, mediaSet);
        int indexToSwapTill = -1;
        for (int i = 1; i < numSets; ++i) {
            MediaSet set = mediaSets.get(i);
            if (set == mediaSet) {
                mediaSets.set(i, setToFind);
                indexToSwapTill = i;
                break;
            }
        }
        if (indexToSwapTill != Shared.INVALID) {
            for (int i = indexToSwapTill; i > 1; --i) {
                MediaSet setEnd = mediaSets.get(i);
                MediaSet setPrev = mediaSets.get(i - 1);
                mediaSets.set(i, setPrev);
                mediaSets.set(i - 1, setEnd);
            }
        }
    }

    public MediaSet replaceMediaSet(long setId, DataSource dataSource) {
        MediaSet mediaSet = new MediaSet(dataSource);
        mediaSet.mId = setId;
        ArrayList<MediaSet> mediaSets = mMediaSets;
        int numSets = mediaSets.size();
        for (int i = 0; i < numSets; ++i) {
            if (mediaSets.get(i).mId == setId) {
                MediaSet thisSet = mediaSets.get(i);
                mediaSet.mName = thisSet.mName;
                mediaSets.set(i, mediaSet);
                break;
            }
        }
        return mediaSet;
    }
    
    public void setSingleImageMode(boolean singleImageMode) {
        mSingleImageMode = singleImageMode;
    }

    public boolean isSingleImageMode() {
        return mSingleImageMode;
    }

    public MediaSet getExpandedMediaSet() {
        if (mExpandedMediaSetIndex == Shared.INVALID)
            return null;
        if (mExpandedMediaSetIndex >= mMediaSets.size())
            return null;
        return mMediaSets.get(mExpandedMediaSetIndex);
    }
}
