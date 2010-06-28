package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

public class GridSlotAdapter implements SlotView.Model {
    private static final int LENGTH_LIMIT = 162;
    private static final double EXPECTED_AREA = 150 * 120;
    private static final int SLOT_WIDTH = 162;
    private static final int SLOT_HEIGHT = 132;
    private static final int INITIAL_CACHE_CAPACITY = 48;

    private Map<Integer, MyDisplayItem> mItemMap =
            new HashMap<Integer, MyDisplayItem>(INITIAL_CACHE_CAPACITY);
    private LinkedHashSet<Integer> mLruSlot =
            new LinkedHashSet<Integer>(INITIAL_CACHE_CAPACITY);
    private final NinePatchTexture mFrame;

    private MediaSet mMediaSet;
    private Context mContext;

    public GridSlotAdapter(Context context, MediaSet mediaSet) {
        mContext = context;
        mMediaSet = mediaSet;
        mFrame = new NinePatchTexture(context, R.drawable.grid_frame);
    }

    public void putSlot(int slotIndex, int x, int y, DisplayItemPanel panel) {
        MyDisplayItem displayItem = mItemMap.get(slotIndex);
        if (displayItem == null) {
            MediaItem item = mMediaSet.getMediaItem(slotIndex);
            Bitmap bitmap = item.getImage(mContext.getContentResolver(),
                    MediaItem.TYPE_THUMBNAIL);
            displayItem = new MyDisplayItem(new BitmapTexture(bitmap), mFrame);

            // Remove an item if the size of mItemsetMap is no less than
            // INITIAL_CACHE_CAPACITY and there exists a slot in mLruSlot.
            if (mItemMap.size() >= INITIAL_CACHE_CAPACITY && !mLruSlot.isEmpty()) {
                Iterator<Integer> iter = mLruSlot.iterator();
                int index = iter.next();
                mItemMap.remove(index);
                mLruSlot.remove(index);
            }
            mItemMap.put(slotIndex, displayItem);
            mLruSlot.remove(slotIndex);
        }

        x += getSlotWidth() / 2;
        y += getSlotHeight() / 2;
        panel.putDisplayItem(displayItem, x, y, 0);
    }

    public int getSlotHeight() {
        return SLOT_HEIGHT;
    }

    public int getSlotWidth() {
        return SLOT_WIDTH;
    }

    public int size() {
        return mMediaSet.getMediaItemCount();
    }

    private static class MyDisplayItem extends DisplayItem {

        private static final String TAG = "MyDisplayItem";
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
        panel.removeDisplayItem(mItemMap.get(index));
        mLruSlot.add(index);
    }
}
