package com.cooliris.media;

import java.util.Random;

import android.content.Context;

import com.cooliris.media.FloatUtils;

/**
 * A simple structure for a MediaItem that can be rendered.
 */
public final class DisplayItem {
    private static final float STACK_SPACING = 0.2f;
    private DirectLinkedList.Entry<DisplayItem> mAnimatablesEntry = new DirectLinkedList.Entry<DisplayItem>(this);
    private static final Random random = new Random();
    private Vector3f mStacktopPosition = new Vector3f(-1.0f, -1.0f, -1.0f);
    private Vector3f mJitteredPosition = new Vector3f();
    private boolean mHasFocus;
    private Vector3f mTargetPosition = new Vector3f();
    private float mTargetTheta;
    private float mImageTheta;
    private int mStackId;
    private MediaItemTexture mThumbnailImage = null;
    private Texture mScreennailImage = null;
    private UriTexture mHiResImage = null;
    private float mConvergenceSpeed = 1.0f;

    public final MediaItem mItemRef;
    public float mAnimatedTheta;
    public float mAnimatedImageTheta;
    public float mAnimatedPlaceholderFade = 0f;
    public boolean mAlive;
    public Vector3f mAnimatedPosition = new Vector3f();

    public DisplayItem(MediaItem item) {
        mItemRef = item;
        mAnimatedImageTheta = item.mRotation;
        mImageTheta = item.mRotation;
        if (item == null)
            throw new UnsupportedOperationException("Cannot create a displayitem from a null MediaItem.");
    }

    public DirectLinkedList.Entry<DisplayItem> getAnimatablesEntry() {
        return mAnimatablesEntry;
    }

    public final void rotateImageBy(float theta) {
        mImageTheta += theta;
    }

    public final void set(Vector3f position, int stackIndex, boolean performTransition) {
        mConvergenceSpeed = 1.0f;
        Vector3f animatedPosition = mAnimatedPosition;
        Vector3f targetPosition = mTargetPosition;
        int seed = stackIndex;
        int randomSeed = stackIndex;

        if (seed > 3) {
            seed = 3;
            randomSeed = 0;
        }

        if (!mAlive) {
            animatedPosition.set(position);
            animatedPosition.z = -3.0f + stackIndex * STACK_SPACING;
        }

        targetPosition.set(position);
        if (mStackId != stackIndex && stackIndex >= 0) {
            mStackId = stackIndex;
        }

        if (randomSeed == 0) {
            mTargetTheta = 0.0f;
            mTargetPosition.z = seed * STACK_SPACING;
            mJitteredPosition.set(0, 0, seed * STACK_SPACING);
        } else {
            int sign = (seed % 2 == 0) ? 1 : -1;
            if (seed != 0 && !mStacktopPosition.equals(position) && mTargetTheta == 0) {
                mTargetTheta = 30.0f * (0.5f - (float) Math.random());
                mJitteredPosition.x = sign * 12.0f * seed + (0.5f - random.nextFloat()) * 4 * seed;
                mJitteredPosition.y = sign * 4 + ((sign == 1) ? -8.0f : sign * (random.nextFloat()) * 16.0f);
                mJitteredPosition.x *= Gallery.PIXEL_DENSITY;
                mJitteredPosition.y *= Gallery.PIXEL_DENSITY;
                mJitteredPosition.z = seed * STACK_SPACING;
            }
        }
        mTargetPosition.add(mJitteredPosition);
        mStacktopPosition.set(position);
    }

    public int getStackIndex() {
        return mStackId;
    }

    public Texture getThumbnailImage(Context context, MediaItemTexture.Config config) {
        MediaItemTexture texture = mThumbnailImage;
        if (texture == null && config != null) {
            if (mItemRef.mId != Shared.INVALID) {
                texture = new MediaItemTexture(context, config, mItemRef);
            }
            mThumbnailImage = texture;
        }
        return texture;
    }

    public Texture getScreennailImage(Context context) {
        Texture texture = mScreennailImage;
        if (texture == null || texture.mState == Texture.STATE_ERROR) {
            MediaSet parentMediaSet = mItemRef.mParentMediaSet;
            if (parentMediaSet != null && parentMediaSet.mDataSource.getThumbnailCache() == LocalDataSource.sThumbnailCache) {
                if (mItemRef.mId != Shared.INVALID && mItemRef.mId != 0) {
                    texture = new MediaItemTexture(context, null, mItemRef);
                } else if (mItemRef.mContentUri != null) {
                    texture = new UriTexture(mItemRef.mContentUri);
                }
            } else {
                texture = new UriTexture(mItemRef.mScreennailUri);
                ((UriTexture) texture).setCacheId(Utils.Crc64Long(mItemRef.mFilePath));
            }
            mScreennailImage = texture;
        }
        return texture;
    }

    public void clearScreennailImage() {
        if (mScreennailImage != null) {
            mScreennailImage = null;
            mHiResImage = null;
        }
    }

    public void clearHiResImage() {
        mHiResImage = null;
    }

    public void clearThumbnail() {
        mThumbnailImage = null;
    }

    /**
     * Use this function to query the animation state of the display item
     * 
     * @return true if the display item is animating
     */
    public boolean isAnimating() {
        return mAlive
                && (!mAnimatedPosition.equals(mTargetPosition) || mAnimatedTheta != mTargetTheta
                        || mAnimatedImageTheta != mImageTheta || mAnimatedPlaceholderFade != 1f);
    }

    /**
     * This function should be called every time the frame needs to be updated.
     */
    public final void update(float timeElapsedInSec) {
        if (mAlive) {
            timeElapsedInSec *= 1.25f;
            Vector3f animatedPosition = mAnimatedPosition;
            Vector3f targetPosition = mTargetPosition;
            timeElapsedInSec *= mConvergenceSpeed;
            animatedPosition.x = FloatUtils.animate(animatedPosition.x, targetPosition.x, timeElapsedInSec);
            animatedPosition.y = FloatUtils.animate(animatedPosition.y, targetPosition.y, timeElapsedInSec);
            mAnimatedTheta = FloatUtils.animate(mAnimatedTheta, mTargetTheta, timeElapsedInSec);
            mAnimatedImageTheta = FloatUtils.animate(mAnimatedImageTheta, mImageTheta, timeElapsedInSec);
            mAnimatedPlaceholderFade = FloatUtils.animate(mAnimatedPlaceholderFade, 1f, timeElapsedInSec);
            animatedPosition.z = FloatUtils.animate(animatedPosition.z, targetPosition.z, timeElapsedInSec);
        }
    }

    /**
     * Commits all animations for the Display Item
     */
    public final void commit() {
        mAnimatedPosition.set(mTargetPosition);
        mAnimatedTheta = mTargetTheta;
        mAnimatedImageTheta = mImageTheta;
    }

    public final void setHasFocus(boolean hasFocus, boolean pushDown) {
        mConvergenceSpeed = 2.0f;
        mHasFocus = hasFocus;
        int seed = mStackId;
        if (seed > 3) {
            seed = 3;
        }
        if (hasFocus) {
            mTargetPosition.set(mStacktopPosition);
            mTargetPosition.add(mJitteredPosition);
            mTargetPosition.add(mJitteredPosition);
            mTargetPosition.z = seed * STACK_SPACING + (pushDown ? 1.0f : -0.5f);
        } else {
            mTargetPosition.set(mStacktopPosition);
            mTargetPosition.add(mJitteredPosition);
            mTargetPosition.z = seed * STACK_SPACING;
        }
    }

    public final boolean getHasFocus() {
        return mHasFocus;
    }

    public final Texture getHiResImage(Context context) {
        UriTexture texture = mHiResImage;
        if (texture == null) {
            texture = new UriTexture(mItemRef.mContentUri);
            texture.setCacheId(Utils.Crc64Long(mItemRef.mFilePath));
            mHiResImage = texture;
        }
        return texture;
    }

    public boolean isAlive() {
        return mAlive;
    }

    public float getImageTheta() {
        return mImageTheta;
    }
}
