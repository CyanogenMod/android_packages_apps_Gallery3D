package com.cooliris.media;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL11;
import com.cooliris.media.R;

import android.os.SystemClock;

public final class LoadingLayer extends Layer {
    private static final float FADE_INTERVAL = 0.5f;
    private static final float GRAY_VALUE = 0.1f;
    private static final int[] PRELOAD_RESOURCES_ASYNC_UNSCALED = { R.drawable.stack_frame, R.drawable.grid_frame,
            R.drawable.stack_frame_focus, R.drawable.stack_frame_gold, R.drawable.btn_location_filter_unscaled,
            R.drawable.videooverlay, R.drawable.grid_check_on, R.drawable.grid_check_off, R.drawable.icon_camera_small_unscaled,
            R.drawable.icon_picasa_small_unscaled };

    private static final int[] PRELOAD_RESOURCES_ASYNC_SCALED = {/*
                                                                  * R.drawable.btn_camera_pressed
                                                                  * ,
                                                                  * R.drawable.
                                                                  * btn_camera_focus
                                                                  * ,
                                                                  * R.drawable.
                                                                  * fullscreen_hud_bg
                                                                  * ,
                                                                  * R.drawable.
                                                                  * icon_delete,
                                                                  * R.drawable.
                                                                  * icon_edit,
                                                                  * R.drawable.
                                                                  * icon_more,
                                                                  * R.drawable.
                                                                  * icon_share,
                                                                  * R.drawable.
                                                                  * selection_bg_upper
                                                                  * ,
                                                                  * R.drawable.
                                                                  * selection_menu_bg
                                                                  * ,
                                                                  * R.drawable.
                                                                  * selection_menu_bg_pressed
                                                                  * ,
                                                                  * R.drawable.
                                                                  * selection_menu_bg_pressed_left
                                                                  * ,
                                                                  * R.drawable.
                                                                  * selection_menu_bg_pressed_right
                                                                  * ,
                                                                  * R.drawable.
                                                                  * selection_menu_divider
                                                                  * ,
                                                                  * R.drawable.
                                                                  * timebar_bg,
                                                                  * R.drawable.
                                                                  * timebar_knob
                                                                  * ,
                                                                  * R.drawable.
                                                                  * timebar_knob_pressed
                                                                  * ,
                                                                  * R.drawable.
                                                                  * timebar_prev
                                                                  * ,
                                                                  * R.drawable.
                                                                  * timebar_next
                                                                  * ,
                                                                  * R.drawable.
                                                                  * mode_grid,
                                                                  * R.drawable.
                                                                  * mode_stack,
                                                                  * R.drawable.
                                                                  * icon_camera_small
                                                                  * ,
                                                                  * R.drawable.
                                                                  * icon_location_small
                                                                  * ,
                                                                  * R.drawable.
                                                                  * icon_picasa_small
                                                                  * ,
                                                                  * R.drawable.
                                                                  * icon_folder_small
                                                                  * ,
                                                                  * R.drawable.
                                                                  * scroller_new
                                                                  * ,
                                                                  * R.drawable.
                                                                  * scroller_pressed_new
                                                                  * ,
                                                                  * R.drawable.
                                                                  * btn_camera,
                                                                  * R.drawable.
                                                                  * btn_play,
                                                                  * R.drawable
                                                                  * .pathbar_bg,
                                                                  * R.drawable.
                                                                  * pathbar_cap,
                                                                  * R.drawable.
                                                                  * pathbar_join
                                                                  * ,
                                                                  * R.drawable.
                                                                  * transparent,
                                                                  * R.drawable.
                                                                  * icon_home_small
                                                                  * ,
                                                                  * R.drawable.
                                                                  * ic_fs_details
                                                                  * ,
                                                                  * R.drawable.
                                                                  * ic_spinner1,
                                                                  * R.drawable.
                                                                  * ic_spinner2,
                                                                  * R.drawable.
                                                                  * ic_spinner3,
                                                                  * R.drawable.
                                                                  * ic_spinner4,
                                                                  * R.drawable.
                                                                  * ic_spinner5,
                                                                  * R.drawable.
                                                                  * ic_spinner6,
                                                                  * R.drawable.
                                                                  * ic_spinner7,
                                                                  * R.drawable.
                                                                  * ic_spinner8
                                                                  */};

    private boolean mLoaded = false;
    private final FloatAnim mOpacity = new FloatAnim(1f);
    private IntBuffer mVertexBuffer;

    public LoadingLayer() {
        // Create vertex buffer for a screen-spanning quad.
        int dimension = 10000 * 0x10000;
        int[] vertices = { -dimension, -dimension, 0, dimension, -dimension, 0, -dimension, dimension, 0, dimension, dimension, 0 };
        ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        mVertexBuffer = vertexByteBuffer.asIntBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);
    }

    public boolean isLoaded() {
        return true;
    }

    @Override
    public void generate(RenderView view, RenderView.Lists lists) {
        // Add to drawing list.
        lists.blendedList.add(this);

        // Start loading textures.
        int[] textures = PRELOAD_RESOURCES_ASYNC_UNSCALED;
        for (int i = 0; i != textures.length; ++i) {
            view.loadTexture(view.getResource(textures[i], false));
        }
        textures = PRELOAD_RESOURCES_ASYNC_SCALED;
        for (int i = 0; i != textures.length; ++i) {
            view.loadTexture(view.getResource(textures[i]));
        }
    }

    @Override
    public void renderBlended(RenderView view, GL11 gl) {
        // Wait for textures to finish loading before fading out.
        if (!mLoaded) {
            // Request that the view upload all loaded textures.
            view.processAllTextures();

            // Determine if all textures have loaded.
            int[] textures = PRELOAD_RESOURCES_ASYNC_SCALED;
            boolean complete = true;
            for (int i = 0; i != textures.length; ++i) {
                if (view.getResource(textures[i]).mState != Texture.STATE_LOADED) {
                    complete = false;
                    break;
                }
            }
            textures = PRELOAD_RESOURCES_ASYNC_UNSCALED;
            for (int i = 0; i != textures.length; ++i) {
                if (view.getResource(textures[i], false).mState != Texture.STATE_LOADED) {
                    complete = false;
                    break;
                }
            }
            if (complete) {
                mLoaded = true;
                mOpacity.animateValue(0f, FADE_INTERVAL, SystemClock.uptimeMillis());
            }
        }

        // Draw the loading screen.
        float alpha = mOpacity.getValue(view.getFrameTime());
        if (alpha > 0.004f) {
            float gray = GRAY_VALUE * alpha;
            gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
            gl.glColor4f(gray, gray, gray, alpha);
            gl.glVertexPointer(3, GL11.GL_FIXED, 0, mVertexBuffer);
            gl.glDisable(GL11.GL_TEXTURE_2D);
            gl.glDisable(GL11.GL_DEPTH_TEST);
            gl.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
            gl.glEnable(GL11.GL_DEPTH_TEST);
            gl.glEnable(GL11.GL_TEXTURE_2D);
            view.resetColor();
        } else {
            // Hide the layer once completely faded out.
            setHidden(true);
        }
    }

    void reset() {
        mLoaded = false;
        mOpacity.setValue(1.0f);
        setHidden(false);
    }
}
