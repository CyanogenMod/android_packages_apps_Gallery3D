package com.android.gallery3d.ui;


public class SlotView extends GLView {

    public static interface Model {
        public int size();
        public int getSlotHeight();
        public int getSlotWidth();
        public void putSlot(int index, int x, int y, DisplayItemPanel panel);
        public void freeSlot(int index);
    }

    private Model mModel;
    private final DisplayItemPanel mPanel;

    private int mVerticalGap;
    private int mHorizontalGap;
    private int mSlotWidth;
    private int mSlotHeight;
    private int mRowCount;
    private int mScroll;
    private int mScrollLimit;

    public SlotView() {
        mPanel = new DisplayItemPanel();
        super.addComponent(mPanel);
    }

    @Override
    public void addComponent(GLView view) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeComponent(GLView view) {
        throw new UnsupportedOperationException();
    }

    public void setModel(Model model) {
        if (model == mModel) return;
        mModel = model;
        if (model != null) initializeLayoutParams();
        notifyDataChanged();
    }

    private void initializeLayoutParams() {
        int size = mModel.size();
        int slotWidth = mSlotWidth = mModel.getSlotWidth();
        int slotHeight = mSlotHeight = mModel.getSlotHeight();
        int totalHeight= getHeight();
        int rowCount = totalHeight / mSlotHeight;
        if (rowCount == 0) rowCount = 1;
        mRowCount = rowCount;
        int hGap = mHorizontalGap = 10;
        int vGap = mVerticalGap = (
                totalHeight - rowCount * slotHeight) / (rowCount + 1);
        mScrollLimit = ((size + rowCount - 1) / rowCount) * (hGap + slotWidth)
                + hGap - getWidth();
    }

    @Override
    protected void onLayout(boolean changeSize, int l, int t, int r, int b) {
        mPanel.layout(0, 0, r - l, b - t);
        initializeLayoutParams();
        mScroll = Util.clamp(mScroll, 0, mScrollLimit);
        layoutContent();
    }

    public void notifyDataChanged() {
        mScroll = 0;
        notifyDataInvalidate();
    }

    public void notifyDataInvalidate() {
        layoutContent();
    }

    private void layoutContent() {
        int hGap = mHorizontalGap;
        int vGap = mVerticalGap;
        int slotWidth = mSlotWidth;
        int slotHeight = mSlotHeight;
        int rowCount = mRowCount;

        int colWidth = hGap + slotWidth;
        int rowHeight = vGap + slotHeight;

        int startColumn = mScroll / colWidth;
        int endColumn = (mScroll + getWidth() + slotWidth - 1) / colWidth;

        int startIndex = startColumn * rowCount;
        int endIndex = Math.min(endColumn * rowCount, mModel.size());

        mPanel.prepareTransition();
        for (int i = startIndex; i < endIndex; ++i) {
            int col = i / rowCount;
            int row = i - col * rowCount;
            mModel.putSlot(i, col * colWidth + hGap - mScroll,
                    row * rowHeight + vGap, mPanel);
        }
        mPanel.startTransition();
        invalidate();
    }
}
