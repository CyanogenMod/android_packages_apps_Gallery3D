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

package com.android.gallery3d.ui;

import android.content.Context;
import android.os.Vibrator;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class SelectionManager {
    private Set<Integer> mSelectedSet;
    private Vibrator mVibrator;
    private final SelectionDrawer mDrawer;

    public interface SelectionChangeListener {
        public void onSelectionChange(int index);
        public void onSelectionModeChange();
    }

    private final SelectionChangeListener mListener;

    public SelectionManager(Context context, SelectionChangeListener listener) {
        mListener = listener;
        mDrawer = new SelectionDrawer(context);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public SelectionDrawer getSelectionDrawer() {
        return mDrawer;
    }

    public boolean isSelectionMode() {
        return mSelectedSet != null;
    }

    public void leaveSelectionMode() {
        mSelectedSet = null;
        mDrawer.setSelectionMode(false);
    }

    public void switchSelectionMode(int slotIndex) {
        if (mSelectedSet == null) {
            mSelectedSet = new HashSet<Integer>();
            mVibrator.vibrate(100);
            mSelectedSet.add(slotIndex);
            mDrawer.setSelectionMode(true);
            mListener.onSelectionModeChange();
            return;
        }

        boolean selected = mSelectedSet.contains(slotIndex);
        if (selected) {
            mSelectedSet.remove(slotIndex);
        } else {
            mSelectedSet = null;
            leaveSelectionMode();
        }
    }

    public boolean isSlotSelected(int slotIndex) {
        return (mSelectedSet != null) && mSelectedSet.contains(slotIndex);
    }

    public void selectSlot(int slotIndex) {
        if (mSelectedSet != null) {
            if (mSelectedSet.contains(slotIndex)) {
                mSelectedSet.remove(slotIndex);
            } else {
                mSelectedSet.add(slotIndex);
            }
            mListener.onSelectionChange(slotIndex);
        }
    }
}
