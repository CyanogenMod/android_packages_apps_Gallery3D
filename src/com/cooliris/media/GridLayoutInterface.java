package com.cooliris.media;

import java.util.ArrayList;

import com.cooliris.app.App;

public final class GridLayoutInterface extends LayoutInterface {
    public GridLayoutInterface(int numRows) {
        mNumRows = numRows;
        mSpacingX = (int) (20 * App.PIXEL_DENSITY);
        mSpacingY = (int) (40 * App.PIXEL_DENSITY);
    }

    public void getPositionForSlotIndex(int slotIndex, int itemWidth, int itemHeight, ArrayList<Integer> breaks,
            Vector3f outPosition) {
        int numRows = mNumRows;
        int resultSlotIndex = slotIndex;
        int numLines = 0;
        if (breaks != null) {
            int numBreaks = breaks.size();
            int numPads = 0;
            for (int i = 0; i < numBreaks; ++i) {
                int breakSlotId = breaks.get(i);
                if (breakSlotId > slotIndex)
                    break;
                breakSlotId += numPads;
                int mod = breakSlotId % numRows;
                ++numLines;
                int add = (numRows - mod) + numRows;
                if (add >= numRows * 2) {
                    add -= numRows;
                }
                numPads += add;
                resultSlotIndex += add;
            }
        }
        outPosition.x = (resultSlotIndex / numRows) * (itemWidth + mSpacingX);
        outPosition.y = (resultSlotIndex % numRows) * (itemHeight + mSpacingY);
        outPosition.x -= ((itemWidth + mSpacingX) * numLines) / 2;
        int maxY = (numRows - 1) * (itemHeight + mSpacingY);
        outPosition.y -= (maxY >> 1);
        outPosition.z = 0;
    }

    public int mNumRows;
    public int mSpacingX;
    public int mSpacingY;
}
