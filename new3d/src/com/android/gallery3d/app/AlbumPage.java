/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;

import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaOperation;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.ui.AdaptiveBackground;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.GridSlotAdapter;
import com.android.gallery3d.ui.HeadUpDisplay;
import com.android.gallery3d.ui.MenuHandler;
import com.android.gallery3d.ui.MenuItem;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;

import java.util.Set;

public class AlbumPage extends ActivityState implements SlotView.SlotTapListener {
    private static final String TAG = "AlbumPage";
    public static final String KEY_BUCKET_INDEX = "keyBucketIndex";
    private static final int CHANGE_BACKGROUND = 1;
    private static final int MARGIN_HUD_SLOTVIEW = 5;
    private static final int HORIZONTAL_GAP_SLOTS = 5;
    private static final int VERTICAL_GAP_SLOTS = 5;

    private AdaptiveBackground mBackground;
    private SlotView mSlotView;
    private HeadUpDisplay mHud;
    private SynchronizedHandler mHandler;
    private Bitmap mBgImages[];
    private int mBgIndex = 0;
    private int mBucketIndex;
    protected SelectionManager mSelectionManager;
    private MediaSet mMediaSet;
    private MenuHandler mMenuHandler;

    private GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            mBackground.layout(0, 0, right - left, bottom - top);
            mHud.layout(0, 0, right - left, bottom - top);

            int slotViewTop = mHud.getTopBarBottomPosition() + MARGIN_HUD_SLOTVIEW;
            int slotViewBottom = mHud.getBottomBarTopPosition() - MARGIN_HUD_SLOTVIEW;
            mSlotView.layout(0, slotViewTop, right - left, slotViewBottom);
        }
    };

    @Override
    public void onBackPressed() {
        if (mSelectionManager.isSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSingleTapUp(int slotIndex) {
        if (!mSelectionManager.isSelectionMode()) {
            Bundle data = new Bundle();
            data.putInt(PhotoPage.KEY_SET_INDEX, mBucketIndex);
            data.putInt(PhotoPage.KEY_PHOTO_INDEX, slotIndex);

            mContext.getStateManager().startState(PhotoPage.class, data);
        } else {
            mSelectionManager.selectSlot(slotIndex);
            mSlotView.invalidate();
        }
    }

    @Override
    public void onLongTap(int slotIndex) {
        mSelectionManager.switchSelectionMode(slotIndex);
        mSlotView.invalidate();
    }

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        initializeViews();
        intializeData(data);

        mHandler = new SynchronizedHandler(mContext.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case CHANGE_BACKGROUND:
                        changeBackground();
                        mHandler.sendEmptyMessageDelayed(
                                CHANGE_BACKGROUND, 3000);
                        break;
                }
            }
        };

        mMenuHandler = new MenuHandler(mContext);
    }

    @Override
    public void onPause() {
        mHandler.removeMessages(CHANGE_BACKGROUND);
        mMenuHandler.onPause();
    }

    private void initializeViews() {
        mBackground = new AdaptiveBackground();
        mRootPane.addComponent(mBackground);
        mSlotView = new SlotView(mContext.getAndroidContext(), mContext.getPositionRepository());

        mSelectionManager = new SelectionManager(mContext.getAndroidContext());
        mRootPane.addComponent(mSlotView);
        mHud = new HeadUpDisplay(mContext.getAndroidContext()) {
            @Override
            protected void initializeMenu() {
                MenuItem share = addBottomMenuItem(R.drawable.icon_share,
                        R.string.share);
                MenuItem delete = addBottomMenuItem(R.drawable.icon_delete,
                        R.string.delete);
                MenuItem more = addBottomMenuItem(R.drawable.icon_more, R.string.more);

                //TODO: refactor to support dynamic submenu construction.
                buildShareMenu(share);
                delete.addMenuItem(R.drawable.icon_delete, R.string.confirm_delete)
                        .addClickListener(new MenuItem.MenuItemClickListener() {
                            @Override
                            public void onClick() {
                                Set<Integer> set = mSelectionManager.getSelectedSet();
                                if (set.size() == 0) return;

                                mMenuHandler.handleMediaItemOperation(
                                        MediaOperation.DELETE, R.string.delete, mMediaSet,
                                        set, false);

                                mSelectionManager.leaveSelectionMode();
                            }
                        });
                delete.addMenuItem(R.drawable.icon_cancel, R.string.cancel);
                more.addMenuItem(R.drawable.icon_details, R.string.details);
            }
        };
        mRootPane.addComponent(mHud);
        mSlotView.setSlotGaps(HORIZONTAL_GAP_SLOTS, VERTICAL_GAP_SLOTS);
        mSlotView.setSlotTapListener(this);

        loadBackgroundBitmap(R.drawable.square,
                R.drawable.potrait, R.drawable.landscape);
        mBackground.setImage(mBgImages[mBgIndex]);
    }

    private void intializeData(Bundle data) {
        mBucketIndex = data.getInt(KEY_BUCKET_INDEX);
        mMediaSet = mContext.getDataManager().getSubMediaSet(mBucketIndex);
        mSlotView.setListener(new GridSlotAdapter(mContext.getAndroidContext(),
                mMediaSet, mSlotView, mSelectionManager));
    }

    private void changeBackground() {
        mBackground.setImage(mBgImages[mBgIndex]);
        if (++mBgIndex == mBgImages.length) mBgIndex = 0;
    }

    private void loadBackgroundBitmap(int ... ids) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        mBgImages = new Bitmap[ids.length];
        Resources res = mContext.getResources();
        for (int i = 0, n = ids.length; i < n; ++i) {
            Bitmap bitmap = BitmapFactory.decodeResource(res, ids[i], options);
            mBgImages[i] = mBackground.getAdaptiveBitmap(bitmap);
            bitmap.recycle();
        }
    }

    @Override
    public void onResume() {
        mHandler.sendEmptyMessage(CHANGE_BACKGROUND);
        setContentPane(mRootPane);
    }
}
