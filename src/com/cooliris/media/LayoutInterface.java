package com.cooliris.media;

import java.util.ArrayList;

public abstract class LayoutInterface {
    public abstract void getPositionForSlotIndex(int displayIndex, int itemWidth, int itemHeight, ArrayList<Integer> breakSlots, Vector3f outPosition); // the
    // positions
    // of the
    // individual
    // slots
}
