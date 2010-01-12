package com.cooliris.media;

import java.util.HashMap;

// CR: this stuff needs comments really badly.
public final class DisplaySlot {
    private MediaSet mSetRef;
    private String mTitle;
    private StringTexture mTitleImage;
    private String mLocation;
    private StringTexture mLocationImage;

    private static final StringTexture.Config CAPTION_STYLE = new StringTexture.Config();
    private static final StringTexture.Config CLUSTER_STYLE = new StringTexture.Config();
    private static final StringTexture.Config LOCATION_STYLE = new StringTexture.Config();

    static {
        CAPTION_STYLE.sizeMode = StringTexture.Config.SIZE_TEXT_TO_BOUNDS;
        CAPTION_STYLE.fontSize = 16 * Gallery.PIXEL_DENSITY;
        CAPTION_STYLE.bold = true;
        CAPTION_STYLE.width = (Gallery.PIXEL_DENSITY < 1.5f) ? 128 : 256;
        CAPTION_STYLE.height = (Gallery.PIXEL_DENSITY < 1.5f) ? 32 : 64;
        CAPTION_STYLE.yalignment = StringTexture.Config.ALIGN_TOP;
        CAPTION_STYLE.xalignment = StringTexture.Config.ALIGN_HCENTER;

        CLUSTER_STYLE.sizeMode = StringTexture.Config.SIZE_TEXT_TO_BOUNDS;
        CLUSTER_STYLE.width = (Gallery.PIXEL_DENSITY < 1.5f) ? 128 : 256;
        CLUSTER_STYLE.height = (Gallery.PIXEL_DENSITY < 1.5f) ? 32 : 64;
        CLUSTER_STYLE.yalignment = StringTexture.Config.ALIGN_TOP;
        CLUSTER_STYLE.fontSize = 16 * Gallery.PIXEL_DENSITY;
        CLUSTER_STYLE.bold = true;
        CLUSTER_STYLE.xalignment = StringTexture.Config.ALIGN_HCENTER;

        LOCATION_STYLE.sizeMode = StringTexture.Config.SIZE_TEXT_TO_BOUNDS;
        LOCATION_STYLE.fontSize = 12 * Gallery.PIXEL_DENSITY;
        LOCATION_STYLE.width = (Gallery.PIXEL_DENSITY < 1.5f) ? 128 : 256;
        LOCATION_STYLE.height = (Gallery.PIXEL_DENSITY < 1.5f) ? 32 : 64;
        LOCATION_STYLE.fontSize = 12 * Gallery.PIXEL_DENSITY;
        LOCATION_STYLE.xalignment = StringTexture.Config.ALIGN_HCENTER;
    }

    public void setMediaSet(MediaSet set) {
        mSetRef = set;
        mTitle = null;
        mTitleImage = null;
        mLocationImage = null;
        if (set.mReverseGeocodedLocation == null) {
            set.mReverseGeocodedLocationRequestMade = false;
            set.mReverseGeocodedLocationComputed = false;
        }
    }

    public MediaSet getMediaSet() {
        return mSetRef;
    }

    public boolean hasValidLocation() {
        if (mSetRef != null) {
            return (mSetRef.mReverseGeocodedLocation != null);
        } else {
            return false;
        }
    }

    private StringTexture getTextureForString(String string, HashMap<String, StringTexture> textureTable,
            StringTexture.Config config) {
        StringTexture texture = null;
        if (textureTable != null && textureTable.containsKey(string)) {
            texture = textureTable.get(string);
        }
        if (texture == null) {
            texture = new StringTexture(string, config);
            if (textureTable != null) {
                textureTable.put(string, texture);
            }
        }
        return texture;
    }

    public StringTexture getTitleImage(HashMap<String, StringTexture> textureTable) {
        if (mSetRef == null) {
            return null;
        }
        StringTexture texture = mTitleImage;
        String title = mSetRef.mTruncTitleString;
        if (texture == null && title != null && !(title.equals(mTitle))) {
            texture = getTextureForString(title, textureTable, ((mSetRef.mId != Shared.INVALID && mSetRef.mId != 0) ? CAPTION_STYLE
                    : CLUSTER_STYLE));
            mTitleImage = texture;
            mTitle = title;
        }
        return texture;
    }

    public StringTexture getLocationImage(ReverseGeocoder reverseGeocoder, HashMap<String, StringTexture> textureTable) {
        if (mSetRef == null || mSetRef.mTitleString == null) {
            return null;
        }
        if (mLocationImage == null) {
            if (!mSetRef.mReverseGeocodedLocationRequestMade && reverseGeocoder != null) {
                reverseGeocoder.enqueue(mSetRef);
                mSetRef.mReverseGeocodedLocationRequestMade = true;
            }
            if (mSetRef.mReverseGeocodedLocationComputed) {
                String geocodedLocation = mSetRef.mReverseGeocodedLocation;
                if (geocodedLocation != null) {
                    mLocation = geocodedLocation;
                    mLocationImage = getTextureForString(mLocation, textureTable, LOCATION_STYLE);
                }
            }
        }
        return mLocationImage;
    }
}
