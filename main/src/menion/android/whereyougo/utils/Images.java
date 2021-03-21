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

package menion.android.whereyougo.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import cgeo.geocaching.R;
import menion.android.whereyougo.preferences.Preferences;

public class Images {

    /**
     * bigger icon size for titles, logos, etc...
     */
    public static final int SIZE_HUGE = (int) Utils.getDpPixels(48);
    /**
     * bigger icon size for titles, logos, etc...
     */
    public static final int SIZE_BIG = (int) Utils.getDpPixels(32);
    /**
     * smaller icon size mainly for map items
     */
    public static final int SIZE_MEDIUM = (int) Utils.getDpPixels(24);
    /**
     * smallest icon size
     */
    public static final int SIZE_SMALL = (int) Utils.getDpPixels(16);
    private static final String TAG = "Images";
    public static final Bitmap IMAGE_EMPTY_B = getImageB(R.drawable.var_empty);

    public static Bitmap getImageB(int id) {
        try {
            if (id <= 0)
                return IMAGE_EMPTY_B;
            return BitmapFactory.decodeResource(A.getApp().getResources(), id);
        } catch (Exception e) {
            Logger.w(TAG, "getImageB(" + id + "), e:" + e.toString());
            return IMAGE_EMPTY_B;
        }
    }

    public static Bitmap getImageB(int id, int width) {
        Bitmap bitmap = getImageB(id);
        return resizeBitmap(bitmap, width);
    }

    public static Drawable getImageD(Bitmap bitmap) {
        Drawable draw = new BitmapDrawable(bitmap);
        draw.setBounds(0, 0, draw.getIntrinsicWidth(), draw.getIntrinsicHeight());
        return draw;
    }

    public static Drawable getImageD(int id) {
        try {
            Drawable draw = A.getApp().getResources().getDrawable(id);
            draw.setBounds(0, 0, draw.getIntrinsicWidth(), draw.getIntrinsicHeight());
            return draw;
        } catch (Exception e) {
            // some exceptions on market (crash errors)
            return null;
        }
    }

    public static Drawable getImageD(int id, int size) {
        if (A.getApp() == null)
            return null;

        Drawable draw = A.getApp().getResources().getDrawable(id);
        return getSizeOptimizedIcon(draw, size);
    }

    public static Drawable getSizeOptimizedIcon(Drawable draw, int newSize) {
        if (draw == null)
            return getImageD(R.drawable.var_empty);
        draw.setBounds(0, 0, newSize, newSize);
        draw.invalidateSelf();
        return draw;
    }

    public static Bitmap resizeBitmap(Bitmap bmp) {
        if (Preferences.APPEARANCE_IMAGE_STRETCH) {
            return resizeBitmap(bmp, Const.SCREEN_WIDTH);
        } else {
            return bmp;
        }
    }

    public static Bitmap resizeBitmap(Bitmap bmp, int newWidth) {
        if (bmp == null)
            return null;

        return resizeBitmap(bmp, newWidth, newWidth * bmp.getHeight() / bmp.getWidth());
    }

    public static Bitmap resizeBitmap(Bitmap bmp, int newWidth, int newHeight) {
        if (bmp == null || newWidth <= 0 || bmp.getWidth() == newWidth)
            return bmp;

        return Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true);
    }

    public static Bitmap overlayBitmapToCenter(Bitmap bitmap1, Bitmap bitmap2) {
        int bitmapWidth = Math.max(bitmap1.getWidth(), bitmap2.getWidth());
        int bitmapHeight = Math.max(bitmap1.getHeight(), bitmap2.getHeight());

        float margin1Left = (bitmapWidth - bitmap1.getWidth()) * 0.5f;
        float margin1Top = (bitmapHeight - bitmap1.getHeight()) * 0.5f;

        float margin2Left = (bitmapWidth - bitmap2.getWidth()) * 0.5f;
        float margin2Top = (bitmapHeight - bitmap2.getHeight()) * 0.5f;

        Bitmap overlayBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, bitmap1.getConfig());
        Canvas canvas = new Canvas(overlayBitmap);
        canvas.drawBitmap(bitmap1, margin1Left, margin1Top, null);
        canvas.drawBitmap(bitmap2, margin2Left, margin2Top, null);
        return overlayBitmap;
    }
}
