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

package com.android.gallery3d.ui;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.ui.MenuExecutor.MediaOperation;
import com.android.gallery3d.ui.MenuExecutor.OnProgressUpdateListener;

import java.util.ArrayList;
import java.util.List;

public class HudMenu implements HudMenuInterface, SelectionManager.SelectionListener {
    private static final String TAG = "HudMenu";
    private static final int DELETE_MODEL = 0;
    private static final int MORE_MODEL = 1;
    private static final int SHARE_IMAGE_MODEL = 2;
    private static final int SHARE_VIDEO_MODEL = 3;
    private static final int SHARE_ALL_MODEL = 4;
    private static final int TOTAL_MODEL_COUNT = 5;

    GalleryContext mContext;
    MenuBar mTopBar;
    MenuItemBar mBottomBar;
    NinePatchTexture mHighlight;
    SelectionManager mSelectionManager;
    MenuModel[] mMenuModels;
    MenuExecutor mMenuExecutor;
    MenuItem mShare;
    MenuItem mDelete;
    MenuItem mMore;
    ProgressUpdateDialog mDialog;

    private class ProgressUpdateDialog extends ProgressDialog implements OnProgressUpdateListener {
        Handler mUiHandler;
        MediaOperation mOperation;
        boolean mIsDead;

        public ProgressUpdateDialog(int title, int total) {
            super(mContext.getAndroidContext());
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mUiHandler = new Handler();
            setTitle(title);
            setMax(total);
            show();
        }

        /**
         * The following two methods are callbacks from data thread.
         */
        public void onProgressUpdate(final int index, Object result) {
            mUiHandler.post(new Runnable() {
                public void run() {
                    if (!mIsDead) setProgress(index);
                }
            });
        }

        public void onProgressComplete() {
            // SourceMediaSet remains unchanged since SelectionManager is created so race condition
            // between UI and data threads is not a concern here.
            // reload has to be called from data thread.
            mSelectionManager.getSourceMediaSet().reload();
            // dismiss() can be called from any thread.
            dismiss();
        }

        // This will be called when back is pressed or dismiss() is called.
        protected void onStop() {
            stop();
            mSelectionManager.leaveSelectionMode();
        }

        public void stop() {
            if (!mIsDead) {
                mIsDead = true;
                mOperation.cancel();
            }
        }

        public void setOperation(MediaOperation operation) {
            mOperation = operation;
        }
    }

    public HudMenu(GalleryContext context, SelectionManager manager) {
        mContext = context;
        mSelectionManager = manager;
        mHighlight = new NinePatchTexture(context.getAndroidContext(), R.drawable.menu_highlight);
        manager.setSelectionListener(this);
        mMenuModels = new MenuModel[TOTAL_MODEL_COUNT];
        mMenuExecutor = new MenuExecutor(context.getDataManager());
    }

    public void onPause() {
        if (mDialog != null) {
            mDialog.stop();
            mDialog = null;
        }
    }

    public MenuBar getTopMenuBar() {
        if (mTopBar == null) createTopMenuBar();
        return mTopBar;
    }

    public MenuItemBar getBottomMenuBar() {
        if (mBottomBar == null) createBottomMenuBar();
        return mBottomBar;
    }

    protected void createTopMenuBar() {
        Context context = mContext.getAndroidContext();
        mTopBar = new MenuBar(GLView.INVISIBLE);
        mTopBar.setBackground(new NinePatchTexture(context, R.drawable.top_menu_bar_bg));

        // Select all
        MenuButton btn = new MenuButton(context, IconLabel.NULL_ID, R.string.select_all);
        btn.setHighlight(mHighlight);
        btn.setOnClickListener(new MenuButton.OnClickedListener() {
            public void onClicked(GLView source) {
                mSelectionManager.selectAll();
            }
        });
        mTopBar.addComponent(btn);

        // Deselect all
        mTopBar.addComponent(new IconLabel(context, IconLabel.NULL_ID, R.string.items));
        btn = new MenuButton(context, IconLabel.NULL_ID, R.string.deselect_all);
        btn.setHighlight(mHighlight);
        btn.setOnClickListener(new MenuButton.OnClickedListener() {
            public void onClicked(GLView source) {
                mSelectionManager.deSelectAll();
            }
        });
        mTopBar.addComponent(btn);
    }

    public class MenuModel implements GLListView.Model {
        ArrayList<MenuItem> mItems;
        public MenuModel() {
            mItems = new ArrayList<MenuItem>();
        }

        public MenuModel(MenuItem[] items) {
            mItems = new ArrayList<MenuItem>(items.length);
            for (MenuItem item : items) {
                mItems.add(item);
            }
        }

        public void addItem(MenuItem item) {
            mItems.add(item);
        }

        public GLView getView(int index) {
            return mItems.get(index);
        }

        public boolean isSelectable(int index) {
            return true;
        }

        public int size() {
            return mItems.size();
        }

        /**
         * Handle the menu operation here.
         */
        public void onItemSelected(int position) {
            MenuItem item = (MenuItem) getView(position);
            int title;
            int action = item.getItemId();
            switch (action) {
                case MenuExecutor.ACTION_DELETE:
                    title = R.string.delete;
                    break;
                case MenuExecutor.ACTION_ROTATE_CW:
                    title = R.string.rotate_right;
                    break;
                case MenuExecutor.ACTION_ROTATE_CCW:
                    title = R.string.rotate_left;
                    break;
                case MenuExecutor.ACTION_DETAILS:
                    title = R.string.details;
                    break;
                default:
                    return;
            }

            ArrayList<Long> ids = mSelectionManager.getSelected(false);
            if (mDialog != null) mDialog.stop();
            mDialog = new ProgressUpdateDialog(title, ids.size());
            MediaOperation operation = mMenuExecutor.startMediaOperation(action, ids, mDialog);
            mDialog.setOperation(operation);
        }
    }

    private class ShareModel extends MenuModel {
        List<ResolveInfo> mInfo;
        Intent mIntent;

        public ShareModel(int shareType) {
            Context context = mContext.getAndroidContext();
            mIntent = new Intent(Intent.ACTION_SEND);
            switch (shareType) {
                case SHARE_IMAGE_MODEL:
                    mIntent.setType("image/*");
                    break;
                case SHARE_VIDEO_MODEL:
                    mIntent.setType("video/*");
                    break;
                default:
                    mIntent.setType("*/*");
            }
            PackageManager packageManager = context.getPackageManager();
            mInfo = packageManager.queryIntentActivities(mIntent, 0);
            for(ResolveInfo info : mInfo) {
                String label = info.loadLabel(packageManager).toString();
                Drawable icon = info.loadIcon(packageManager);
                MenuItem item = createMenuItem(context, icon, label);
                addItem(item);
            }
        }

        private MenuItem createMenuItem(Context context, Drawable icon, String title) {
            DrawableTexture iconTexture = null;
            if (icon != null) {
                iconTexture = new DrawableTexture(icon);
                float target = 45;
                int width = icon.getIntrinsicWidth();
                int height = icon.getIntrinsicHeight();
                float scale = target / Math.max(width, height);
                iconTexture.setSize((int) (width * scale + .5f),
                        (int) (height * scale + .5f));
            }
            MenuItem item = new MenuItem(context, iconTexture, title);
            item.setHighlight(mHighlight);
            return item;
        }

        public void onItemSelected(int position) {
            ResolveInfo info = mInfo.get(position);
            ArrayList<Long> items = mSelectionManager.getSelected(true);
            ArrayList<Uri> uris = new ArrayList<Uri>(items.size());
            DataManager manager = mContext.getDataManager();
            for (Long id : items) {
                if ((manager.getSupportedOperations(id) & MediaSet.SUPPORT_SHARE) != 0) {
                    uris.add(manager.getMediaItemUri(id));
                }
            }

            if (uris.isEmpty()) {
                // TODO: explain to user that some items can't be shared.
                throw new UnsupportedOperationException();
            } else if (uris.size() == 1) {
                mIntent.setAction(Intent.ACTION_SEND);
                mIntent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            } else {
                mIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                mIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            }
            ActivityInfo ai = info.activityInfo;
            mIntent.setComponent(new ComponentName(ai.packageName, ai.name));
            mContext.getAndroidContext().startActivity(mIntent);
        }
    }

    protected void createBottomMenuBar() {
        Context context = mContext.getAndroidContext();
        mBottomBar = new MenuItemBar(GLView.INVISIBLE);
        mBottomBar.setBackground(
                new NinePatchTexture(context, R.drawable.top_menu_bar_bg));

        // Share menu
        mShare = new MenuItem(context, R.drawable.icon_share, R.string.share, mHighlight);
        mBottomBar.addComponent(mShare);

        // Delete menu & its submenu
        mDelete = new MenuItem(context, R.drawable.icon_delete, R.string.delete
                , mHighlight);
        mMenuModels[DELETE_MODEL] = new MenuModel(new MenuItem[] {
                new MenuItem(context, R.drawable.icon_delete, R.string.confirm_delete,
                        mHighlight, MenuExecutor.ACTION_DELETE),
                new MenuItem(context, R.drawable.icon_cancel, R.string.cancel,
                        mHighlight)
         });
        mBottomBar.addComponent(mDelete);

        // More menu & its submenu
        mMore = new MenuItem(context, R.drawable.icon_more, R.string.more, mHighlight);
        mMenuModels[MORE_MODEL] = new MenuModel(new MenuItem[] {
                new MenuItem(context, R.drawable.icon_details, R.string.details,
                        mHighlight, MenuExecutor.ACTION_DETAILS),
                new MenuItem(context, R.drawable.icon_details, R.string.rotate_right,
                        mHighlight, MenuExecutor.ACTION_ROTATE_CW),
                new MenuItem(context, R.drawable.icon_details, R.string.rotate_left,
                        mHighlight, MenuExecutor.ACTION_ROTATE_CCW),
        });
        mBottomBar.addComponent(mMore);
    }

    private int getShareType() {
        ArrayList<Long> items = mSelectionManager.getSelected(false);
        DataManager manager = mContext.getDataManager();
        int type = 0;

        for (long id : items) {
            type |= manager.getMediaType(id);
        }

        switch (type) {
            case MediaSet.MEDIA_TYPE_IMAGE:
                return SHARE_IMAGE_MODEL;
            case MediaSet.MEDIA_TYPE_VIDEO:
                return SHARE_VIDEO_MODEL;
            default:
                return SHARE_ALL_MODEL;
        }
    }

    public GLListView.Model getMenuModel(GLView item) {
         if (item == mDelete) {
            return mMenuModels[DELETE_MODEL];
        } else if (item == mMore) {
            return mMenuModels[MORE_MODEL];
        } else if (item == mShare) {
            int shareType = getShareType();
            if (mMenuModels[shareType] == null) {
                mMenuModels[shareType] = new ShareModel(shareType);
            }
            return mMenuModels[shareType];
        }

        return null;
    }

    public void onMenuItemSelected(GLListView.Model listViewModel, int position) {
        MenuModel model = (MenuModel) listViewModel;
        model.onItemSelected(position);
    }

    public void onSelectionModeChange(boolean inSelectionMode) {
        int visibility = inSelectionMode ? GLView.VISIBLE : GLView.INVISIBLE;
        getTopMenuBar().setVisibility(visibility);
        getBottomMenuBar().setVisibility(visibility);
    }
}
