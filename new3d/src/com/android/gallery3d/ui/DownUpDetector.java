package com.android.gallery3d.ui;

import android.view.MotionEvent;

public class DownUpDetector {
    public interface DownUpListener {
        void onDown(MotionEvent e);
        void onUp(MotionEvent e);
    }

    private boolean mStillDown;
    private DownUpListener mListener;

    public DownUpDetector(DownUpListener listener) {
        mListener = listener;
    }

    private void setState(boolean down, MotionEvent e) {
        if (down == mStillDown) return;
        mStillDown = down;
        if (down) {
            mListener.onDown(e);
        } else {
            mListener.onUp(e);
        }
    }

    public void onTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            setState(true, ev);
            break;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_POINTER_DOWN:  // Multitouch event - abort.
            setState(false, ev);
            break;
        }
    }

    public boolean isDown() {
        return mStillDown;
    }
}
