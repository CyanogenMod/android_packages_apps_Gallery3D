package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;

public class MenuItem extends IconLabel {
    private boolean mSelected;
    private Texture mHighlight;

    public MenuItem(Context context, int icon, int label) {
        super(context, icon, label);
    }

    public void setHighlight(Texture texture) {
        mHighlight = texture;
    }

    protected void setSelected(boolean selected) {
        if (selected == mSelected) return;
        mSelected = selected;
        invalidate();
    }

    @Override
    protected void render(GLCanvas canvas) {
        if (mSelected) {
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
