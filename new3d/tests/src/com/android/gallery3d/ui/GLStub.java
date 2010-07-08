package com.android.gallery3d.ui;

import android.util.Log;

import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL10Ext;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

public class GLStub implements GL, GL10, GL10Ext, GL11, GL11Ext {
    private static final String TAG = "GLStub";

    public void glActiveTexture(
        int texture
    ){}

    public void glAlphaFunc(
        int func,
        float ref
    ){}

    public void glAlphaFuncx(
        int func,
        int ref
    ){}

    public void glBindTexture(
        int target,
        int texture
    ){}

    public void glBlendFunc(
        int sfactor,
        int dfactor
    ){}

    public void glClear(
        int mask
    ){}

    public void glClearColor(
        float red,
        float green,
        float blue,
        float alpha
    ){}

    public void glClearColorx(
        int red,
        int green,
        int blue,
        int alpha
    ){}

    public void glClearDepthf(
        float depth
    ){}

    public void glClearDepthx(
        int depth
    ){}

    public void glClearStencil(
        int s
    ){}

    public void glClientActiveTexture(
        int texture
    ){}

    public void glColor4f(
        float red,
        float green,
        float blue,
        float alpha
    ){}

    public void glColor4x(
        int red,
        int green,
        int blue,
        int alpha
    ){}

    public void glColorMask(
        boolean red,
        boolean green,
        boolean blue,
        boolean alpha
    ){}

    public void glColorPointer(
        int size,
        int type,
        int stride,
        java.nio.Buffer pointer
    ){}

    public void glCompressedTexImage2D(
        int target,
        int level,
        int internalformat,
        int width,
        int height,
        int border,
        int imageSize,
        java.nio.Buffer data
    ){}

    public void glCompressedTexSubImage2D(
        int target,
        int level,
        int xoffset,
        int yoffset,
        int width,
        int height,
        int format,
        int imageSize,
        java.nio.Buffer data
    ){}

    public void glCopyTexImage2D(
        int target,
        int level,
        int internalformat,
        int x,
        int y,
        int width,
        int height,
        int border
    ){}

    public void glCopyTexSubImage2D(
        int target,
        int level,
        int xoffset,
        int yoffset,
        int x,
        int y,
        int width,
        int height
    ){}

    public void glCullFace(
        int mode
    ){}

    public void glDeleteTextures(
        int n,
        int[] textures,
        int offset
    ){}

    public void glDeleteTextures(
        int n,
        java.nio.IntBuffer textures
    ){}

    public void glDepthFunc(
        int func
    ){}

    public void glDepthMask(
        boolean flag
    ){}

    public void glDepthRangef(
        float zNear,
        float zFar
    ){}

    public void glDepthRangex(
        int zNear,
        int zFar
    ){}

    public void glDisable(
        int cap
    ){}

    public void glDisableClientState(
        int array
    ){}

    public void glDrawArrays(
        int mode,
        int first,
        int count
    ){}

    public void glDrawElements(
        int mode,
        int count,
        int type,
        java.nio.Buffer indices
    ){}

    public void glEnable(
        int cap
    ){}

    public void glEnableClientState(
        int array
    ){}

    public void glFinish(
    ){}

    public void glFlush(
    ){}

    public void glFogf(
        int pname,
        float param
    ){}

    public void glFogfv(
        int pname,
        float[] params,
        int offset
    ){}

    public void glFogfv(
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glFogx(
        int pname,
        int param
    ){}

    public void glFogxv(
        int pname,
        int[] params,
        int offset
    ){}

    public void glFogxv(
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glFrontFace(
        int mode
    ){}

    public void glFrustumf(
        float left,
        float right,
        float bottom,
        float top,
        float zNear,
        float zFar
    ){}

    public void glFrustumx(
        int left,
        int right,
        int bottom,
        int top,
        int zNear,
        int zFar
    ){}

    public void glGenTextures(
        int n,
        int[] textures,
        int offset
    ){}

    public void glGenTextures(
        int n,
        java.nio.IntBuffer textures
    ){}

    public int glGetError(
    ){ throw new UnsupportedOperationException(); }

    public void glGetIntegerv(
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetIntegerv(
        int pname,
        java.nio.IntBuffer params
    ){}

    public String glGetString(
        int name
    ){ throw new UnsupportedOperationException(); }

    public void glHint(
        int target,
        int mode
    ){}

    public void glLightModelf(
        int pname,
        float param
    ){}

    public void glLightModelfv(
        int pname,
        float[] params,
        int offset
    ){}

    public void glLightModelfv(
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glLightModelx(
        int pname,
        int param
    ){}

    public void glLightModelxv(
        int pname,
        int[] params,
        int offset
    ){}

    public void glLightModelxv(
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glLightf(
        int light,
        int pname,
        float param
    ){}

    public void glLightfv(
        int light,
        int pname,
        float[] params,
        int offset
    ){}

    public void glLightfv(
        int light,
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glLightx(
        int light,
        int pname,
        int param
    ){}

    public void glLightxv(
        int light,
        int pname,
        int[] params,
        int offset
    ){}

    public void glLightxv(
        int light,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glLineWidth(
        float width
    ){}

    public void glLineWidthx(
        int width
    ){}

    public void glLoadIdentity(
    ){}

    public void glLoadMatrixf(
        float[] m,
        int offset
    ){}

    public void glLoadMatrixf(
        java.nio.FloatBuffer m
    ){}

    public void glLoadMatrixx(
        int[] m,
        int offset
    ){}

    public void glLoadMatrixx(
        java.nio.IntBuffer m
    ){}

    public void glLogicOp(
        int opcode
    ){}

    public void glMaterialf(
        int face,
        int pname,
        float param
    ){}

    public void glMaterialfv(
        int face,
        int pname,
        float[] params,
        int offset
    ){}

    public void glMaterialfv(
        int face,
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glMaterialx(
        int face,
        int pname,
        int param
    ){}

    public void glMaterialxv(
        int face,
        int pname,
        int[] params,
        int offset
    ){}

    public void glMaterialxv(
        int face,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glMatrixMode(
        int mode
    ){}

    public void glMultMatrixf(
        float[] m,
        int offset
    ){}

    public void glMultMatrixf(
        java.nio.FloatBuffer m
    ){}

    public void glMultMatrixx(
        int[] m,
        int offset
    ){}

    public void glMultMatrixx(
        java.nio.IntBuffer m
    ){}

    public void glMultiTexCoord4f(
        int target,
        float s,
        float t,
        float r,
        float q
    ){}

    public void glMultiTexCoord4x(
        int target,
        int s,
        int t,
        int r,
        int q
    ){}

    public void glNormal3f(
        float nx,
        float ny,
        float nz
    ){}

    public void glNormal3x(
        int nx,
        int ny,
        int nz
    ){}

    public void glNormalPointer(
        int type,
        int stride,
        java.nio.Buffer pointer
    ){}

    public void glOrthof(
        float left,
        float right,
        float bottom,
        float top,
        float zNear,
        float zFar
    ){}

    public void glOrthox(
        int left,
        int right,
        int bottom,
        int top,
        int zNear,
        int zFar
    ){}

    public void glPixelStorei(
        int pname,
        int param
    ){}

    public void glPointSize(
        float size
    ){}

    public void glPointSizex(
        int size
    ){}

    public void glPolygonOffset(
        float factor,
        float units
    ){}

    public void glPolygonOffsetx(
        int factor,
        int units
    ){}

    public void glPopMatrix(
    ){}

    public void glPushMatrix(
    ){}

    public void glReadPixels(
        int x,
        int y,
        int width,
        int height,
        int format,
        int type,
        java.nio.Buffer pixels
    ){}

    public void glRotatef(
        float angle,
        float x,
        float y,
        float z
    ){}

    public void glRotatex(
        int angle,
        int x,
        int y,
        int z
    ){}

    public void glSampleCoverage(
        float value,
        boolean invert
    ){}

    public void glSampleCoveragex(
        int value,
        boolean invert
    ){}

    public void glScalef(
        float x,
        float y,
        float z
    ){}

    public void glScalex(
        int x,
        int y,
        int z
    ){}

    public void glScissor(
        int x,
        int y,
        int width,
        int height
    ){}

    public void glShadeModel(
        int mode
    ){}

    public void glStencilFunc(
        int func,
        int ref,
        int mask
    ){}

    public void glStencilMask(
        int mask
    ){}

    public void glStencilOp(
        int fail,
        int zfail,
        int zpass
    ){}

    public void glTexCoordPointer(
        int size,
        int type,
        int stride,
        java.nio.Buffer pointer
    ){}

    public void glTexEnvf(
        int target,
        int pname,
        float param
    ){}

    public void glTexEnvfv(
        int target,
        int pname,
        float[] params,
        int offset
    ){}

    public void glTexEnvfv(
        int target,
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glTexEnvx(
        int target,
        int pname,
        int param
    ){}

    public void glTexEnvxv(
        int target,
        int pname,
        int[] params,
        int offset
    ){}

    public void glTexEnvxv(
        int target,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glTexImage2D(
        int target,
        int level,
        int internalformat,
        int width,
        int height,
        int border,
        int format,
        int type,
        java.nio.Buffer pixels
    ){}

    public void glTexParameterf(
        int target,
        int pname,
        float param
    ){}

    public void glTexParameterx(
        int target,
        int pname,
        int param
    ){}

    public void glTexSubImage2D(
        int target,
        int level,
        int xoffset,
        int yoffset,
        int width,
        int height,
        int format,
        int type,
        java.nio.Buffer pixels
    ){}

    public void glTranslatef(
        float x,
        float y,
        float z
    ){}

    public void glTranslatex(
        int x,
        int y,
        int z
    ){}

    public void glVertexPointer(
        int size,
        int type,
        int stride,
        java.nio.Buffer pointer
    ){}

    public void glViewport(
        int x,
        int y,
        int width,
        int height
    ){}

    public int glQueryMatrixxOES(
        int[] mantissa,
        int mantissaOffset,
        int[] exponent,
        int exponentOffset
    ){ throw new UnsupportedOperationException(); }

    public int glQueryMatrixxOES(
        java.nio.IntBuffer mantissa,
        java.nio.IntBuffer exponent
    ){ throw new UnsupportedOperationException(); }

    public void glGetPointerv(int pname, java.nio.Buffer[] params){}
    public void glBindBuffer(
        int target,
        int buffer
    ){}

    public void glBufferData(
        int target,
        int size,
        java.nio.Buffer data,
        int usage
    ){}

    public void glBufferSubData(
        int target,
        int offset,
        int size,
        java.nio.Buffer data
    ){}

    public void glClipPlanef(
        int plane,
        float[] equation,
        int offset
    ){}

    public void glClipPlanef(
        int plane,
        java.nio.FloatBuffer equation
    ){}

    public void glClipPlanex(
        int plane,
        int[] equation,
        int offset
    ){}

    public void glClipPlanex(
        int plane,
        java.nio.IntBuffer equation
    ){}

    public void glColor4ub(
        byte red,
        byte green,
        byte blue,
        byte alpha
    ){}

    public void glColorPointer(
        int size,
        int type,
        int stride,
        int offset
    ){}

    public void glDeleteBuffers(
        int n,
        int[] buffers,
        int offset
    ){}

    public void glDeleteBuffers(
        int n,
        java.nio.IntBuffer buffers
    ){}

    public void glDrawElements(
        int mode,
        int count,
        int type,
        int offset
    ){}

    public void glGenBuffers(
        int n,
        int[] buffers,
        int offset
    ){}

    public void glGenBuffers(
        int n,
        java.nio.IntBuffer buffers
    ){}

    public void glGetBooleanv(
        int pname,
        boolean[] params,
        int offset
    ){}

    public void glGetBooleanv(
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glGetBufferParameteriv(
        int target,
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetBufferParameteriv(
        int target,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glGetClipPlanef(
        int pname,
        float[] eqn,
        int offset
    ){}

    public void glGetClipPlanef(
        int pname,
        java.nio.FloatBuffer eqn
    ){}

    public void glGetClipPlanex(
        int pname,
        int[] eqn,
        int offset
    ){}

    public void glGetClipPlanex(
        int pname,
        java.nio.IntBuffer eqn
    ){}

    public void glGetFixedv(
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetFixedv(
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glGetFloatv(
        int pname,
        float[] params,
        int offset
    ){}

    public void glGetFloatv(
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glGetLightfv(
        int light,
        int pname,
        float[] params,
        int offset
    ){}

    public void glGetLightfv(
        int light,
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glGetLightxv(
        int light,
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetLightxv(
        int light,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glGetMaterialfv(
        int face,
        int pname,
        float[] params,
        int offset
    ){}

    public void glGetMaterialfv(
        int face,
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glGetMaterialxv(
        int face,
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetMaterialxv(
        int face,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glGetTexEnviv(
        int env,
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetTexEnviv(
        int env,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glGetTexEnvxv(
        int env,
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetTexEnvxv(
        int env,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glGetTexParameterfv(
        int target,
        int pname,
        float[] params,
        int offset
    ){}

    public void glGetTexParameterfv(
        int target,
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glGetTexParameteriv(
        int target,
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetTexParameteriv(
        int target,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glGetTexParameterxv(
        int target,
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetTexParameterxv(
        int target,
        int pname,
        java.nio.IntBuffer params
    ){}

    public boolean glIsBuffer(
        int buffer
    ){ throw new UnsupportedOperationException(); }

    public boolean glIsEnabled(
        int cap
    ){ throw new UnsupportedOperationException(); }

    public boolean glIsTexture(
        int texture
    ){ throw new UnsupportedOperationException(); }

    public void glNormalPointer(
        int type,
        int stride,
        int offset
    ){}

    public void glPointParameterf(
        int pname,
        float param
    ){}

    public void glPointParameterfv(
        int pname,
        float[] params,
        int offset
    ){}

    public void glPointParameterfv(
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glPointParameterx(
        int pname,
        int param
    ){}

    public void glPointParameterxv(
        int pname,
        int[] params,
        int offset
    ){}

    public void glPointParameterxv(
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glPointSizePointerOES(
        int type,
        int stride,
        java.nio.Buffer pointer
    ){}

    public void glTexCoordPointer(
        int size,
        int type,
        int stride,
        int offset
    ){}

    public void glTexEnvi(
        int target,
        int pname,
        int param
    ){}

    public void glTexEnviv(
        int target,
        int pname,
        int[] params,
        int offset
    ){}

    public void glTexEnviv(
        int target,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glTexParameterfv(
        int target,
        int pname,
        float[] params,
        int offset
    ){}

    public void glTexParameterfv(
        int target,
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glTexParameteri(
        int target,
        int pname,
        int param
    ){}

    public void glTexParameteriv(
        int target,
        int pname,
        int[] params,
        int offset
    ){}

    public void glTexParameteriv(
        int target,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glTexParameterxv(
        int target,
        int pname,
        int[] params,
        int offset
    ){}

    public void glTexParameterxv(
        int target,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glVertexPointer(
        int size,
        int type,
        int stride,
        int offset
    ){}

    public void glCurrentPaletteMatrixOES(
        int matrixpaletteindex
    ){}

    public void glDrawTexfOES(
        float x,
        float y,
        float z,
        float width,
        float height
    ){}

    public void glDrawTexfvOES(
        float[] coords,
        int offset
    ){}

    public void glDrawTexfvOES(
        java.nio.FloatBuffer coords
    ){}

    public void glDrawTexiOES(
        int x,
        int y,
        int z,
        int width,
        int height
    ){}

    public void glDrawTexivOES(
        int[] coords,
        int offset
    ){}

    public void glDrawTexivOES(
        java.nio.IntBuffer coords
    ){}

    public void glDrawTexsOES(
        short x,
        short y,
        short z,
        short width,
        short height
    ){}

    public void glDrawTexsvOES(
        short[] coords,
        int offset
    ){}

    public void glDrawTexsvOES(
        java.nio.ShortBuffer coords
    ){}

    public void glDrawTexxOES(
        int x,
        int y,
        int z,
        int width,
        int height
    ){}

    public void glDrawTexxvOES(
        int[] coords,
        int offset
    ){}

    public void glDrawTexxvOES(
        java.nio.IntBuffer coords
    ){}

    public void glLoadPaletteFromModelViewMatrixOES(
    ){}

    public void glMatrixIndexPointerOES(
        int size,
        int type,
        int stride,
        java.nio.Buffer pointer
    ){}

    public void glMatrixIndexPointerOES(
        int size,
        int type,
        int stride,
        int offset
    ){}

    public void glWeightPointerOES(
        int size,
        int type,
        int stride,
        java.nio.Buffer pointer
    ){}

    public void glWeightPointerOES(
        int size,
        int type,
        int stride,
        int offset
    ){}

    public void glBindFramebufferOES(
        int target,
        int framebuffer
    ){}

    public void glBindRenderbufferOES(
        int target,
        int renderbuffer
    ){}

    public void glBlendEquation(
        int mode
    ){}

    public void glBlendEquationSeparate(
        int modeRGB,
        int modeAlpha
    ){}

    public void glBlendFuncSeparate(
        int srcRGB,
        int dstRGB,
        int srcAlpha,
        int dstAlpha
    ){}

    public int glCheckFramebufferStatusOES(
        int target
    ){ throw new UnsupportedOperationException(); }

    public void glDeleteFramebuffersOES(
        int n,
        int[] framebuffers,
        int offset
    ){}

    public void glDeleteFramebuffersOES(
        int n,
        java.nio.IntBuffer framebuffers
    ){}

    public void glDeleteRenderbuffersOES(
        int n,
        int[] renderbuffers,
        int offset
    ){}

    public void glDeleteRenderbuffersOES(
        int n,
        java.nio.IntBuffer renderbuffers
    ){}

    public void glFramebufferRenderbufferOES(
        int target,
        int attachment,
        int renderbuffertarget,
        int renderbuffer
    ){}

    public void glFramebufferTexture2DOES(
        int target,
        int attachment,
        int textarget,
        int texture,
        int level
    ){}

    public void glGenerateMipmapOES(
        int target
    ){}

    public void glGenFramebuffersOES(
        int n,
        int[] framebuffers,
        int offset
    ){}

    public void glGenFramebuffersOES(
        int n,
        java.nio.IntBuffer framebuffers
    ){}

    public void glGenRenderbuffersOES(
        int n,
        int[] renderbuffers,
        int offset
    ){}

    public void glGenRenderbuffersOES(
        int n,
        java.nio.IntBuffer renderbuffers
    ){}

    public void glGetFramebufferAttachmentParameterivOES(
        int target,
        int attachment,
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetFramebufferAttachmentParameterivOES(
        int target,
        int attachment,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glGetRenderbufferParameterivOES(
        int target,
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetRenderbufferParameterivOES(
        int target,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glGetTexGenfv(
        int coord,
        int pname,
        float[] params,
        int offset
    ){}

    public void glGetTexGenfv(
        int coord,
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glGetTexGeniv(
        int coord,
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetTexGeniv(
        int coord,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glGetTexGenxv(
        int coord,
        int pname,
        int[] params,
        int offset
    ){}

    public void glGetTexGenxv(
        int coord,
        int pname,
        java.nio.IntBuffer params
    ){}

    public boolean glIsFramebufferOES(
        int framebuffer
    ){ throw new UnsupportedOperationException(); }

    public boolean glIsRenderbufferOES(
        int renderbuffer
    ){ throw new UnsupportedOperationException(); }

    public void glRenderbufferStorageOES(
        int target,
        int internalformat,
        int width,
        int height
    ){}

    public void glTexGenf(
        int coord,
        int pname,
        float param
    ){}

    public void glTexGenfv(
        int coord,
        int pname,
        float[] params,
        int offset
    ){}

    public void glTexGenfv(
        int coord,
        int pname,
        java.nio.FloatBuffer params
    ){}

    public void glTexGeni(
        int coord,
        int pname,
        int param
    ){}

    public void glTexGeniv(
        int coord,
        int pname,
        int[] params,
        int offset
    ){}

    public void glTexGeniv(
        int coord,
        int pname,
        java.nio.IntBuffer params
    ){}

    public void glTexGenx(
        int coord,
        int pname,
        int param
    ){}

    public void glTexGenxv(
        int coord,
        int pname,
        int[] params,
        int offset
    ){}

    public void glTexGenxv(
        int coord,
        int pname,
        java.nio.IntBuffer params
    ){}
}
