package com.cooliris.media;

import java.util.HashMap;

import javax.microedition.khronos.opengles.GL11;
import android.os.Process;

public final class GridDrawables {
    // The display primitives.
    public GridQuad mGrid;
    public final GridQuadFrame mFrame;
    public final GridQuad mTextGrid;
    public final GridQuad mSelectedGrid;
    public final GridQuad mVideoGrid;
    public final GridQuad mLocationGrid;
    public final GridQuad mSourceIconGrid;
    public final GridQuad[] mFullscreenGrid = new GridQuad[3];
    private GridQuad mGridNew;

    // All the resource Textures.
    private static final int TEXTURE_FRAME = R.drawable.stack_frame;
    private static final int TEXTURE_GRID_FRAME = R.drawable.grid_frame;
    private static final int TEXTURE_FRAME_FOCUS = R.drawable.stack_frame_focus;
    private static final int TEXTURE_FRAME_PRESSED = R.drawable.stack_frame_gold;
    private static final int TEXTURE_LOCATION = R.drawable.btn_location_filter_unscaled;
    private static final int TEXTURE_VIDEO = R.drawable.videooverlay;
    private static final int TEXTURE_CHECKMARK_ON = R.drawable.grid_check_on;
    private static final int TEXTURE_CHECKMARK_OFF = R.drawable.grid_check_off;
    private static final int TEXTURE_CAMERA_SMALL = R.drawable.icon_camera_small_unscaled;
    private static final int TEXTURE_PICASA_SMALL = R.drawable.icon_picasa_small_unscaled;
    public static final int[] TEXTURE_SPINNER = new int[8];
    private static final int TEXTURE_TRANSPARENT = R.drawable.transparent;
    private static final int TEXTURE_PLACEHOLDER = R.drawable.grid_placeholder;
    
    public Texture mTextureFrame;
    public Texture mTextureGridFrame;
    public Texture mTextureFrameFocus;
    public Texture mTextureFramePressed;
    public Texture mTextureLocation;
    public Texture mTextureVideo;
    public Texture mTextureCheckmarkOn;
    public Texture mTextureCheckmarkOff;
    public Texture mTextureCameraSmall;
    public Texture mTexturePicasaSmall;
    public Texture[] mTextureSpinner = new Texture[8];
    public Texture mTextureTransparent;
    public Texture mTexturePlaceholder;

    // The textures generated from strings.
    public final HashMap<String, StringTexture> mStringTextureTable = new HashMap<String, StringTexture>(128);

    public GridDrawables(final int itemWidth, final int itemHeight) {
        // We first populate the spinner textures.
        final int[] textureSpinner = TEXTURE_SPINNER;
        textureSpinner[0] = R.drawable.ic_spinner1;
        textureSpinner[1] = R.drawable.ic_spinner2;
        textureSpinner[2] = R.drawable.ic_spinner3;
        textureSpinner[3] = R.drawable.ic_spinner4;
        textureSpinner[4] = R.drawable.ic_spinner5;
        textureSpinner[5] = R.drawable.ic_spinner6;
        textureSpinner[6] = R.drawable.ic_spinner7;
        textureSpinner[7] = R.drawable.ic_spinner8;
        
        final float height = 1.0f;
        final float width = (float) (height * itemWidth) / (float) itemHeight;
        final float aspectRatio = (float) itemWidth / (float) itemHeight;
        final float oneByAspect = 1.0f / aspectRatio;

        // We create the grid quad.
        mGrid = GridQuad.createGridQuad(width, height, 0, 0, 1.0f, oneByAspect, false);

        // We create the quads used in fullscreen.
        mFullscreenGrid[0] = GridQuad.createGridQuad(width, height, 0, 0, 1.0f, oneByAspect, false);
        mFullscreenGrid[0].setDynamic(true);
        mFullscreenGrid[1] = GridQuad.createGridQuad(width, height, 0, 0, 1.0f, oneByAspect, false);
        mFullscreenGrid[1].setDynamic(true);
        mFullscreenGrid[2] = GridQuad.createGridQuad(width, height, 0, 0, 1.0f, oneByAspect, false);
        mFullscreenGrid[2].setDynamic(true);

        // We create supplementary quads for the checkmarks, video overlay and location button
        float sizeOfSelectedIcon = 32 * Gallery.PIXEL_DENSITY;  // In pixels.
        sizeOfSelectedIcon /= itemHeight;
        float sizeOfLocationIcon = 52 * Gallery.PIXEL_DENSITY;  // In pixels.
        sizeOfLocationIcon /= itemHeight;
        float sizeOfSourceIcon = 76 * Gallery.PIXEL_DENSITY;  // In pixels.
        sizeOfSourceIcon /= itemHeight;
        mSelectedGrid = GridQuad.createGridQuad(sizeOfSelectedIcon, sizeOfSelectedIcon, -0.5f, 0.25f, 1.0f, 1.0f, false);
        mVideoGrid = GridQuad.createGridQuad(sizeOfSelectedIcon, sizeOfSelectedIcon, -0.08f, -0.09f, 1.0f, 1.0f, false);
        mLocationGrid = GridQuad.createGridQuad(sizeOfLocationIcon, sizeOfLocationIcon, 0, 0, 1.0f, 1.0f, false);
        mSourceIconGrid = GridQuad.createGridQuad(sizeOfSourceIcon, sizeOfSourceIcon, 0, 0, 1.0f, 1.0f, false);
        
        // We create the quad for the text label.
        float seedTextWidth = (Gallery.PIXEL_DENSITY < 1.5f) ? 128.0f : 256.0f;
        float textWidth = (seedTextWidth / (float) itemWidth) * width;
        float textHeightPow2 = (Gallery.PIXEL_DENSITY < 1.5f) ? 32.0f : 64.0f;
        float textHeight = (textHeightPow2 / (float) itemHeight) * height;
        float textOffsetY = 0.0f;
        mTextGrid = GridQuad.createGridQuad(textWidth, textHeight, 0, textOffsetY, 1.0f, 1.0f, false);

        // We finally create the frame around every grid item
        mFrame = GridQuadFrame.createFrame(width, height, itemWidth, itemHeight);
        
        Thread creationThread = new Thread() {
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    ;
                }
                mGridNew = GridQuad.createGridQuad(width, height, 0, 0, 1.0f, oneByAspect, true);
            }
        };
        creationThread.start();
    }
    
    public void swapGridQuads(GL11 gl) {
        if (mGridNew != null) {
            mGrid.freeHardwareBuffers(gl);
            mGrid = mGridNew;
            mGrid.generateHardwareBuffers(gl);
            mGridNew = null;
        }
    }

    public void onSurfaceCreated(RenderView view, GL11 gl) {
        // The grid quad.
        mGrid.freeHardwareBuffers(gl);
        mGrid.generateHardwareBuffers(gl);

        // The fullscreen quads.
        mFullscreenGrid[0].freeHardwareBuffers(gl);
        mFullscreenGrid[1].freeHardwareBuffers(gl);
        mFullscreenGrid[2].freeHardwareBuffers(gl);
        mFullscreenGrid[0].generateHardwareBuffers(gl);
        mFullscreenGrid[1].generateHardwareBuffers(gl);
        mFullscreenGrid[2].generateHardwareBuffers(gl);

        // Supplementary quads.
        mSelectedGrid.freeHardwareBuffers(gl);
        mVideoGrid.freeHardwareBuffers(gl);
        mLocationGrid.freeHardwareBuffers(gl);
        mSourceIconGrid.freeHardwareBuffers(gl);
        mSelectedGrid.generateHardwareBuffers(gl);
        mVideoGrid.generateHardwareBuffers(gl);
        mLocationGrid.generateHardwareBuffers(gl);
        mSourceIconGrid.generateHardwareBuffers(gl);

        // Text quads.
        mTextGrid.freeHardwareBuffers(gl);
        mTextGrid.generateHardwareBuffers(gl);

        // Frame mesh.
        mFrame.freeHardwareBuffers(gl);
        mFrame.generateHardwareBuffers(gl);

        // Clear the string table.
        mStringTextureTable.clear();
        
        // Regenerate all the textures.
        mTextureFrame = view.getResource(TEXTURE_FRAME, false);
        mTextureGridFrame = view.getResource(TEXTURE_GRID_FRAME, false);
        view.loadTexture(mTextureGridFrame);
        mTextureFrameFocus = view.getResource(TEXTURE_FRAME_FOCUS, false);
        mTextureFramePressed = view.getResource(TEXTURE_FRAME_PRESSED, false);
        mTextureLocation = view.getResource(TEXTURE_LOCATION, false);
        mTextureVideo = view.getResource(TEXTURE_VIDEO, false);
        mTextureCheckmarkOn = view.getResource(TEXTURE_CHECKMARK_ON, false);
        mTextureCheckmarkOff = view.getResource(TEXTURE_CHECKMARK_OFF, false);
        mTextureCameraSmall = view.getResource(TEXTURE_CAMERA_SMALL, false);
        mTexturePicasaSmall = view.getResource(TEXTURE_PICASA_SMALL, false);
        mTextureTransparent = view.getResource(TEXTURE_TRANSPARENT, false);
        mTexturePlaceholder = view.getResource(TEXTURE_PLACEHOLDER, false);
        
        mTextureSpinner[0] = view.getResource(R.drawable.ic_spinner1);
        mTextureSpinner[1] = view.getResource(R.drawable.ic_spinner2);
        mTextureSpinner[2] = view.getResource(R.drawable.ic_spinner3);
        mTextureSpinner[3] = view.getResource(R.drawable.ic_spinner4);
        mTextureSpinner[4] = view.getResource(R.drawable.ic_spinner5);
        mTextureSpinner[5] = view.getResource(R.drawable.ic_spinner6);
        mTextureSpinner[6] = view.getResource(R.drawable.ic_spinner7);
        mTextureSpinner[7] = view.getResource(R.drawable.ic_spinner8);
    }

    public int getIconForSet(MediaSet set, boolean scaled) {
        // We return the scaled version for HUD rendering and the unscaled version for 3D rendering.
        if (scaled) {
            if (set == null) {
                return R.drawable.icon_folder_small;
            }
            if (set.mPicasaAlbumId != Shared.INVALID) {
                return R.drawable.icon_picasa_small;
            } else if (set.mId == LocalDataSource.CAMERA_BUCKET_ID) {
                return R.drawable.icon_camera_small;
            } else {
                return R.drawable.icon_folder_small;
            }
        } else {
            if (set == null) {
                return R.drawable.icon_folder_small_unscaled;
            }
            if (set.mPicasaAlbumId != Shared.INVALID) {
                return R.drawable.icon_picasa_small_unscaled;
            } else if (set.mId == LocalDataSource.CAMERA_BUCKET_ID) {
                return R.drawable.icon_camera_small_unscaled;
            } else {
                return R.drawable.icon_folder_small_unscaled;
            }
        }
    }
}
