package com.android.gallery3d.ui;

import static com.android.gallery3d.ui.Util.dpToPixel;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.animation.Transformation;

import javax.microedition.khronos.opengles.GL11;

public class IconLabel extends GLView {

    public static final int NULL_ID = 0;

    private static final int FONT_COLOR = Color.WHITE;
    private static final float FONT_SIZE = 18;

    private static final int MINIMAL_HEIGHT = 32;

    private static final int NO_ICON_LEADING_SPACE = 10;
    private static final int TEXT_LEFT_PADDING = 6;
    private static final int TEXT_RIGHT_PADDING = 10;

    private static final float ENABLED_ALPHA = 1f;
    private static final float DISABLED_ALPHA = 0.3f;

    private static final int HORIZONTAL_PADDINGS = 4;
    private static final int VERTICAL_PADDINGS = 2;

    private static int sNoIconLeadingSpace;
    private static int sTextLeftPadding;
    private static int sTextRightPadding;
    private static int sMinimalHeight;
    private static float sFontSize;
    private static int sHorizontalPaddings = -1;
    private static int sVerticalPaddings;

    private final BasicTexture mIcon;
    private final StringTexture mText;
    private boolean mEnabled = true;

    private static void initializeStaticVariables(Context context) {
        if (sHorizontalPaddings >= 0) return;

        sNoIconLeadingSpace = dpToPixel(context, NO_ICON_LEADING_SPACE);
        sTextLeftPadding = dpToPixel(context, TEXT_LEFT_PADDING);
        sTextRightPadding = dpToPixel(context, TEXT_RIGHT_PADDING);
        sMinimalHeight = dpToPixel(context, MINIMAL_HEIGHT);
        sHorizontalPaddings = dpToPixel(context, HORIZONTAL_PADDINGS);
        sVerticalPaddings = dpToPixel(context, VERTICAL_PADDINGS);

        sFontSize = dpToPixel(context, FONT_SIZE);
    }

    public IconLabel(Context context, int iconId, int stringId) {
        this(context, iconId == NULL_ID
                ? null : new ResourceTexture(context, iconId),
                context.getString(stringId));
        initializeStaticVariables(context);
    }

    public IconLabel(Context context, BasicTexture icon, String label) {
        initializeStaticVariables(context);
        mIcon = icon;
        mText = StringTexture.newInstance(label, sFontSize, FONT_COLOR);
        setPaddings(sHorizontalPaddings,
                sVerticalPaddings, sHorizontalPaddings, sVerticalPaddings);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = mIcon == null ? sNoIconLeadingSpace : mIcon.getWidth();
        width += mText.getWidth();
        width += sTextRightPadding + sTextLeftPadding;
        int height = Math.max(
                mText.getHeight(), mIcon == null ? 0 : mIcon.getHeight());
        height = Math.max(sMinimalHeight, height);
        MeasureHelper.getInstance(this)
                .setPreferredContentSize(width, height)
                .measure(widthSpec, heightSpec);
    }

    @Override
    protected void render(GLRootView root, GL11 gl) {
        /*
         * The layout: |--padding--|--mIcon--|--sTextLeftPadding--|--mText--|
         *     --mTextRightPadding--|--padding--|
         */
        Rect p = mPaddings;

        int width = getWidth() - p.left - p.right;
        int height = getHeight() - p.top - p.bottom;

        int xoffset = p.left;

        Transformation trans = root.getTransformation();
        float oldAlpha = trans.getAlpha();
        trans.setAlpha(oldAlpha * (mEnabled ? ENABLED_ALPHA : DISABLED_ALPHA));

        BasicTexture icon = mIcon;
        if (icon != null) {
            icon.draw(root, xoffset,
                    p.top + (height - icon.getHeight()) / 2);
            xoffset += icon.getWidth();
        } else {
            xoffset += sNoIconLeadingSpace;
        }

        StringTexture title = mText;
        xoffset += sTextLeftPadding;
        int yoffset = p.top + (height - title.getHeight()) / 2;
        //TODO: cut the text if it is too long
        title.draw(root, xoffset, yoffset);

        trans.setAlpha(oldAlpha);
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled == enabled) return;
        mEnabled = enabled;
        invalidate();
    }
}
