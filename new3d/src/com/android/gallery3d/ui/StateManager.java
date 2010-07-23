// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.ui;

import android.os.Bundle;

import com.android.gallery3d.app.Gallery;
import com.android.gallery3d.app.GalleryContext;

import java.util.Stack;

public class StateManager {
    private static final String TAG = "StateManager";

    private GalleryContext mContext;
    private GLRootView mRootView;
    private Stack<StateView> mStateStack = new Stack<StateView>();
    private Stack<Bundle> mBundleStack = new Stack<Bundle>();

    public StateManager(GalleryContext context, GLRootView rootView) {
        mContext = context;
        mRootView = rootView;
    }

    public void startStateView(Class<? extends StateView> stateClass,
            Bundle data) {
        StateView stateView = null;
        try {
            stateView = stateClass.newInstance();
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InstantiationException e) {
            throw new AssertionError(e);
        }
        stateView.setContext(mContext);

        if (!mStateStack.isEmpty()) {
            mStateStack.peek().onPause();
        }

        mRootView.setContentPane(stateView);
        mStateStack.push(stateView);
        mBundleStack.push(data);
        stateView.onStart(data);
    }

    public StateView peekState() {
        return mStateStack.isEmpty() ? null : mStateStack.peek();
    }

    void finish(StateView stateView) {
        if (stateView != mStateStack.peek()) {
            throw new IllegalArgumentException("The stateview to be finished"
                    + " is not at the top of the stack!");
        }
        // Remove the top stateview.
        popState();
        stateView.onPause();
        stateView.onDestroy();
        if (mStateStack.isEmpty()) {
            ((Gallery) mContext).finish();
        } else {
            // Restore the immediately previous stateview
            StateView restoredState = mStateStack.peek();
            mRootView.setContentPane(restoredState);
            restoredState.onResume();
        }
    }

    private void popState() {
        mStateStack.pop();
        mBundleStack.pop();
    }
}
