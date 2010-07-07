/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.util.Log;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

@SmallTest
public class LocalMediaSetTest extends TestCase {
    private static final String TAG = "LocalMediaSetTest";

    public void testEmptySet() {
        LocalMediaSet s = new LocalMediaSet(42, "Empty Set");
        assertEquals(0, s.getMediaItemCount());
        assertEquals(0, s.getSubMediaSetCount());
        assertEquals(0, s.getTotalMediaItemCount());
        assertEquals("Empty Set", s.getTitle());
        assertEquals(0, s.getCoverMediaItems().length);
        assertNull(s.getSubMediaSetById(42));
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
        assertEquals(1, s.getMediaItemCount());
        assertEquals(0, s.getSubMediaSetCount());
        assertEquals(1, s.getTotalMediaItemCount());
        assertSame(item, s.getCoverMediaItems()[0]);
        assertSame(item, s.getMediaItem(0));
    }

    public void testTwoItems() {
        LocalMediaSet s = new LocalMediaSet(2, "Two Items Set");
        MediaItem item1 = new MyMediaItem();
        MediaItem item2 = new MyMediaItem();
        s.addMediaItem(item1);
        s.addMediaItem(item2);
        assertEquals(2, s.getMediaItemCount());
        assertEquals(0, s.getSubMediaSetCount());
        assertEquals(2, s.getTotalMediaItemCount());
        assertTrue(s.getCoverMediaItems()[0] == item1
                || s.getCoverMediaItems()[0] == item2);
    }

    public void testEmptySubMediaSet() {
        LocalMediaSet s = new LocalMediaSet(3, "One Empty Sub-MediaSet");
        LocalMediaSet t = new LocalMediaSet(42, "Empty Set");
        s.addSubMediaSet(t);
        assertEquals(0, s.getMediaItemCount());
        assertEquals(1, s.getSubMediaSetCount());
        assertEquals(0, s.getTotalMediaItemCount());
        assertEquals("One Empty Sub-MediaSet", s.getTitle());
        assertEquals(0, s.getCoverMediaItems().length);
        assertSame(t, s.getSubMediaSet(0));
        assertSame(t, s.getSubMediaSetById(42));
        assertNull(s.getSubMediaSetById(0));
        assertEquals("Empty Set", t.getTitle());
    }

    public void testSubSubMediaSet() {
        LocalMediaSet s = new LocalMediaSet(0, "Set 0");
        LocalMediaSet s1 = new LocalMediaSet(1, "Set 1");
        LocalMediaSet s2 = new LocalMediaSet(2, "Set 2");
        MediaItem item = new MyMediaItem();
        s.addSubMediaSet(s1);
        assertEquals(0, s.getMediaItemCount());
        assertEquals(1, s.getSubMediaSetCount());
        assertEquals(0, s.getTotalMediaItemCount());
        assertEquals(0, s.getCoverMediaItems().length);
        assertSame(s1, s.getSubMediaSet(0));
        assertNull(s.getSubMediaSetById(0));
        assertSame(s1, s.getSubMediaSetById(1));
        assertNull(s.getSubMediaSetById(2));
        s1.addSubMediaSet(s2);
        assertEquals(0, s.getMediaItemCount());
        assertEquals(1, s.getSubMediaSetCount());
        assertEquals(0, s.getTotalMediaItemCount());
        assertEquals(0, s.getCoverMediaItems().length);
        assertSame(s1, s.getSubMediaSet(0));
        assertNull(s.getSubMediaSetById(0));
        assertSame(s1, s.getSubMediaSetById(1));
        assertNull(s.getSubMediaSetById(2));
        assertSame(s2, s1.getSubMediaSet(0));
        assertSame(s2, s1.getSubMediaSetById(2));
        s2.addMediaItem(item);
        assertEquals(0, s.getMediaItemCount());
        assertEquals(1, s.getSubMediaSetCount());
        assertEquals(1, s.getTotalMediaItemCount());
        assertEquals(1, s.getCoverMediaItems().length);
        assertSame(s1, s.getSubMediaSet(0));
        assertNull(s.getSubMediaSetById(0));
        assertSame(s1, s.getSubMediaSetById(1));
        assertNull(s.getSubMediaSetById(2));
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

        assertEquals(2, s.getMediaItemCount());
        assertEquals(2, s.getSubMediaSetCount());
        assertEquals(6, s.getTotalMediaItemCount());
        assertTrue(s.getCoverMediaItems().length > 0);
        assertSame(s1, s.getSubMediaSet(0));
        assertSame(s4, s.getSubMediaSet(1));
        assertSame(s1, s.getSubMediaSetById(1));
        assertSame(s4, s.getSubMediaSetById(4));
        assertNull(s.getSubMediaSetById(8));
        assertSame(s8, s4.getSubMediaSetById(8));
        assertNull(s.getSubMediaSetById(LocalMediaSet.ROOT_SET_ID));

        MediaItem[] m = s.getCoverMediaItems();
        for (int i = 0; i < m.length; i++) {
            assertTrue(m[i] == t2 || m[i] == t3 || m[i] == t5
                    || m[i] == t6 || m[i] == t7 || m[i] == t10);
            for (int j = 0; j < i; j++) {
                assertNotSame(m[j], m[i]);
            }
        }
    }
}
