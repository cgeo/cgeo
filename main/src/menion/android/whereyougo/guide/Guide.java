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

import android.net.Uri;

import java.util.Timer;
import java.util.TimerTask;

import cgeo.geocaching.R;
import menion.android.whereyougo.audio.AudioClip;
import menion.android.whereyougo.geo.location.Location;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.preferences.PreferenceValues;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.Logger;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class Guide implements IGuide {

    private static final String TAG = "WaypointGuide";

    private final Location location;
    private int id;
    private final String name;

    private float azimuth;
    private float distance;

    /**
     * last sound sonar call
     */
    private long lastSonarCall;
    /**
     * audio sound for beeping
     */
    private AudioClip audioBeep;

    private boolean alreadyBeeped;

    /**
     * Creates new waypoint navigator
     *
     * @param name
     * @param location
     */
    public Guide(String name, Location location) {
        this.name = name;
        this.location = location;
        alreadyBeeped = false;
        lastSonarCall = 0;
        try {
            audioBeep = new AudioClip(A.getApp(), R.raw.sound_beep_01);
        } catch (Exception e) {
            Logger.e(TAG, "Guide(" + R.raw.sound_beep_01 + "), e:" + e.toString());
        }
    }

    public void actualizeState(Location actualLocation) {
        azimuth = actualLocation.bearingTo(location);
        distance = actualLocation.distanceTo(location);
    }

    @Override
    public float getAzimuthToTarget() {
        return azimuth;
    }

    @Override
    public float getDistanceToTarget() {
        return distance;
    }

    public int getId() {
        return id;
    }

    private long getSonarTimeout(double distance) {
        if (distance < Preferences.GUIDING_WAYPOINT_SOUND_DISTANCE) {
            return (long) (distance * 1000 / 33);
        } else {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public Location getTargetLocation() {
        return location;
    }

    @Override
    public String getTargetName() {
        return name;
    }

    @Override
    public long getTimeToTarget() {
        if (LocationState.getLocation().getSpeed() > 1.0) {
            return (long) ((getDistanceToTarget() / LocationState.getLocation().getSpeed()) * 1000);
        } else {
            return 0;
        }
    }

    @Override
    public void manageDistanceSoundsBeeping(double distance) {
        try {
            switch (Preferences.GUIDING_WAYPOINT_SOUND) {
                case PreferenceValues.VALUE_GUIDING_WAYPOINT_SOUND_BEEP_ON_DISTANCE:
                    if (distance < Preferences.GUIDING_WAYPOINT_SOUND_DISTANCE && !alreadyBeeped) {
                        playSingleBeep();
                        alreadyBeeped = true;
                    }
                    break;
                case PreferenceValues.VALUE_GUIDING_WAYPOINT_SOUND_INCREASE_CLOSER:
                    long currentTime = System.currentTimeMillis();
                    float sonarTimeout = getSonarTimeout(distance);
                    if ((currentTime - lastSonarCall) > sonarTimeout) { // (currentTime - lastSonarCall) >
                        // soundSonarDuration &&
                        lastSonarCall = currentTime;
                        playSingleBeep();
                    }
                    break;
                case PreferenceValues.VALUE_GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND:
                    if (distance < Preferences.GUIDING_WAYPOINT_SOUND_DISTANCE && !alreadyBeeped) {
                        playCustomSound();
                        alreadyBeeped = true;
                    }
                    break;
            }
        } catch (Exception e) {
            Logger.e(TAG, "manageDistanceSounds(" + distance + "), e:" + e.toString());
        }
    }

    protected void playCustomSound() {
        String uri = Preferences.GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND_URI;
        try {
            final AudioClip audioClip = new AudioClip(A.getApp(), Uri.parse(uri));
            audioClip.play();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    AudioClip.destroyAudio(audioClip);
                }
            }, 5000);
        } catch (Exception e) {
            Logger.e(TAG, "playCustomSound(" + uri + "), e:" + e.toString());
        }
    }

    protected void playSingleBeep() {
        if (audioBeep != null)
            audioBeep.play();
    }
}
