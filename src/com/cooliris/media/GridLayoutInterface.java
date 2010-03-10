package com.cooliris.media;

import java.util.ArrayList;

import com.cooliris.app.App;

public final class GridLayoutInterface extends LayoutInterface {
    public GridLayoutInterface(int numRows) {
        mNumRows = numRows;
        mSpacingX = (int) (20 * App.PIXEL_DENSITY);
        mSpacingY = (int) (40 * App.PIXEL_DENSITY);
    }
    
    public float getSpacingForBreak() {
        return mSpacingX / 2;
    }

    public int getNextSlotIndexForBreak(int breakSlotIndex) {
        int numRows = mNumRows;
        int mod = breakSlotIndex % numRows;
        int add = (numRows - mod);
        if (add >= numRows)
            add -= numRows;
        return breakSlotIndex + add;
    }

    public void getPositionForSlotIndex(int slotIndex, int itemWidth, int itemHeight, Vector3f outPosition) {
        int numRows = mNumRows;
        int resultSlotIndex = slotIndex;
        outPosition.x = (resultSlotIndex / numRows) * (itemWidth + mSpacingX);
        outPosition.y = (resultSlotIndex % numRows) * (itemHeight + mSpacingY);
        int maxY = (numRows - 1) * (itemHeight + mSpacingY);
        outPosition.y -= (maxY >> 1);
        outPosition.z = 0;
    }

    public int mNumRows;
    public int mSpacingX;
    public int mSpacingY;
}
