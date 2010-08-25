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

import javax.microedition.khronos.opengles.GL11;

// RawTexture is used for texture created by glCopyTexImage2D.
//
// It will throw RuntimeException in onBind() if used with a different GL
// context. It is only used internally by copyTexture() in GLCanvas.
class RawTexture extends BasicTexture {

    private RawTexture(GLCanvas canvas, int id) {
        super(canvas, id, STATE_LOADED);
    }

    public static RawTexture newInstance(GLCanvas canvas) {
        int[] textureId = new int[1];
        GL11 gl = canvas.getGLInstance();
        gl.glGenTextures(1, textureId, 0);
        return new RawTexture(canvas, textureId[0]);
    }

    @Override
    protected boolean onBind(GLCanvas canvas) {
        if (mCanvasRef.get() != canvas) {
            throw new RuntimeException("cannot bind to different canvas");
        }
        return true;
    }

    public boolean isOpaque() {
        return true;
    }

    @Override
    public void yield() {
        // we cannot free the texture because we have no backup.
    }
}
