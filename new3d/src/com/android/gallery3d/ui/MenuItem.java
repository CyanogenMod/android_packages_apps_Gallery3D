package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;

import javax.microedition.khronos.opengles.GL11;

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
    protected void render(GLRootView root, GL11 gl) {
        if (mSelected) {
            int width = getWidth();
            int height = getHeight();
            if (mHighlight instanceof NinePatchTexture) {
                Rect p = ((NinePatchTexture) mHighlight).getPaddings();
                mHighlight.draw(root, -p.left, -p.top,
                        width + p.left + p.right, height + p.top + p.bottom);
            } else {
                mHighlight.draw(root, 0, 0, width, height);
            }
        }
        super.render(root, gl);
    }
}
