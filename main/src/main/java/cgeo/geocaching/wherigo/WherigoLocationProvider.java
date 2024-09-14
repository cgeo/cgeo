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

package cgeo.geocaching.wherigo;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import cz.matejcik.openwig.platform.LocationService;
import io.reactivex.rxjava3.disposables.Disposable;

public class WherigoLocationProvider extends GeoDirHandler implements LocationService {

    private static final WherigoLocationProvider INSTANCE = new WherigoLocationProvider();


    private Disposable disposable = null;
    private GeoData geoData;
    private float direction;
    private Geopoint fixedLocation;

    private double lastSentLatitude = 0d;
    private double lastSentLongitude = 0d;
    private double lastSentAltitude = 0d;
    private double lastSentDirection = 0d;

    private long lastNotifyTimestamp = 0;

    public static WherigoLocationProvider get() {
        return INSTANCE;
    }

    private WherigoLocationProvider() {
        //singleton
    }

    @Override
    public void updateGeoDir(@NonNull final GeoData geoData, final float direction) {
        this.geoData = geoData;
        this.direction = direction;
    }

    @Override
    public void connect() {
        disconnect();
        this.geoData = LocationDataProvider.getInstance().currentGeo();
        this.direction = LocationDataProvider.getInstance().currentDirection();
        this.disposable = this.start(GeoDirHandler.UPDATE_GEODIR);
        Log.w("STARTED: " + this);
    }

    @Override
    public void disconnect() {
        if (disposable != null) {
            disposable.dispose();
            geoData = null;
            direction = 0;
            disposable = null;
        }
        Log.w("STOPPED: " + this);
    }

    public void setFixedLocation(final Geopoint fixedLocation) {
        this.fixedLocation = fixedLocation;
    }


    @Override
    public double getAltitude() {
        final double newAltitude = geoData == null ? 0 : geoData.getAltitude();
        lastSentAltitude = checkNotify(lastSentAltitude, newAltitude);
        return newAltitude <= 0 ? 1 : newAltitude; // important that altitute is over 0
    }

    @Override
    public double getHeading() {
        lastSentDirection = checkNotify(lastSentDirection, direction);
        return direction;
    }

    @Override
    public double getLatitude() {
        if (fixedLocation != null) {
            return fixedLocation.getLatitude();
        }
        final double newLatitude = geoData == null ? 0 : geoData.getLatitude();
        lastSentLatitude = checkNotify(lastSentLatitude, newLatitude);
        return newLatitude;
    }

    @Override
    public double getLongitude() {
        if (fixedLocation != null) {
            return fixedLocation.getLongitude();
        }
        final double newLongitude = geoData == null ? 0 : geoData.getLongitude();
        lastSentLongitude = checkNotify(lastSentLongitude, newLongitude);
        return newLongitude;
    }

    @Override
    public double getPrecision() {
        return 0d;
    }

    @Override
    public int getState() {
        return fixedLocation == null && geoData == null ? LocationService.OFFLINE : LocationService.ONLINE;
    }

    private double checkNotify(final double lastSentValue, final double newValue) {
        final long timeMillis = System.currentTimeMillis();
        if (lastSentValue != newValue && timeMillis - lastNotifyTimestamp > 3000) {
            Log.d("WHERIGOGAME LOCATIOn DIFF: " + lastSentValue + " <-> " + newValue);
            WherigoGame.get().notifyListeners(WherigoGame.NotifyType.LOCATION);
            lastNotifyTimestamp = timeMillis;
            return newValue;
        }
        return lastSentValue;
    }

    @Override
    @NonNull
    public String toString() {
        return "geoData:" + geoData + ", dir:" + direction + ", fixedLocation: " + fixedLocation;
    }
}
