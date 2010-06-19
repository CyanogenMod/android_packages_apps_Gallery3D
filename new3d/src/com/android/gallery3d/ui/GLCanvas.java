package com.android.gallery3d.ui;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLU;
import android.view.animation.Transformation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Stack;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

public class GLCanvas {
    // We need 16 vertices for a normal nine-patch image (the 4x4 vertices)
    private static final int VERTEX_BUFFER_SIZE = 16 * 2;

    // We need 22 indices for a normal nine-patch image
    private static final int INDEX_BUFFER_SIZE = 22;

    private static final float OPAQUE_ALPHA = 0.95f;

    private final GL11 mGL;

    private final Stack<Transformation> mFreeTransform =
            new Stack<Transformation>();

    private final Transformation mTransformation = new Transformation();
    private final Stack<Transformation> mTransformStack =
            new Stack<Transformation>();

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

    private int mWidth;
    private int mHeight;

    GLCanvas(GL11 gl) {
        mGL = gl;
        mGLState = new GLState(gl);
        initialize();
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        GL11 gl = mGL;

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL11.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluOrtho2D(gl, 0, width, 0, height);

        Matrix matrix = mTransformation.getMatrix();
        matrix.reset();
        matrix.preTranslate(0, mHeight);
        matrix.preScale(1, -1);
    }

    public long currentAnimationTimeMillis() {
        return mAnimationTime;
    }

    public Transformation obtainTransformation() {
        if (!mFreeTransform.isEmpty()) {
            Transformation t = mFreeTransform.pop();
            t.clear();
            return t;
        }
        return new Transformation();
    }

    public void freeTransformation(Transformation freeTransformation) {
        mFreeTransform.push(freeTransformation);
    }

    public Transformation getTransformation() {
        return mTransformation;
    }

    public Transformation pushTransform() {
        Transformation trans = obtainTransformation();
        trans.set(mTransformation);
        mTransformStack.push(trans);
        return mTransformation;
    }

    public void popTransform() {
        Transformation trans = mTransformStack.pop();
        mTransformation.set(trans);
        freeTransformation(trans);
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

    public void setColor(int color) {
        float alpha = mTransformation.getAlpha();
        mGLState.setBlendEnabled(!Util.isOpaque(color) || alpha < OPAQUE_ALPHA);
        mGLState.setFragmentColor(color, alpha);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        float matrix[] = mMatrixValues;
        mTransformation.getMatrix().getValues(matrix);
        GL11 gl = mGL;
        gl.glPushMatrix();
        gl.glMultMatrixf(toGLMatrix(matrix), 0);
        float buffer[] = mXyBuffer;
        buffer[0] = x1;
        buffer[1] = y1;
        buffer[2] = x2;
        buffer[3] = y2;
        mXyPointer.put(buffer, 0, 4).position(0);
        gl.glDrawArrays(GL11.GL_LINE_STRIP, 0, 2);
        gl.glPopMatrix();
    }

    public void fillRect(Rect r) {
        fillRect(r.left, r.top, r.right - r.left, r.bottom - r.top);
    }

    public void fillRect(int x, int y, int width, int height) {
        float matrix[] = mMatrixValues;
        mTransformation.getMatrix().getValues(matrix);
        fillRect(x, y, width, height, matrix);
    }

    private void fillRect(
            int x, int y, int width, int height, float matrix[]) {
        GL11 gl = mGL;
        gl.glPushMatrix();
        gl.glMultMatrixf(toGLMatrix(matrix), 0);
        putRectangle(x, y, width, height, mXyBuffer, mXyPointer);
        gl.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        gl.glPopMatrix();
    }

    public void drawNinePatch(
            NinePatchTexture tex, int x, int y, int width, int height) {
        float alpha = mTransformation.getAlpha();
        NinePatchChunk chunk = tex.getNinePatchChunk();

        mGLState.setTexture2DEnabled(true);

        // The code should be easily extended to handle the general cases by
        // allocating more space for buffers. But let's just handle the only
        // use case.
        if (chunk.mDivX.length != 2 || chunk.mDivY.length != 2) {
            throw new RuntimeException("unsupported nine patch");
        }
        tex.bind(this);
        if (width <= 0 || height <= 0) return ;

        int divX[] = mNinePatchX;
        int divY[] = mNinePatchY;
        float divU[] = mNinePatchU;
        float divV[] = mNinePatchV;

        int nx = stretch(divX, divU, chunk.mDivX, tex.getWidth(), width);
        int ny = stretch(divY, divV, chunk.mDivY, tex.getHeight(), height);
        mGLState.setBlendEnabled(!tex.isOpaque() || alpha < OPAQUE_ALPHA);

        mGLState.setTextureAlpha(alpha);
        Matrix matrix = mTransformation.getMatrix();
        matrix.getValues(mMatrixValues);
        GL11 gl = mGL;
        gl.glPushMatrix();
        gl.glMultMatrixf(toGLMatrix(mMatrixValues), 0);
        gl.glTranslatef(x, y, 0);
        drawMesh(divX, divY, divU, divV, nx, ny);
        gl.glPopMatrix();
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
        int textureSize = Util.nextPowerOf2(source);
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

    private float[] mapPoints(Matrix matrix, int x1, int y1, int x2, int y2) {
        float[] point = mXyBuffer;
        point[0] = x1; point[1] = y1; point[2] = x2; point[3] = y2;
        matrix.mapPoints(point, 0, point, 0, 4);
        return point;
    }

    public void clipRect(int x, int y, int width, int height) {
        float point[] = mapPoints(
                mTransformation.getMatrix(), x, y + height, x + width, y);

        // mMatrix could be a rotation matrix. In this case, we need to find
        // the boundaries after rotation. (only handle 90 * n degrees)
        if (point[0] > point[2]) {
            x = (int) point[2];
            width = (int) point[0] - x;
        } else {
            x = (int) point[0];
            width = (int) point[2] - x;
        }
        if (point[1] > point[3]) {
            y = (int) point[3];
            height = (int) point[1] - y;
        } else {
            y = (int) point[1];
            height = (int) point[3] - y;
        }
        mGL.glScissor(x, y, width, height);
    }

    public void clearClip() {
        mGL.glScissor(0, 0, mWidth, mHeight);
    }

    private static float[] toGLMatrix(float v[]) {
        v[15] = v[8]; v[13] = v[5]; v[5] = v[4]; v[4] = v[1];
        v[12] = v[2]; v[1] = v[3]; v[3] = v[6];
        v[2] = v[6] = v[8] = v[9] = 0;
        v[10] = 1;
        return v;
    }

    public void drawColor(int x, int y, int width, int height, int color) {
        float alpha = mTransformation.getAlpha();
        mGLState.setBlendEnabled(!Util.isOpaque(color) || alpha < OPAQUE_ALPHA);
        mGLState.setFragmentColor(color, alpha);
        fillRect(x, y, width, height);
    }

    private void drawBoundTexture(
            BasicTexture texture, int x, int y, int width, int height) {
        Matrix matrix = mTransformation.getMatrix();
        matrix.getValues(mMatrixValues);

        // Test whether it has been rotated or flipped, if so, glDrawTexiOES
        // won't work
        if (isMatrixRotatedOrFlipped(mMatrixValues)) {
            putRectangle(0, 0,
                    (texture.mWidth - 0.5f) / texture.mTextureWidth,
                    (texture.mHeight - 0.5f) / texture.mTextureHeight,
                    mUvBuffer, mUvPointer);
            fillRect(x, y, width, height, mMatrixValues);
        } else {
            // draw the rect from bottom-left to top-right
            float points[] = mapPoints(matrix, x, y + height, x + width, y);
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
        drawTexture(texture, x, y, width, height, mTransformation.getAlpha());
    }

    public void drawTexture(BasicTexture texture,
            int x, int y, int width, int height, float alpha) {
        if (width <= 0 || height <= 0) return ;

        mGLState.setBlendEnabled(!texture.isOpaque() || alpha < OPAQUE_ALPHA);
        mGLState.setTexture2DEnabled(true);
        texture.bind(this);
        mGLState.setTextureAlpha(alpha);
        drawBoundTexture(texture, x, y, width, height);
    }

    public void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int w, int h) {
        drawMixed(from, to, ratio, x, y, w, h, mTransformation.getAlpha());
    }

    private void setTextureColor(float r, float g, float b, float alpha) {
        float[] color = mTextureColor;
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = alpha;
    }

    public void handleLowMemory() {
    }

    public void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int width, int height, float alpha) {
        if (alpha < OPAQUE_ALPHA) {
            throw new RuntimeException("Cannot support alpha value");
        }
        mGLState.setBlendEnabled(!from.isOpaque() || !to.isOpaque());
        mGLState.setTextureAlpha(1);
        mGLState.setTexture2DEnabled(true);

        final GL11 gl = mGL;
        from.bind(this);

        gl.glActiveTexture(GL11.GL_TEXTURE1);
        to.bind(this);
        gl.glEnable(GL11.GL_TEXTURE_2D);

        // Interpolate the RGB and alpha values between both textures.
        mGLState.setTexEnvMode(GL11.GL_COMBINE);
        gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_COMBINE_RGB, GL11.GL_INTERPOLATE);
        gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_COMBINE_ALPHA, GL11.GL_INTERPOLATE);

        // Specify the interpolation factor via the alpha component of
        // GL_TEXTURE_ENV_COLORes.
        setTextureColor(ratio, ratio, ratio, ratio);
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

    private static boolean isMatrixRotatedOrFlipped(float matrix[]) {
        return matrix[Matrix.MSKEW_X] != 0 || matrix[Matrix.MSKEW_Y] != 0
                || matrix[Matrix.MSCALE_X] < 0 || matrix[Matrix.MSCALE_Y] > 0;
    }

    public void copyTexture2D(
            RawTexture texture, int x, int y, int width, int height) {
        Matrix matrix = mTransformation.getMatrix();
        matrix.getValues(mMatrixValues);

        if (isMatrixRotatedOrFlipped(mMatrixValues)) {
            throw new IllegalArgumentException("cannot support rotated matrix");
        }
        float points[] = mapPoints(matrix, x, y + height, x + width, y);
        x = (int) points[0];
        y = (int) points[1];
        width = (int) points[2] - x;
        height = (int) points[3] - y;

        GL11 gl = mGL;
        int newWidth = Util.nextPowerOf2(width);
        int newHeight = Util.nextPowerOf2(height);
        int glError = GL11.GL_NO_ERROR;

        gl.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());

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
                GL11.GL_RGBA, x, y, newWidth, newHeight, 0);
        glError = gl.glGetError();

        if (glError == GL11.GL_OUT_OF_MEMORY) {
            throw new GLOutOfMemoryException();
        }

        if (glError != GL11.GL_NO_ERROR) {
            throw new RuntimeException(
                    "Texture copy fail, glError " + glError);
        }

        texture.setSize(width, height);
        texture.setTextureSize(newWidth, newHeight);
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
            gl.glEnable(GL11.GL_STENCIL_TEST);

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

        public void setFragmentColor(int color, float alpha) {
            // Set mTextureAlpha to an invalid value, so that it will reset
            // again in setTextureAlpha(float) later.
            mTextureAlpha = -1.0f;

            setTexture2DEnabled(false);
            int prealpha = (int) ((color >>> 24) * alpha + 0.5);
            mGL.glColor4x(
                    ((color >> 16) & 0xFF) * prealpha,
                    ((color >> 8) & 0xFF) * prealpha,
                    (color & 0xFF) * prealpha, prealpha << 8);
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

    protected void setCurrentTimeMillis(long time) {
        mAnimationTime = time;
    }

    public void clearBuffer() {
        mGL.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_STENCIL_BUFFER_BIT);
    }

    public void setTextureCoords(RectF source) {
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

    public void releaseTextures(Collection<? extends BasicTexture> c) {
        IntArray array = new IntArray();
        for (BasicTexture t : c) {
            if (t.isLoaded(this)) array.add(t.mId);
        }
        if (array.size() > 0) {
            mGL.glDeleteTextures(array.size(), array.toArray(null), 0);
        }
    }
}
