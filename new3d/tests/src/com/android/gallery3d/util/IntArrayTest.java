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

package com.android.gallery3d.util;

import com.android.gallery3d.util.IntArray;

import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import java.util.Arrays;
import junit.framework.TestCase;

@SmallTest
public class IntArrayTest extends TestCase {
    private static final String TAG = "IntArrayTest";

    public void testIntArray() {
        IntArray a = new IntArray();
        assertEquals(0, a.size());
        assertTrue(Arrays.equals(new int[] {}, a.toArray(null)));

        a.add(0);
        assertEquals(1, a.size());
        assertTrue(Arrays.equals(new int[] {0}, a.toArray(null)));

        a.add(1);
        assertEquals(2, a.size());
        assertTrue(Arrays.equals(new int[] {0, 1}, a.toArray(null)));

        int[] buf = new int[2];
        int[] result = a.toArray(buf);
        assertSame(buf, result);

        IntArray b = new IntArray();
        for (int i = 0; i < 100; i++) {
            b.add(i * i);
        }

        assertEquals(100, b.size());
        result = b.toArray(buf);
        assertEquals(100, result.length);
        for (int i = 0; i < 100; i++) {
            assertEquals(i * i, result[i]);
        }
    }
}
