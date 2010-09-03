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

import com.android.gallery3d.util.IntArray;
import com.android.gallery3d.util.Utils;

import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLU;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Stack;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

public class GLCanvasImp implements GLCanvas {
    @SuppressWarnings("unused")
    private static final String TAG = "GLCanvasImp";

    private static final float OPAQUE_ALPHA = 0.95f;

    // The first four pairs of (x, y) are for drawing a rectangle, and the last
    // two are for drawing a line.
    private static final float[] BOX_COORDINATES = {
            0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1 };

    private final GL11 mGL;

    private final float mMatrixValues[] = new float[16];
    private final float mTextureMatrixValues[] = new float[16];

    // mapPoints needs 10 input and output numbers.
    private final float mMapPointsBuffer[] = new float[10];

    private final float mTextureColor[] = new float[4];

    private int mBoxCoords;

    private final GLState mGLState;

    private long mAnimationTime;

    private float mAlpha;
    private final Rect mClipRect = new Rect();
    private final Stack<ConfigState> mRestoreStack =
            new Stack<ConfigState>();
    private ConfigState mRecycledRestoreAction;

    private final RectF mDrawTextureSourceRect = new RectF();
    private final RectF mDrawTextureTargetRect = new RectF();
    private final float[] mTempMatrix = new float[32];
    private final IntArray mUnboundTextures = new IntArray();
    private final IntArray mDeleteBuffers = new IntArray();

    // Drawing statistics
    int mCountDrawLine;
    int mCountFillRect;
    int mCountDrawMesh;
    int mCountTextureRect;
    int mCountTextureOES;

    GLCanvasImp(GL11 gl) {
        mGL = gl;
        mGLState = new GLState(gl);
        initialize();
    }

    public void setSize(int width, int height) {
        Utils.Assert(width >= 0 && height >= 0);

        GL11 gl = mGL;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL11.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluOrtho2D(gl, 0, width, 0, height);

        gl.glMatrixMode(GL11.GL_MODELVIEW);
        gl.glLoadIdentity();

        // The positive direction in Y coordinate in OpenGL is from bottom to
        // top, which is different from the coordinate system in Java. So, we
        // flip it here.
        float matrix[] = mMatrixValues;
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(matrix, 0, 0, height, 0);
        Matrix.scaleM(matrix, 0, 1, -1, 1);

        mClipRect.set(0, 0, width, height);
        gl.glScissor(0, 0, width, height);
    }

    public long currentAnimationTimeMillis() {
        return mAnimationTime;
    }

    public void setAlpha(float alpha) {
        Utils.Assert(alpha >= 0 && alpha <= 1);
        mAlpha = alpha;
    }

    public void multiplyAlpha(float alpha) {
        Utils.Assert(alpha >= 0 && alpha <= 1);
        mAlpha *= alpha;
    }

    public float getAlpha() {
        return mAlpha;
    }

    private static ByteBuffer allocateDirectNativeOrderBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    private void initialize() {
        GL11 gl = mGL;

        // First create an nio buffer, then create a VBO from it.
        int size = BOX_COORDINATES.length * Float.SIZE / Byte.SIZE;
        FloatBuffer xyBuffer = allocateDirectNativeOrderBuffer(size).asFloatBuffer();
        xyBuffer.put(BOX_COORDINATES, 0, BOX_COORDINATES.length).position(0);

        int[] name = new int[1];
        gl.glGenBuffers(1, name, 0);
        mBoxCoords = name[0];

        gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, mBoxCoords);
        gl.glBufferData(GL11.GL_ARRAY_BUFFER,
                xyBuffer.capacity() * (Float.SIZE / Byte.SIZE),
                xyBuffer, GL11.GL_STATIC_DRAW);

        gl.glVertexPointer(2, GL11.GL_FLOAT, 0, 0);
        gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);

        // Enable the texture coordinate array for Texture 1
        gl.glClientActiveTexture(GL11.GL_TEXTURE1);
        gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);
        gl.glClientActiveTexture(GL11.GL_TEXTURE0);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        // mMatrixValues will be initialized in setSize()
        mAlpha = 1.0f;
    }

    private static void putRectangle(float x, float y,
            float width, float height, float[] buffer, FloatBuffer pointer) {
        buffer[0] = x;
        buffer[1] = y;
        buffer[2] = x + width;
        buffer[3] = y;
        buffer[4] = x;
        buffer[5] = y + height;
        buffer[6] = x + width;
        buffer[7] = y + height;
        pointer.put(buffer, 0, 8).position(0);
    }

    public void drawLine(int x1, int y1, int x2, int y2, int color) {
        mGLState.setColorMode(color, mAlpha);
        GL11 gl = mGL;

        saveTransform();
        translate(x1, y1, 0);
        scale(x2 - x1, y2 - y1, 1);

        gl.glLoadMatrixf(mMatrixValues, 0);
        gl.glDrawArrays(GL11.GL_LINE_STRIP, 4, 2);

        restoreTransform();
        mCountDrawLine++;
    }

    public void fillRect(float x, float y, float width, float height, int color) {
        mGLState.setColorMode(color, mAlpha);
        GL11 gl = mGL;

        saveTransform();
        translate(x, y, 0);
        scale(width, height, 1);

        gl.glLoadMatrixf(mMatrixValues, 0);
        gl.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

        restoreTransform();
        mCountFillRect++;
    }

    public void translate(float x, float y, float z) {
        Matrix.translateM(mMatrixValues, 0, x, y, z);
    }

    public void scale(float sx, float sy, float sz) {
        Matrix.scaleM(mMatrixValues, 0, sx, sy, sz);
    }

    public void rotate(float angle, float x, float y, float z) {
        float[] temp = mTempMatrix;
        Matrix.setRotateM(temp, 0, angle, x, y, z);
        Matrix.multiplyMM(temp, 16, mMatrixValues, 0, temp, 0);
        System.arraycopy(temp, 16, mMatrixValues, 0, 16);
    }

    private void textureRect(float x, float y, float width, float height) {
        GL11 gl = mGL;

        saveTransform();
        translate(x, y, 0);
        scale(width, height, 1);

        gl.glLoadMatrixf(mMatrixValues, 0);
        gl.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

        restoreTransform();
        mCountTextureRect++;
    }

    public void drawMesh(BasicTexture tex, int x, int y, int xyBuffer,
            int uvBuffer, int indexBuffer, int indexCount) {
        float alpha = mAlpha;
        if (!bindTexture(tex)) return;

        mGLState.setBlendEnabled(!tex.isOpaque() || alpha < OPAQUE_ALPHA);
        mGLState.setTextureAlpha(alpha);

        // Reset the texture matrix. We will set our own texture coordinates
        // below.
        setTextureCoords(0, 0, 1, 1);

        saveTransform();
        translate(x, y, 0);

        mGL.glLoadMatrixf(mMatrixValues, 0);

        mGL.glBindBuffer(GL11.GL_ARRAY_BUFFER, xyBuffer);
        mGL.glVertexPointer(2, GL11.GL_FLOAT, 0, 0);

        mGL.glBindBuffer(GL11.GL_ARRAY_BUFFER, uvBuffer);
        mGL.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);

        mGL.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        mGL.glDrawElements(GL11.GL_TRIANGLE_STRIP,
                indexCount, GL11.GL_UNSIGNED_BYTE, 0);

        mGL.glBindBuffer(GL11.GL_ARRAY_BUFFER, mBoxCoords);
        mGL.glVertexPointer(2, GL11.GL_FLOAT, 0, 0);
        mGL.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);

        restoreTransform();
        mCountDrawMesh++;
    }

    private float[] mapPoints(float matrix[], int x1, int y1, int x2, int y2) {
        float[] point = mMapPointsBuffer;
        int srcOffset = 6;
        point[srcOffset] = x1;
        point[srcOffset + 1] = y1;
        point[srcOffset + 2] = 0;
        point[srcOffset + 3] = 1;

        int resultOffset = 0;
        Matrix.multiplyMV(point, resultOffset, matrix, 0, point, srcOffset);
        point[resultOffset] /= point[resultOffset + 3];
        point[resultOffset + 1] /= point[resultOffset + 3];

        // map the second point
        point[srcOffset] = x2;
        point[srcOffset + 1] = y2;
        resultOffset = 2;
        Matrix.multiplyMV(point, resultOffset, matrix, 0, point, srcOffset);
        point[resultOffset] /= point[resultOffset + 3];
        point[resultOffset + 1] /= point[resultOffset + 3];

        return point;
    }

    public boolean clipRect(int left, int top, int right, int bottom) {
        float point[] = mapPoints(mMatrixValues, left, top, right, bottom);

        // mMatrix could be a rotation matrix. In this case, we need to find
        // the boundaries after rotation. (only handle 90 * n degrees)
        if (point[0] > point[2]) {
            left = (int) point[2];
            right = (int) point[0];
        } else {
            left = (int) point[0];
            right = (int) point[2];
        }
        if (point[1] > point[3]) {
            top = (int) point[3];
            bottom = (int) point[1];
        } else {
            top = (int) point[1];
            bottom = (int) point[3];
        }
        Rect clip = mClipRect;

        boolean intersect = clip.intersect(left, top, right, bottom);
        if (!intersect) clip.set(0, 0, 0, 0);
        mGL.glScissor(clip.left, clip.top, clip.width(), clip.height());
        return intersect;
    }

    private void drawBoundTexture(
            BasicTexture texture, int x, int y, int width, int height) {
        // Test whether it has been rotated or flipped, if so, glDrawTexiOES
        // won't work
        if (isMatrixRotatedOrFlipped(mMatrixValues)) {
            setTextureCoords(0, 0,
                    (texture.getWidth() - 0.5f) / texture.getTextureWidth(),
                    (texture.getHeight() - 0.5f) / texture.getTextureHeight());
            textureRect(x, y, width, height);
        } else {
            // draw the rect from bottom-left to top-right
            float points[] = mapPoints(
                    mMatrixValues, x, y + height, x + width, y);
            x = (int) points[0];
            y = (int) points[1];
            width = (int) points[2] - x;
            height = (int) points[3] - y;
            if (width > 0 && height > 0) {
                ((GL11Ext) mGL).glDrawTexiOES(x, y, 0, width, height);
                mCountTextureOES++;
            }
        }
    }

    public void drawTexture(
            BasicTexture texture, int x, int y, int width, int height) {
        drawTexture(texture, x, y, width, height, mAlpha);
    }

    public void drawTexture(BasicTexture texture,
            int x, int y, int width, int height, float alpha) {
        if (width <= 0 || height <= 0) return;

        mGLState.setBlendEnabled(!texture.isOpaque() || alpha < OPAQUE_ALPHA);
        if (!bindTexture(texture)) return;
        mGLState.setTextureAlpha(alpha);
        drawBoundTexture(texture, x, y, width, height);
    }

    public void drawTexture(BasicTexture texture, RectF source, RectF target) {
        if (target.width() <= 0 || target.height() <= 0) return;

        // Copy the input to avoid changing it.
        mDrawTextureSourceRect.set(source);
        mDrawTextureTargetRect.set(target);
        source = mDrawTextureSourceRect;
        target = mDrawTextureTargetRect;

        mGLState.setBlendEnabled(!texture.isOpaque() || mAlpha < OPAQUE_ALPHA);
        if (!bindTexture(texture)) return;
        convertCoordinate(source, target, texture);
        setTextureCoords(source);
        mGLState.setTextureAlpha(mAlpha);
        textureRect(target.left, target.top, target.width(), target.height());
    }

    // This function changes the source coordinate to the texture coordinates.
    // It also clips the source and target coordinates if it is beyond the
    // bound of the texture.
    private void convertCoordinate(RectF source, RectF target,
            BasicTexture texture) {

        int width = texture.getWidth();
        int height = texture.getHeight();
        int texWidth = texture.getTextureWidth();
        int texHeight = texture.getTextureHeight();
        // Convert to texture coordinates
        source.left /= texWidth;
        source.right /= texWidth;
        source.top /= texHeight;
        source.bottom /= texHeight;

        // Clip if the rendering range is beyond the bound of the texture.
        if (width < texWidth) {
            float xBound = (width - 0.5f) / texWidth;
            if (source.right > xBound) {
                target.right = target.left + target.width() *
                        (xBound - source.left) / source.width();
                source.right = xBound;
            }
        }
        if (height < texHeight) {
            float yBound = (height - 0.5f) / texHeight;
            if (source.bottom > yBound) {
                target.bottom = target.top + target.height() *
                        (yBound - source.top) / source.height();
                source.bottom = yBound;
            }
        }
    }

    public void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int w, int h) {
        drawMixed(from, to, ratio, x, y, w, h, mAlpha);
    }

    private boolean bindTexture(BasicTexture texture) {
        if (!texture.onBind(this)) return false;
        mGLState.setTexture2DEnabled(true);
        mGL.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
        return true;
    }

    private void setTextureColor(float r, float g, float b, float alpha) {
        float[] color = mTextureColor;
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = alpha;
    }

    private void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int width, int height, float alpha) {

        if (ratio <= 0) {
            drawTexture(from, x, y, width, height, alpha);
            return;
        } else if (ratio >= 1) {
            drawTexture(to, x, y, width, height, alpha);
            return;
        }

        // In the current implementation the two textures must have the
        // same size.
        Utils.Assert(from.getWidth() == to.getWidth()
                && from.getHeight() == to.getHeight());

        mGLState.setBlendEnabled(!from.isOpaque()
                || !to.isOpaque() || alpha < OPAQUE_ALPHA);

        final GL11 gl = mGL;
        if (!bindTexture(from)) return;

        //
        // The formula we want:
        //     alpha * ((1 - ratio) * from + ratio * to)
        // The formula that GL supports is in the form of:
        //     (1 - combo) * (modulate * from) + combo * to
        //
        // So, we have combo = alpha * ratio
        //     and     modulate = alpha * (1f - ratio) / (1 - combo)
        //
        float comboRatio = alpha * ratio;

        // handle the case that (1 - comboRatio) == 0
        if (alpha < OPAQUE_ALPHA) {
            mGLState.setTextureAlpha(alpha * (1f - ratio) / (1f - comboRatio));
        } else {
            mGLState.setTextureAlpha(1f);
        }

        gl.glActiveTexture(GL11.GL_TEXTURE1);
        if (!bindTexture(to)) {
            // Disable TEXTURE1.
            gl.glDisable(GL11.GL_TEXTURE_2D);
            // Switch back to the default texture unit.
            gl.glActiveTexture(GL11.GL_TEXTURE0);
            return;
        }
        gl.glEnable(GL11.GL_TEXTURE_2D);

        // Interpolate the RGB and alpha values between both textures.
        mGLState.setTexEnvMode(GL11.GL_COMBINE);
        gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_COMBINE_RGB, GL11.GL_INTERPOLATE);
        gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_COMBINE_ALPHA, GL11.GL_INTERPOLATE);

        // Specify the interpolation factor via the alpha component of
        // GL_TEXTURE_ENV_COLORs.
        // We don't use the RGB color, so just give them 0s.
        setTextureColor(0, 0, 0, comboRatio);
        gl.glTexEnvfv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, mTextureColor, 0);

        // Wire up the interpolation factor for RGB.
        gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_SRC2_RGB, GL11.GL_CONSTANT);
        gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_OPERAND2_RGB, GL11.GL_SRC_ALPHA);

        // Wire up the interpolation factor for alpha.
        gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_SRC2_ALPHA, GL11.GL_CONSTANT);
        gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_OPERAND2_ALPHA, GL11.GL_SRC_ALPHA);

        // Draw the combined texture.
        drawBoundTexture(to, x, y, width, height);

        // Disable TEXTURE1.
        gl.glDisable(GL11.GL_TEXTURE_2D);
        // Switch back to the default texture unit.
        gl.glActiveTexture(GL11.GL_TEXTURE0);
    }

    // TODO: the code only work for 2D should get fixed for 3D or removed
    private static final int MSKEW_X = 4;
    private static final int MSKEW_Y = 1;
    private static final int MSCALE_X = 0;
    private static final int MSCALE_Y = 5;

    private static boolean isMatrixRotatedOrFlipped(float matrix[]) {
        final float eps = 1e-5f;
        return Math.abs(matrix[MSKEW_X]) > eps
                || Math.abs(matrix[MSKEW_Y]) > eps
                || matrix[MSCALE_X] < -eps
                || matrix[MSCALE_Y] > eps;
    }

    public BasicTexture copyTexture(int x, int y, int width, int height) {

        if (isMatrixRotatedOrFlipped(mMatrixValues)) {
            throw new IllegalArgumentException("cannot support rotated matrix");
        }
        float points[] = mapPoints(mMatrixValues, x, y + height, x + width, y);
        x = (int) points[0];
        y = (int) points[1];
        width = (int) points[2] - x;
        height = (int) points[3] - y;

        GL11 gl = mGL;

        RawTexture texture = RawTexture.newInstance(this);
        gl.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
        texture.setSize(width, height);

        int[] cropRect = {0,  0, width, height};
        gl.glTexParameteriv(GL11.GL_TEXTURE_2D,
                GL11Ext.GL_TEXTURE_CROP_RECT_OES, cropRect, 0);
        gl.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        gl.glTexParameterf(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        gl.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0,
                GL11.GL_RGB, x, y, texture.getTextureWidth(),
                texture.getTextureHeight(), 0);

        return texture;
    }

    private static class GLState {

        private final GL11 mGL;

        private int mTexEnvMode = GL11.GL_REPLACE;
        private float mTextureAlpha = 1.0f;
        private boolean mTexture2DEnabled = true;
        private boolean mBlendEnabled = true;

        public GLState(GL11 gl) {
            mGL = gl;

            // Disable unused state
            gl.glDisable(GL11.GL_LIGHTING);

            // Enable used features
            gl.glEnable(GL11.GL_DITHER);
            gl.glEnable(GL11.GL_SCISSOR_TEST);

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glEnable(GL11.GL_TEXTURE_2D);

            gl.glTexEnvf(GL11.GL_TEXTURE_ENV,
                    GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);

            // Set the background color
            gl.glClearColor(0f, 0f, 0f, 0f);
            gl.glClearStencil(0);

            gl.glEnable(GL11.GL_BLEND);
            gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

            // We use 565 or 8888 format, so set the alignment to 2 bytes/pixel.
            gl.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 2);
        }

        public void setTexEnvMode(int mode) {
            if (mTexEnvMode == mode) return;
            mTexEnvMode = mode;
            mGL.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, mode);
        }

        public void setTextureAlpha(float alpha) {
            if (mTextureAlpha == alpha) return;
            mTextureAlpha = alpha;
            if (alpha >= OPAQUE_ALPHA) {
                // The alpha is need for those texture without alpha channel
                mGL.glColor4f(1, 1, 1, 1);
                setTexEnvMode(GL11.GL_REPLACE);
            } else {
                mGL.glColor4f(alpha, alpha, alpha, alpha);
                setTexEnvMode(GL11.GL_MODULATE);
            }
        }

        public void setColorMode(int color, float alpha) {
            setBlendEnabled(!Utils.isOpaque(color) || alpha < OPAQUE_ALPHA);

            // Set mTextureAlpha to an invalid value, so that it will reset
            // again in setTextureAlpha(float) later.
            mTextureAlpha = -1.0f;

            setTexture2DEnabled(false);

            float prealpha = (color >>> 24) * alpha * 65535f / 255f / 255f;
            mGL.glColor4x(
                    Math.round(((color >> 16) & 0xFF) * prealpha),
                    Math.round(((color >> 8) & 0xFF) * prealpha),
                    Math.round((color & 0xFF) * prealpha),
                    Math.round(255 * prealpha));
        }

        public void setTexture2DEnabled(boolean enabled) {
            if (mTexture2DEnabled == enabled) return;
            mTexture2DEnabled = enabled;
            if (enabled) {
                mGL.glEnable(GL11.GL_TEXTURE_2D);
            } else {
                mGL.glDisable(GL11.GL_TEXTURE_2D);
            }
        }

        public void setBlendEnabled(boolean enabled) {
            if (mBlendEnabled == enabled) return;
            mBlendEnabled = enabled;
            if (enabled) {
                mGL.glEnable(GL11.GL_BLEND);
            } else {
                mGL.glDisable(GL11.GL_BLEND);
            }
        }
    }

    public GL11 getGLInstance() {
        return mGL;
    }

    public void setCurrentAnimationTimeMillis(long time) {
        Utils.Assert(time >= 0);
        mAnimationTime = time;
    }

    public void clearBuffer() {
        mGL.glClear(GL10.GL_COLOR_BUFFER_BIT);
    }

    private void setTextureCoords(RectF source) {
        setTextureCoords(source.left, source.top, source.right, source.bottom);
    }

    private void setTextureCoords(float left, float top,
            float right, float bottom) {
        mGL.glMatrixMode(GL11.GL_TEXTURE);
        mTextureMatrixValues[0] = right - left;
        mTextureMatrixValues[5] = bottom - top;
        mTextureMatrixValues[10] = 1;
        mTextureMatrixValues[12] = left;
        mTextureMatrixValues[13] = top;
        mTextureMatrixValues[15] = 1;
        mGL.glLoadMatrixf(mTextureMatrixValues, 0);
        mGL.glMatrixMode(GL11.GL_MODELVIEW);
    }

    // unloadTexture and deleteBuffer can be called from the finalizer thread,
    // so we synchronized on the mUnboundTextures object.
    public boolean unloadTexture(BasicTexture t) {
        synchronized (mUnboundTextures) {
            if (!t.isLoaded(this)) return false;
            mUnboundTextures.add(t.mId);
            return true;
        }
    }

    public void deleteBuffer(int bufferId) {
        synchronized (mUnboundTextures) {
            mDeleteBuffers.add(bufferId);
        }
    }

    public void deleteRecycledResources() {
        synchronized (mUnboundTextures) {
            IntArray ids = mUnboundTextures;
            if (ids.size() > 0) {
                mGL.glDeleteTextures(ids.size(), ids.getInternalArray(), 0);
                ids.clear();
            }

            ids = mDeleteBuffers;
            if (ids.size() > 0) {
                mGL.glDeleteBuffers(ids.size(), ids.getInternalArray(), 0);
                ids.clear();
            }
        }
    }

    public int save() {
        return save(SAVE_FLAG_ALL);
    }

    public int save(int saveFlags) {
        ConfigState config = obtainRestoreConfig();

        if ((saveFlags & SAVE_FLAG_ALPHA) != 0) {
            config.mAlpha = mAlpha;
        } else {
            config.mAlpha = -1;
        }

        if ((saveFlags & SAVE_FLAG_CLIP) != 0) {
            config.mRect.set(mClipRect);
        } else {
            config.mRect.left = Integer.MAX_VALUE;
        }

        if ((saveFlags & SAVE_FLAG_MATRIX) != 0) {
            System.arraycopy(mMatrixValues, 0, config.mMatrix, 0, 16);
        } else {
            config.mMatrix[0] = Float.NEGATIVE_INFINITY;
        }

        mRestoreStack.push(config);
        return mRestoreStack.size() - 1;
    }

    public void restore() {
        if (mRestoreStack.isEmpty()) throw new IllegalStateException();
        ConfigState config = mRestoreStack.pop();
        config.restore(this);
        freeRestoreConfig(config);
    }

    private void freeRestoreConfig(ConfigState action) {
        action.mNextFree = mRecycledRestoreAction;
        mRecycledRestoreAction = action;
    }

    private ConfigState obtainRestoreConfig() {
        if (mRecycledRestoreAction != null) {
            ConfigState result = mRecycledRestoreAction;
            mRecycledRestoreAction = result.mNextFree;
            return result;
        }
        return new ConfigState();
    }

    private static class ConfigState {
        float mAlpha;
        Rect mRect = new Rect();
        float mMatrix[] = new float[16];
        ConfigState mNextFree;

        public void restore(GLCanvasImp canvas) {
            if (mAlpha >= 0) canvas.setAlpha(mAlpha);
            if (mRect.left != Integer.MAX_VALUE) {
                Rect rect = mRect;
                canvas.mClipRect.set(rect);
                canvas.mGL.glScissor(
                        rect.left, rect.top, rect.width(), rect.height());
            }
            if (mMatrix[0] != Float.NEGATIVE_INFINITY) {
                System.arraycopy(mMatrix, 0, canvas.mMatrixValues, 0, 16);
            }
        }
    }

    public void dumpStatisticsAndClear() {
        String line = String.format(
                "MESH:%d, TEX_OES:%d, TEX_RECT:%d, FILL_RECT:%d, LINE:%d",
                mCountDrawMesh, mCountTextureRect, mCountTextureOES,
                mCountFillRect, mCountDrawLine);
        mCountDrawMesh = 0;
        mCountTextureRect = 0;
        mCountTextureOES = 0;
        mCountFillRect = 0;
        mCountDrawLine = 0;
        Log.v(TAG, line);
    }

    private void saveTransform() {
        System.arraycopy(mMatrixValues, 0, mTempMatrix, 0, 16);
    }

    private void restoreTransform() {
        System.arraycopy(mTempMatrix, 0, mMatrixValues, 0, 16);
    }
}
