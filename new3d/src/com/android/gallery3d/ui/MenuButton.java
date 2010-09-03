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
import android.graphics.Rect;
import android.view.MotionEvent;

public class MenuButton extends IconLabel {

    private boolean mPressed;
    private Texture mHighlight;
    private OnClickedListener mOnClickListener;

    public interface OnClickedListener {
        public void onClicked(GLView source);
    }

    public MenuButton(Context context, int icon, int label) {
        super(context, icon, label);
    }

    public void setOnClickListener(OnClickedListener listener) {
        mOnClickListener = listener;
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
