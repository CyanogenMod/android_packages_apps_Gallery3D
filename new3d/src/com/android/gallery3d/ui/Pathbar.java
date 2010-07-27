/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;

import com.android.gallery3d.R;
import com.android.gallery3d.anim.IntAnimation;
import com.android.gallery3d.util.Utils;

import java.util.ArrayList;
import java.util.NoSuchElementException;

public class Pathbar extends GLView {

    public static final int INDEX_NONE = -1;

    private static final int FONT_COLOR = Color.WHITE;
    private static final int FONT_SIZE = 18;

    private static final int PROGRESS_STEP_COUNT = 8;

    private static final float SHOW_TITLE_THRESHOLD = 0.9f;
    private static final int PUSH_ANIMATION = 1;
    private static final int POP_ANIMATION = 2;
    private static final int NO_ANIMATION = 0;

    private static final int HORIZONTAL_PADDING = 3;
    private static final int VERTICAL_PADDING = 0;

    private static final int UPDATE_PROGRESS = 1;

    private int mAnimationType = NO_ANIMATION;

    private static int sHorizontalPadding = -1;
    private static int sVerticalPadding;
    private static int sFontSize;

    private final ArrayList<Item> mCrumb = new ArrayList<Item>();
    private IntAnimation mAnim;

    private final NinePatchTexture mPathbar;
    private final NinePatchTexture mPathcap;
    private final NinePatchTexture mPathbarPressed;
    private final NinePatchTexture mPathcapPressed;

    private final BasicTexture mProgressIcon;
    private int mProgressStep = 0;
    private boolean mProgressUpdated = false;

    private Handler mHandler;
    private boolean mProgressActivate = false;

    private int mOffsetX;
    private boolean mRequestRender;

    private boolean mPressedItemChanged;
    private OnClickedListener mOnClickedListener;
    private int mPressedIndex = INDEX_NONE;

    public static interface OnClickedListener {
        public void onClicked(Pathbar source, int index);
    }

    private static void initializeStaticVariables(Context context) {
        if (sHorizontalPadding >= 0) return;
        sHorizontalPadding = Utils.dpToPixel(context, HORIZONTAL_PADDING);
        sVerticalPadding = Utils.dpToPixel(context, VERTICAL_PADDING);
        sFontSize = Utils.dpToPixel(context, FONT_SIZE);
    }

    public Pathbar(Context context) {
        initializeStaticVariables(context);

        mProgressIcon = new ResourceTexture(context, R.drawable.ic_spinner);
        mPathbar = new NinePatchTexture(context, R.drawable.pathbar);
        mPathcap = new NinePatchTexture(context, R.drawable.pathcap);
        mPathbarPressed = new NinePatchTexture(context, R.drawable.pathbar_pressed);
        mPathcapPressed = new NinePatchTexture(context, R.drawable.pathcap_pressed);
    }

    public void setOnClickedListener(OnClickedListener listener) {
        mOnClickedListener = listener;
    }

    public Item get(int index) {
        return mCrumb.get(index);
    }

    public int size() {
        return mCrumb.size();
    }

    private void setPressedItem(int index) {
        if (mPressedIndex == index) return;

        Log.v("Pathbar", "index change from " + mPressedIndex + " to " + index);
        mPressedIndex = index;
        mPressedItemChanged = true;
        invalidate();
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        int x = (int) event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPressedItem(getItemAtPosition(x));
                mPressedItemChanged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                setPressedItem(getItemAtPosition(x));
                break;
            case MotionEvent.ACTION_UP:
                if (!mPressedItemChanged && mOnClickedListener != null) {
                    mOnClickedListener.onClicked(this, mPressedIndex);
                }
                setPressedItem(INDEX_NONE);

        }
        return true;
    }

    private int getItemAtPosition(int x) {
        int xoffset = 0;
        int size = mCrumb.size();
        int padding = mPathbar.getPaddings().right;

        for (int i = 0, n = size - 1; i < n; ++i) {
            xoffset += getItemWidthWithoutTitle(mCrumb.get(i)) + padding;
            if (xoffset > x) return i;
        }
        if (size > 0) {
            int i = size - 1;
            xoffset += getItemWidth(mCrumb.get(i)) + padding;
            if (xoffset > x) return i;
        }
        return INDEX_NONE;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int height = mProgressIcon.getHeight();
        for (Item item : mCrumb) {
            height = Math.max(height, item.mIcon.getHeight());
            height = Math.max(height, item.mTitle.getHeight());
        }
        height += sVerticalPadding * 2;
        MeasureHelper.getInstance(this)
                .setPreferredContentSize(Integer.MAX_VALUE, height)
                .measure(widthSpec, heightSpec);
    }

    private StringTexture getStringTexture(String text) {
        return StringTexture.newInstance(text, sFontSize, FONT_COLOR);
    }

    @Override
    protected void onAttachToRoot(GLRoot root) {
        super.onAttachToRoot(root);
        mHandler = new SynchronizedHandler(root) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case UPDATE_PROGRESS:
                        if (++mProgressStep == PROGRESS_STEP_COUNT) {
                            mProgressStep = 0;
                        }
                        mProgressUpdated = false;
                        invalidate();
                }
            }
        };
    }

    public void push(BasicTexture icon, String title) {
        mCrumb.add(new Item(icon, getStringTexture(title)));
        if (mCrumb.size() > 1) {
            Item item = mCrumb.get(mCrumb.size() - 2);
            mAnim = new IntAnimation(
                    getItemWidth(item), getItemWidthWithoutTitle(item), 300);
            mAnim.start();
            mAnimationType = PUSH_ANIMATION;
        }
        invalidate();
    }

    public Item pop() {
        if (mCrumb.isEmpty()) throw new NoSuchElementException();
        Item result = mCrumb.remove(mCrumb.size() - 1);
        if (mCrumb.size() > 0) {
            Item item = mCrumb.get(mCrumb.size() - 1);
            mAnim = new IntAnimation(
                    getItemWidthWithoutTitle(item), getItemWidth(item), 300);
            mAnim.start();
            mAnimationType = POP_ANIMATION;
        }
        invalidate();
        return result;
    }

    public void pop(int number) {
        if (mCrumb.size() < number) throw new IllegalArgumentException();

        int index = mCrumb.size() - 1;
        for (int i = 0; i < number; ++i) {
            mCrumb.remove(index - i);
        }
        if (mCrumb.size() > 0) {
            Item item = mCrumb.get(mCrumb.size() - 1);
            mAnim = new IntAnimation(
                    getItemWidthWithoutTitle(item), getItemWidth(item), 300);
            mAnim.start();
            mAnimationType = POP_ANIMATION;
        }
        invalidate();
    }

    public Item getLast() {
        if (mCrumb.isEmpty()) throw new NoSuchElementException();
        return mCrumb.get(mCrumb.size() - 1);
    }

    private static int getItemWidth(Item item) {
        return sHorizontalPadding * 3
                + item.mIcon.getWidth() + item.mTitle.getWidth();
    }

    private static int getItemWidthWithoutTitle(Item item) {
        return sHorizontalPadding * 2 + item.mIcon.getWidth();
    }

    private void renderItem(GLCanvas canvas, BasicTexture icon,
            StringTexture title, NinePatchTexture background, int width) {
        int height = getHeight();
        int offsetX = mOffsetX;

        Rect p = background.getPaddings();
        background.draw(
                canvas, offsetX - p.left, 0, width + p.left + p.right, height);

        int offsetY = (height - icon.getHeight()) / 2;
        offsetX += sHorizontalPadding;
        if (icon != mProgressIcon) {
            icon.draw(canvas, offsetX, offsetY);
        } else {
            int degrees = mProgressStep * 360 / PROGRESS_STEP_COUNT;
            int pivotX = offsetX + icon.getWidth() / 2;
            int pivotY = offsetY + icon.getHeight() / 2;
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            canvas.translate(pivotX, pivotY, 0);
            canvas.rotate(degrees, 0, 0, 1);
            canvas.translate(-pivotX, -pivotY, 0);
            icon.draw(canvas, offsetX, offsetY);
            canvas.restore();
            if (!mProgressUpdated) {
                mProgressUpdated = true;
                mHandler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 125);
            }
        }
        offsetX += icon.getWidth();
        if (title != null) {
            offsetY = (height - title.getHeight()) / 2;
            title.draw(canvas, offsetX, offsetY);
        }
        mOffsetX += width + p.right;
    }

    private void renderItemWithAnimation(GLCanvas canvas, BasicTexture icon,
            StringTexture title, NinePatchTexture background) {

        boolean moreAnim = mAnim.calculate(canvas.currentAnimationTimeMillis());

        int width = mAnim.get();
        if (width < (3 * sHorizontalPadding
                + icon.getWidth() + title.getWidth()) * SHOW_TITLE_THRESHOLD) {
            title = null;
        }
        if (!moreAnim) {
            mAnimationType = NO_ANIMATION;
            mAnim = null;
        } else {
            mRequestRender = true;
        }
        renderItem(canvas, icon, title, background, width);
    }

    @Override
    protected void render(GLCanvas canvas) {
        mRequestRender = false;
        mOffsetX = 0;

        int size = mCrumb.size();
        for (int i = 0, n = size - 2; i < n; ++i) {
            Item item = mCrumb.get(i);
            int width = getItemWidthWithoutTitle(item);
            renderItem(canvas, item.mIcon, null,
                    i == mPressedIndex ? mPathbarPressed : mPathbar, width);
        }

        // render the 2nd last item
        if (size >= 2) {
            int index = size - 2;
            Item item = mCrumb.get(index);
            NinePatchTexture bg =
                    index == mPressedIndex ? mPathbarPressed : mPathbar;
            if (mAnimationType == PUSH_ANIMATION) {
                renderItemWithAnimation(canvas, item.mIcon, item.mTitle, bg);
            } else {
                renderItem(canvas, item.mIcon,
                        null, bg, getItemWidthWithoutTitle(item));
            }
        }

        // render the last item
        if (size >= 1) {
            int index = size - 1;
            Item item = mCrumb.get(index);
            BasicTexture icon = mProgressActivate ? mProgressIcon : item.mIcon;
            NinePatchTexture bg =
                    index == mPressedIndex ? mPathcapPressed : mPathcap;
            if (mAnimationType == POP_ANIMATION) {
                renderItemWithAnimation(canvas, icon, item.mTitle, bg);
            } else {
                renderItem(canvas, icon, item.mTitle, bg, getItemWidth(item));
            }
        }
        if (mRequestRender) invalidate();
    }

    public void startProgressAnimation() {
        if (mProgressActivate) return;

        mProgressActivate = true;
        mHandler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 125);
        invalidate();
    }

    public void stopProgressAnimation() {
        if (!mProgressActivate) return;

        mProgressActivate = false;
        mHandler.removeMessages(UPDATE_PROGRESS);
        invalidate();
    }

    public class Item {
        private BasicTexture mIcon;
        private StringTexture mTitle;

        Item(BasicTexture icon, StringTexture title) {
            mIcon = icon;
            mTitle = title;
        }

        public void setIcon(BasicTexture icon) {
            if (icon == mIcon) return;
            mIcon = icon;
            invalidate();
        }

        public void setTitle(String title) {
            mTitle = getStringTexture(title);
            invalidate();
        }
    }
}
