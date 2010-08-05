package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.MotionEvent;
import android.util.Log;

import junit.framework.TestCase;

import javax.microedition.khronos.opengles.GL11;

@SmallTest
public class GLViewTest extends TestCase {
    private static final String TAG = "GLViewTest";

    @SmallTest
    public void testVisibility() {
        GLViewMock a = new GLViewMock();
        assertEquals(GLView.VISIBLE, a.getVisibility());
        assertEquals(0, a.mOnVisibilityChangedCalled);
        a.setVisibility(GLView.INVISIBLE);
        assertEquals(GLView.INVISIBLE, a.getVisibility());
        assertEquals(1, a.mOnVisibilityChangedCalled);
        a.setVisibility(GLView.VISIBLE);
        assertEquals(GLView.VISIBLE, a.getVisibility());
        assertEquals(2, a.mOnVisibilityChangedCalled);
    }

    @SmallTest
    public void testComponents() {
        GLView view = new GLView();
        assertEquals(0, view.getComponentCount());
        try {
            view.getComponent(0);
            fail();
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }

        GLView x = new GLView();
        GLView y = new GLView();
        view.addComponent(x);
        view.addComponent(y);
        assertEquals(2, view.getComponentCount());
        assertSame(x, view.getComponent(0));
        assertSame(y, view.getComponent(1));
        view.removeComponent(x);
        assertSame(y, view.getComponent(0));
        try {
            view.getComponent(1);
            fail();
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }
        try {
            view.addComponent(y);
            fail();
        } catch (IllegalStateException ex) {
            // expected
        }
        view.addComponent(x);
        view.removeAllComponents();
        assertEquals(0, view.getComponentCount());
    }

    @SmallTest
    public void testBounds() {
        GLView view = new GLView();

        assertEquals(0, view.getWidth());
        assertEquals(0, view.getHeight());

        Rect b = view.bounds();
        assertEquals(0, b.left);
        assertEquals(0, b.top);
        assertEquals(0, b.right);
        assertEquals(0, b.bottom);

        view.layout(10, 20, 30, 100);
        assertEquals(20, view.getWidth());
        assertEquals(80, view.getHeight());

        b = view.bounds();
        assertEquals(10, b.left);
        assertEquals(20, b.top);
        assertEquals(30, b.right);
        assertEquals(100, b.bottom);
    }

    @SmallTest
    public void testPaddings() {
        GLView view = new GLView();

        Rect p = view.getPaddings();
        assertEquals(0, p.left);
        assertEquals(0, p.top);
        assertEquals(0, p.right);
        assertEquals(0, p.bottom);

        view.setPaddings(10, 20, 30, 100);
        p = view.getPaddings();
        assertEquals(10, p.left);
        assertEquals(20, p.top);
        assertEquals(30, p.right);
        assertEquals(100, p.bottom);

        p = new Rect(11, 22, 33, 104);
        view.setPaddings(p);
        p = view.getPaddings();
        assertEquals(11, p.left);
        assertEquals(22, p.top);
        assertEquals(33, p.right);
        assertEquals(104, p.bottom);
    }

    @SmallTest
    public void testParent() {
        GLView a = new GLView();
        GLView b = new GLView();
        assertNull(b.mParent);
        a.addComponent(b);
        assertSame(a, b.mParent);
        a.removeComponent(b);
        assertNull(b.mParent);
    }

    @SmallTest
    public void testRoot() {
        GLViewMock a = new GLViewMock();
        GLViewMock b = new GLViewMock();
        GLRoot r = new GLRootStub();
        GLRoot r2 = new GLRootStub();
        a.addComponent(b);

        // Attach to root r
        assertEquals(0, a.mOnAttachCalled);
        assertEquals(0, b.mOnAttachCalled);
        a.attachToRoot(r);
        assertEquals(1, a.mOnAttachCalled);
        assertEquals(1, b.mOnAttachCalled);
        assertSame(r, a.getGLRoot());
        assertSame(r, b.getGLRoot());

        // Detach from r
        assertEquals(0, a.mOnDetachCalled);
        assertEquals(0, b.mOnDetachCalled);
        a.detachFromRoot();
        assertEquals(1, a.mOnDetachCalled);
        assertEquals(1, b.mOnDetachCalled);

        // Attach to another root r2
        assertEquals(1, a.mOnAttachCalled);
        assertEquals(1, b.mOnAttachCalled);
        a.attachToRoot(r2);
        assertEquals(2, a.mOnAttachCalled);
        assertEquals(2, b.mOnAttachCalled);
        assertSame(r2, a.getGLRoot());
        assertSame(r2, b.getGLRoot());

        // Detach from r2
        assertEquals(1, a.mOnDetachCalled);
        assertEquals(1, b.mOnDetachCalled);
        a.detachFromRoot();
        assertEquals(2, a.mOnDetachCalled);
        assertEquals(2, b.mOnDetachCalled);
    }

    @SmallTest
    public void testRoot2() {
        GLView a = new GLViewMock();
        GLViewMock b = new GLViewMock();
        GLRoot r = new GLRootStub();

        a.attachToRoot(r);

        assertEquals(0, b.mOnAttachCalled);
        a.addComponent(b);
        assertEquals(1, b.mOnAttachCalled);

        assertEquals(0, b.mOnDetachCalled);
        a.removeComponent(b);
        assertEquals(1, b.mOnDetachCalled);
    }

    @SmallTest
    public void testInvalidate() {
        GLView a = new GLView();
        GLRootMock r = new GLRootMock();
        a.attachToRoot(r);
        assertEquals(0, r.mRequestRenderCalled);
        a.invalidate();
        assertEquals(1, r.mRequestRenderCalled);
    }

    @SmallTest
    public void testRequestLayout() {
        GLView a = new GLView();
        GLView b = new GLView();
        GLRootMock r = new GLRootMock();
        a.attachToRoot(r);
        a.addComponent(b);
        assertEquals(0, r.mRequestLayoutContentPaneCalled);
        b.requestLayout();
        assertEquals(1, r.mRequestLayoutContentPaneCalled);
    }

    @SmallTest
    public void testLayout() {
        GLViewMock a = new GLViewMock();
        GLViewMock b = new GLViewMock();
        GLViewMock c = new GLViewMock();
        GLRootMock r = new GLRootMock();

        a.attachToRoot(r);
        a.addComponent(b);
        a.addComponent(c);

        assertEquals(0, a.mOnLayoutCalled);
        a.layout(10, 20, 60, 100);
        assertEquals(1, a.mOnLayoutCalled);
        assertEquals(1, b.mOnLayoutCalled);
        assertEquals(1, c.mOnLayoutCalled);
        assertTrue(a.mOnLayoutChangeSize);
        assertTrue(b.mOnLayoutChangeSize);
        assertTrue(c.mOnLayoutChangeSize);

        // same size should not trigger onLayout
        a.layout(10, 20, 60, 100);
        assertEquals(1, a.mOnLayoutCalled);

        // unless someone requested it, but only those on the path
        // to the requester.
        assertEquals(0, r.mRequestLayoutContentPaneCalled);
        b.requestLayout();
        a.layout(10, 20, 60, 100);
        assertEquals(1, r.mRequestLayoutContentPaneCalled);
        assertEquals(2, a.mOnLayoutCalled);
        assertEquals(2, b.mOnLayoutCalled);
        assertEquals(1, c.mOnLayoutCalled);
    }

    @SmallTest
    public void testRender() {
        GLViewMock a = new GLViewMock();
        GLViewMock b = new GLViewMock();

        a.addComponent(b);
        GLCanvasStub canvas = new GLCanvasStub();
        assertEquals(0, a.mRenderBackgroundCalled);
        assertEquals(0, b.mRenderBackgroundCalled);
        a.render(canvas);
        assertEquals(1, a.mRenderBackgroundCalled);
        assertEquals(1, b.mRenderBackgroundCalled);
    }

    @SmallTest
    public void testMeasure() {
        GLViewMock a = new GLViewMock();
        GLViewMock b = new GLViewMock();
        GLViewMock c = new GLViewMock();
        GLRootMock r = new GLRootMock();

        a.addComponent(b);
        a.addComponent(c);
        a.attachToRoot(r);

        assertEquals(0, a.mOnMeasureCalled);
        a.measure(100, 200);
        assertEquals(1, a.mOnMeasureCalled);
        assertEquals(1, b.mOnMeasureCalled);
        assertEquals(100, a.mOnMeasureWidthSpec);
        assertEquals(200, a.mOnMeasureHeightSpec);
        assertEquals(100, b.mOnMeasureWidthSpec);
        assertEquals(200, b.mOnMeasureHeightSpec);
        assertEquals(100, a.getMeasuredWidth());
        assertEquals(200, b.getMeasuredHeight());

        // same spec should not trigger onMeasure
        a.measure(100, 200);
        assertEquals(1, a.mOnMeasureCalled);

        // unless someone requested it, but only those on the path
        // to the requester.
        b.requestLayout();
        a.measure(100, 200);
        assertEquals(2, a.mOnMeasureCalled);
        assertEquals(2, b.mOnMeasureCalled);
        assertEquals(1, c.mOnMeasureCalled);
    }

    class MyGLView extends GLView {
        private int mWidth;
        int mOnTouchCalled;
        int mOnTouchX;
        int mOnTouchY;
        int mOnTouchAction;

        public MyGLView(int width) {
            mWidth = width;
        }

        @Override
        protected void onLayout(boolean changeSize, int left, int top,
                int right, int bottom) {
            // layout children from left to right
            // call children's layout.
            int x = 0;
            for (int i = 0, n = getComponentCount(); i < n; ++i) {
                GLView item = getComponent(i);
                item.measure(0, 0);
                int w = item.getMeasuredWidth();
                int h = item.getMeasuredHeight();
                item.layout(x, 0, x + w, h);
                x += w;
            }
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            setMeasuredSize(mWidth, 100);
        }

        @Override
        protected boolean onTouch(MotionEvent event) {
            mOnTouchCalled++;
            mOnTouchX = (int) event.getX();
            mOnTouchY = (int) event.getY();
            mOnTouchAction = event.getAction();
            return true;
        }
    }

    private MotionEvent NewMotionEvent(int action, int x, int y) {
        return MotionEvent.obtain(0, 0, action, x, y, 0);
    }

    @SmallTest
    public void testTouchEvent() {
        // We construct a tree with four nodes. Only the x coordinate is used:
        // A = [0..............................300)
        // B = [0......100)
        // C =             [100......200)
        // D =             [100..150)

        MyGLView a = new MyGLView(300);
        MyGLView b = new MyGLView(100);
        MyGLView c = new MyGLView(100);
        MyGLView d = new MyGLView(50);
        GLRoot r = new GLRootStub();

        a.addComponent(b);
        a.addComponent(c);
        c.addComponent(d);
        a.attachToRoot(r);
        a.layout(0, 0, 300, 100);

        int DOWN = MotionEvent.ACTION_DOWN;
        int UP = MotionEvent.ACTION_UP;
        int MOVE = MotionEvent.ACTION_MOVE;
        int CANCEL = MotionEvent.ACTION_CANCEL;

        // simple case
        assertEquals(0, a.mOnTouchCalled);
        a.dispatchTouchEvent(NewMotionEvent(DOWN, 250, 0));
        assertEquals(DOWN, a.mOnTouchAction);
        a.dispatchTouchEvent(NewMotionEvent(UP, 250, 0));
        assertEquals(UP, a.mOnTouchAction);
        assertEquals(2, a.mOnTouchCalled);

        // pass to a child, check the location is offseted.
        assertEquals(0, c.mOnTouchCalled);
        a.dispatchTouchEvent(NewMotionEvent(DOWN, 175, 0));
        a.dispatchTouchEvent(NewMotionEvent(UP, 175, 0));
        assertEquals(75, c.mOnTouchX);
        assertEquals(0, c.mOnTouchY);
        assertEquals(2, c.mOnTouchCalled);
        assertEquals(2, a.mOnTouchCalled);

        // motion target cancel event
        assertEquals(0, d.mOnTouchCalled);
        a.dispatchTouchEvent(NewMotionEvent(DOWN, 125, 0));
        assertEquals(1, d.mOnTouchCalled);
        a.dispatchTouchEvent(NewMotionEvent(MOVE, 250, 0));
        assertEquals(2, d.mOnTouchCalled);
        a.dispatchTouchEvent(NewMotionEvent(MOVE, 50, 0));
        assertEquals(3, d.mOnTouchCalled);
        a.dispatchTouchEvent(NewMotionEvent(DOWN, 175, 0));
        assertEquals(4, d.mOnTouchCalled);
        assertEquals(CANCEL, d.mOnTouchAction);
        assertEquals(3, c.mOnTouchCalled);
        assertEquals(DOWN, c.mOnTouchAction);
        a.dispatchTouchEvent(NewMotionEvent(UP, 175, 0));

        // motion target is removed
        assertEquals(4, d.mOnTouchCalled);
        a.dispatchTouchEvent(NewMotionEvent(DOWN, 125, 0));
        assertEquals(5, d.mOnTouchCalled);
        a.removeComponent(c);
        assertEquals(6, d.mOnTouchCalled);
        assertEquals(CANCEL, d.mOnTouchAction);

        // invisible component should not get events
        assertEquals(2, a.mOnTouchCalled);
        assertEquals(0, b.mOnTouchCalled);
        b.setVisibility(GLView.INVISIBLE);
        a.dispatchTouchEvent(NewMotionEvent(DOWN, 50, 0));
        assertEquals(3, a.mOnTouchCalled);
        assertEquals(0, b.mOnTouchCalled);
    }
}
