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

package menion.android.whereyougo.guide;

import android.os.Bundle;

import java.util.ArrayList;

import menion.android.whereyougo.geo.location.ILocationEventListener;
import menion.android.whereyougo.geo.location.Location;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.geo.location.SatellitePosition;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.utils.Logger;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class GuideContent implements ILocationEventListener {

    private static final String TAG = "NavigationContent";

    /**
     * actual navigator
     */
    private IGuide mGuide;

    /**
     * last location
     */
    private Location mLocation;

    /**
     * name of target
     */
    private String mTargetName;
    /**
     * azimuth to actual target
     */
    private float mAzimuthToTarget;
    /**
     * distance to target
     */
    private float mDistanceToTarget;


    /**
     * actual array of listeners
     */
    private final ArrayList<IGuideEventListener> listeners;

    public GuideContent() {
        listeners = new ArrayList<>();
    }

    public void addGuidingListener(IGuideEventListener listener) {
        this.listeners.add(listener);
        // actualize data and send event to new listener
        onLocationChanged(LocationState.getLocation());
    }

    public IGuide getGuide() {
        return mGuide;
    }

    @Override
    public String getName() {
        return TAG;
    }

    public int getPriority() {
        return ILocationEventListener.PRIORITY_HIGH;
    }

    public Location getTargetLocation() {
        if (mGuide == null)
            return null;
        else
            return mGuide.getTargetLocation();
    }

    public void guideStart(IGuide guide) {
        this.mGuide = guide;

        // set location listener
        LocationState.addLocationChangeListener(this);
        // call one onLocationChange, to update actual values immediately
        onLocationChanged(LocationState.getLocation());
        // Logger.d(TAG, "X");
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (mGuide != null) {
                        if (Preferences.GUIDING_SOUNDS) {
                            mGuide.manageDistanceSoundsBeeping(mDistanceToTarget);
                        }
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "guideStart(" + mGuide + ")", e);
                }
            }
        });
        thread.start();

        for (IGuideEventListener list : listeners) {
            list.guideStart();
        }
    }

    public void guideStop() {
        this.mGuide = null;

        LocationState.removeLocationChangeListener(this);
        onLocationChanged(LocationState.getLocation());
        for (IGuideEventListener list : listeners) {
            list.guideStop();
        }
    }

    public boolean isGuiding() {
        return getTargetLocation() != null;
    }

    @Override
    public boolean isRequired() {
        return Preferences.GUIDING_GPS_REQUIRED;
    }

    public void onGpsStatusChanged(int event, ArrayList<SatellitePosition> sats) {
    }

    public void onLocationChanged(Location location) {
        // Logger.d(TAG, "onLocationChanged(" + location + ")");
        if (mGuide != null && location != null) {
            mGuide.actualizeState(location);

            mTargetName = mGuide.getTargetName();
            mAzimuthToTarget = mGuide.getAzimuthToTarget();
            mDistanceToTarget = mGuide.getDistanceToTarget();

            mLocation = location;
        } else {
            mTargetName = null;
            mAzimuthToTarget = 0.0f;
            mDistanceToTarget = 0.0f;
        }

        for (IGuideEventListener list : listeners) {
            list.receiveGuideEvent(mGuide, mTargetName, mAzimuthToTarget, mDistanceToTarget);
        }
    }

    public void onStatusChanged(String provider, int state, Bundle extra) {
    }

    public void removeGuidingListener(IGuideEventListener listener) {
        this.listeners.remove(listener);
    }

    protected void trackGuideCallRecalculate() {
        for (IGuideEventListener list : listeners) {
            list.trackGuideCallRecalculate();
        }
    }
}
