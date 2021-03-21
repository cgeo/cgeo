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

package menion.android.whereyougo.gui.activity;

import android.location.LocationManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import cz.matejcik.openwig.EventTable;
import cgeo.geocaching.R;
import cgeo.geocaching.R.id;
import menion.android.whereyougo.geo.location.Location;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.geo.orientation.IOrientationEventListener;
import menion.android.whereyougo.gui.IRefreshable;
import menion.android.whereyougo.gui.activity.wherigo.DetailsActivity;
import menion.android.whereyougo.gui.extension.activity.CustomActivity;
import menion.android.whereyougo.gui.utils.UtilsWherigo;
import menion.android.whereyougo.gui.view.CompassView;
import menion.android.whereyougo.guide.Guide;
import menion.android.whereyougo.guide.IGuide;
import menion.android.whereyougo.guide.IGuideEventListener;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.UtilsFormat;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class GuidingActivity extends CustomActivity implements IGuideEventListener,
        IOrientationEventListener, IRefreshable {

    // private static final String TAG = "GuidingScreen";

    private CompassView viewCompass;

    private TextView viewName;
    private TextView viewProvider;
    private TextView viewLat;
    private TextView viewLon;
    private TextView viewAlt;
    private TextView viewAcc;
    private TextView viewSpeed;
    private TextView viewTimeToTarget;

    /**
     * azimuth from compass
     */
    private float mAzimuth;
    private float mPitch;
    private float mRoll;
    // azimuth to target
    private float azimuthToTarget;

    @Override
    public void guideStart() {
    }

    @Override
    public void guideStop() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (A.getMain() == null) {
            finish();
            return;
        }
        setContentView(R.layout.layout_guiding_screen);

        mAzimuth = 0.0f;
        mPitch = 0.0f;
        mRoll = 0.0f;

        azimuthToTarget = 0.0f;

        viewCompass = new CompassView(this);
        ((LinearLayout) findViewById(R.id.linearLayoutCompass)).addView(viewCompass,
                LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

        viewName = (TextView) findViewById(R.id.textViewName);
        viewProvider = (TextView) findViewById(id.textViewProvider);
        viewAlt = (TextView) findViewById(R.id.textViewAltitude);
        viewSpeed = (TextView) findViewById(R.id.textViewSpeed);
        viewAcc = (TextView) findViewById(R.id.textViewAccuracy);
        viewLat = (TextView) findViewById(R.id.textViewLatitude);
        viewLon = (TextView) findViewById(R.id.textViewLongitude);
        viewTimeToTarget = (TextView) findViewById(R.id.text_view_time_to_target);

        onOrientationChanged(mAzimuth, mPitch, mRoll);
    }

    @Override
    public void onOrientationChanged(float azimuth, float pitch, float roll) {
        // Logger.d(TAG, "onOrientationChanged(" + azimuth + ", " + pitch + ", " + roll + ")");
        Location loc = LocationState.getLocation();
        mAzimuth = azimuth;
        mPitch = pitch;
        mRoll = roll;

        String provider = loc.getProvider();
        switch (provider) {
            case LocationManager.GPS_PROVIDER:
                provider = getString(R.string.provider_gps);
                break;
            case LocationManager.NETWORK_PROVIDER:
                provider = getString(R.string.provider_network);
                break;
            default:
                provider = "-";
                break;
        }
        viewProvider.setText(provider);
        viewLat.setText(UtilsFormat.formatLatitude(loc.getLatitude()));
        viewLon.setText(UtilsFormat.formatLongitude(loc.getLongitude()));
        viewAlt.setText(UtilsFormat.formatAltitude(loc.getAltitude(), true));
        viewAcc.setText(UtilsFormat.formatDistance((double) loc.getAccuracy(), false));
        viewSpeed.setText(UtilsFormat.formatSpeed(loc.getSpeed(), false));

        repaint();
    }

    public void onStart() {
        super.onStart();
        A.getGuidingContent().addGuidingListener(this);
        A.getRotator().addListener(this);
    }

    public void onStop() {
        super.onStop();
        A.getGuidingContent().removeGuidingListener(this);
        A.getRotator().removeListener(this);
    }

    @Override
    public void receiveGuideEvent(IGuide guide, String targetName, float azimuthToTarget,
                                  double distanceToTarget) {
        this.viewName.setText(targetName);
        this.azimuthToTarget = azimuthToTarget;
        viewCompass.setDistance(distanceToTarget);
        if (LocationState.getLocation().getSpeed() > 1) {
            viewTimeToTarget.setText(UtilsFormat.formatTime(true,
                    (long) (distanceToTarget / LocationState.getLocation().getSpeed()) * 1000));
        } else {
            viewTimeToTarget.setText(UtilsFormat.formatTime(true, 0));
        }
        repaint();
    }

    @Override
    public void refresh() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // refresh target position
                EventTable et = DetailsActivity.et;
                if (et == null || !et.isLocated() || !et.isVisible() || A.getGuidingContent() == null)
                    return;
                Location currentTarget = A.getGuidingContent().getTargetLocation();
                Location newTarget = UtilsWherigo.extractLocation(et);
                if (newTarget != null && !newTarget.equals(currentTarget)) {
                    A.getGuidingContent().guideStart(new Guide(et.name, newTarget));
                }

            }
        });
    }

    private void repaint() {
        viewCompass.moveAngles(azimuthToTarget, mAzimuth, mPitch, mRoll);
    }

    @Override
    public void trackGuideCallRecalculate() {
        // ignore
    }
}
