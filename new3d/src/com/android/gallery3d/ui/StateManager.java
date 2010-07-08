// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.ui;

import android.content.Context;
import android.os.Bundle;

import java.util.Stack;

public class StateManager {
    private static StateManager mInstance;
    private Context mContext;
    private GLRootView mRootView;
    private Stack<StateView> mStateStack = new Stack<StateView>();
    private Stack<Bundle> mBundleStack = new Stack<Bundle>();

    public StateManager(Context context, GLRootView rootView) {
        mContext = context;
        mRootView = rootView;
    }

    public static void initialize(Context context, GLRootView rootView) {
        mInstance = new StateManager(context, rootView);
    }

    public static synchronized StateManager getInstance() {
        if (mInstance == null) throw new IllegalStateException();
        return mInstance;
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

    void popState() {
        if (!mStateStack.isEmpty()) {
            mStateStack.pop();
            mBundleStack.pop();
        }
    }
}
