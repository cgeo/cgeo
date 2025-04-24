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
import cgeo.geocaching.location.Units;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.utils.GeoHeightUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.wherigo.openwig.platform.LocationService;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.disposables.Disposable;

public class WherigoLocationProvider extends GeoDirHandler implements LocationService {

    private static final WherigoLocationProvider INSTANCE = new WherigoLocationProvider();


    private Disposable disposable = null;

    private GeoData geoData;
    private Float direction;

    private Geopoint fixedLocation;

    private long lastNotifyTimestamp = 0;
    private double lastNotifyLatitude = 0d;
    private double lastNotifyLongitude = 0d;
    private double lastNotifyDirection = 0d;


    public static WherigoLocationProvider get() {
        return INSTANCE;
    }

    private WherigoLocationProvider() {
        connect();
    }

    @Override
    public void updateGeoDir(@NonNull final GeoData geoData, final float direction) {
        this.geoData = geoData;
        this.direction = direction;
        checkNotify(false);
    }

    @Override
    public void connect() {
        if (disposable == null) {
            this.geoData = LocationDataProvider.getInstance().currentGeo();
            this.direction = LocationDataProvider.getInstance().currentDirection();
            this.disposable = this.start(GeoDirHandler.UPDATE_GEODIR);
            Log.w("STARTED: " + this);
        }
    }

    @Override
    public void disconnect() {
        if (disposable != null) {
            disposable.dispose();
            geoData = null;
            direction = null;
            disposable = null;
        }
        Log.w("STOPPED: " + this);
    }

    public void setFixedLocation(final Geopoint fixedLocation) {
        this.fixedLocation = fixedLocation;
        checkNotify(true);
    }

    public boolean hasFixedLocation() {
        return this.fixedLocation != null;
    }

    public Geopoint getLocation() {
        if (hasFixedLocation()) {
            return this.fixedLocation;
        }
        return new Geopoint(getLatitude(), getLongitude());
    }


    @Override
    public double getAltitude() {
        return geoData == null ? 1 : Math.max(1, GeoHeightUtils.getAltitude(geoData)); // it is important that altitute is over 0
    }

    @Override
    public double getHeading() {
        return direction != null ? direction : LocationDataProvider.getInstance().currentDirection();
    }

    @Override
    public double getLatitude() {
        checkNotify(false);
        return getLatitudeInternal();
    }

    private double getLatitudeInternal() {
        if (fixedLocation != null) {
            return fixedLocation.getLatitude();
        }
        if (geoData != null) {
            return geoData.getLatitude();
        }
        return LocationDataProvider.getInstance().currentGeo().getLatitude();
    }

    @Override
    public double getLongitude() {
        checkNotify(false);
        return getLongitudeInternal();
    }

    private double getLongitudeInternal() {
        if (fixedLocation != null) {
            return fixedLocation.getLongitude();
        }
        if (geoData != null) {
            return geoData.getLongitude();
        }
        return LocationDataProvider.getInstance().currentGeo().getLongitude();
    }

    @Override
    public double getPrecision() {
        return 0d;
    }

    @Override
    public int getState() {
        return fixedLocation == null && geoData == null ? LocationService.OFFLINE : LocationService.ONLINE;
    }

    private void checkNotify(final boolean force) {
        final long currentTime = System.currentTimeMillis();
        final double currentLat = getLatitudeInternal();
        final double currentLon = getLongitudeInternal();
        final double currentDir = getHeading();

        final long timePassed = currentTime - lastNotifyTimestamp;
        final boolean differs = currentLat != lastNotifyLatitude || currentLon != lastNotifyLongitude || currentDir != lastNotifyDirection;
        final boolean differsSignificant = Math.abs(currentLat - lastNotifyLatitude) > 0.00001d || Math.abs(currentLon - lastNotifyLongitude) > 0.00001d;

        if (differsSignificant || force || (differs && timePassed > 3000)) {
            lastNotifyLatitude = currentLat;
            lastNotifyLongitude = currentLon;
            lastNotifyDirection = currentDir;
            lastNotifyTimestamp = currentTime;
            WherigoGame.get().notifyListeners(WherigoGame.NotifyType.LOCATION);
        }
    }

    public String toUserDisplayableString() {
        return getLocation() + " · " + Units.formatElevation((float) getAltitude()) + (hasFixedLocation() ? " (fixed)" : "");
    }

    @Override
    @NonNull
    public String toString() {
        return "geoData:" + geoData + ", dir:" + direction + ", fixedLocation: " + fixedLocation;
    }
}
