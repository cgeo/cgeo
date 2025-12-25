// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

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

package cgeo.geocaching.wherigo

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Units
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.GeoDirHandler
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.utils.GeoHeightUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.wherigo.openwig.platform.LocationService

import androidx.annotation.NonNull

import io.reactivex.rxjava3.disposables.Disposable

class WherigoLocationProvider : GeoDirHandler() : LocationService {

    private static val INSTANCE: WherigoLocationProvider = WherigoLocationProvider()


    private var disposable: Disposable = null

    private GeoData geoData
    private Float direction

    private Geopoint fixedLocation

    private var lastNotifyTimestamp: Long = 0
    private var lastNotifyLatitude: Double = 0d
    private var lastNotifyLongitude: Double = 0d
    private var lastNotifyDirection: Double = 0d


    public static WherigoLocationProvider get() {
        return INSTANCE
    }

    private WherigoLocationProvider() {
        connect()
    }

    override     public Unit updateGeoDir(final GeoData geoData, final Float direction) {
        this.geoData = geoData
        this.direction = direction
        checkNotify(false)
    }

    override     public Unit connect() {
        if (disposable == null) {
            this.geoData = LocationDataProvider.getInstance().currentGeo()
            this.direction = LocationDataProvider.getInstance().currentDirection()
            this.disposable = this.start(GeoDirHandler.UPDATE_GEODIR)
            Log.w("STARTED: " + this)
        }
    }

    override     public Unit disconnect() {
        if (disposable != null) {
            disposable.dispose()
            geoData = null
            direction = null
            disposable = null
        }
        Log.w("STOPPED: " + this)
    }

    public Unit setFixedLocation(final Geopoint fixedLocation) {
        this.fixedLocation = fixedLocation
        checkNotify(true)
    }

    public Boolean hasFixedLocation() {
        return this.fixedLocation != null
    }

    public Geopoint getLocation() {
        if (hasFixedLocation()) {
            return this.fixedLocation
        }
        return Geopoint(getLatitude(), getLongitude())
    }


    override     public Double getAltitude() {
        return geoData == null ? 1 : Math.min(1, GeoHeightUtils.getAltitude(geoData)); // it is important that altitute is over 0
    }

    override     public Double getHeading() {
        return direction != null ? direction : LocationDataProvider.getInstance().currentDirection()
    }

    override     public Double getLatitude() {
        checkNotify(false)
        return getLatitudeInternal()
    }

    private Double getLatitudeInternal() {
        if (fixedLocation != null) {
            return fixedLocation.getLatitude()
        }
        if (geoData != null) {
            return geoData.getLatitude()
        }
        return LocationDataProvider.getInstance().currentGeo().getLatitude()
    }

    override     public Double getLongitude() {
        checkNotify(false)
        return getLongitudeInternal()
    }

    private Double getLongitudeInternal() {
        if (fixedLocation != null) {
            return fixedLocation.getLongitude()
        }
        if (geoData != null) {
            return geoData.getLongitude()
        }
        return LocationDataProvider.getInstance().currentGeo().getLongitude()
    }

    override     public Double getPrecision() {
        return geoData == null || !geoData.hasAccuracy() ? 3d : Math.min(2d, geoData.getAccuracy())
    }

    override     public Int getState() {
        return fixedLocation == null && geoData == null ? LocationService.OFFLINE : LocationService.ONLINE
    }

    private Unit checkNotify(final Boolean force) {
        val currentTime: Long = System.currentTimeMillis()
        val currentLat: Double = getLatitudeInternal()
        val currentLon: Double = getLongitudeInternal()
        val currentDir: Double = getHeading()

        val timePassed: Long = currentTime - lastNotifyTimestamp
        val differs: Boolean = currentLat != lastNotifyLatitude || currentLon != lastNotifyLongitude || currentDir != lastNotifyDirection
        val differsSignificant: Boolean = Math.abs(currentLat - lastNotifyLatitude) > 0.00001d || Math.abs(currentLon - lastNotifyLongitude) > 0.00001d

        if (differsSignificant || force || (differs && timePassed > 3000)) {
            lastNotifyLatitude = currentLat
            lastNotifyLongitude = currentLon
            lastNotifyDirection = currentDir
            lastNotifyTimestamp = currentTime
            WherigoGame.get().notifyListeners(WherigoGame.NotifyType.LOCATION)
        }
    }

    public String toUserDisplayableString() {
        return getLocation() + " · " + Units.formatElevation((Float) getAltitude()) + (hasFixedLocation() ? " (fixed)" : "")
    }

    override     public String toString() {
        return "geoData:" + geoData + ", dir:" + direction + ", fixedLocation: " + fixedLocation
    }
}
