package com.cooliris.media;

import java.util.ArrayList;
import java.util.HashMap;

// CR: comment.
public final class DisplayList {
    private DirectLinkedList<DisplayItem> mAnimatables = new DirectLinkedList<DisplayItem>();
    private HashMap<MediaItem, DisplayItem> mDisplayMap = new HashMap<MediaItem, DisplayItem>(1024);
    private ArrayList<DisplayItem> mItems = new ArrayList<DisplayItem>(1024);

    public DisplayItem get(MediaItem item) {
        HashMap<MediaItem, DisplayItem> displayMap = mDisplayMap;
        DisplayItem displayItem = displayMap.get(item);
        if (displayItem == null) {
            displayItem = new DisplayItem(item);
            displayMap.put(item, displayItem);
            mItems.add(displayItem);
        }
        return displayItem;
    }

    public void setPositionAndStackIndex(DisplayItem item, Vector3f position, int stackId, boolean performTransition) {
        item.set(position, stackId, performTransition);
        if (!performTransition) {
            item.commit();
        } else {
            markIfDirty(item);
        }
    }

    public void setHasFocus(DisplayItem item, boolean hasFocus, boolean pushDown) {
        boolean currentHasFocus = item.getHasFocus();
        if (currentHasFocus != hasFocus) {
            item.setHasFocus(hasFocus, pushDown);
            markIfDirty(item);
        }
    }

    public ArrayList<DisplayItem> getAllDisplayItems() {
        return mItems;
    }

    public void update(float timeElapsed) {
        final DirectLinkedList<DisplayItem> animatables = mAnimatables;
        synchronized (animatables) {
            DirectLinkedList.Entry<DisplayItem> entry = animatables.getHead();
            while (entry != null) {
                DisplayItem item = entry.value;
                item.update(timeElapsed);
                if (!item.isAnimating()) {
                    entry = animatables.remove(entry);
                } else {
                    entry = entry.next;
                }
            }
        }
    }

    public int getNumAnimatables() {
        return mAnimatables.size();
    }

    public void setAlive(DisplayItem item, boolean alive) {
        item.mAlive = alive;
        if (alive && item.isAnimating()) {
            final DirectLinkedList.Entry<DisplayItem> entry = item.getAnimatablesEntry();
            if (!entry.inserted) {
                mAnimatables.add(entry);
            }
        }
    }

    public void commit(DisplayItem item) {
        item.commit();
        final DirectLinkedList<DisplayItem> animatables = mAnimatables;
        synchronized (animatables) {
            animatables.remove(item.getAnimatablesEntry());
        }
    }

    public void addToAnimatables(DisplayItem item) {
        final DirectLinkedList.Entry<DisplayItem> entry = item.getAnimatablesEntry();
        if (!entry.inserted) {
            final DirectLinkedList<DisplayItem> animatables = mAnimatables;
            synchronized (animatables) {
                animatables.add(entry);
            }
        }
    }

    private void markIfDirty(DisplayItem item) {
        if (item.isAnimating()) {
            addToAnimatables(item);
        }
    }

    public void clear() {
        mDisplayMap.clear();
        synchronized (mItems) {
            mItems.clear();
        }
    }

    public void clearExcept(DisplayItem[] displayItems) {
        HashMap<MediaItem, DisplayItem> displayMap = mDisplayMap;
        displayMap.clear();
        synchronized (mItems) {
            mItems.clear();
            int numItems = displayItems.length;
            for (int i = 0; i < numItems; ++i) {
                DisplayItem displayItem = displayItems[i];
                if (displayItem != null) {
                    displayMap.put(displayItem.mItemRef, displayItem);
                    mItems.add(displayItem);
                }
            }
        }
    }
}
