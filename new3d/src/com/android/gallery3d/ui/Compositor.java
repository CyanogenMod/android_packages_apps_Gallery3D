package com.android.gallery3d.ui;

import com.android.gallery3d.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Compositor extends GLView {
    private static final int MARGIN_HUD_SLOTVIEW = 5;
    private static final int HORIZONTAL_GAP_SLOTS = 5;
    private static final int VERTICAL_GAP_SLOTS = 5;
    private AdaptiveBackground mBackground;
    private SlotView mSlotView;
    private HeadUpDisplay mHud;

    private Bitmap mBgImages[];
    private int mBgIndex = 0;

    private Context mContext;

    public Compositor(Context context) {
        mContext = context;
        initialize();
    }

    private void initialize() {
        mBackground = new AdaptiveBackground();
        addComponent(mBackground);
        mSlotView = new SlotView(mContext);
        addComponent(mSlotView);
        mHud = new HeadUpDisplay(mContext);
        addComponent(mHud);
        mSlotView.setGaps(HORIZONTAL_GAP_SLOTS, VERTICAL_GAP_SLOTS);

        loadBackgroundBitmap(R.drawable.square,
                R.drawable.potrait, R.drawable.landscape);
        mBackground.setImage(mBgImages[mBgIndex]);
    }

    public SlotView getSlotView() {
        return mSlotView;
    }

    @Override
    protected void onLayout(
            boolean changed, int left, int top, int right, int bottom) {
        mBackground.layout(0, 0, right - left, bottom - top);
        mHud.layout(0, 0, right - left, bottom - top);

        int slotViewTop = mHud.getTopBarBottomPosition() + MARGIN_HUD_SLOTVIEW;
        int slotViewBottom = mHud.getBottomBarTopPosition() - MARGIN_HUD_SLOTVIEW;

        mSlotView.layout(0, slotViewTop, right - left, slotViewBottom);
    }

    public void changeBackground() {
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
}
