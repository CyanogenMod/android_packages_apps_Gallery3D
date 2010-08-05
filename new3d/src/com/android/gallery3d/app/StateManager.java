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

package com.android.gallery3d.app;

import android.os.Bundle;
import android.os.Parcelable;

import com.android.gallery3d.util.Utils;

import java.util.Stack;

public class StateManager {
    private static final String TAG = "StateManager";
    private boolean mIsResumed = false;

    private static final String MAIN_KEY = "activity-state";
    private static final String DATA_KEY = "data";
    private static final String STATE_KEY = "bundle";
    private static final String CLASS_KEY = "class";

    private GalleryContext mContext;
    private Stack<StateEntry> mStack = new Stack<StateEntry>();

    public StateManager(GalleryContext context) {
        mContext = context;
    }

    public void startState(Class<? extends ActivityState> stateClass,
            Bundle data) {
        ActivityState stateView = null;
        try {
            stateView = stateClass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        stateView.setContext(mContext);

        if (!mStack.isEmpty()) {
            mStack.peek().activityState.onPause();
        }

        mStack.push(new StateEntry(data, stateView));
        stateView.onCreate(data, null);
        if (mIsResumed) stateView.onResume();
    }

    public void resume() {
        if (mIsResumed) return;
        mIsResumed = true;
        if (!mStack.isEmpty()) getTopState().onResume();
    }

    public void pause() {
        if (!mIsResumed) return;
        mIsResumed = false;
        if (!mStack.isEmpty()) getTopState().onPause();
    }

    void finishState(ActivityState stateView) {
        if (stateView != mStack.peek().activityState) {
            throw new IllegalArgumentException("The stateview to be finished"
                    + " is not at the top of the stack!");
        }
        // Remove the top stateview.
        mStack.pop();
        stateView.onPause();
        stateView.onDestroy();

        if (mStack.isEmpty()) {
            ((Gallery) mContext).finish();
        } else {
            // Restore the immediately previous stateview
            mStack.peek().activityState.onResume();
        }
    }

    @SuppressWarnings("unchecked")
    public void restoreFromState(Bundle inState) {
        Parcelable list[] = inState.getParcelableArray(MAIN_KEY);

        for (Parcelable parcelable : list) {
            Bundle bundle = (Bundle) parcelable;
            Class<? extends ActivityState> klass =
                    (Class<? extends ActivityState>) bundle.getSerializable(CLASS_KEY);

            Bundle data = bundle.getBundle(DATA_KEY);
            Bundle state = bundle.getBundle(STATE_KEY);

            ActivityState activityState;
            try {
                activityState = klass.newInstance();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            activityState.setContext(mContext);
            activityState.onCreate(data, state);
            mStack.push(new StateEntry(data, activityState));
        }
    }

    public void saveState(Bundle outState) {
        Parcelable list[] = new Parcelable[mStack.size()];

        int i = 0;
        for (StateEntry entry : mStack) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(CLASS_KEY, entry.activityState.getClass());
            bundle.putBundle(DATA_KEY, entry.data);
            Bundle state = new Bundle();
            entry.activityState.onSaveState(state);
            bundle.putBundle(STATE_KEY, state);
            list[i++] = bundle;
        }
        outState.putParcelableArray(MAIN_KEY, list);
    }

    public ActivityState getTopState() {
        Utils.Assert(!mStack.isEmpty());
        return mStack.peek().activityState;
    }

    private static class StateEntry {
        public Bundle data;
        public ActivityState activityState;

        public StateEntry(Bundle data, ActivityState state) {
            this.data = data;
            this.activityState = state;
        }
    }
}
