package com.android.gallery3d.ui;

public class SlotView {

    public static interface Slot {
        public void putOn(int x, int y, DisplayItemPanel panel);
    }

    public static interface Model {
        public int size();
        public int getSlotHeight();
        public int getSlotWidth();
        public Slot getSlot(int index);
        public void free(Slot slot);
    }
}
