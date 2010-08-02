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
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

public class BlobCacheTest extends AndroidTestCase {
    private static final String TAG = "BlobCacheTest";

    @SmallTest
    public void testReadIntLong() {
        byte[] buf = new byte[9];
        assertEquals(0, BlobCache.readInt(buf, 0));
        assertEquals(0, BlobCache.readLong(buf, 0));
        buf[0] = 1;
        assertEquals(1, BlobCache.readInt(buf, 0));
        assertEquals(1, BlobCache.readLong(buf, 0));
        buf[3] = 0x7f;
        assertEquals(0x7f000001, BlobCache.readInt(buf, 0));
        assertEquals(0x7f000001, BlobCache.readLong(buf, 0));
        assertEquals(0x007f0000, BlobCache.readInt(buf, 1));
        assertEquals(0x007f0000, BlobCache.readLong(buf, 1));
        buf[3] = (byte) 0x80;
        buf[7] = (byte) 0xA0;
        buf[0] = 0;
        assertEquals(0x80000000, BlobCache.readInt(buf, 0));
        assertEquals(0xA000000080000000L, BlobCache.readLong(buf, 0));
        for (int i = 0; i < 8; i++) {
            buf[i] = (byte) (0x11 * (i+8));
        }
        assertEquals(0xbbaa9988, BlobCache.readInt(buf, 0));
        assertEquals(0xffeeddccbbaa9988L, BlobCache.readLong(buf, 0));
        buf[8] = 0x33;
        assertEquals(0x33ffeeddccbbaa99L, BlobCache.readLong(buf, 1));
    }

    @SmallTest
    public void testWriteIntLong() {
        byte[] buf = new byte[8];
        BlobCache.writeInt(buf, 0, 0x12345678);
        assertEquals(0x78, buf[0]);
        assertEquals(0x56, buf[1]);
        assertEquals(0x34, buf[2]);
        assertEquals(0x12, buf[3]);
        assertEquals(0x00, buf[4]);
        BlobCache.writeLong(buf, 0, 0xffeeddccbbaa9988L);
        for (int i = 0; i < 8; i++) {
            assertEquals((byte) (0x11 * (i+8)), buf[i]);
        }
    }

    @MediumTest
    public void testChecksum() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, MAX_ENTRIES, MAX_BYTES, true);
        byte[] buf = new byte[0];
        assertEquals(0x1, bc.checkSum(buf));
        buf = new byte[1];
        assertEquals(0x10001, bc.checkSum(buf));
        buf[0] = 0x47;
        assertEquals(0x480048, bc.checkSum(buf));
        buf = new byte[3];
        buf[0] = 0x10;
        buf[1] = 0x30;
        buf[2] = 0x01;
        assertEquals(0x940042, bc.checkSum(buf));
        assertEquals(0x310031, bc.checkSum(buf, 1, 1));
        assertEquals(0x1, bc.checkSum(buf, 1, 0));
        assertEquals(0x630032, bc.checkSum(buf, 1, 2));
        buf = new byte[1024];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte)(i*i);
        }
        assertEquals(0x3574a610, bc.checkSum(buf));
        bc.close();
    }

    private static final int HEADER_SIZE = 32;
    private static final int DATA_HEADER_SIZE = 4;
    private static final int BLOB_HEADER_SIZE = 20;

    private static final String TEST_FILE_NAME = "/sdcard/btest";
    private static final int MAX_ENTRIES = 100;
    private static final int MAX_BYTES = 1000;
    private static final int INDEX_SIZE = HEADER_SIZE + MAX_ENTRIES * 12 * 2;
    private static final long KEY_0 = 0x1122334455667788L;
    private static final long KEY_1 = 0x1122334455667789L;
    private static final long KEY_2 = 0x112233445566778AL;
    private static byte[] DATA_0 = new byte[10];
    private static byte[] DATA_1 = new byte[10];

    @MediumTest
    public void testBasic() throws IOException {
        String name = TEST_FILE_NAME;
        BlobCache bc;
        File idxFile = new File(name + ".idx");
        File data0File = new File(name + ".0");
        File data1File = new File(name + ".1");

        // Create a brand new cache.
        bc = new BlobCache(name, MAX_ENTRIES, MAX_BYTES, true);
        bc.close();

        // Make sure the initial state is correct.
        assertTrue(idxFile.exists());
        assertTrue(data0File.exists());
        assertTrue(data1File.exists());
        assertEquals(INDEX_SIZE, idxFile.length());
        assertEquals(DATA_HEADER_SIZE, data0File.length());
        assertEquals(DATA_HEADER_SIZE, data1File.length());
        assertEquals(0, bc.getActiveCount());

        // Re-open it.
        bc = new BlobCache(name, MAX_ENTRIES, MAX_BYTES, false);
        assertNull(bc.lookup(KEY_0));

        // insert one blob
        genData(DATA_0, 1);
        bc.insert(KEY_0, DATA_0);
        assertSameData(DATA_0, bc.lookup(KEY_0));
        assertEquals(1, bc.getActiveCount());
        bc.close();

        // Make sure the file size is right.
        assertEquals(INDEX_SIZE, idxFile.length());
        assertEquals(DATA_HEADER_SIZE + BLOB_HEADER_SIZE + DATA_0.length,
                data0File.length());
        assertEquals(DATA_HEADER_SIZE, data1File.length());

        // Re-open it and make sure we can get the old data
        bc = new BlobCache(name, MAX_ENTRIES, MAX_BYTES, false);
        assertSameData(DATA_0, bc.lookup(KEY_0));

        // insert with the same key (but using a different blob)
        genData(DATA_0, 2);
        bc.insert(KEY_0, DATA_0);
        assertSameData(DATA_0, bc.lookup(KEY_0));
        assertEquals(1, bc.getActiveCount());
        bc.close();

        // Make sure the file size is right.
        assertEquals(INDEX_SIZE, idxFile.length());
        assertEquals(DATA_HEADER_SIZE + 2 * (BLOB_HEADER_SIZE + DATA_0.length),
                data0File.length());
        assertEquals(DATA_HEADER_SIZE, data1File.length());

        // Re-open it and make sure we can get the old data
        bc = new BlobCache(name, MAX_ENTRIES, MAX_BYTES, false);
        assertSameData(DATA_0, bc.lookup(KEY_0));

        // insert another key and make sure we can get both key.
        assertNull(bc.lookup(KEY_1));
        genData(DATA_1, 3);
        bc.insert(KEY_1, DATA_1);
        assertSameData(DATA_0, bc.lookup(KEY_0));
        assertSameData(DATA_1, bc.lookup(KEY_1));
        assertEquals(2, bc.getActiveCount());
        bc.close();

        // Make sure the file size is right.
        assertEquals(INDEX_SIZE, idxFile.length());
        assertEquals(DATA_HEADER_SIZE + 3 * (BLOB_HEADER_SIZE + DATA_0.length),
                data0File.length());
        assertEquals(DATA_HEADER_SIZE, data1File.length());

        // Re-open it and make sure we can get the old data
        bc = new BlobCache(name, 100, 1000, false);
        assertSameData(DATA_0, bc.lookup(KEY_0));
        assertSameData(DATA_1, bc.lookup(KEY_1));
        assertEquals(2, bc.getActiveCount());
        bc.close();
    }

    @MediumTest
    public void testNegativeKey() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, MAX_ENTRIES, MAX_BYTES, true);

        // insert one blob
        genData(DATA_0, 1);
        bc.insert(-123, DATA_0);
        assertSameData(DATA_0, bc.lookup(-123));
        bc.close();
    }

    @MediumTest
    public void testEmptyBlob() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, MAX_ENTRIES, MAX_BYTES, true);

        byte[] data = new byte[0];
        bc.insert(123, data);
        assertSameData(data, bc.lookup(123));
        bc.close();
    }

    @MediumTest
    public void testLookupRequest() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, MAX_ENTRIES, MAX_BYTES, true);

        // insert one blob
        genData(DATA_0, 1);
        bc.insert(1, DATA_0);
        assertSameData(DATA_0, bc.lookup(1));

        // the same size buffer
        byte[] buf = new byte[DATA_0.length];
        BlobCache.LookupRequest req = new BlobCache.LookupRequest();
        req.key = 1;
        req.buffer = buf;
        assertTrue(bc.lookup(req));
        assertEquals(1, req.key);
        assertSame(buf, req.buffer);
        assertEquals(DATA_0.length, req.length);

        // larger buffer
        buf = new byte[DATA_0.length + 22];
        req = new BlobCache.LookupRequest();
        req.key = 1;
        req.buffer = buf;
        assertTrue(bc.lookup(req));
        assertEquals(1, req.key);
        assertSame(buf, req.buffer);
        assertEquals(DATA_0.length, req.length);

        // smaller buffer
        buf = new byte[DATA_0.length - 1];
        req = new BlobCache.LookupRequest();
        req.key = 1;
        req.buffer = buf;
        assertTrue(bc.lookup(req));
        assertEquals(1, req.key);
        assertNotSame(buf, req.buffer);
        assertEquals(DATA_0.length, req.length);
        assertSameData(DATA_0, req.buffer, DATA_0.length);

        // null buffer
        req = new BlobCache.LookupRequest();
        req.key = 1;
        req.buffer = null;
        assertTrue(bc.lookup(req));
        assertEquals(1, req.key);
        assertNotNull(req.buffer);
        assertEquals(DATA_0.length, req.length);
        assertSameData(DATA_0, req.buffer, DATA_0.length);

        bc.close();
    }

    @MediumTest
    public void testKeyCollision() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, MAX_ENTRIES, MAX_BYTES, true);

        for (int i = 0; i < MAX_ENTRIES / 2; i++) {
            genData(DATA_0, i);
            long key = KEY_1 + i * MAX_ENTRIES;
            bc.insert(key, DATA_0);
        }

        for (int i = 0; i < MAX_ENTRIES / 2; i++) {
            genData(DATA_0, i);
            long key = KEY_1 + i * MAX_ENTRIES;
            assertSameData(DATA_0, bc.lookup(key));
        }
        bc.close();
    }

    @MediumTest
    public void testRegionFlip() throws IOException {
        String name = TEST_FILE_NAME;
        BlobCache bc;
        File idxFile = new File(name + ".idx");
        File data0File = new File(name + ".0");
        File data1File = new File(name + ".1");

        // Create a brand new cache.
        bc = new BlobCache(name, MAX_ENTRIES, MAX_BYTES, true);

        // This is the number of blobs fits into a region.
        int maxFit = (MAX_BYTES - DATA_HEADER_SIZE) /
                (BLOB_HEADER_SIZE + DATA_0.length);

        for (int k = 0; k < maxFit; k++) {
            genData(DATA_0, k);
            bc.insert(k, DATA_0);
        }
        assertEquals(maxFit, bc.getActiveCount());

        // Make sure the file size is right.
        assertEquals(INDEX_SIZE, idxFile.length());
        assertEquals(DATA_HEADER_SIZE + maxFit * (BLOB_HEADER_SIZE + DATA_0.length),
                data0File.length());
        assertEquals(DATA_HEADER_SIZE, data1File.length());

        // Now insert another one and let it flip.
        genData(DATA_0, 777);
        bc.insert(KEY_1, DATA_0);
        assertEquals(1, bc.getActiveCount());

        assertEquals(INDEX_SIZE, idxFile.length());
        assertEquals(DATA_HEADER_SIZE + maxFit * (BLOB_HEADER_SIZE + DATA_0.length),
                data0File.length());
        assertEquals(DATA_HEADER_SIZE + 1 * (BLOB_HEADER_SIZE + DATA_0.length),
                data1File.length());

        // Make sure we can find the new data
        assertSameData(DATA_0, bc.lookup(KEY_1));

        // Now find an old blob
        int old = maxFit / 2;
        genData(DATA_0, old);
        assertSameData(DATA_0, bc.lookup(old));
        assertEquals(2, bc.getActiveCount());

        // Observed data is copied.
        assertEquals(INDEX_SIZE, idxFile.length());
        assertEquals(DATA_HEADER_SIZE + maxFit * (BLOB_HEADER_SIZE + DATA_0.length),
                data0File.length());
        assertEquals(DATA_HEADER_SIZE + 2 * (BLOB_HEADER_SIZE + DATA_0.length),
                data1File.length());

        // Now copy everything over (except we should have no space for the last one)
        assertTrue(old < maxFit - 1);
        for (int k = 0; k < maxFit; k++) {
            genData(DATA_0, k);
            assertSameData(DATA_0, bc.lookup(k));
        }
        assertEquals(maxFit, bc.getActiveCount());

        // Now both file should be full.
        assertEquals(INDEX_SIZE, idxFile.length());
        assertEquals(DATA_HEADER_SIZE + maxFit * (BLOB_HEADER_SIZE + DATA_0.length),
                data0File.length());
        assertEquals(DATA_HEADER_SIZE + maxFit * (BLOB_HEADER_SIZE + DATA_0.length),
                data1File.length());

        // Now insert one to make it flip.
        genData(DATA_0, 888);
        bc.insert(KEY_2, DATA_0);
        assertEquals(1, bc.getActiveCount());

        // Check the size after the second flip.
        assertEquals(INDEX_SIZE, idxFile.length());
        assertEquals(DATA_HEADER_SIZE + 1 * (BLOB_HEADER_SIZE + DATA_0.length),
                data0File.length());
        assertEquals(DATA_HEADER_SIZE + maxFit * (BLOB_HEADER_SIZE + DATA_0.length),
                data1File.length());

        // Now the last key should be gone.
        assertNull(bc.lookup(maxFit - 1));

        // But others should remain
        for (int k = 0; k < maxFit - 1; k++) {
            genData(DATA_0, k);
            assertSameData(DATA_0, bc.lookup(k));
        }

        assertEquals(maxFit, bc.getActiveCount());
        genData(DATA_0, 777);
        assertSameData(DATA_0, bc.lookup(KEY_1));
        genData(DATA_0, 888);
        assertSameData(DATA_0, bc.lookup(KEY_2));
        assertEquals(maxFit, bc.getActiveCount());

        // Now two files should be full.
        assertEquals(INDEX_SIZE, idxFile.length());
        assertEquals(DATA_HEADER_SIZE + maxFit * (BLOB_HEADER_SIZE + DATA_0.length),
                data0File.length());
        assertEquals(DATA_HEADER_SIZE + maxFit * (BLOB_HEADER_SIZE + DATA_0.length),
                data1File.length());

        bc.close();
    }

    @MediumTest
    public void testEntryLimit() throws IOException {
        String name = TEST_FILE_NAME;
        BlobCache bc;
        File idxFile = new File(name + ".idx");
        File data0File = new File(name + ".0");
        File data1File = new File(name + ".1");
        int maxEntries = 10;
        int maxFit = maxEntries / 2;
        int indexSize = HEADER_SIZE + maxEntries * 12 * 2;

        // Create a brand new cache with a small entry limit.
        bc = new BlobCache(name, maxEntries, MAX_BYTES, true);

        // Fill to just before flipping
        for (int i = 0; i < maxFit; i++) {
            genData(DATA_0, i);
            bc.insert(i, DATA_0);
        }
        assertEquals(maxFit, bc.getActiveCount());

        // Check the file size.
        assertEquals(indexSize, idxFile.length());
        assertEquals(DATA_HEADER_SIZE + maxFit * (BLOB_HEADER_SIZE + DATA_0.length),
                data0File.length());
        assertEquals(DATA_HEADER_SIZE, data1File.length());

        // Insert one and make it flip
        genData(DATA_0, 777);
        bc.insert(777, DATA_0);
        assertEquals(1, bc.getActiveCount());

        // Check the file size.
        assertEquals(indexSize, idxFile.length());
        assertEquals(DATA_HEADER_SIZE + maxFit * (BLOB_HEADER_SIZE + DATA_0.length),
                data0File.length());
        assertEquals(DATA_HEADER_SIZE + 1 * (BLOB_HEADER_SIZE + DATA_0.length),
                data1File.length());
        bc.close();
    }

    @LargeTest
    public void testDataIntegrity() throws IOException {
        String name = TEST_FILE_NAME;
        File idxFile = new File(name + ".idx");
        File data0File = new File(name + ".0");
        File data1File = new File(name + ".1");
        RandomAccessFile f;

        Log.v(TAG, "It should be readable if the content is not changed.");
        prepareNewCache();
        f = new RandomAccessFile(data0File, "rw");
        f.seek(1);
        byte b = f.readByte();
        f.seek(1);
        f.write(b);
        f.close();
        assertReadable();

        Log.v(TAG, "Change the data file magic field");
        prepareNewCache();
        f = new RandomAccessFile(data0File, "rw");
        f.seek(1);
        f.write(0xFF);
        f.close();
        assertUnreadable();

        prepareNewCache();
        f = new RandomAccessFile(data1File, "rw");
        f.write(0xFF);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the blob key");
        prepareNewCache();
        f = new RandomAccessFile(data0File, "rw");
        f.seek(4);
        f.write(0x00);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the blob checksum");
        prepareNewCache();
        f = new RandomAccessFile(data0File, "rw");
        f.seek(4 + 8);
        f.write(0x00);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the blob offset");
        prepareNewCache();
        f = new RandomAccessFile(data0File, "rw");
        f.seek(4 + 12);
        f.write(0x20);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the blob length: some other value");
        prepareNewCache();
        f = new RandomAccessFile(data0File, "rw");
        f.seek(4 + 16);
        f.write(0x20);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the blob length: -1");
        prepareNewCache();
        f = new RandomAccessFile(data0File, "rw");
        f.seek(4 + 16);
        f.writeInt(0xFFFFFFFF);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the blob length: big value");
        prepareNewCache();
        f = new RandomAccessFile(data0File, "rw");
        f.seek(4 + 16);
        f.writeInt(0xFFFFFF00);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the blob content");
        prepareNewCache();
        f = new RandomAccessFile(data0File, "rw");
        f.seek(4 + 20);
        f.write(0x01);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the index magic");
        prepareNewCache();
        f = new RandomAccessFile(idxFile, "rw");
        f.seek(1);
        f.write(0x00);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the active region");
        prepareNewCache();
        f = new RandomAccessFile(idxFile, "rw");
        f.seek(12);
        f.write(0x01);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the reserved data");
        prepareNewCache();
        f = new RandomAccessFile(idxFile, "rw");
        f.seek(24);
        f.write(0x01);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the checksum");
        prepareNewCache();
        f = new RandomAccessFile(idxFile, "rw");
        f.seek(29);
        f.write(0x00);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the key");
        prepareNewCache();
        f = new RandomAccessFile(idxFile, "rw");
        f.seek(32 + 12 * (KEY_1 % MAX_ENTRIES));
        f.write(0x00);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the offset");
        prepareNewCache();
        f = new RandomAccessFile(idxFile, "rw");
        f.seek(32 + 12 * (KEY_1 % MAX_ENTRIES) + 8);
        f.write(0x05);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Change the offset");
        prepareNewCache();
        f = new RandomAccessFile(idxFile, "rw");
        f.seek(32 + 12 * (KEY_1 % MAX_ENTRIES) + 8 + 3);
        f.write(0xFF);
        f.close();
        assertUnreadable();

        Log.v(TAG, "Garbage index");
        prepareNewCache();
        f = new RandomAccessFile(idxFile, "rw");
        int n = (int) idxFile.length();
        f.seek(32);
        byte[] garbage = new byte[1024];
        for (int i = 0; i < garbage.length; i++) {
            garbage[i] = (byte) 0x80;
        }
        int i = 32;
        while (i < n) {
            int todo = Math.min(garbage.length, n - i);
            f.write(garbage, 0, todo);
            i += todo;
        }
        f.close();
        assertUnreadable();
    }

    // Create a brand new cache and put one entry into it.
    private void prepareNewCache() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, MAX_ENTRIES, MAX_BYTES, true);
        genData(DATA_0, 777);
        bc.insert(KEY_1, DATA_0);
        bc.close();
    }

    private void assertReadable() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, MAX_ENTRIES, MAX_BYTES, false);
        genData(DATA_0, 777);
        assertSameData(DATA_0, bc.lookup(KEY_1));
        bc.close();
    }

    private void assertUnreadable() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, MAX_ENTRIES, MAX_BYTES, false);
        genData(DATA_0, 777);
        assertNull(bc.lookup(KEY_1));
        bc.close();
    }

    @LargeTest
    public void testRandomSize() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, MAX_ENTRIES, MAX_BYTES, true);

        // Random size test
        Random rand = new Random(0);
        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[rand.nextInt(MAX_BYTES*12/10)];
            try {
                bc.insert(rand.nextLong(), data);
                if (data.length > MAX_BYTES - 4 - 20) fail();
            } catch (RuntimeException ex) {
                if (data.length <= MAX_BYTES - 4 - 20) fail();
            }
        }

        bc.close();
    }

    @LargeTest
    public void testBandwidth() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, 1000, 10000000, true);

        // Write
        int count = 0;
        byte[] data = new byte[20000];
        long t0 = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            bc.insert(i, data);
            count += data.length;
        }
        bc.syncAll();
        float delta = (System.nanoTime() - t0) * 1e-3f;
        Log.v(TAG, "write bandwidth = " + (count / delta) + " M/s");

        // Copy over
        BlobCache.LookupRequest req = new BlobCache.LookupRequest();
        count = 0;
        t0 = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            req.key = i;
            req.buffer = data;
            if (bc.lookup(req)) {
                count += req.length;
            }
        }
        bc.syncAll();
        delta = (System.nanoTime() - t0) * 1e-3f;
        Log.v(TAG, "copy over bandwidth = " + (count / delta) + " M/s");

        // Read
        count = 0;
        t0 = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            req.key = i;
            req.buffer = data;
            if (bc.lookup(req)) {
                count += req.length;
            }
        }
        bc.syncAll();
        delta = (System.nanoTime() - t0) * 1e-3f;
        Log.v(TAG, "read bandwidth = " + (count / delta) + " M/s");

        bc.close();
    }

    @LargeTest
    public void testSmallSize() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, MAX_ENTRIES, 40, true);

        // Small size test
        Random rand = new Random(0);
        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[rand.nextInt(3)];
            bc.insert(rand.nextLong(), data);
        }

        bc.close();
    }

    @LargeTest
    public void testManyEntries() throws IOException {
        BlobCache bc = new BlobCache(TEST_FILE_NAME, 1, MAX_BYTES, true);

        // Many entries test
        Random rand = new Random(0);
        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[rand.nextInt(10)];
        }

        bc.close();
    }

    private void genData(byte[] data, int seed) {
        for(int i = 0; i < data.length; i++) {
            data[i] = (byte) (seed * i);
        }
    }

    private void assertSameData(byte[] data1, byte[] data2) {
        if (data1 == null && data2 == null) return;
        if (data1 == null || data2 == null) fail();
        if (data1.length != data2.length) fail();
        for (int i = 0; i < data1.length; i++) {
            if (data1[i] != data2[i]) fail();
        }
    }

    private void assertSameData(byte[] data1, byte[] data2, int n) {
        if (data1 == null || data2 == null) fail();
        for (int i = 0; i < n; i++) {
            if (data1[i] != data2[i]) fail();
        }
    }
}
