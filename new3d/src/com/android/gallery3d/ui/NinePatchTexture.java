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

import com.android.gallery3d.util.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.microedition.khronos.opengles.GL11;

// NinePatchTexture is a texture backed by a NinePatch resource.
//
// getPaddings() returns paddings specified in the NinePatch.
// getNinePatchChunk() returns the layout data specified in the NinePatch.
//
class NinePatchTexture extends ResourceTexture {
    private static final String TAG = "NinePatchTexture";
    private NinePatchChunk mChunk;
    private MyCacheMap<Long, NinePatchInstance> mInstanceCache =
            new MyCacheMap<Long, NinePatchInstance>();

    public NinePatchTexture(Context context, int resId) {
        super(context, resId);
    }

    @Override
    protected Bitmap onGetBitmap() {
        if (mBitmap != null) return mBitmap;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeResource(
                mContext.getResources(), mResId, options);
        mBitmap = bitmap;
        setSize(bitmap.getWidth(), bitmap.getHeight());
        byte[] chunkData = bitmap.getNinePatchChunk();
        mChunk = chunkData == null
                ? null
                : NinePatchChunk.deserialize(bitmap.getNinePatchChunk());
        if (mChunk == null) {
            throw new RuntimeException("invalid nine-patch image: " + mResId);
        }
        return bitmap;
    }

    public Rect getPaddings() {
        // get the paddings from nine patch
        if (mChunk == null) onGetBitmap();
        return mChunk.mPaddings;
    }

    public NinePatchChunk getNinePatchChunk() {
        if (mChunk == null) onGetBitmap();
        return mChunk;
    }

    private static class MyCacheMap<K, V> extends LinkedHashMap<K, V> {
        private int CACHE_SIZE = 16;
        private V mJustRemoved;

        public MyCacheMap() {
            super(4, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            if (size() > CACHE_SIZE) {
                mJustRemoved = eldest.getValue();
                return true;
            }
            return false;
        }

        public V getJustRemoved() {
            V result = mJustRemoved;
            mJustRemoved = null;
            return result;
        }
    }

    private NinePatchInstance findInstance(GLCanvas canvas, int w, int h) {
        long key = w;
        key = (key << 32) | h;
        NinePatchInstance instance = mInstanceCache.get(key);

        if (instance == null) {
            instance = new NinePatchInstance(this, w, h);
            mInstanceCache.put(key, instance);
            NinePatchInstance removed = mInstanceCache.getJustRemoved();
            if (removed != null) {
                removed.recycle(canvas);
            }
        }

        return instance;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        if (!isLoaded(canvas)) {
            mInstanceCache.clear();
        }

        findInstance(canvas, w, h).draw(canvas, this, x, y);
    }

    @Override
    public void recycle() {
        super.recycle();
        GLCanvas canvas = mCanvasRef == null ? null : mCanvasRef.get();
        if (canvas == null) return;
        for (NinePatchInstance instance : mInstanceCache.values()) {
            instance.recycle(canvas);
        }
        mInstanceCache.clear();
    }
}

// This keeps data for a specialization of NinePatchTexture with the size
// (width, height). We pre-compute the coordinates for efficiency.
class NinePatchInstance {
    private static final String TAG = "NinePatchInstance";

    // We need 16 vertices for a normal nine-patch image (the 4x4 vertices)
    private static final int VERTEX_BUFFER_SIZE = 16 * 2;

    // We need 22 indices for a normal nine-patch image, plus 2 for each
    // transparent region. Current there are at most 1 transparent region.
    private static final int INDEX_BUFFER_SIZE = 22 + 2;

    private FloatBuffer mXyBuffer;
    private FloatBuffer mUvBuffer;
    private ByteBuffer mIndexBuffer;

    // Names for buffer names: xy, uv, index.
    private int[] mBufferNames;

    private int mIdxCount;

    // These members are used by NinePatchTexture to maintain the cache.
    int mWidth, mHeight;
    NinePatchInstance mNext;

    public NinePatchInstance(NinePatchTexture tex, int width, int height) {
        NinePatchChunk chunk = tex.getNinePatchChunk();

        if (width <= 0 || height <= 0) {
            throw new RuntimeException("invalid dimension");
        }

        mWidth = width;
        mHeight = height;

        // The code should be easily extended to handle the general cases by
        // allocating more space for buffers. But let's just handle the only
        // use case.
        if (chunk.mDivX.length != 2 || chunk.mDivY.length != 2) {
            throw new RuntimeException("unsupported nine patch");
        }

        int divX[] = new int[4];
        int divY[] = new int[4];
        float divU[] = new float[4];
        float divV[] = new float[4];

        int nx = stretch(divX, divU, chunk.mDivX, tex.getWidth(), width);
        int ny = stretch(divY, divV, chunk.mDivY, tex.getHeight(), height);

        prepareVertexData(divX, divY, divU, divV, nx, ny, chunk.mColor);
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
    private static int stretch(
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

    private void prepareVertexData(int x[], int y[], float u[], float v[],
            int nx, int ny, int[] color) {
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
        float xy[] = new float[VERTEX_BUFFER_SIZE];;
        float uv[] = new float[VERTEX_BUFFER_SIZE];;
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

        int idxCount = 1;
        boolean isForward = false;
        byte index[] = new byte[INDEX_BUFFER_SIZE];
        for (int row = 0; row < ny - 1; row++) {
            --idxCount;
            isForward = !isForward;

            int start, end, inc;
            if (isForward) {
                start = 0;
                end = nx;
                inc = 1;
            } else {
                start = nx - 1;
                end = -1;
                inc = -1;
            }

            for (int col = start; col != end; col += inc) {
                int k = row * nx + col;
                if (col != start) {
                    int colorIdx = row * (nx - 1) + col;
                    if (isForward) colorIdx--;
                    if (color[colorIdx] == NinePatchChunk.TRANSPARENT_COLOR) {
                        index[idxCount] = index[idxCount - 1];
                        ++idxCount;
                        index[idxCount++] = (byte) k;
                    }
                }

                index[idxCount++] = (byte) k;
                index[idxCount++] = (byte) (k + nx);
            }
        }

        mIdxCount = idxCount;

        int size = (pntCount * 2) * (Float.SIZE / Byte.SIZE);
        mXyBuffer = allocateDirectNativeOrderBuffer(size).asFloatBuffer();
        mUvBuffer = allocateDirectNativeOrderBuffer(size).asFloatBuffer();
        mIndexBuffer = allocateDirectNativeOrderBuffer(mIdxCount);

        mXyBuffer.put(xy, 0, pntCount * 2).position(0);
        mUvBuffer.put(uv, 0, pntCount * 2).position(0);
        mIndexBuffer.put(index, 0, idxCount).position(0);
    }

    private static ByteBuffer allocateDirectNativeOrderBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    private void prepareBuffers(GLCanvas canvas) {
        mBufferNames = new int[3];
        GL11 gl = canvas.getGLInstance();
        gl.glGenBuffers(3, mBufferNames, 0);

        gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, mBufferNames[0]);
        gl.glBufferData(GL11.GL_ARRAY_BUFFER,
                mXyBuffer.capacity() * (Float.SIZE / Byte.SIZE),
                mXyBuffer, GL11.GL_STATIC_DRAW);

        gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, mBufferNames[1]);
        gl.glBufferData(GL11.GL_ARRAY_BUFFER,
                mUvBuffer.capacity() * (Float.SIZE / Byte.SIZE),
                mUvBuffer, GL11.GL_STATIC_DRAW);

        gl.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, mBufferNames[2]);
        gl.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER,
                mIndexBuffer.capacity(),
                mIndexBuffer, GL11.GL_STATIC_DRAW);

        // These buffers are never used again.
        mXyBuffer = null;
        mUvBuffer = null;
        mIndexBuffer = null;
    }

    public void draw(GLCanvas canvas, NinePatchTexture tex, int x, int y) {
        if (mBufferNames == null) {
            prepareBuffers(canvas);
        }
        canvas.drawMesh(tex, x, y, mBufferNames[0], mBufferNames[1],
                mBufferNames[2], mIdxCount);
    }

    public void recycle(GLCanvas canvas) {
        if (mBufferNames != null) {
            canvas.deleteBuffer(mBufferNames[0]);
            canvas.deleteBuffer(mBufferNames[1]);
            canvas.deleteBuffer(mBufferNames[2]);
            mBufferNames = null;
        }
    }
}
