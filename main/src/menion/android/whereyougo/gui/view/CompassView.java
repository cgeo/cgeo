/*
 * This file is part of WhereYouGo.
 *
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package menion.android.whereyougo.gui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import cgeo.geocaching.R;
import menion.android.whereyougo.preferences.Locale;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.Images;
import menion.android.whereyougo.utils.Utils;
import menion.android.whereyougo.utils.UtilsFormat;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class CompassView extends View {

    private float mAzimuth;

    /* azimuth for target arrow */
    private float mAzimuthToTarget;
    /* distance to target */
    private double mDistanceToTarget;

    private float cX1, cY1;
    private float r1;

    private Paint paintValueLabel;
    private Paint paintValueDistance;
    private Paint paintValueAzimuth;
    private Paint paintValueTilt;

    private Drawable bitCompassBg;
    private Drawable bitCompassArrow;

    private int lastWidth;

    public CompassView(Context context) {
        super(context);
        initialize();
    }

    public CompassView(Context context, AttributeSet attr) {
        super(context, attr);
        initialize();
    }

    public void draw(Canvas c) {
        super.draw(c);
        // init basic values
        setConstants(c);

        // draw background
        c.save();
        c.translate(cX1, cY1);
        c.rotate(-mAzimuth);
        bitCompassBg.setBounds((int) (-r1), (int) (-r1), (int) (+r1), (int) (+r1));
        bitCompassBg.draw(c);
        c.restore();

        if (A.getGuidingContent().isGuiding()) {
            c.save();
            c.translate(cX1, cY1);
            c.rotate(mAzimuthToTarget - mAzimuth);
            bitCompassArrow.setBounds((int) (-r1), (int) (-r1), (int) (+r1), (int) (+r1));
            bitCompassArrow.draw(c);
            c.restore();
        }

        // draw compass texts
        drawCompassTexts(c);
    }

    private void drawCompassTexts(Canvas c) {
        float space = r1 / 20;
        c.drawText(Locale.getString(R.string.distance), cX1, cY1 - paintValueDistance.getTextSize() - space,
                paintValueLabel);
        c.drawText(UtilsFormat.formatDistance(mDistanceToTarget, false), cX1, cY1 - space,
                paintValueDistance);

        c.drawText(Locale.getString(R.string.azimuth), cX1, cY1 + paintValueLabel.getTextSize() + space,
                paintValueLabel);
        c.drawText(UtilsFormat.formatAngle(mAzimuthToTarget - mAzimuth), cX1,
                cY1 + paintValueLabel.getTextSize() + paintValueAzimuth.getTextSize() + space,
                paintValueAzimuth);
    }

    private void initialize() {
        mAzimuth = 0.0f;
        mAzimuthToTarget = 0.0f;

        // load images
        bitCompassBg = Images.getImageD(R.drawable.var_compass);
        bitCompassArrow = Images.getImageD(R.drawable.var_compass_arrow);

        // set paint methods
        Paint mPaintBitmap = new Paint();
        mPaintBitmap.setAntiAlias(true);
        mPaintBitmap.setFilterBitmap(true);

        paintValueLabel = new Paint();
        paintValueLabel.setAntiAlias(true);
        paintValueLabel.setTextAlign(Align.CENTER);
        paintValueLabel.setColor(Color.WHITE);
        paintValueLabel.setTextSize(Utils.getDpPixels(12.0f));

        paintValueDistance = new Paint(paintValueLabel);
        paintValueAzimuth = new Paint(paintValueDistance);

        paintValueTilt = new Paint(paintValueDistance);
        paintValueTilt.setColor(Color.parseColor("#00a2e6"));
        paintValueTilt.setTypeface(Typeface.DEFAULT_BOLD);
        paintValueTilt.setShadowLayer(Utils.getDpPixels(3), 0, 0, Color.BLACK);
    }

    /**
     * Function which rotate arrow and compass (angles in degrees)
     *
     * @param azimuth         new angle for compas north
     * @param azimuthToTarget new angle for arrow
     */
    public void moveAngles(float azimuthToTarget, float azimuth, float pitch, float roll) {
        this.mAzimuthToTarget = azimuthToTarget;
        this.mAzimuth = azimuth;
        invalidate();
    }

    private void setConstants(Canvas c) {
        if (lastWidth == c.getWidth())
            return;

        lastWidth = c.getWidth();

        // set basic constants
        int w = c.getClipBounds().width();
        int h = c.getClipBounds().height();

        float neededHeight = Math.min(w, h);
        r1 = neededHeight / 2 * 0.90f;

        cX1 = w / 2.0f;
        cY1 = h / 2.0f;

        // center distance text
        paintValueDistance.setTextSize(r1 / 5);
        paintValueAzimuth.setTextSize(r1 / 6);
        paintValueTilt.setTextSize(r1 / 8);
    }

    public void setDistance(double distance) {
        this.mDistanceToTarget = distance;
        invalidate();
    }
}
