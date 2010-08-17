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

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootStub;

import android.content.ContentResolver;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;

// This test reads real data directly and dump information out in the log.
public class RealDataTest extends AndroidTestCase {
    private static final String TAG = "RealDataTest";
    private HashSet<Long> mUsedId = new HashSet<Long>();

    @LargeTest
    public void testRealData() {
        mUsedId.clear();
        run(new TestLocalImageThread());
        run(new TestLocalVideoThread());
        run(new TestPicasaThread());
    }

    static void run(Thread t) {
        t.start();
        try {
            t.join();
        } catch (InterruptedException ex) {
            fail();
        }
    }

    class TestLocalImageThread extends Thread {
        public void run() {
            Looper.prepare();
            GalleryContext context = new GalleryContextMock(
                    mContext,
                    mContext.getContentResolver());

            LocalAlbumSet albumSet = new LocalAlbumSet(context, true);
            MyListener listener = new MyListener();
            albumSet.setContentListener(listener);
            albumSet.reload();
            Looper.loop();

            Log.v(TAG, "LocalAlbumSet (Image)");
            dumpMediaSet(albumSet, "");
            assertEquals(1, listener.count);
        }
    }

    class TestLocalVideoThread extends Thread {
        public void run() {
            Looper.prepare();
            GalleryContext context = new GalleryContextMock(
                    mContext,
                    mContext.getContentResolver());

            LocalAlbumSet albumSet = new LocalAlbumSet(context, false);
            MyListener listener = new MyListener();
            albumSet.setContentListener(listener);
            albumSet.reload();
            Looper.loop();

            Log.v(TAG, "LocalAlbumSet (Video)");
            dumpMediaSet(albumSet, "");
            assertEquals(1, listener.count);
        }
    }

    class TestPicasaThread extends Thread {
        public void run() {
            Looper.prepare();
            GalleryContext context = new GalleryContextMock(
                    mContext,
                    mContext.getContentResolver());

            PicasaAlbumSet albumSet = new PicasaAlbumSet(context);
            MyListener listener = new MyListener();
            albumSet.setContentListener(listener);
            albumSet.reload();
            Looper.loop();

            Log.v(TAG, "PicasaAlbumSet");
            dumpMediaSet(albumSet, "");
            assertEquals(1, listener.count);
        }
    }

    void dumpMediaSet(MediaSet set, String prefix) {
        Log.v(TAG, "getName() = " + set.getName());
        Log.v(TAG, "getUniqueId() = " + Long.toHexString(set.getUniqueId()));
        Log.v(TAG, "getMediaItemCount() = " + set.getMediaItemCount());
        Log.v(TAG, "getSubMediaSetCount() = " + set.getSubMediaSetCount());
        Log.v(TAG, "getTotalMediaItemCount() = " + set.getTotalMediaItemCount());
        assertNewId(set.getUniqueId());
        for (int i = 0, n = set.getSubMediaSetCount(); i < n; i++) {
            MediaSet sub = set.getSubMediaSet(i);
            Log.v(TAG, prefix + "got set " + i);
            dumpMediaSet(sub, prefix + "  ");
        }
        for (int i = 0, n = set.getMediaItemCount(); i < n; i += 10) {
            ArrayList<MediaItem> list = set.getMediaItem(i, 10);
            Log.v(TAG, prefix + "got item " + i + " (+" + list.size() + ")");
            for (MediaItem item : list) {
                dumpMediaItem(item, prefix + "..");
            }
        }
    }

    void dumpMediaItem(MediaItem item, String prefix) {
        assertNewId(item.getUniqueId());
        Log.v(TAG, prefix + "getUniqueId() = "
                + Long.toHexString(item.getUniqueId()));
    }

    void assertNewId(Long key) {
        assertFalse(mUsedId.contains(key));
        mUsedId.add(key);
    }

    static class MyListener implements MediaSet.MediaSetListener {
        int count;
        public void onContentChanged() {
            count++;
            Looper.myLooper().quit();
        }
    }
}
