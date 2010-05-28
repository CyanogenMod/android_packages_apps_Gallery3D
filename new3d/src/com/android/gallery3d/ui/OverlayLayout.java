package com.android.gallery3d.ui;

public class OverlayLayout extends GLView {

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            GLView component = getComponent(i);
            component.layout(0, 0, width, height);
        }
    }
}
