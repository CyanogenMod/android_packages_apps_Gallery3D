package com.android.gallery3d.ui;

public class OverlayLayout extends GLView {

    @Override
    protected void onLayout(
            boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            GLView component = getComponent(i);
            component.layout(0, 0, width, height);
        }
    }
}
