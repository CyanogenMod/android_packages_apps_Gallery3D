package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;

public class MenuButton extends IconLabel {

    private boolean mPressed;
    private Texture mHighlight;
    private OnClickedListener mOnClickListener;

    public MenuButton(Context context, int icon, int label) {
        super(context, icon, label);
    }

    public void setHighlight(Texture texture) {
        mHighlight = texture;
    }

    @SuppressWarnings("fallthrough")
    @Override
    protected boolean onTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPressed = true;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (mOnClickListener != null) {
                    mOnClickListener.onClicked(this);
                }
                // fall-through
            case MotionEvent.ACTION_CANCEL:
                mPressed = false;
                invalidate();
                break;
        }
        return true;
    }

    @Override
    protected void render(GLCanvas canvas) {
        if (mPressed) {
            int width = getWidth();
            int height = getHeight();
            if (mHighlight instanceof NinePatchTexture) {
                Rect p = ((NinePatchTexture) mHighlight).getPaddings();
                mHighlight.draw(canvas, -p.left, -p.top,
                        width + p.left + p.right, height + p.top + p.bottom);
            } else {
                mHighlight.draw(canvas, 0, 0, width, height);
            }
        }
        super.render(canvas);
    }
}
