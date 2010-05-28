package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;

import java.util.Random;

public class MediaSetSlotAdapter implements SlotView.Model {
    private static final int LENGTH_LIMIT = 180;
    private static final double EXPECTED_AREA = LENGTH_LIMIT * LENGTH_LIMIT / 2;
    private static final int SLOT_WIDTH = 220;
    private static final int SLOT_HEIGHT = 200;
    private static final int MARGIN_TO_SLOTSIDE = 10;

    private final NinePatchTexture mFrame;

    private final Random mRandom = new Random();

    private MediaSet mRootSet;
    private Context mContext;


    public MediaSetSlotAdapter(Context context, MediaSet rootSet) {
        mContext = context;
        mRootSet = rootSet;
        mFrame = new NinePatchTexture(context, R.drawable.stack_frame);
        Random random = mRandom;
    }

    public void freeSlot(int index) {
    }

    public void putSlot(int slotIndex, int x, int y, DisplayItemPanel panel) {
        MediaSet set = mRootSet.getSubMediaSet(slotIndex);
        MediaItem[] items = set.getCoverMediaItems();
        Random random = mRandom;
        int left = x + MARGIN_TO_SLOTSIDE;
        int right = x + getSlotWidth() - MARGIN_TO_SLOTSIDE;

        x += getSlotWidth() / 2;
        y += getSlotHeight() / 2;

        // Put the cover items in reverse order, so that the first item is on
        // top of the rest.
        for (int i = items.length -1; i > 0; --i) {
            Bitmap bitmap = items[i].getImage(mContext.getContentResolver(),
                    MediaItem.TYPE_THUMBNAIL);
            DisplayItem displayItem = new MyDisplayItem(
                    new ImageTexture(bitmap), mFrame);
            int dx = random.nextInt(11) - 5;
            int itemX = (i & 0x01) == 0
                    ? left + dx + displayItem.getWidth() / 2
                    : right + dx - displayItem.getWidth() / 2;
            int dy = random.nextInt(11) - 10;
            int theta = random.nextInt(31) - 15;
            panel.putDisplayItem(displayItem, itemX, y + dy, theta);
        }
        Bitmap bitmap = items[0].getImage(mContext.getContentResolver(),
                MediaItem.TYPE_THUMBNAIL);
        DisplayItem displayItem = new MyDisplayItem(
                new ImageTexture(bitmap), mFrame);
        panel.putDisplayItem(displayItem, x, y, 0);
    }

    public int getSlotHeight() {
        return SLOT_HEIGHT;
    }

    public int getSlotWidth() {
        return SLOT_WIDTH;
    }

    public int size() {
        return mRootSet.getSubMediaSetCount();
    }

    private static class MyDisplayItem extends DisplayItem {

        private final BasicTexture mContent;
        private final NinePatchTexture mFrame;

        public MyDisplayItem(BasicTexture content, NinePatchTexture frame) {
            mContent = content;
            mFrame = frame;

            Rect p = frame.getPaddings();

            int width = mContent.getWidth();
            int height = mContent.getHeight();

            float scale = (float) Math.sqrt(EXPECTED_AREA / (width * height));
            width = (int) (width * scale + 0.5f);
            height = (int) (height * scale + 0.5f);

            int widthLimit = LENGTH_LIMIT - p.left - p.right;
            int heightLimit = LENGTH_LIMIT - p.top - p.bottom;

            if (width > widthLimit || height > heightLimit) {
                if (width * heightLimit > height * widthLimit) {
                    height = height * widthLimit / width;
                    width = widthLimit;
                } else {
                    width = width * heightLimit / height;
                    height = heightLimit;
                }
            }
            setSize(width + p.left + p.right, height + p.top + p.bottom);
        }

        @Override
        public void render(GLRootView root) {
            int x = -mWidth / 2;
            int y = -mHeight / 2;

            Rect p = mFrame.getPaddings();
            mContent.draw(root, x + p.left, y + p.top,
                    mWidth - p.left - p.right, mHeight - p.top - p.bottom);
            mFrame.draw(root, x, y, mWidth, mHeight);
        }
    }

    public void freeSlot(int index, DisplayItemPanel panel) {
        // TODO: Need to implement the method.
    }

}

