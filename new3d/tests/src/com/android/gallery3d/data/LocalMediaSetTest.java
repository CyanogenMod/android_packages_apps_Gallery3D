package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.util.Log;

import junit.framework.TestCase;

public class LocalMediaSetTest extends TestCase {
    private static final String TAG = "LocalMediaSetTest";

    public void testEmptySet() {
        LocalMediaSet s = new LocalMediaSet(42, "Empty Set");
        assertTrue(s.getMediaItemCount() == 0);
        assertTrue(s.getSubMediaSetCount() == 0);
        assertTrue(s.getTotalMediaItemCount() == 0);
        assertTrue(s.getTitle().equals("Empty Set"));
        assertTrue(s.getCoverMediaItems().length == 0);
        assertTrue(s.getSubMediaSetById(42) == null);
    }

    private static class MyMediaItem implements MediaItem {
        public String getMediaUri() { return ""; }
        public String getTitle() { return ""; }
        public Bitmap getImage(ContentResolver cr, int type) { return null; }
        public void setListener(MediaItemListener listener) {}
    }

    public void testOneItem() {
        LocalMediaSet s = new LocalMediaSet(1, "One Item Set");
        MediaItem item = new MyMediaItem();
        s.addMediaItem(item);
        assertTrue(s.getMediaItemCount() == 1);
        assertTrue(s.getSubMediaSetCount() == 0);
        assertTrue(s.getTotalMediaItemCount() == 1);
        assertTrue(s.getCoverMediaItems()[0] == item);
        assertTrue(s.getMediaItem(0) == item);
    }

    public void testTwoItems() {
        LocalMediaSet s = new LocalMediaSet(2, "Two Items Set");
        MediaItem item1 = new MyMediaItem();
        MediaItem item2 = new MyMediaItem();
        s.addMediaItem(item1);
        s.addMediaItem(item2);
        assertTrue(s.getMediaItemCount() == 2);
        assertTrue(s.getSubMediaSetCount() == 0);
        assertTrue(s.getTotalMediaItemCount() == 2);
        assertTrue(s.getCoverMediaItems()[0] == item1
                || s.getCoverMediaItems()[0] == item2);
    }

    public void testEmptySubMediaSet() {
        LocalMediaSet s = new LocalMediaSet(3, "One Empty Sub-MediaSet");
        LocalMediaSet t = new LocalMediaSet(42, "Empty Set");
        s.addSubMediaSet(t);
        assertTrue(s.getMediaItemCount() == 0);
        assertTrue(s.getSubMediaSetCount() == 1);
        assertTrue(s.getTotalMediaItemCount() == 0);
        assertTrue(s.getTitle().equals("One Empty Sub-MediaSet"));
        assertTrue(s.getCoverMediaItems().length == 0);
        assertTrue(s.getSubMediaSet(0) == t);
        assertTrue(s.getSubMediaSetById(42) == t);
        assertTrue(s.getSubMediaSetById(0) == null);
        assertTrue(t.getTitle().equals("Empty Set"));
    }

    public void testSubSubMediaSet() {
        LocalMediaSet s = new LocalMediaSet(0, "Set 0");
        LocalMediaSet s1 = new LocalMediaSet(1, "Set 1");
        LocalMediaSet s2 = new LocalMediaSet(2, "Set 2");
        MediaItem item = new MyMediaItem();
        s.addSubMediaSet(s1);
        assertTrue(s.getMediaItemCount() == 0);
        assertTrue(s.getSubMediaSetCount() == 1);
        assertTrue(s.getTotalMediaItemCount() == 0);
        assertTrue(s.getCoverMediaItems().length == 0);
        assertTrue(s.getSubMediaSet(0) == s1);
        assertTrue(s.getSubMediaSetById(0) == null);
        assertTrue(s.getSubMediaSetById(1) == s1);
        assertTrue(s.getSubMediaSetById(2) == null);
        s1.addSubMediaSet(s2);
        assertTrue(s.getMediaItemCount() == 0);
        assertTrue(s.getSubMediaSetCount() == 1);
        assertTrue(s.getTotalMediaItemCount() == 0);
        assertTrue(s.getCoverMediaItems().length == 0);
        assertTrue(s.getSubMediaSet(0) == s1);
        assertTrue(s.getSubMediaSetById(0) == null);
        assertTrue(s.getSubMediaSetById(1) == s1);
        assertTrue(s.getSubMediaSetById(2) == null);
        assertTrue(s1.getSubMediaSet(0) == s2);
        assertTrue(s1.getSubMediaSetById(2) == s2);
        s2.addMediaItem(item);
        assertTrue(s.getMediaItemCount() == 0);
        assertTrue(s.getSubMediaSetCount() == 1);
        assertTrue(s.getTotalMediaItemCount() == 1);
        assertTrue(s.getCoverMediaItems().length == 1);
        assertTrue(s.getSubMediaSet(0) == s1);
        assertTrue(s.getSubMediaSetById(0) == null);
        assertTrue(s.getSubMediaSetById(1) == s1);
        assertTrue(s.getSubMediaSetById(2) == null);
    }

    //
    // [0] - [1]
    //     -  2
    //     -  3
    //     - [4] -  5
    //           -  6
    //           -  7
    //           - [8] - [9] - 10
    //                 - [11]
    //
    public void testMediaSetTree() {
        LocalMediaSet s0 = new LocalMediaSet(LocalMediaSet.ROOT_SET_ID, "Set 0");
        LocalMediaSet s1 = new LocalMediaSet(1, "Set 1");
        LocalMediaSet s4 = new LocalMediaSet(4, "Set 4");
        LocalMediaSet s8 = new LocalMediaSet(8, "Set 8");
        LocalMediaSet s9 = new LocalMediaSet(9, "Set 9");
        LocalMediaSet s11 = new LocalMediaSet(11, "Set 11");
        MediaItem t2 = new MyMediaItem();
        MediaItem t3 = new MyMediaItem();
        MediaItem t5 = new MyMediaItem();
        MediaItem t6 = new MyMediaItem();
        MediaItem t7 = new MyMediaItem();
        MediaItem t10 = new MyMediaItem();

        s0.addSubMediaSet(s1);
        s0.addMediaItem(t2);
        s0.addMediaItem(t3);
        s0.addSubMediaSet(s4);
        s4.addMediaItem(t5);
        s4.addMediaItem(t6);
        s4.addMediaItem(t7);
        s4.addSubMediaSet(s8);
        s8.addSubMediaSet(s9);
        s9.addMediaItem(t10);
        s8.addSubMediaSet(s11);

        LocalMediaSet s = s0;

        assertTrue(s.getMediaItemCount() == 2);
        assertTrue(s.getSubMediaSetCount() == 2);
        assertTrue(s.getTotalMediaItemCount() == 6);
        assertTrue(s.getCoverMediaItems().length > 0);
        assertTrue(s.getSubMediaSet(0) == s1);
        assertTrue(s.getSubMediaSet(1) == s4);
        assertTrue(s.getSubMediaSetById(1) == s1);
        assertTrue(s.getSubMediaSetById(4) == s4);
        assertTrue(s.getSubMediaSetById(8) == null);
        assertTrue(s4.getSubMediaSetById(8) == s8);
        assertTrue(s.getSubMediaSetById(LocalMediaSet.ROOT_SET_ID) == null);

        MediaItem[] m = s.getCoverMediaItems();
        for (int i = 0; i < m.length; i++) {
            assertTrue(m[i] == t2 || m[i] == t3 || m[i] == t5
                    || m[i] == t6 || m[i] == t7 || m[i] == t10);
            for (int j = 0; j < i; j++) {
                assertTrue(m[j] != m[i]);
            }
        }
    }

}
