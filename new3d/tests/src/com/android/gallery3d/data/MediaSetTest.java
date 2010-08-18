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
    private static final int DUMMY_PARENT_ID = 0x777;
    private static final int KEY_COMBO = 1;
    private static final int KEY_MERGE = 2;

    @SmallTest
    public void testComboAlbumSet() {
        DataManager dataManager = new GalleryContextMock(null, null, null)
                .getDataManager();

        MediaSetMock set00 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 0, 0, 2000);
        MediaSetMock set01 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 1, 1, 3000);
        MediaSetMock set10 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 2, 2, 4000);
        MediaSetMock set11 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 3, 3, 5000);
        MediaSetMock set12 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 4, 4, 6000);

        MediaSetMock set0 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 5, 7, 7000);
        set0.addMediaSet(set00);
        set0.addMediaSet(set01);

        MediaSetMock set1 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 6, 8, 8000);
        set1.addMediaSet(set10);
        set1.addMediaSet(set11);
        set1.addMediaSet(set12);

        MediaSet combo = new ComboAlbumSet(dataManager, DUMMY_PARENT_ID,
                KEY_COMBO, set0, set1);
        assertEquals(DUMMY_PARENT_ID, DataManager.extractParentId(combo.getUniqueId()));
        assertEquals(5, combo.getSubMediaSetCount());
        assertEquals(0, combo.getMediaItemCount());
        assertEquals(0x77700000001L, combo.getSubMediaSet(0).getUniqueId());
        assertEquals(0x77700000002L, combo.getSubMediaSet(1).getUniqueId());
        assertEquals(0x77700000003L, combo.getSubMediaSet(2).getUniqueId());
        assertEquals(0x77700000004L, combo.getSubMediaSet(3).getUniqueId());
        assertEquals(0x77700000005L, combo.getSubMediaSet(4).getUniqueId());

        assertEquals(25, combo.getTotalMediaItemCount());
    }

    @SmallTest
    public void testMergeAlbumSet() {
        DataManager dataManager = new GalleryContextMock(null, null, null)
                .getDataManager();

        MediaSetMock set00 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 0, 0, 3, 0x4000);
        MediaSetMock set01 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 1, 1, 4, 0x3000);
        MediaSetMock set11 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 2, 1, 6, 0x2000);
        MediaSetMock set12 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 3, 2, 1, 0x1000);

        MediaSetMock set0 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 4);
        set0.addMediaSet(set00);
        set0.addMediaSet(set01);
        MediaSetMock set1 = new MediaSetMock(dataManager, DUMMY_PARENT_ID, 5);
        set1.addMediaSet(set11);
        set1.addMediaSet(set12);

        Comparator<MediaItem> comparator = new Comparator<MediaItem>() {
                public int compare(MediaItem object1, MediaItem object2) {
                    int id1 = DataManager.extractSelfId(object1.getUniqueId());
                    int id2 = DataManager.extractSelfId(object2.getUniqueId());
                    if (id1 > id2) return 1;
                    else if (id1 < id2) return -1;
                    else return 0;
                }
        };

        // set01 and set11 will be merged, but not set00 or set12.
        MediaSet merge = new MergeAlbumSet(dataManager, DUMMY_PARENT_ID,
                KEY_MERGE, comparator, set0, set1);

        assertEquals(3, merge.getSubMediaSetCount());
        MediaSet s0 = merge.getSubMediaSet(0);
        MediaSet s1 = merge.getSubMediaSet(1);
        MediaSet s2 = merge.getSubMediaSet(2);

        assertEquals(3, s0.getMediaItemCount());
        assertEquals(10, s1.getMediaItemCount());
        assertEquals(1, s2.getMediaItemCount());

        ArrayList<MediaItem> items = s1.getMediaItem(0, 10);
        assertEquals(0x300002000L, items.get(0).getUniqueId());
        assertEquals(0x300002005L, items.get(5).getUniqueId());
        assertEquals(0x200003000L, items.get(6).getUniqueId());
        assertEquals(0x200003003L, items.get(9).getUniqueId());

        assertEquals(14, merge.getTotalMediaItemCount());
    }
}
