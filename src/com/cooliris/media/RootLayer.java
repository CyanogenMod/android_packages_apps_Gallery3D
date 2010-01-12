package com.cooliris.media;

import javax.microedition.khronos.opengles.GL11;
import android.hardware.SensorEvent;
import android.view.KeyEvent;

public abstract class RootLayer extends Layer {
    public void onOrientationChanged(int orientation) {
    }

    public void onSurfaceCreated(RenderView renderView, GL11 gl) {
    }

    public void onSurfaceChanged(RenderView view, int width, int height) {
    }

    public void onSensorChanged(RenderView view, SensorEvent e) {
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    public void handleLowMemory() {

    }
}
