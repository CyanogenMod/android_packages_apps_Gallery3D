package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;

import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;

public class MediaSetSlotAdapter implements SlotView.Model {
    private static final int LENGTH_LIMIT = 180;
    private static final double EXPECTED_AREA = LENGTH_LIMIT * LENGTH_LIMIT / 2;
    private static final int SLOT_WIDTH = 220;
    private static final int SLOT_HEIGHT = 200;
    private static final int MARGIN_TO_SLOTSIDE = 10;
    private static final int INITIAL_CACHE_CAPACITY = 32;

    private final NinePatchTexture mFrame;

    private final Random mRandom = new Random();

    private final MediaSet mRootSet;
    private final Texture mWaitLoadingTexture;

    private final Map<Integer, MyDisplayItem[]> mItemsetMap =
            new HashMap<Integer, MyDisplayItem[]>(INITIAL_CACHE_CAPACITY);
    private final LinkedHashSet<Integer> mLruSlot =
            new LinkedHashSet<Integer>(INITIAL_CACHE_CAPACITY);
    private final SlotView mSlotView;

    public MediaSetSlotAdapter(
            Context context, MediaSet rootSet, SlotView view) {
        mRootSet = rootSet;
        mFrame = new NinePatchTexture(context, R.drawable.stack_frame);
        ColorTexture gray = new ColorTexture(Color.GRAY);
        gray.setSize(64, 48);
        mWaitLoadingTexture = gray;
        mSlotView = view;
    }

    public void putSlot(int slotIndex, int x, int y, DisplayItemPanel panel) {

        // Get displayItems from mItemsetMap or create them from MediaSet.
        MyDisplayItem[] displayItems = mItemsetMap.get(slotIndex);
        if (displayItems == null) {
            displayItems = createDisplayItems(slotIndex);
        }

        // Put displayItems to the panel.
        Random random = mRandom;
        int left = x + MARGIN_TO_SLOTSIDE;
        int right = x + getSlotWidth() - MARGIN_TO_SLOTSIDE;
        x += getSlotWidth() / 2;
        y += getSlotHeight() / 2;

        // Put the cover items in reverse order, so that the first item is on
        // top of the rest.
        for (int i = displayItems.length -1; i > 0; --i) {
            int dx = random.nextInt(11) - 5;
            int itemX = (i & 0x01) == 0
                    ? left + dx + displayItems[i].getWidth() / 2
                    : right + dx - displayItems[i].getWidth() / 2;
            int dy = random.nextInt(11) - 10;
            int theta = random.nextInt(31) - 15;
            panel.putDisplayItem(displayItems[i], itemX, y + dy, theta);
        }
        panel.putDisplayItem(displayItems[0], x, y, 0);
    }

    private MyDisplayItem[] createDisplayItems(int slotIndex) {
        MediaSet set = mRootSet.getSubMediaSet(slotIndex);
        MediaItem[] items = set.getCoverMediaItems();

        MyDisplayItem[] displayItems = new MyDisplayItem[items.length];
        for (int i = 0; i < items.length; ++i) {
            items[i].setListener(new MyMediaItemListener(slotIndex, i));
            switch (items[i].requestImage(MediaItem.TYPE_MICROTHUMBNAIL)) {
                case MediaItem.IMAGE_READY:
                    Bitmap bitmap =
                            items[i].getImage(MediaItem.TYPE_MICROTHUMBNAIL);
                    // TODO: Need to replace BitmapTexture with something else
                    // whose bitmap is freeable.
                    displayItems[i] = new MyDisplayItem(
                            new BitmapTexture(bitmap), mFrame);
                    break;
                default:
                    displayItems[i] =
                            new MyDisplayItem(mWaitLoadingTexture, mFrame);
                    break;
            }
        }
        addSlotToCache(slotIndex, displayItems);
        return displayItems;
    }

    private void addSlotToCache(int slotIndex, MyDisplayItem[] displayItems) {
        // Remove an itemset if the size of mItemsetMap is no less than
        // INITIAL_CACHE_CAPACITY and there exists a slot in mLruSlot.
        Iterator<Integer> iter = mLruSlot.iterator();
        for (int i = mItemsetMap.size() - INITIAL_CACHE_CAPACITY;
                i >= 0 && iter.hasNext(); --i) {
            mItemsetMap.remove(iter.next());
            iter.remove();
        }
        mItemsetMap.put(slotIndex, displayItems);
        mLruSlot.remove(slotIndex);
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

    private class MyMediaItemListener implements MediaItem.MediaItemListener {

        private final int mSlotIndex;
        private final int mItemIndex;

        public MyMediaItemListener(int slotIndex, int itemIndex) {
            mSlotIndex = slotIndex;
            mItemIndex = itemIndex;
        }

        @Override
        public void onImageCanceled(MediaItem abstractMediaItem, int type) {
            // Do nothing
        }

        @Override
        public void onImageError(MediaItem item, int type, Throwable error) {
            // Do nothing
        }

        @Override
        public void onImageReady(MediaItem item, int type, Bitmap bitmap) {
            MyDisplayItem[] items = mItemsetMap.get(mSlotIndex);
            items[mItemIndex].updateContent(new BitmapTexture(bitmap));
            mSlotView.notifyDataInvalidate();
        }
    }

    private static class MyDisplayItem extends DisplayItem {

        private Texture mContent;
        private final NinePatchTexture mFrame;

        public MyDisplayItem(Texture content, NinePatchTexture frame) {
            mFrame = frame;
            updateContent(content);
        }

        public void updateContent(Texture content) {
            mContent = content;
            Rect p = mFrame.getPaddings();

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
        public void render(GLCanvas canvas) {
            int x = -mWidth / 2;
            int y = -mHeight / 2;

            Rect p = mFrame.getPaddings();
            mContent.draw(canvas, x + p.left, y + p.top,
                    mWidth - p.left - p.right, mHeight - p.top - p.bottom);
            mFrame.draw(canvas, x, y, mWidth, mHeight);
        }
    }

    public void freeSlot(int index, DisplayItemPanel panel) {
        MyDisplayItem[] displayItems = mItemsetMap.get(index);
        for (MyDisplayItem item : displayItems) {
            panel.removeDisplayItem(item);
        }
        mLruSlot.add(index);
    }
}

