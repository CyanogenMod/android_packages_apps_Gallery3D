package com.cooliris.media;

import java.util.ArrayList;
import javax.microedition.khronos.opengles.GL11;

import android.hardware.SensorEvent;
import android.opengl.GLU;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.content.Context;

public final class GridLayer extends RootLayer implements MediaFeed.Listener, TimeBar.Listener {
    public static final int STATE_MEDIA_SETS = 0;
    public static final int STATE_GRID_VIEW = 1;
    public static final int STATE_FULL_SCREEN = 2;
    public static final int STATE_TIMELINE = 3;

    public static final int ANCHOR_LEFT = 0;
    public static final int ANCHOR_RIGHT = 1;
    public static final int ANCHOR_CENTER = 2;

    public static final int MAX_ITEMS_PER_SLOT = 12;
    public static final int MAX_DISPLAYED_ITEMS_PER_SLOT = 4;
    public static final int MAX_DISPLAY_SLOTS = 96;
    public static final int MAX_ITEMS_DRAWABLE = MAX_ITEMS_PER_SLOT * MAX_DISPLAY_SLOTS;

    private static final float SLIDESHOW_TRANSITION_TIME = 3.5f;

    private HudLayer mHud;
    private int mState;
    private static final IndexRange sBufferedVisibleRange = new IndexRange();
    private static final IndexRange sVisibleRange = new IndexRange();
    private static final IndexRange sPreviousDataRange = new IndexRange();
    private static final IndexRange sCompleteRange = new IndexRange();

    private static final Pool<Vector3f> sTempVec;
    private static final Pool<Vector3f> sTempVecAlt;
    static {
        Vector3f[] vectorPool = new Vector3f[128];
        int length = vectorPool.length;
        for (int i = 0; i < length; ++i) {
            vectorPool[i] = new Vector3f();
        }
        Vector3f[] vectorPoolRenderThread = new Vector3f[128];
        length = vectorPoolRenderThread.length;
        for (int i = 0; i < length; ++i) {
            vectorPoolRenderThread[i] = new Vector3f();
        }
        sTempVec = new Pool<Vector3f>(vectorPool);
        sTempVecAlt = new Pool<Vector3f>(vectorPoolRenderThread);
    }

    private static final ArrayList<MediaItem> sTempList = new ArrayList<MediaItem>();
    private static final MediaItem[] sTempHash = new MediaItem[64];

    private static final Vector3f sDeltaAnchorPositionUncommited = new Vector3f();
    private static Vector3f sDeltaAnchorPosition = new Vector3f();

    // The display primitives.
    final private GridDrawables mDrawables;
    private float mSelectedAlpha = 0.0f;
    private float mTargetAlpha = 0.0f;

    final private GridCamera mCamera;
    final private GridCameraManager mCameraManager;
    final private GridDrawManager mDrawManager;
    final private GridInputProcessor mInputProcessor;

    private boolean mFeedAboutToChange;
    private boolean mPerformingLayoutChange;
    private boolean mFeedChanged;

    private final LayoutInterface mLayoutInterface;
    private static final LayoutInterface sfullScreenLayoutInterface = new GridLayoutInterface(1);

    private MediaFeed mMediaFeed;
    private boolean mInAlbum = false;
    private int mCurrentExpandedSlot;

    private static final DisplayList sDisplayList = new DisplayList();
    private static final DisplayItem[] sDisplayItems = new DisplayItem[MAX_ITEMS_DRAWABLE];
    private static final DisplaySlot[] sDisplaySlots = new DisplaySlot[MAX_DISPLAY_SLOTS];
    private static ArrayList<MediaItem> sVisibleItems;

    private float mTimeElapsedSinceTransition;
    private final BackgroundLayer mBackground;
    private boolean mLocationFilter;
    private float mZoomValue = 1.0f;
    private float mCurrentFocusItemWidth = 1.0f;
    private float mCurrentFocusItemHeight = 1.0f;
    private float mTimeElapsedSinceGridViewReady = 0.0f;

    private boolean mSlideshowMode;
    private boolean mNoDeleteMode = false;
    private float mTimeElapsedSinceView;
    private static final MediaBucketList sSelectedBucketList = new MediaBucketList();
    private static final MediaBucketList sMarkedBucketList = new MediaBucketList();
    private float mTimeElapsedSinceStackViewReady;

    private Context mContext;
    private RenderView mView;
    private boolean mPickIntent;
    private boolean mViewIntent;
    private WakeLock mWakeLock;
    private int mStartMemoryRange;
    private int mFramesDirty;
    private String mRequestFocusContentUri;
    private int mFrameCount;

    public GridLayer(Context context, int itemWidth, int itemHeight, LayoutInterface layoutInterface, RenderView view) {
        mBackground = new BackgroundLayer(this);
        mContext = context;
        mView = view;

        DisplaySlot[] displaySlots = sDisplaySlots;
        for (int i = 0; i < MAX_DISPLAY_SLOTS; ++i) {
            DisplaySlot slot = new DisplaySlot();
            displaySlots[i] = slot;
        }
        mLayoutInterface = layoutInterface;
        mCamera = new GridCamera(0, 0, itemWidth, itemHeight);
        mDrawables = new GridDrawables(itemWidth, itemHeight);
        sBufferedVisibleRange.set(Shared.INVALID, Shared.INVALID);
        sVisibleRange.set(Shared.INVALID, Shared.INVALID);
        sCompleteRange.set(Shared.INVALID, Shared.INVALID);
        sPreviousDataRange.set(Shared.INVALID, Shared.INVALID);
        sDeltaAnchorPosition.set(0, 0, 0);
        sDeltaAnchorPositionUncommited.set(0, 0, 0);
        sSelectedBucketList.clear();

        sVisibleItems = new ArrayList<MediaItem>();
        mHud = new HudLayer(context);
        mHud.setContext(context);
        mHud.setGridLayer(this);
        mHud.getPathBar().clear();
        mHud.setGridLayer(this);
        mHud.getTimeBar().setListener(this);
        mHud.getPathBar().pushLabel(R.drawable.icon_home_small, context.getResources().getString(R.string.app_name),
                new Runnable() {
                    public void run() {
                        if (mHud.getAlpha() == 1.0f) {
                            if (!mFeedAboutToChange) {
                                setState(STATE_MEDIA_SETS);
                            }
                        } else {
                            mHud.setAlpha(1.0f);
                        }
                    }
                });
        mCameraManager = new GridCameraManager(mCamera);
        mDrawManager = new GridDrawManager(context, mCamera, mDrawables, sDisplayList, sDisplayItems, sDisplaySlots);
        mInputProcessor = new GridInputProcessor(context, mCamera, this, mView, sTempVec, sDisplayItems);
        setState(STATE_MEDIA_SETS);
    }

    public HudLayer getHud() {
        return mHud;
    }

    public void shutdown() {
        if (mMediaFeed != null) {
            mMediaFeed.shutdown();
        }
        mContext = null;
        sSelectedBucketList.clear();
        mView = null;
    }

    public void stop() {
        endSlideshow();
        mBackground.clear();
        handleLowMemory();
    }

    @Override
    public void generate(RenderView view, RenderView.Lists lists) {
        lists.updateList.add(this);
        lists.opaqueList.add(this);
        mBackground.generate(view, lists);
        lists.blendedList.add(this);
        lists.hitTestList.add(this);
        mHud.generate(view, lists);
    }

    @Override
    protected void onSizeChanged() {
        mHud.setSize(mWidth, mHeight);
        mHud.setAlpha(1.0f);
        mBackground.setSize(mWidth, mHeight);
        mTimeElapsedSinceTransition = 0.0f;
        if (mView != null) {
            mView.requestRender();
        }
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        boolean feedUnchanged = false;
        if (mState == state) {
            feedUnchanged = true;
        }
        GridLayoutInterface layoutInterface = (GridLayoutInterface) mLayoutInterface;
        GridLayoutInterface oldLayout = (GridLayoutInterface) sfullScreenLayoutInterface;
        oldLayout.mNumRows = layoutInterface.mNumRows;
        oldLayout.mSpacingX = layoutInterface.mSpacingX;
        oldLayout.mSpacingY = layoutInterface.mSpacingY;
        GridCamera camera = mCamera;
        int numMaxRows = (camera.mHeight >= camera.mWidth) ? 4 : 3;
        MediaFeed feed = mMediaFeed;
        boolean performLayout = true;
        mZoomValue = 1.0f;
        float yStretch = camera.mDefaultAspectRatio / camera.mAspectRatio;
        if (yStretch < 1.0f) {
            yStretch = 1.0f;
        }
        switch (state) {
        case STATE_GRID_VIEW:
            mTimeElapsedSinceGridViewReady = 0.0f;
            if (feed != null && feedUnchanged == false) {
                boolean updatedData = feed.restorePreviousClusteringState();
                if (updatedData) {
                    performLayout = false;
                }
            }
            layoutInterface.mNumRows = numMaxRows;
            layoutInterface.mSpacingX = (int) (10 * Gallery.PIXEL_DENSITY);
            layoutInterface.mSpacingY = (int) (10 * Gallery.PIXEL_DENSITY);
            if (mState == STATE_MEDIA_SETS) {
                // Entering album.
                mInAlbum = true;
                MediaSet set = feed.getCurrentSet();
                int icon = mDrawables.getIconForSet(set, true);
                if (set != null) {
                    mHud.getPathBar().pushLabel(icon, set.mNoCountTitleString, new Runnable() {
                        public void run() {
                            if (mFeedAboutToChange) {
                                return;
                            }
                            if (mHud.getAlpha() == 1.0f) {
                                disableLocationFiltering();
                                mInputProcessor.clearSelection();
                                setState(STATE_GRID_VIEW);
                            } else {
                                mHud.setAlpha(1.0f);
                            }
                        }
                    });
                }
            }
            if (mState == STATE_FULL_SCREEN) {
                mHud.getPathBar().popLabel();
            }
            break;
        case STATE_TIMELINE:
            mTimeElapsedSinceStackViewReady = 0.0f;
            if (feed != null && feedUnchanged == false) {
                feed.performClustering();
                performLayout = false;
            }
            disableLocationFiltering();
            layoutInterface.mNumRows = numMaxRows - 1;
            layoutInterface.mSpacingX = (int) (100 * Gallery.PIXEL_DENSITY);
            layoutInterface.mSpacingY = (int) (70 * Gallery.PIXEL_DENSITY * yStretch);
            break;
        case STATE_FULL_SCREEN:
            layoutInterface.mNumRows = 1;
            layoutInterface.mSpacingX = (int) (40 * Gallery.PIXEL_DENSITY);
            layoutInterface.mSpacingY = (int) (40 * Gallery.PIXEL_DENSITY);
            if (mState != STATE_FULL_SCREEN) {
                mHud.getPathBar().pushLabel(R.drawable.ic_fs_details, "", new Runnable() {
                    public void run() {
                        if (mHud.getAlpha() == 1.0f) {
                            mHud.swapFullscreenLabel();
                        }
                        mHud.setAlpha(1.0f);
                    }
                });
            }
            break;
        case STATE_MEDIA_SETS:
            mTimeElapsedSinceStackViewReady = 0.0f;
            if (feed != null && feedUnchanged == false) {
                feed.restorePreviousClusteringState();
                sMarkedBucketList.clear();
                feed.expandMediaSet(Shared.INVALID);
                performLayout = false;
            }
            disableLocationFiltering();
            mInputProcessor.clearSelection();
            layoutInterface.mNumRows = numMaxRows - 1;
            layoutInterface.mSpacingX = (int) (100 * Gallery.PIXEL_DENSITY);
            layoutInterface.mSpacingY = (int) (70 * Gallery.PIXEL_DENSITY * yStretch);
            if (mInAlbum) {
                if (mState == STATE_FULL_SCREEN) {
                    mHud.getPathBar().popLabel();
                }
                mHud.getPathBar().popLabel();
                mInAlbum = false;
            }
            break;
        }
        mState = state;
        mHud.onGridStateChanged();
        if (performLayout && mFeedAboutToChange == false) {
            onLayout(Shared.INVALID, Shared.INVALID, oldLayout);
        }
        if (state != STATE_FULL_SCREEN) {
            mCamera.moveYTo(0);
            mCamera.moveZTo(0);
        }
    }

    protected void enableLocationFiltering(String label) {
        if (mLocationFilter == false) {
            mLocationFilter = true;
            mHud.getPathBar().pushLabel(R.drawable.icon_location_small, label, new Runnable() {
                public void run() {
                    if (mHud.getAlpha() == 1.0f) {
                        if (mState == STATE_FULL_SCREEN) {
                            mInputProcessor.clearSelection();
                            setState(STATE_GRID_VIEW);
                        } else {
                            disableLocationFiltering();
                        }
                    } else {
                        mHud.setAlpha(1.0f);
                    }
                }
            });
        }
    }

    protected void disableLocationFiltering() {
        if (mLocationFilter) {
            mLocationFilter = false;
            mMediaFeed.removeFilter();
            mHud.getPathBar().popLabel();
        }
    }

    boolean goBack() {
        if (mFeedAboutToChange) {
            return false;
        }
        int state = mState;
        if (mInputProcessor.getCurrentSelectedSlot() == Shared.INVALID) {
            if (mLocationFilter) {
                disableLocationFiltering();
                setState(STATE_TIMELINE);
                return true;
            }
        }
        switch (state) {
        case STATE_GRID_VIEW:
            setState(STATE_MEDIA_SETS);
            break;
        case STATE_TIMELINE:
            setState(STATE_GRID_VIEW);
            break;
        case STATE_FULL_SCREEN:
            setState(STATE_GRID_VIEW);
            mInputProcessor.clearSelection();
            break;
        default:
            return false;
        }
        return true;
    }

    public void endSlideshow() {
        mSlideshowMode = false;
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mWakeLock = null;
        }
        mHud.setAlpha(1.0f);
    }

    @Override
    public void onSensorChanged(RenderView view, SensorEvent event) {
        mInputProcessor.onSensorChanged(view, event, mState);
    }

    public DataSource getDataSource() {
        if (mMediaFeed != null)
            return mMediaFeed.getDataSource();
        return null;
    }

    public void setDataSource(DataSource dataSource) {
        MediaFeed feed = mMediaFeed;
        if (feed != null) {
            feed.shutdown();
            sDisplayList.clear();
            mBackground.clear();
        }
        mMediaFeed = new MediaFeed(mContext, dataSource, this);
        mMediaFeed.start();
    }

    public IndexRange getVisibleRange() {
        return sVisibleRange;
    }

    public IndexRange getBufferedVisibleRange() {
        return sBufferedVisibleRange;
    }

    public IndexRange getCompleteRange() {
        return sCompleteRange;
    }

    private int hitTest(Vector3f worldPos, int itemWidth, int itemHeight) {
        int retVal = Shared.INVALID;
        int firstSlotIndex = 0;
        int lastSlotIndex = 0;
        IndexRange rangeToUse = sVisibleRange;
        synchronized (rangeToUse) {
            firstSlotIndex = rangeToUse.begin;
            lastSlotIndex = rangeToUse.end;
        }
        Pool<Vector3f> pool = sTempVec;
        float itemWidthBy2 = itemWidth * 0.5f;
        float itemHeightBy2 = itemHeight * 0.5f;
        Vector3f position = pool.create();
        Vector3f deltaAnchorPosition = pool.create();
        try {
            deltaAnchorPosition.set(sDeltaAnchorPosition);
            for (int i = firstSlotIndex; i <= lastSlotIndex; ++i) {
                GridCameraManager.getSlotPositionForSlotIndex(i, mCamera, mLayoutInterface, deltaAnchorPosition, position);
                if (FloatUtils.boundsContainsPoint(position.x - itemWidthBy2, position.x + itemWidthBy2,
                        position.y - itemHeightBy2, position.y + itemHeightBy2, worldPos.x, worldPos.y)) {
                    retVal = i;
                    break;
                }
            }
        } finally {
            pool.delete(deltaAnchorPosition);
            pool.delete(position);
        }
        return retVal;
    }

    void centerCameraForSlot(int slotIndex, float baseConvergence) {
        float imageTheta = 0.0f;
        DisplayItem displayItem = getDisplayItemForSlotId(slotIndex);
        if (displayItem != null) {
            imageTheta = displayItem.getImageTheta();
        }
        mCameraManager.centerCameraForSlot(mLayoutInterface, slotIndex, baseConvergence, sDeltaAnchorPositionUncommited,
                mInputProcessor.getCurrentSelectedSlot(), mZoomValue, imageTheta, mState);
    }

    boolean constrainCameraForSlot(int slotIndex) {
        return mCameraManager.constrainCameraForSlot(mLayoutInterface, slotIndex, sDeltaAnchorPosition, mCurrentFocusItemWidth,
                mCurrentFocusItemHeight);
    }

    // Called on render thread before rendering.
    @Override
    public boolean update(RenderView view, float timeElapsed) {
        if (mFeedAboutToChange == false) {
            mTimeElapsedSinceTransition += timeElapsed;
            mTimeElapsedSinceGridViewReady += timeElapsed;
            if (mTimeElapsedSinceGridViewReady >= 1.0f) {
                mTimeElapsedSinceGridViewReady = 1.0f;
            }
            mTimeElapsedSinceStackViewReady += timeElapsed;
            if (mTimeElapsedSinceStackViewReady >= 1.0f) {
                mTimeElapsedSinceStackViewReady = 1.0f;
            }
        } else {
            mTimeElapsedSinceTransition = 0;
        }
        if (mMediaFeed != null && mMediaFeed.isSingleImageMode()) {
            HudLayer hud = getHud();
            hud.getPathBar().setHidden(true);
            hud.getMenuBar().setHidden(true);
            if (hud.getMode() != HudLayer.MODE_NORMAL)
                hud.setMode(HudLayer.MODE_NORMAL);
        }
        if (view.elapsedLoadingExpensiveTextures() > 150 || (mMediaFeed != null && mMediaFeed.getWaitingForMediaScanner())) {
            mHud.getPathBar().setAnimatedIcons(GridDrawables.TEXTURE_SPINNER);
        } else {
            mHud.getPathBar().setAnimatedIcons(null);
        }

        // In that case, we need to commit the respective Display Items when the
        // feed was updated.
        GridCamera camera = mCamera;
        camera.update(timeElapsed);
        DisplayItem anchorDisplayItem = getAnchorDisplayItem(ANCHOR_CENTER);
        if (anchorDisplayItem != null && !mHud.getTimeBar().isDragged()) {
            mHud.getTimeBar().setItem(anchorDisplayItem.mItemRef);
        }
        sDisplayList.update(timeElapsed);
        mInputProcessor.update(timeElapsed);
        mSelectedAlpha = FloatUtils.animate(mSelectedAlpha, mTargetAlpha, timeElapsed * 0.5f);
        if (mState == STATE_FULL_SCREEN) {
            mHud.autoHide(true);
        } else {
            mHud.autoHide(false);
            mHud.setAlpha(1.0f);
        }
        GridQuad[] fullscreenQuads = GridDrawables.sFullscreenGrid;
        int numFullScreenQuads = fullscreenQuads.length;
        for (int i = 0; i < numFullScreenQuads; ++i) {
            fullscreenQuads[i].update(timeElapsed);
        }
        if (mSlideshowMode && mState == STATE_FULL_SCREEN) {
            mTimeElapsedSinceView += timeElapsed;
            if (mTimeElapsedSinceView > SLIDESHOW_TRANSITION_TIME) {
                // time to go to the next slide
                mTimeElapsedSinceView = 0.0f;
                changeFocusToNextSlot(0.5f);
                mCamera.commitMoveInX();
                mCamera.commitMoveInY();
            }
        }
        if (mState == STATE_MEDIA_SETS || mState == STATE_TIMELINE) {
            mCamera.moveYTo(-0.1f);
            mCamera.commitMoveInY();
        }
        boolean dirty = mDrawManager.update(timeElapsed);
        dirty |= mSlideshowMode;
        dirty |= mFramesDirty > 0;
        ++mFrameCount;
        if (mFramesDirty > 0) {
            --mFramesDirty;
        }
        try {
            if (mMediaFeed != null && (mMediaFeed.getWaitingForMediaScanner())) {
                // We limit the drawing of the frame so that the MediaScanner
                // thread can do its work
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {

        }
        if (sDisplayList.getNumAnimatables() != 0 || mCamera.isAnimating()
                || (mTimeElapsedSinceTransition > 0.0f && mTimeElapsedSinceTransition < 1.0f) || mSelectedAlpha != mTargetAlpha
                // || (mAnimatedFov != mTargetFov)
                || dirty)
            return true;
        else
            return false;
    }

    private void computeVisibleRange() {
        if (mPerformingLayoutChange)
            return;
        if (sDeltaAnchorPosition.equals(sDeltaAnchorPositionUncommited) == false) {
            sDeltaAnchorPosition.set(sDeltaAnchorPositionUncommited);
        }
        mCameraManager.computeVisibleRange(mMediaFeed, mLayoutInterface, sDeltaAnchorPosition, sVisibleRange,
                sBufferedVisibleRange, sCompleteRange, mState);
    }

    private void computeVisibleItems() {
        if (mFeedAboutToChange == true || mPerformingLayoutChange == true) {
            return;
        }
        computeVisibleRange();
        int deltaBegin = sBufferedVisibleRange.begin - sPreviousDataRange.begin;
        int deltaEnd = sBufferedVisibleRange.end - sPreviousDataRange.end;
        if (deltaBegin != 0 || deltaEnd != 0) {
            // The delta has changed, we have to compute the display items
            // again.
            // We find the intersection range, these slots have not changed at
            // all.
            int firstVisibleSlotIndex = sBufferedVisibleRange.begin;
            int lastVisibleSlotIndex = sBufferedVisibleRange.end;
            sPreviousDataRange.begin = firstVisibleSlotIndex;
            sPreviousDataRange.end = lastVisibleSlotIndex;

            Pool<Vector3f> pool = sTempVec;
            Vector3f position = pool.create();
            Vector3f deltaAnchorPosition = pool.create();
            try {
                MediaFeed feed = mMediaFeed;
                DisplayList displayList = sDisplayList;
                DisplayItem[] displayItems = sDisplayItems;
                DisplaySlot[] displaySlots = sDisplaySlots;
                int numDisplayItems = displayItems.length;
                int numDisplaySlots = displaySlots.length;
                ArrayList<MediaItem> visibleItems = sVisibleItems;
                deltaAnchorPosition.set(sDeltaAnchorPosition);
                LayoutInterface layout = mLayoutInterface;
                GridCamera camera = mCamera;
                for (int i = firstVisibleSlotIndex; i <= lastVisibleSlotIndex; ++i) {
                    GridCameraManager.getSlotPositionForSlotIndex(i, camera, layout, deltaAnchorPosition, position);
                    MediaSet set = feed.getSetForSlot(i);
                    int indexIntoSlots = i - firstVisibleSlotIndex;

                    if (set != null && indexIntoSlots >= 0 && indexIntoSlots < numDisplaySlots) {
                        ArrayList<MediaItem> items = set.getItems();
                        displaySlots[indexIntoSlots].setMediaSet(set);
                        ArrayList<MediaItem> bestItems = sTempList;
                        if (mTimeElapsedSinceTransition < 1.0f) {
                            // We always show the same top thumbnails for a
                            // stack of albums
                            if (mState == STATE_MEDIA_SETS)
                                ArrayUtils.computeSortedIntersection(items, visibleItems, MAX_ITEMS_PER_SLOT, bestItems, sTempHash);
                            else
                                ArrayUtils.computeSortedIntersection(visibleItems, items, MAX_ITEMS_PER_SLOT, bestItems, sTempHash);
                        }

                        int numItemsInSet = set.getNumItems();
                        int numBestItems = bestItems.size();
                        int originallyFoundItems = numBestItems;
                        if (numBestItems < MAX_ITEMS_PER_SLOT) {
                            int itemsRemaining = MAX_ITEMS_PER_SLOT - numBestItems;
                            for (int currItemPos = 0; currItemPos < numItemsInSet; currItemPos++) {
                                MediaItem item = items.get(currItemPos);
                                if (mTimeElapsedSinceTransition >= 1.0f || !bestItems.contains(item)) {
                                    bestItems.add(item);
                                    if (--itemsRemaining == 0) {
                                        break;
                                    }
                                }
                            }
                        }
                        numBestItems = bestItems.size();
                        int baseIndex = (i - firstVisibleSlotIndex) * MAX_ITEMS_PER_SLOT;
                        for (int j = 0; j < numBestItems; ++j) {
                            if (baseIndex + j >= numDisplayItems) {
                                break;
                            }
                            if (j >= numItemsInSet) {
                                displayItems[baseIndex + j] = null;
                            } else {
                                MediaItem item = bestItems.get(j);
                                if (item != null) {
                                    DisplayItem displayItem = displayList.get(item);
                                    if ((mState == STATE_FULL_SCREEN && i != mInputProcessor.getCurrentSelectedSlot())
                                            || (mState == STATE_GRID_VIEW && (mTimeElapsedSinceTransition > 1.0f || j >= originallyFoundItems))) {
                                        displayItem.set(position, j, false);
                                        displayItem.commit();
                                    } else {
                                        displayList.setPositionAndStackIndex(displayItem, position, j, true);
                                    }
                                    displayItems[baseIndex + j] = displayItem;
                                }
                            }
                        }
                        for (int j = numBestItems; j < MAX_ITEMS_PER_SLOT; ++j) {
                            displayItems[baseIndex + j] = null;
                        }
                        bestItems.clear();
                    }
                }
                if (mFeedChanged) {
                    mFeedChanged = false;
                    if (mInputProcessor != null && mState == STATE_FULL_SCREEN && mRequestFocusContentUri == null) {
                        int currentSelectedSlot = mInputProcessor.getCurrentSelectedSlot();
                        if (currentSelectedSlot > sCompleteRange.end)
                            currentSelectedSlot = sCompleteRange.end;
                        mInputProcessor.setCurrentSelectedSlot(currentSelectedSlot);
                    }
                    if (mState == STATE_GRID_VIEW) {
                        MediaSet expandedSet = mMediaFeed.getExpandedMediaSet();
                        if (expandedSet != null) {
                            if (!mHud.getPathBar().getCurrentLabel().equals(expandedSet.mNoCountTitleString)) {
                                mHud.getPathBar().changeLabel(expandedSet.mNoCountTitleString);
                            }
                        }
                    }
                    if (mRequestFocusContentUri != null) {
                        // We have to find the item that has this contentUri
                        if (mState == STATE_FULL_SCREEN) {
                            int numSlots = sCompleteRange.end + 1;
                            for (int i = 0; i < numSlots; ++i) {
                                MediaSet set = feed.getSetForSlot(i);
                                ArrayList<MediaItem> items = set.getItems();
                                int numItems = items.size();
                                for (int j = 0; j < numItems; ++j) {
                                    String itemUri = items.get(j).mContentUri;
                                    if (itemUri != null && mRequestFocusContentUri != null) {
                                        if (itemUri.equals(mRequestFocusContentUri)) {
                                            mInputProcessor.setCurrentSelectedSlot(i);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        mRequestFocusContentUri = null;
                    }
                }
            } finally {
                pool.delete(position);
                pool.delete(deltaAnchorPosition);
            }
            // We keep upto 400 thumbnails in memory.
            int numThumbnailsToKeepInMemory = (mState == STATE_MEDIA_SETS || mState == STATE_TIMELINE) ? 100 : 400;
            int startMemoryRange = (sBufferedVisibleRange.begin / numThumbnailsToKeepInMemory) * numThumbnailsToKeepInMemory;
            if (mStartMemoryRange != startMemoryRange) {
                mStartMemoryRange = startMemoryRange;
                clearUnusedThumbnails();
            }
        }
    }

    @Override
    public void handleLowMemory() {
        clearUnusedThumbnails();
        GridDrawables.sStringTextureTable.clear();
        mBackground.clearCache();
    }

    // This method can be potentially expensive
    public void clearUnusedThumbnails() {
        sDisplayList.clearExcept(sDisplayItems);
    }

    @Override
    public void onSurfaceCreated(RenderView view, GL11 gl) {
        sDisplayList.clear();
        mHud.clear();
        mHud.reset();
        GridDrawables.sStringTextureTable.clear();
        mDrawables.onSurfaceCreated(view, gl);
        mBackground.clear();
    }

    @Override
    public void onSurfaceChanged(RenderView view, int width, int height) {
        mCamera.viewportChanged(width, height, mCamera.mItemWidth, mCamera.mItemHeight);
        view.setFov(mCamera.mFov);
        setState(mState);
    }

    // Renders the node in a given pass.
    public void renderOpaque(RenderView view, GL11 gl) {
        GridCamera camera = mCamera;
        int selectedSlotIndex = mInputProcessor.getCurrentSelectedSlot();
        computeVisibleItems();

        gl.glMatrixMode(GL11.GL_MODELVIEW);
        gl.glLoadIdentity();
        GLU.gluLookAt(gl, -camera.mEyeX, -camera.mEyeY, -camera.mEyeZ, -camera.mLookAtX, -camera.mLookAtY, -camera.mLookAtZ,
                camera.mUpX, camera.mUpY, camera.mUpZ);
        view.setAlpha(1.0f);
        if (mSelectedAlpha != 1.0f) {
            gl.glEnable(GL11.GL_BLEND);
            gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            view.setAlpha(mSelectedAlpha);
        }
        if (selectedSlotIndex != Shared.INVALID) {
            mTargetAlpha = 0.0f;
        } else {
            mTargetAlpha = 1.0f;
        }
        mDrawManager.prepareDraw(sBufferedVisibleRange, sVisibleRange, selectedSlotIndex, mInputProcessor.getCurrentFocusSlot(),
                mInputProcessor.isFocusItemPressed());
        if (mSelectedAlpha != 0.0f) {
            mDrawManager.drawThumbnails(view, gl, mState);
        }
        if (mSelectedAlpha != 1.0f) {
            gl.glDisable(GL11.GL_BLEND);
        }
        // We draw the selected slotIndex.
        if (selectedSlotIndex != Shared.INVALID) {
            mDrawManager.drawFocusItems(view, gl, mZoomValue, mSlideshowMode, mTimeElapsedSinceView);
            mCurrentFocusItemWidth = mDrawManager.getFocusQuadWidth();
            mCurrentFocusItemHeight = mDrawManager.getFocusQuadHeight();
        }
        view.setAlpha(mSelectedAlpha);
    }

    public void renderBlended(RenderView view, GL11 gl) {
        // We draw the placeholder for all visible slots.
        if (mHud != null && mDrawManager != null) {
            if (mMediaFeed != null) {
                mDrawManager.drawBlendedComponents(view, gl, mSelectedAlpha, mState, mHud.getMode(),
                        mTimeElapsedSinceStackViewReady, mTimeElapsedSinceGridViewReady, sSelectedBucketList, sMarkedBucketList,
                        mMediaFeed.getWaitingForMediaScanner() || mFeedAboutToChange || mMediaFeed.isLoading());
            }
        }
    }

    public synchronized void onLayout(int newAnchorSlotIndex, int currentAnchorSlotIndex, LayoutInterface oldLayout) {
        if (mPerformingLayoutChange || !sDeltaAnchorPosition.equals(sDeltaAnchorPositionUncommited)) {
            return;
        }

        mTimeElapsedSinceTransition = 0.0f;
        mPerformingLayoutChange = true;
        LayoutInterface layout = mLayoutInterface;
        if (oldLayout == null) {
            oldLayout = sfullScreenLayoutInterface;
        }
        GridCamera camera = mCamera;
        if (currentAnchorSlotIndex == Shared.INVALID) {
            currentAnchorSlotIndex = getAnchorSlotIndex(ANCHOR_CENTER);
            if (mCurrentExpandedSlot != Shared.INVALID) {
                currentAnchorSlotIndex = mCurrentExpandedSlot;
            }
            int selectedSlotIndex = mInputProcessor.getCurrentSelectedSlot();
            if (selectedSlotIndex != Shared.INVALID) {
                currentAnchorSlotIndex = selectedSlotIndex;
            }
        }
        if (newAnchorSlotIndex == Shared.INVALID) {
            newAnchorSlotIndex = currentAnchorSlotIndex;
        }
        int itemHeight = camera.mItemHeight;
        int itemWidth = camera.mItemWidth;
        Pool<Vector3f> pool = sTempVec;
        Vector3f deltaAnchorPosition = pool.create();
        Vector3f currentSlotPosition = pool.create();
        try {
            deltaAnchorPosition.set(0, 0, 0);
            if (currentAnchorSlotIndex != Shared.INVALID && newAnchorSlotIndex != Shared.INVALID) {
                layout.getPositionForSlotIndex(newAnchorSlotIndex, itemWidth, itemHeight, deltaAnchorPosition);
                oldLayout.getPositionForSlotIndex(currentAnchorSlotIndex, itemWidth, itemHeight, currentSlotPosition);
                currentSlotPosition.subtract(sDeltaAnchorPosition);
                deltaAnchorPosition.subtract(currentSlotPosition);
                deltaAnchorPosition.y = 0;
                deltaAnchorPosition.z = 0;
            }
            sDeltaAnchorPositionUncommited.set(deltaAnchorPosition);
        } finally {
            pool.delete(deltaAnchorPosition);
            pool.delete(currentSlotPosition);
        }
        centerCameraForSlot(newAnchorSlotIndex, 1.0f);
        mCurrentExpandedSlot = Shared.INVALID;

        // Force recompute of visible items and their positions.
        ((GridLayoutInterface) oldLayout).mNumRows = ((GridLayoutInterface) layout).mNumRows;
        ((GridLayoutInterface) oldLayout).mSpacingX = ((GridLayoutInterface) layout).mSpacingX;
        ((GridLayoutInterface) oldLayout).mSpacingY = ((GridLayoutInterface) layout).mSpacingY;
        forceRecomputeVisibleRange();
        mPerformingLayoutChange = false;
    }

    private void forceRecomputeVisibleRange() {
        sPreviousDataRange.begin = Shared.INVALID;
        sPreviousDataRange.end = Shared.INVALID;
        if (mView != null) {
            mView.requestRender();
        }
    }

    // called on background thread
    public synchronized void onFeedChanged(MediaFeed feed, boolean needsLayout) {
        if (!needsLayout) {
            mFeedChanged = true;
            forceRecomputeVisibleRange();
            if (mState == STATE_GRID_VIEW || mState == STATE_FULL_SCREEN)
                mHud.setFeed(feed, mState, needsLayout);
            return;
        }

        while (mPerformingLayoutChange == true) {
            Thread.yield();
        }
        if (mState == STATE_GRID_VIEW) {
            if (mHud != null) {
                MediaSet set = feed.getCurrentSet();
                if (set != null && !mLocationFilter)
                    mHud.getPathBar().changeLabel(set.mNoCountTitleString);
            }
        }
        DisplayItem[] displayItems = sDisplayItems;
        int firstBufferedVisibleSlotIndex = sBufferedVisibleRange.begin;
        int lastBufferedVisibleSlotIndex = sBufferedVisibleRange.end;
        int currentlyVisibleSlotIndex = getAnchorSlotIndex(ANCHOR_CENTER);
        if (mCurrentExpandedSlot != Shared.INVALID) {
            currentlyVisibleSlotIndex = mCurrentExpandedSlot;
        }
        MediaItem anchorItem = null;
        ArrayList<MediaItem> visibleItems = sVisibleItems;
        visibleItems.clear();
        visibleItems.ensureCapacity(lastBufferedVisibleSlotIndex - firstBufferedVisibleSlotIndex);
        if (currentlyVisibleSlotIndex != Shared.INVALID && currentlyVisibleSlotIndex >= firstBufferedVisibleSlotIndex
                && currentlyVisibleSlotIndex <= lastBufferedVisibleSlotIndex) {
            int baseIndex = (currentlyVisibleSlotIndex - firstBufferedVisibleSlotIndex) * MAX_ITEMS_PER_SLOT;
            for (int i = 0; i < MAX_ITEMS_PER_SLOT; ++i) {
                DisplayItem displayItem = displayItems[baseIndex + i];
                if (displayItem != null) {
                    if (anchorItem == null) {
                        anchorItem = displayItem.mItemRef;
                    }
                    visibleItems.add(displayItem.mItemRef);
                }
            }
        }
        // We want to add items from the middle.
        int numItems = lastBufferedVisibleSlotIndex - firstBufferedVisibleSlotIndex + 1;
        int midPoint = (lastBufferedVisibleSlotIndex - firstBufferedVisibleSlotIndex) / 2;
        int length = displayItems.length;
        for (int i = 0; i < numItems; ++i) {
            int index = midPoint + Shared.midPointIterator(i);
            int indexIntoDisplayItem = (index - firstBufferedVisibleSlotIndex) * MAX_ITEMS_PER_SLOT;
            if (indexIntoDisplayItem >= 0 && indexIntoDisplayItem < length) {
                for (int j = 0; j < MAX_ITEMS_PER_SLOT; ++j) {
                    DisplayItem displayItem = displayItems[indexIntoDisplayItem + j];
                    if (displayItem != null) {
                        MediaItem item = displayItem.mItemRef;
                        if (item != anchorItem) {
                            visibleItems.add(item);
                        }
                    }
                }
            }
        }
        int newSlotIndex = Shared.INVALID;
        if (anchorItem != null) {
            // We try to find the anchor item in the new feed.
            int numSlots = feed.getNumSlots();
            for (int i = 0; i < numSlots; ++i) {
                MediaSet set = feed.getSetForSlot(i);
                if (set != null && set.containsItem(anchorItem)) {
                    newSlotIndex = i;
                    break;
                }
            }
        }

        if (anchorItem != null && newSlotIndex == Shared.INVALID) {
            int numSlots = feed.getNumSlots();
            MediaSet parentSet = anchorItem.mParentMediaSet;
            for (int i = 0; i < numSlots; ++i) {
                MediaSet set = feed.getSetForSlot(i);
                if (set != null && set.mId == parentSet.mId) {
                    newSlotIndex = i;
                    break;
                }
            }
        }

        // We must create a new display store now since the data has changed.
        if (newSlotIndex != Shared.INVALID) {
            if (mState == STATE_MEDIA_SETS) {
                sDisplayList.clearExcept(displayItems);
            }
            onLayout(newSlotIndex, currentlyVisibleSlotIndex, null);
        } else {
            forceRecomputeVisibleRange();
        }
        mCurrentExpandedSlot = Shared.INVALID;
        mFeedAboutToChange = false;
        mFeedChanged = true;
        if (feed != null) {
            if (mState == STATE_GRID_VIEW || mState == STATE_FULL_SCREEN)
                mHud.setFeed(feed, mState, needsLayout);
        }
        if (mView != null) {
            mView.requestRender();
        }
    }

    public DisplayItem getRepresentativeDisplayItem() {
        int slotIndex = Shared.INVALID;
        if (mInputProcessor != null) {
            slotIndex = mInputProcessor.getCurrentFocusSlot();
        }
        if (slotIndex == Shared.INVALID) {
            slotIndex = getAnchorSlotIndex(ANCHOR_CENTER);
        }
        int index = (slotIndex - sBufferedVisibleRange.begin) * MAX_ITEMS_PER_SLOT;
        if (index >= 0 && index < MAX_ITEMS_DRAWABLE) {
            return sDisplayItems[index];
        } else {
            return null;
        }
    }

    public DisplayItem getAnchorDisplayItem(int type) {
        int slotIndex = getAnchorSlotIndex(type);
        return sDisplayItems[(slotIndex - sBufferedVisibleRange.begin) * MAX_ITEMS_PER_SLOT];
    }

    public float getScrollPosition() {
        return (mCamera.mLookAtX * mCamera.mScale + sDeltaAnchorPosition.x); // in
        // pixels
    }

    public DisplayItem getDisplayItemForScrollPosition(float posX) {
        Pool<Vector3f> pool = sTempVecAlt;
        MediaFeed feed = mMediaFeed;
        int itemWidth = mCamera.mItemWidth;
        int itemHeight = mCamera.mItemHeight;
        GridLayoutInterface gridInterface = (GridLayoutInterface) mLayoutInterface;
        float absolutePosX = posX;
        int left = (int) ((absolutePosX / itemWidth) * gridInterface.mNumRows);
        int right = feed == null ? 0 : (int) (feed.getNumSlots());
        int retSlot = left;
        Vector3f position = pool.create();
        try {
            for (int i = left; i < right; ++i) {
                gridInterface.getPositionForSlotIndex(i, itemWidth, itemHeight, position);
                retSlot = i;
                if (position.x >= absolutePosX) {
                    break;
                }
            }
        } finally {
            pool.delete(position);
        }
        if (mFeedAboutToChange) {
            return null;
        }
        right = feed == null ? 0 : feed.getNumSlots();
        if (right == 0) {
            return null;
        }

        if (retSlot >= right)
            retSlot = right - 1;
        MediaSet set = feed.getSetForSlot(retSlot);
        if (set != null) {
            ArrayList<MediaItem> items = set.getItems();
            if (items != null && set.getNumItems() > 0) {
                return (sDisplayList.get(items.get(0)));
            }
        }
        return null;
    }

    // Returns the top left-most item.
    public int getAnchorSlotIndex(int anchorType) {
        int retVal = 0;
        switch (anchorType) {
        case ANCHOR_LEFT:
            retVal = sVisibleRange.begin;
            break;
        case ANCHOR_RIGHT:
            retVal = sVisibleRange.end;
            break;
        case ANCHOR_CENTER:
            retVal = (sVisibleRange.begin + sVisibleRange.end) / 2;
            break;
        }
        return retVal;
    }

    DisplayItem getDisplayItemForSlotId(int slotId) {
        int index = slotId - sBufferedVisibleRange.begin;
        if (index >= 0 && slotId <= sBufferedVisibleRange.end) {
            return sDisplayItems[index * MAX_ITEMS_PER_SLOT];
        }
        return null;
    }

    boolean changeFocusToNextSlot(float convergence) {
        int currentSelectedSlot = mInputProcessor.getCurrentSelectedSlot();
        boolean retVal = changeFocusToSlot(currentSelectedSlot + 1, convergence);
        if (mInputProcessor.getCurrentSelectedSlot() == currentSelectedSlot) {
            endSlideshow();
            mHud.setAlpha(1.0f);
        }
        return retVal;
    }

    boolean changeFocusToSlot(int slotId, float convergence) {
        mZoomValue = 1.0f;
        int index = slotId - sBufferedVisibleRange.begin;
        if (index >= 0 && slotId <= sBufferedVisibleRange.end) {
            DisplayItem displayItem = sDisplayItems[index * MAX_ITEMS_PER_SLOT];
            if (displayItem != null) {
                MediaItem item = displayItem.mItemRef;
                mHud.fullscreenSelectionChanged(item, slotId + 1, sCompleteRange.end + 1);
                if (slotId != Shared.INVALID && slotId <= sCompleteRange.end) {
                    mInputProcessor.setCurrentFocusSlot(slotId);
                    centerCameraForSlot(slotId, convergence);
                    return true;
                } else {
                    centerCameraForSlot(mInputProcessor.getCurrentSelectedSlot(), convergence);
                    return false;
                }
            }
        }
        return false;
    }

    boolean changeFocusToPreviousSlot(float convergence) {
        return changeFocusToSlot(mInputProcessor.getCurrentSelectedSlot() - 1, convergence);
    }

    public ArrayList<MediaBucket> getSelectedBuckets() {
        return sSelectedBucketList.get();
    }

    public void selectAll() {
        if (mState != STATE_FULL_SCREEN) {
            int numSlots = sCompleteRange.end + 1;
            for (int i = 0; i < numSlots; ++i) {
                addSlotToSelectedItems(i, false, false);
            }
            updateCountOfSelectedItems();
        } else {
            addSlotToSelectedItems(mInputProcessor.getCurrentFocusSlot(), false, true);
        }
    }

    public void deselectOrCancelSelectMode() {
        if (sSelectedBucketList.size() == 0) {
            mHud.cancelSelection();
        } else {
            sSelectedBucketList.clear();
            updateCountOfSelectedItems();
        }
    }

    public void deselectAll() {
        mHud.cancelSelection();
        sSelectedBucketList.clear();
        updateCountOfSelectedItems();
    }

    public void deleteSelection() {
        // Delete the selection and exit selection mode.
        mMediaFeed.performOperation(MediaFeed.OPERATION_DELETE, getSelectedBuckets(), null);
        deselectAll();

        // If the current set is now empty, return to the parent set.
        if (sCompleteRange.isEmpty()) {
            goBack(); // TODO(venkat): This does not work most of the time, can
            // you take a look?
        }
    }

    void addSlotToSelectedItems(int slotId, boolean removeIfAlreadyAdded, boolean updateCount) {
        if (mFeedAboutToChange == false) {
            MediaFeed feed = mMediaFeed;
            sSelectedBucketList.add(slotId, feed, removeIfAlreadyAdded);
            if (updateCount) {
                updateCountOfSelectedItems();
                if (sSelectedBucketList.size() == 0)
                    deselectAll();
            }
        }
        mHud.computeBottomMenu();
    }

    private void updateCountOfSelectedItems() {
        mHud.updateNumItemsSelected(sSelectedBucketList.size());
    }

    public int getMetadataSlotIndexForScreenPosition(int posX, int posY) {
        return getSlotForScreenPosition(posX, posY, mCamera.mItemWidth + (int) (100 * Gallery.PIXEL_DENSITY), mCamera.mItemHeight
                + (int) (100 * Gallery.PIXEL_DENSITY));
    }

    public int getSlotIndexForScreenPosition(int posX, int posY) {
        return getSlotForScreenPosition(posX, posY, mCamera.mItemWidth, mCamera.mItemHeight);
    }

    private int getSlotForScreenPosition(int posX, int posY, int itemWidth, int itemHeight) {
        Pool<Vector3f> pool = sTempVec;
        int retVal = 0;
        Vector3f worldPos = pool.create();
        try {
            GridCamera camera = mCamera;
            camera.convertToCameraSpace(posX, posY, 0, worldPos);
            // slots are expressed in pixels as well
            worldPos.x *= camera.mScale;
            worldPos.y *= camera.mScale;
            // we ignore z
            retVal = hitTest(worldPos, itemWidth, itemHeight);
        } finally {
            pool.delete(worldPos);
        }
        return retVal;
    }

    public boolean tapGesture(int slotIndex, boolean metadata) {
        MediaFeed feed = mMediaFeed;
        if (!feed.isClustered()) {
            // It is not clustering.
            if (!feed.hasExpandedMediaSet()) {
                if (feed.canExpandSet(slotIndex)) {
                    mCurrentExpandedSlot = slotIndex;
                    feed.expandMediaSet(slotIndex);
                    setState(STATE_GRID_VIEW);
                }
                return false;
            } else {
                return true;
            }
        } else {
            // Select a cluster, and recompute a new cluster within this
            // cluster.
            mCurrentExpandedSlot = slotIndex;
            sMarkedBucketList.clear();
            sMarkedBucketList.add(slotIndex, feed, false);
            goBack();
            if (metadata) {
                DisplaySlot slot = sDisplaySlots[slotIndex - sBufferedVisibleRange.begin];
                if (slot.hasValidLocation()) {
                    MediaSet set = slot.getMediaSet();
                    if (set.mReverseGeocodedLocation != null) {
                        enableLocationFiltering(set.mReverseGeocodedLocation);
                    }
                    feed.setFilter(new LocationMediaFilter(set.mMinLatLatitude, set.mMinLonLongitude, set.mMaxLatLatitude,
                            set.mMaxLonLongitude));
                }
            }
            return false;
        }
    }

    public void onTimeChanged(TimeBar timebar) {
        if (mFeedAboutToChange) {
            return;
        }
        // TODO lot of optimization possible here
        MediaItem item = timebar.getItem();
        MediaFeed feed = mMediaFeed;
        int numSlots = feed.getNumSlots();
        for (int i = 0; i < numSlots; ++i) {
            MediaSet set = feed.getSetForSlot(i);
            if (set == null) {
                return;
            }
            ArrayList<MediaItem> items = set.getItems();
            if (items == null || set.getNumItems() == 0) {
                return;
            }
            if (items.contains(item)) {
                centerCameraForSlot(i, 1.0f);
                break;
            }
        }
    }

    public void onFeedAboutToChange(MediaFeed feed) {
        mFeedAboutToChange = true;
        mTimeElapsedSinceTransition = 0;
    }

    public void startSlideshow() {
        endSlideshow();
        mSlideshowMode = true;
        mZoomValue = 1.0f;
        centerCameraForSlot(mInputProcessor.getCurrentSelectedSlot(), 1.0f);
        mTimeElapsedSinceView = SLIDESHOW_TRANSITION_TIME - 1.0f;
        mHud.setAlpha(0);
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "GridView.Slideshow");
        mWakeLock.acquire();
    }

    public void enterSelectionMode() {
        mSlideshowMode = false;
        mHud.enterSelectionMode();
        int currentSlot = mInputProcessor.getCurrentSelectedSlot();
        if (currentSlot == Shared.INVALID) {
            currentSlot = mInputProcessor.getCurrentFocusSlot();
        }
        addSlotToSelectedItems(currentSlot, false, true);
    }

    private float getFillScreenZoomValue() {
        return GridCameraManager.getFillScreenZoomValue(mCamera, sTempVec, mCurrentFocusItemWidth, mCurrentFocusItemHeight);
    }

    public void zoomInToSelectedItem() {
        mSlideshowMode = false;
        float potentialZoomValue = getFillScreenZoomValue();
        if (mZoomValue < potentialZoomValue) {
            mZoomValue = potentialZoomValue;
        } else {
            mZoomValue *= 3.0f;
        }
        if (mZoomValue > 6.0f) {
            mZoomValue = 6.0f;
        }
        mHud.setAlpha(1.0f);
        centerCameraForSlot(mInputProcessor.getCurrentSelectedSlot(), 1.0f);
    }

    public void zoomOutFromSelectedItem() {
        mSlideshowMode = false;
        if (mZoomValue == getFillScreenZoomValue()) {
            mZoomValue = 1.0f;
        } else {
            mZoomValue /= 3.0f;
        }
        if (mZoomValue < 1.0f) {
            mZoomValue = 1.0f;
        }
        mHud.setAlpha(1.0f);
        centerCameraForSlot(mInputProcessor.getCurrentSelectedSlot(), 1.0f);
    }

    public void rotateSelectedItems(float f) {
        MediaBucketList bucketList = sSelectedBucketList;
        ArrayList<MediaBucket> mediaBuckets = bucketList.get();
        DisplayList displayList = sDisplayList;
        int numBuckets = mediaBuckets.size();
        for (int i = 0; i < numBuckets; ++i) {
            MediaBucket bucket = mediaBuckets.get(i);
            ArrayList<MediaItem> mediaItems = bucket.mediaItems;
            if (mediaItems != null) {
                int numMediaItems = mediaItems.size();
                for (int j = 0; j < numMediaItems; ++j) {
                    MediaItem item = mediaItems.get(j);
                    DisplayItem displayItem = displayList.get(item);
                    displayItem.rotateImageBy(f);
                    displayList.addToAnimatables(displayItem);
                }
            }
        }
        if (mState == STATE_FULL_SCREEN) {
            centerCameraForSlot(mInputProcessor.getCurrentSelectedSlot(), 1.0f);
        }
        mMediaFeed.performOperation(MediaFeed.OPERATION_ROTATE, mediaBuckets, new Float(f));
        // we recreate these displayitems from the cache
    }

    public void cropSelectedItem() {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mInputProcessor.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mInputProcessor != null)
            return mInputProcessor.onKeyDown(keyCode, event, mState);
        return false;
    }

    public boolean inSlideShowMode() {
        return mSlideshowMode;
    }

    public boolean noDeleteMode() {
        return mNoDeleteMode || (mMediaFeed != null && mMediaFeed.isSingleImageMode());
    }

    public float getZoomValue() {
        return mZoomValue;
    }

    public boolean feedAboutToChange() {
        return mFeedAboutToChange;
    }

    public boolean isInAlbumMode() {
        return mInAlbum;
    }

    public Vector3f getDeltaAnchorPosition() {
        return sDeltaAnchorPosition;
    }

    public int getExpandedSlot() {
        return mCurrentExpandedSlot;
    }

    public GridLayoutInterface getLayoutInterface() {
        return (GridLayoutInterface) mLayoutInterface;
    }

    public void setZoomValue(float f) {
        mZoomValue = f;
        centerCameraForSlot(mInputProcessor.getCurrentSelectedSlot(), 1.0f);
    }

    public void setPickIntent(boolean b) {
        mPickIntent = b;
        mHud.getPathBar().popLabel();
        mHud.getPathBar().pushLabel(R.drawable.icon_location_small, mContext.getResources().getString(R.string.pick),
                new Runnable() {
                    public void run() {
                        if (mHud.getAlpha() == 1.0f) {
                            if (!mFeedAboutToChange) {
                                setState(STATE_MEDIA_SETS);
                            }
                        } else {
                            mHud.setAlpha(1.0f);
                        }
                    }
                });
    }

    public boolean getPickIntent() {
        return mPickIntent;
    }

    public void setViewIntent(boolean b, final String setName) {
        mViewIntent = b;
        if (b) {
            mMediaFeed.expandMediaSet(0);
            setState(STATE_GRID_VIEW);
            // We need to make sure we haven't pushed the same label twice
            if (mHud.getPathBar().getNumLevels() == 1) {
                mHud.getPathBar().pushLabel(R.drawable.icon_folder_small, setName, new Runnable() {
                    public void run() {
                        if (mFeedAboutToChange) {
                            return;
                        }
                        if (mHud.getAlpha() == 1.0f) {
                            disableLocationFiltering();
                            if (mInputProcessor != null)
                                mInputProcessor.clearSelection();
                            setState(STATE_GRID_VIEW);
                        } else {
                            mHud.setAlpha(1.0f);
                        }
                    }
                });
            }
        }
    }

    public boolean getViewIntent() {
        return mViewIntent;
    }

    public void setSingleImage(boolean noDeleteMode) {
        mNoDeleteMode = noDeleteMode;
        mInputProcessor.setCurrentSelectedSlot(0);
    }

    public MediaFeed getFeed() {
        return mMediaFeed;
    }

    public void markDirty(int numFrames) {
        mFramesDirty = numFrames;
    }

    public void focusItem(String contentUri) {
        mRequestFocusContentUri = contentUri;
        mMediaFeed.updateListener(false);
    }

}
