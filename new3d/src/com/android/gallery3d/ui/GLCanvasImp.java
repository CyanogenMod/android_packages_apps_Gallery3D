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

    // We need 16 vertices for a normal nine-patch image (the 4x4 vertices)
    private static final int VERTEX_BUFFER_SIZE = 16 * 2;

    // We need 22 indices for a normal nine-patch image
    private static final int INDEX_BUFFER_SIZE = 22;

    private static final float OPAQUE_ALPHA = 0.95f;

    private final GL11 mGL;

    private final float mMatrixValues[] = new float[16];

    private final float mUvBuffer[] = new float[VERTEX_BUFFER_SIZE];
    private final float mXyBuffer[] = new float[VERTEX_BUFFER_SIZE];
    private final byte mIndexBuffer[] = new byte[INDEX_BUFFER_SIZE];

    private final int mNinePatchX[] = new int[4];
    private final int mNinePatchY[] = new int[4];
    private final float mNinePatchU[] = new float[4];
    private final float mNinePatchV[] = new float[4];
    private final float mTextureColor[] = new float[4];

    private FloatBuffer mXyPointer;
    private FloatBuffer mUvPointer;
    private ByteBuffer mIndexPointer;

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
    private final IntArray mUnboundIds = new IntArray();

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
        int size = VERTEX_BUFFER_SIZE * Float.SIZE / Byte.SIZE;
        mXyPointer = allocateDirectNativeOrderBuffer(size).asFloatBuffer();
        mUvPointer = allocateDirectNativeOrderBuffer(size).asFloatBuffer();
        mIndexPointer = allocateDirectNativeOrderBuffer(INDEX_BUFFER_SIZE);

        GL11 gl = mGL;

        gl.glVertexPointer(2, GL11.GL_FLOAT, 0, mXyPointer);
        gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, mUvPointer);

        // Enable the texture coordinate array for Texture 1
        gl.glClientActiveTexture(GL11.GL_TEXTURE1);
        gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, mUvPointer);
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
        gl.glLoadMatrixf(mMatrixValues, 0);
        float buffer[] = mXyBuffer;
        buffer[0] = x1;
        buffer[1] = y1;
        buffer[2] = x2;
        buffer[3] = y2;
        mXyPointer.put(buffer, 0, 4).position(0);
        gl.glDrawArrays(GL11.GL_LINE_STRIP, 0, 2);
    }

    public void fillRect(float x, float y, float width, float height, int color) {
        mGLState.setColorMode(color, mAlpha);
        GL11 gl = mGL;
        gl.glLoadMatrixf(mMatrixValues, 0);
        putRectangle(x, y, width, height, mXyBuffer, mXyPointer);
        gl.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
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
        gl.glLoadMatrixf(mMatrixValues, 0);
        putRectangle(x, y, width, height, mXyBuffer, mXyPointer);
        gl.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void drawNinePatch(
            NinePatchTexture tex, int x, int y, int width, int height) {
        float alpha = mAlpha;
        NinePatchChunk chunk = tex.getNinePatchChunk();

        // The code should be easily extended to handle the general cases by
        // allocating more space for buffers. But let's just handle the only
        // use case.
        if (chunk.mDivX.length != 2 || chunk.mDivY.length != 2) {
            throw new RuntimeException("unsupported nine patch");
        }
        bindTexture(tex);
        if (width <= 0 || height <= 0) return;

        int divX[] = mNinePatchX;
        int divY[] = mNinePatchY;
        float divU[] = mNinePatchU;
        float divV[] = mNinePatchV;

        int nx = stretch(divX, divU, chunk.mDivX, tex.getWidth(), width);
        int ny = stretch(divY, divV, chunk.mDivY, tex.getHeight(), height);

        mGLState.setBlendEnabled(!tex.isOpaque() || alpha < OPAQUE_ALPHA);
        mGLState.setTextureAlpha(alpha);

        GL11 gl = mGL;
        gl.glLoadMatrixf(mMatrixValues, 0);
        gl.glTranslatef(x, y, 0);
        drawMesh(divX, divY, divU, divV, nx, ny);
    }

    /**
     * Stretches the texture according to the nine-patch rules. It will
     * linearly distribute the strechy parts defined in the nine-patch chunk to
     * the target area.
     *
     * <pre>
     *                      source
     *          /--------------^---------------\
     *         u0    u1       u2  u3     u4   u5
     * div ---> |fffff|ssssssss|fff|ssssss|ffff| ---> u
     *          |    div0    div1 div2   div3  |
     *          |     |       /   /      /    /
     *          |     |      /   /     /    /
     *          |     |     /   /    /    /
     *          |fffff|ssss|fff|sss|ffff| ---> x
     *         x0    x1   x2  x3  x4   x5
     *          \----------v------------/
     *                  target
     *
     * f: fixed segment
     * s: stretchy segment
     * </pre>
     *
     * @param div the stretch parts defined in nine-patch chunk
     * @param source the length of the texture
     * @param target the length on the drawing plan
     * @param u output, the positions of these dividers in the texture
     *        coordinate
     * @param x output, the corresponding position of these dividers on the
     *        drawing plan
     * @return the number of these dividers.
     */
    private int stretch(
            int x[], float u[], int div[], int source, int target) {
        int textureSize = Utils.nextPowerOf2(source);
        float textureBound = (source - 0.5f) / textureSize;

        int stretch = 0;
        for (int i = 0, n = div.length; i < n; i += 2) {
            stretch += div[i + 1] - div[i];
        }

        float remaining = target - source + stretch;

        int lastX = 0;
        int lastU = 0;

        x[0] = 0;
        u[0] = 0;
        for (int i = 0, n = div.length; i < n; i += 2) {
            // fixed segment
            x[i + 1] = lastX + (div[i] - lastU);
            u[i + 1] = Math.min((float) div[i] / textureSize, textureBound);

            // stretchy segment
            float partU = div[i + 1] - div[i];
            int partX = (int)(remaining * partU / stretch + 0.5f);
            remaining -= partX;
            stretch -= partU;

            lastX = x[i + 1] + partX;
            lastU = div[i + 1];
            x[i + 2] = lastX;
            u[i + 2] = Math.min((float) lastU / textureSize, textureBound);
        }
        // the last fixed segment
        x[div.length + 1] = target;
        u[div.length + 1] = textureBound;

        // remove segments with length 0.
        int last = 0;
        for (int i = 1, n = div.length + 2; i < n; ++i) {
            if (x[last] == x[i]) continue;
            x[++last] = x[i];
            u[last] = u[i];
        }
        return last + 1;
    }

    private void drawMesh(
            int x[], int y[], float u[], float v[], int nx, int ny) {
        /*
         * Given a 3x3 nine-patch image, the vertex order is defined as the
         * following graph:
         *
         * (0) (1) (2) (3)
         *  |  /|  /|  /|
         *  | / | / | / |
         * (4) (5) (6) (7)
         *  | \ | \ | \ |
         *  |  \|  \|  \|
         * (8) (9) (A) (B)
         *  |  /|  /|  /|
         *  | / | / | / |
         * (C) (D) (E) (F)
         *
         * And we draw the triangle strip in the following index order:
         *
         * index: 04152637B6A5948C9DAEBF
         */
        int pntCount = 0;
        float xy[] = mXyBuffer;
        float uv[] = mUvBuffer;
        for (int j = 0; j < ny; ++j) {
            for (int i = 0; i < nx; ++i) {
                int xIndex = (pntCount++) << 1;
                int yIndex = xIndex + 1;
                xy[xIndex] = x[i];
                xy[yIndex] = y[j];
                uv[xIndex] = u[i];
                uv[yIndex] = v[j];
            }
        }
        mUvPointer.put(uv, 0, pntCount << 1).position(0);
        mXyPointer.put(xy, 0, pntCount << 1).position(0);

        int idxCount = 1;
        byte index[] = mIndexBuffer;
        for (int i = 0, bound = nx * (ny - 1); true;) {
            // normal direction
            --idxCount;
            for (int j = 0; j < nx; ++j, ++i) {
                index[idxCount++] = (byte) i;
                index[idxCount++] = (byte) (i + nx);
            }
            if (i >= bound) break;

            // reverse direction
            int sum = i + i + nx - 1;
            --idxCount;
            for (int j = 0; j < nx; ++j, ++i) {
                index[idxCount++] = (byte) (sum - i);
                index[idxCount++] = (byte) (sum - i + nx);
            }
            if (i >= bound) break;
        }
        mIndexPointer.put(index, 0, idxCount).position(0);

        mGL.glDrawElements(GL11.GL_TRIANGLE_STRIP,
                idxCount, GL11.GL_UNSIGNED_BYTE, mIndexPointer);
    }

    private float[] mapPoints(float matrix[], int x1, int y1, int x2, int y2) {
        float[] point = mXyBuffer;
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
            putRectangle(0, 0,
                    (texture.getWidth() - 0.5f) / texture.getTextureWidth(),
                    (texture.getHeight() - 0.5f) / texture.getTextureHeight(),
                    mUvBuffer, mUvPointer);
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
        bindTexture(texture);
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
        bindTexture(texture);
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

    private void bindTexture(BasicTexture texture) {
        texture.onBind(this);
        mGLState.setTexture2DEnabled(true);
        mGL.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
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
        bindTexture(from);

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
        bindTexture(to);
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
        float buffer[] = mUvBuffer;
        buffer[0] = source.left;
        buffer[1] = source.top;
        buffer[2] = source.right;
        buffer[3] = source.top;
        buffer[4] = source.left;
        buffer[5] = source.bottom;
        buffer[6] = source.right;
        buffer[7] = source.bottom;
        mUvPointer.put(buffer, 0, 8).position(0);
    }

    public boolean unloadTexture(BasicTexture t) {
        if (!t.isLoaded(this)) return false;
        mUnboundIds.add(t.mId);
        return true;
    }

    public void deleteRecycledTextures() {
        IntArray ids = mUnboundIds;
        if (ids.size() > 0) {
            mGL.glDeleteTextures(ids.size(), ids.getInternelArray(), 0);
            ids.clear();
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
}
