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

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import java.util.ArrayList;
import java.util.Comparator;

public class MediaSetTest extends AndroidTestCase {
    private static final String TAG = "MediaSetTest";

    @SmallTest
    public void testComboAlbumSet() {

        MediaSetMock set00 = new MediaSetMock(1000L, 0, 2000L);
        MediaSetMock set01 = new MediaSetMock(1001L, 1, 3000L);
        MediaSetMock set10 = new MediaSetMock(1002L, 2, 4000L);
        MediaSetMock set11 = new MediaSetMock(1003L, 3, 5000L);
        MediaSetMock set12 = new MediaSetMock(1004L, 4, 6000L);

        MediaSetMock set0 = new MediaSetMock(1005L, 7, 7000L);
        set0.addMediaSet(set00);
        set0.addMediaSet(set01);

        MediaSetMock set1 = new MediaSetMock(1006L, 8, 8000L);
        set1.addMediaSet(set10);
        set1.addMediaSet(set11);
        set1.addMediaSet(set12);

        MediaSet combo = new ComboAlbumSet(1007L, set0, set1);
        assertEquals(1007L, combo.getUniqueId());
        assertEquals(5, combo.getSubMediaSetCount());
        assertEquals(0, combo.getMediaItemCount());
        assertEquals(1000L, combo.getSubMediaSet(0).getUniqueId());
        assertEquals(1001L, combo.getSubMediaSet(1).getUniqueId());
        assertEquals(1002L, combo.getSubMediaSet(2).getUniqueId());
        assertEquals(1003L, combo.getSubMediaSet(3).getUniqueId());
        assertEquals(1004L, combo.getSubMediaSet(4).getUniqueId());

        assertEquals(25, combo.getTotalMediaItemCount());
    }

    @SmallTest
    public void testMergeAlbumSet() {
        MediaSetMock set00 = new MediaSetMock(DataManager.makeId(0, 0), 3, 4000L);
        MediaSetMock set01 = new MediaSetMock(DataManager.makeId(0, 1), 4, 3000L);
        MediaSetMock set11 = new MediaSetMock(DataManager.makeId(1, 1), 6, 2000L);
        MediaSetMock set12 = new MediaSetMock(DataManager.makeId(1, 2), 1, 1000L);

        MediaSetMock set0 = new MediaSetMock(DataManager.makeId(2,0));
        set0.addMediaSet(set00);
        set0.addMediaSet(set01);
        MediaSetMock set1 = new MediaSetMock(DataManager.makeId(2,1));
        set1.addMediaSet(set11);
        set1.addMediaSet(set12);

        Comparator<MediaItem> comparator = new Comparator<MediaItem>() {
                public int compare(MediaItem object1, MediaItem object2) {
                    long id1 = object1.getUniqueId();
                    long id2 = object2.getUniqueId();
                    if (id1 > id2) return 1;
                    else if (id1 < id2) return -1;
                    else return 0;
                }
        };

        // set01 and set11 will be merged, but not set00 or set12.
        MediaSet merge = new MergeAlbumSet(DataManager.makeId(3, 0),
                comparator, set0, set1);

        assertEquals(3, merge.getSubMediaSetCount());
        MediaSet s0 = merge.getSubMediaSet(0);
        MediaSet s1 = merge.getSubMediaSet(1);
        MediaSet s2 = merge.getSubMediaSet(2);
        assertEquals(DataManager.makeId(0, 0), s0.getUniqueId());
        assertEquals(DataManager.makeId(DataManager.ID_MERGE_LOCAL_ALBUM, 1),
                s1.getUniqueId());
        assertEquals(DataManager.makeId(1, 2), s2.getUniqueId());

        assertEquals(3, s0.getMediaItemCount());
        assertEquals(10, s1.getMediaItemCount());
        assertEquals(1, s2.getMediaItemCount());

        ArrayList<MediaItem> items = s1.getMediaItem(0, 10);
        assertEquals(2000L, items.get(0).getUniqueId());
        assertEquals(2005L, items.get(5).getUniqueId());
        assertEquals(3000L, items.get(6).getUniqueId());
        assertEquals(3003L, items.get(9).getUniqueId());

        assertEquals(14, merge.getTotalMediaItemCount());
    }
}
