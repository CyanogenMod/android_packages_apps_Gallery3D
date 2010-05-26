package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;

import com.android.gallery3d.R;

import java.util.Random;

public class SlotViewMockData implements SlotView.Model {
    private static final int LENGTH_LIMIT = 180;
    private static final double EXPECTED_AREA = LENGTH_LIMIT * LENGTH_LIMIT / 2;
    private static final int DATA_SIZE = 50;
    private static final int PILE_SIZE = 4;

    private final BasicTexture mPhoto[];
    private final NinePatchTexture mFrame;

    private final DisplayItem mItems[];
    private final Random mRandom = new Random();

    public SlotViewMockData(Context context) {
        mPhoto = new BasicTexture[] {
                new ResourceTexture(context, R.drawable.potrait),
                new ResourceTexture(context, R.drawable.square),
                new ResourceTexture(context, R.drawable.landscape),
        };
        mFrame = new NinePatchTexture(context, R.drawable.stack_frame);
        Random random = mRandom;
        mItems = new DisplayItem[DATA_SIZE * PILE_SIZE];
        for (int i = 0; i < DATA_SIZE; ++i) {
            int index = i * PILE_SIZE;
            for (int j = 0; j < PILE_SIZE; ++j) {
                mItems[index++] = new MyDisplayItem(
                        mPhoto[random.nextInt(3)], mFrame);
            }
        }
    }

    public void freeSlot(int index, DisplayItemPanel panel) {
        for (int i = index * PILE_SIZE, n = i + PILE_SIZE; i < n; ++i) {
            panel.removeDisplayItem(mItems[i]);
        }
    }

    public void putSlot(int slotIndex, int x, int y, DisplayItemPanel panel) {
        int index = slotIndex * PILE_SIZE;
        Random random = mRandom;

        int left = x + 10;
        int right = x + getSlotWidth() - 10;

        x += getSlotWidth() / 2;
        y += getSlotHeight() / 2;

        for (int i = 1; i < PILE_SIZE; ++i) {
            DisplayItem item = mItems[index++];

            int dx = random.nextInt(11) - 5;
            int itemX = (i & 0x01) == 0
                    ? left + dx + item.getWidth() / 2
                    : right + dx - item.getWidth() / 2;
            int dy = random.nextInt(11) - 10;
            int theta = random.nextInt(31) - 15;
            panel.putDisplayItem(item, itemX, y + dy, theta);
        }
        panel.putDisplayItem(mItems[index++], x, y, 0);
    }

    public int getSlotHeight() {
        return 200;
    }

    public int getSlotWidth() {
        return 220;
    }

    public int size() {
        return DATA_SIZE;
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
            mFrame.draw(root, x, y, mWidth, mHeight);
            mContent.draw(root, x + p.left, y + p.top,
                    mWidth - p.left - p.right, mHeight - p.top - p.bottom);
        }
    }

}
