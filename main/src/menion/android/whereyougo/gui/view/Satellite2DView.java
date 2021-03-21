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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

import cgeo.geocaching.R;
import menion.android.whereyougo.geo.location.SatellitePosition;
import menion.android.whereyougo.preferences.Locale;
import menion.android.whereyougo.utils.Const;
import menion.android.whereyougo.utils.Images;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.Utils;

/**
 * @author menion
 * @since 26.3.2010 2010
 */
public class Satellite2DView extends View {

    private static final String TAG = "Satellite2DView";

    private static final float SAT_TEXT_SIZE = Utils.getDpPixels(10.0f);

    private boolean drawLock;

    private float r1;
    private float spSize;
    private Drawable bitCompassBg;
    private Bitmap bitSnr;

    private float satsPanelHeight;
    private float space;
    private float lineWidth;
    private float snrWidth;
    private float spX;
    private float spY;

    private Bitmap[] satImages;

    private Paint mPaintBitmap;

    private Paint mPaintText;
    private Paint mPaintSignalLine;

    private int lastWidth;

    private final ArrayList<SatellitePosition> satellites;

    public Satellite2DView(Context context, ArrayList<SatellitePosition> satellites) {
        super(context);
        setBasics();
        this.satellites = satellites;
    }

    public Satellite2DView(Context context, AttributeSet att, ArrayList<SatellitePosition> satellites) {
        super(context, att);
        setBasics();
        this.satellites = satellites;
    }

    private Bitmap getSatImage(float snr) {
        if (snr < 25.0f) {
            return satImages[0];
        } else if (snr >= 20.0f && snr < 40.0f) {
            return satImages[1];
        } else {
            return satImages[2];
        }
    }

    protected void onDraw(Canvas c) {
        if (drawLock)
            return;

        try {
            drawLock = true;
            setConstants(c);

            float x, y, angle, dist;
            int satCount = satellites.size();

            // draw background
            bitCompassBg
                    .setBounds((int) (spX - r1), (int) (spY - r1), (int) (spX + r1), (int) (spY + r1));
            bitCompassBg.draw(c);

            // draw not satellites text
            mPaintText.setColor(Color.BLACK);
            if (satCount == 0) {
                mPaintText.setTextSize(Utils.getDpPixels(20.0f));
                c.drawText(Locale.getString(R.string.no_satellites), spX, spY + mPaintText.descent(), mPaintText);
                drawLock = false;
                return;
            }

            mPaintText.setTextSize(SAT_TEXT_SIZE);
            int snrHeight = bitSnr.getHeight();

            // draw satellite line
            c.drawLine(0, spSize + snrHeight, c.getClipBounds().width(), spSize + snrHeight,
                    mPaintSignalLine);

            double ln100 = Math.log(100.0);
            for (int i = 0; i < satCount; i++) {
                SatellitePosition sat = satellites.get(i);
                float xCenter;
                if (satCount % 2 == 0) {
                    xCenter = spX + (i - satCount / 2) * lineWidth + lineWidth / 2;
                } else {
                    xCenter = spX + (i - (satCount - 1) / 2) * lineWidth;
                }
                // Logger.d(TAG, "drawSatellite(), sat:" + sat.getPrn() + ", " + sat.getSnr() + ", " +
                // sat.getAzimuth() + ", " + sat.getElevation());
                // draw satellite number
                if (sat.isFixed()) {
                    mPaintText.setColor(Color.GREEN);
                } else {
                    mPaintText.setColor(Color.LTGRAY);
                }
                c.drawText((sat.getPrn() < 10 ? "0" : "") + sat.getPrn(), xCenter, spSize + satsPanelHeight,
                        mPaintText);

                int height = (int) (Math.log(sat.getSnr() > 0 ? sat.getSnr() : 1) / ln100 * snrHeight);
                if (height <= 0)
                    height = 1;
                Bitmap snrToDraw =
                        Bitmap.createBitmap(bitSnr, 0, snrHeight - height, bitSnr.getWidth(), height);
                c.drawBitmap(snrToDraw, xCenter - snrWidth / 2, spSize + snrHeight - height, mPaintBitmap);

                // draw satellite image
                angle = sat.getAzimuth();
                dist = (float) (r1 - Math.sin(sat.getElevation() / Const.RHO) * r1);
                dist *= 0.90f;
                x = (float) (spX + dist * Math.sin(angle / Const.RHO));
                y = (float) (spY - dist * Math.cos(angle / Const.RHO));

                // draw satellite number
                mPaintText.setColor(Color.BLACK);
                Bitmap imgSat = getSatImage(sat.getSnr());
                c.drawText("" + sat.getPrn(), x, y - imgSat.getHeight() / 2 - 5, mPaintText);
                c.drawBitmap(imgSat, x - imgSat.getWidth() / 2, y - imgSat.getHeight() / 2, mPaintBitmap);
            }
        } catch (Exception e) {
            Logger.e(TAG, "onDraw()", e);
        }

        drawLock = false;
    }

    private void setBasics() {
        this.drawLock = false;
        space = Utils.getDpPixels(6.0f);

        // background image
        bitCompassBg = Images.getImageD(R.drawable.var_skyplot);

        // load other images
        int imgSize = (int) Utils.getDpPixels(20);
        satImages = new Bitmap[3];
        satImages[0] = Images.getImageB(R.drawable.ic_sat_01, imgSize);
        satImages[1] = Images.getImageB(R.drawable.ic_sat_02, imgSize);
        satImages[2] = Images.getImageB(R.drawable.ic_sat_03, imgSize);

        mPaintBitmap = new Paint();
        mPaintBitmap.setAntiAlias(true);
        mPaintBitmap.setFilterBitmap(true);

        mPaintText = new Paint();
        mPaintText.setAntiAlias(true);
        mPaintText.setTextAlign(Align.CENTER);
        mPaintText.setTextSize(SAT_TEXT_SIZE);
        mPaintText.setShadowLayer(SAT_TEXT_SIZE / 4.0f, 0, 0, Color.WHITE);

        mPaintSignalLine = new Paint();
        mPaintSignalLine.setAntiAlias(true);
        mPaintSignalLine.setStyle(Style.STROKE);
        mPaintSignalLine.setStrokeWidth(2.0f);
        mPaintSignalLine.setColor(Color.GRAY);
    }

    private void setConstants(Canvas c) {
        if (lastWidth == c.getWidth())
            return;

        lastWidth = c.getWidth();

        // set basic constants
        int w = c.getClipBounds().width();
        int h = c.getClipBounds().height();

        // define bottom satellite images
        lineWidth = (w - 2 * space) / 20;
        snrWidth = lineWidth - 2.0f;

        // now get SNR image
        bitSnr = Images.getImageB(R.drawable.var_skyplot_bar, (int) snrWidth);

        // define bottom panel height
        satsPanelHeight = bitSnr.getHeight() + space + mPaintText.getTextSize();

        float skyplotHeight = h - satsPanelHeight - space;
        spSize = Math.min(w, skyplotHeight);
        r1 = spSize / 2.0f * 0.95f;

        // define skyplot center
        spX = c.getClipBounds().width() / 2;
        spY = spSize / 2.0f;

        // Logger.d(TAG, "W:" + Const.SCREEN_WIDTH + ", " + Const.SCREEN_HEIGHT + ", " +
        // c.getClipBounds() + ", lineWidth:" + lineWidth + ", " + satsPanelHeight + ", " + spX + ", " +
        // spY);
    }
}
