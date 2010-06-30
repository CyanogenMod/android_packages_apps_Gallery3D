// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.gallery3d.anim;

import com.android.gallery3d.ui.GLCanvas;

public abstract class CanvasAnimation extends Animation {

    public abstract int getCanvasSaveFlags();
    public abstract void apply(GLCanvas canvas);
}
