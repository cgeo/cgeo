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

package menion.android.whereyougo.gui.activity.wherigo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Vector;

import cz.matejcik.openwig.Action;
import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;
import cz.matejcik.openwig.Thing;
import cgeo.geocaching.R;
import menion.android.whereyougo.geo.location.Location;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.gui.IRefreshable;
import menion.android.whereyougo.gui.extension.DataInfo;
import menion.android.whereyougo.gui.extension.IconedListAdapter;
import menion.android.whereyougo.gui.extension.activity.CustomActivity;
import menion.android.whereyougo.gui.extension.dialog.CustomDialog;
import menion.android.whereyougo.gui.utils.UtilsWherigo;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.Const;
import menion.android.whereyougo.utils.Images;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.Utils;
import menion.android.whereyougo.utils.UtilsFormat;

public abstract class ListVariousActivity extends CustomActivity implements IRefreshable {

    private static final String TAG = "ListVarious";
    private static final Paint paintText;
    private static final Paint paintArrow;
    private static final Paint paintArrowBorder;

    static {
        paintText = new Paint();
        paintText.setColor(Color.RED);
        paintText.setTextSize(Utils.getDpPixels(12.0f));
        paintText.setTypeface(Typeface.DEFAULT_BOLD);
        paintText.setAntiAlias(true);

        paintArrow = new Paint();
        paintArrow.setColor(Color.YELLOW);
        paintArrow.setAntiAlias(true);
        paintArrow.setStyle(Style.FILL);

        paintArrowBorder = new Paint();
        paintArrowBorder.setColor(Color.BLACK);
        paintArrowBorder.setAntiAlias(true);
        paintArrowBorder.setStyle(Style.STROKE);
    }

    private String title;
    private ListView lv;
    private final Vector<Object> stuff = new Vector<>();

    abstract protected void callStuff(Object what);

    private Bitmap getLocatedIcon(EventTable thing) {
        if (!thing.isLocated())
            return Images.IMAGE_EMPTY_B;

        try {
            Bitmap bitmap =
                    Bitmap.createBitmap((int) Utils.getDpPixels(80.0f), (int) Utils.getDpPixels(40.0f),
                            Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
            c.drawColor(Color.TRANSPARENT);

            Location location = UtilsWherigo.extractLocation(thing);

            float azimuth = LocationState.getLocation().bearingTo(location);
            float distance = LocationState.getLocation().distanceTo(location);

            double a;
            int radius = bitmap.getHeight() / 2;
            int cX = radius;
            int cY = bitmap.getHeight() / 2;
            float x1, x2, x3, x4, y1, y2, y3, y4;

            a = azimuth / Const.RHO;
            x1 = (float) (Math.sin(a) * (radius * 0.90));
            y1 = (float) (Math.cos(a) * (radius * 0.90));

            a = (azimuth + 180) / Const.RHO;
            x2 = (float) (Math.sin(a) * (radius * 0.2));
            y2 = (float) (Math.cos(a) * (radius * 0.2));

            a = (azimuth + 140) / Const.RHO;
            x3 = (float) (Math.sin(a) * (radius * 0.6));
            y3 = (float) (Math.cos(a) * (radius * 0.6));

            a = (azimuth + 220) / Const.RHO;
            x4 = (float) (Math.sin(a) * (radius * 0.6));
            y4 = (float) (Math.cos(a) * (radius * 0.6));

            Path path = new Path();
            path.moveTo(cX + x1, cY - y1);
            path.lineTo(cX + x2, cY - y2);
            path.lineTo(cX + x3, cY - y3);
            c.drawPath(path, paintArrow);

            path = new Path();
            path.moveTo(cX + x1, cY - y1);
            path.lineTo(cX + x2, cY - y2);
            path.lineTo(cX + x4, cY - y4);
            c.drawPath(path, paintArrow);

            c.drawLine(cX + x1, cY - y1, cX + x3, cY - y3, paintArrowBorder);
            c.drawLine(cX + x1, cY - y1, cX + x4, cY - y4, paintArrowBorder);
            c.drawLine(cX + x2, cY - y2, cX + x3, cY - y3, paintArrowBorder);
            c.drawLine(cX + x2, cY - y2, cX + x4, cY - y4, paintArrowBorder);

            c.drawText(UtilsFormat.formatDistance(distance, false), radius * 2 + 2,
                    cY + paintText.getTextSize() / 2, paintText);
            return bitmap;
        } catch (Exception e) {
            Logger.e(TAG, "getLocatedIcon(" + thing + ")", e);
            return Images.IMAGE_EMPTY_B;
        }
    }

    Bitmap getStuffIcon(Object object) {
        if (((EventTable) object).isLocated()) {
            return getLocatedIcon((EventTable) object);
        } else {
            Media media = (Media) ((EventTable) object).table.rawget("Icon");
            if (media != null) {
                try {
                    byte[] icon = Engine.mediaFile(media);
                    if (icon != null)
                        return BitmapFactory.decodeByteArray(icon, 0, icon.length);
                } catch (Exception e) {
                    Logger.e(TAG, "getStuffIcon()", e);
                }
            }
            return Images.IMAGE_EMPTY_B;
        }
    }

    abstract protected String getStuffName(Object what);

    abstract protected Vector<Object> getValidStuff();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // kill when GC has cleaned engine instance
        // this code must be implemented in the last onCreate method in inheritance chain
        if (A.getMain() == null || Engine.instance == null) {
            finish();
            return;
        }
        // set layout
        setContentView(R.layout.custom_dialog);

        // set title
        if (getIntent().getStringExtra("title") != null) {
            title = getIntent().getStringExtra("title");
        }
        CustomDialog.setTitle(this, title, null, R.drawable.ic_cancel,
                new CustomDialog.OnClickListener() {

                    @Override
                    public boolean onClick(CustomDialog dialog, View v, int btn) {
                        ListVariousActivity.this.finish();
                        return true;
                    }
                });

        // center linearLayout
        lv = new ListView(ListVariousActivity.this);
        CustomDialog.setContent(this, lv, 0, false, true);

        // set bottom
        CustomDialog.setBottom(this, null, null, null, null, null, null);
    }

    public void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (!stillValid()) {
                    ListVariousActivity.this.finish();
                    return;
                }

                Vector<Object> newStuff = getValidStuff();
                int scrollY = lv.getFirstVisiblePosition();
                // first, validate the stuff already in there
                // TODO
                // for (int i = 0; i < stuff.size(); i++) {
                // Object s = stuff.get(i);
                // int in = newStuff.indexOf(s);
                // if (in == -1) {
                // stuff.remove(i);
                // i--;
                // } else {
                // newStuff.setElementAt(null, in);
                // }
                // }
                // then, add the rest
                stuff.clear();
                for (int i = 0; i < newStuff.size(); i++) {
                    Object s = newStuff.get(i);
                    if (s != null) {
                        stuff.add(s);
                    }
                }

                // create visual part
                ArrayList<DataInfo> data = new ArrayList<>();
                for (int i = 0; i < stuff.size(); i++) {
                    Object s = stuff.get(i);
                    DataInfo dataInfo;
                    // Logger.e("ListVarious", "addToList:" + s + ", " + (s instanceof Action) + ", " + (s
                    // instanceof Cartridge) + ", " + (s instanceof Container) + ", " + (s instanceof Thing));
                    if (s instanceof Thing) {
                        dataInfo = new DataInfo(((Thing) s).name, null, getStuffIcon(s));
                    } else if (s instanceof Action) {
                        dataInfo = new DataInfo(((Action) s).text, null, getStuffIcon(s));
                    } else {
                        dataInfo = new DataInfo(s.toString(), null, getStuffIcon(s));
                    }
                    data.add(dataInfo);
                }

                IconedListAdapter adapter = new IconedListAdapter(ListVariousActivity.this, data, lv);
                adapter.setMultiplyImageSize(1.5f);
                adapter.setTextView02Visible(View.VISIBLE, true);
                lv.setAdapter(adapter);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Logger.d(TAG, "onItemClick:" + position);

                        Object s = null;
                        synchronized (this) {
                            if (position >= 0 && position < stuff.size()) {
                                s = stuff.get(position);
                            }
                        }
                        if (s != null) {
                            callStuff(s);
                        }
                    }
                });

                lv.setSelectionFromTop(scrollY, 5);
            }
        });
    }

    abstract protected boolean stillValid();
}
