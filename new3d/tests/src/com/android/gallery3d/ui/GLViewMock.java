package com.android.gallery3d.ui;

class GLViewMock extends GLView {
    // onAttachToRoot
    int mOnAttachCalled;
    GLRoot mRoot;
    // onDetachFromRoot
    int mOnDetachCalled;
    // onVisibilityChanged
    int mOnVisibilityChangedCalled;
    // onLayout
    int mOnLayoutCalled;
    boolean mOnLayoutChangeSize;
    // renderBackground
    int mRenderBackgroundCalled;
    // onMeasure
    int mOnMeasureCalled;
    int mOnMeasureWidthSpec;
    int mOnMeasureHeightSpec;

    @Override
    public void onAttachToRoot(GLRoot root) {
        mRoot = root;
        mOnAttachCalled++;
        super.onAttachToRoot(root);
    }

    @Override
    public void onDetachFromRoot() {
        mRoot = null;
        mOnDetachCalled++;
        super.onDetachFromRoot();
    }

    @Override
    protected void onVisibilityChanged(int visibility) {
        mOnVisibilityChangedCalled++;
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top,
            int right, int bottom) {
        mOnLayoutCalled++;
        mOnLayoutChangeSize = changeSize;
        // call children's layout.
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            GLView item = getComponent(i);
            item.layout(left, top, right, bottom);
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        mOnMeasureCalled++;
        mOnMeasureWidthSpec = widthSpec;
        mOnMeasureHeightSpec = heightSpec;
        // call children's measure.
        for (int i = 0, n = getComponentCount(); i < n; ++i) {
            GLView item = getComponent(i);
            item.measure(widthSpec, heightSpec);
        }
        setMeasuredSize(widthSpec, heightSpec);
    }

    @Override
    protected void renderBackground(GLCanvas view) {
        mRenderBackgroundCalled++;
    }
}
