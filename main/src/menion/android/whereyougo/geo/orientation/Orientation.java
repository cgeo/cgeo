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

package menion.android.whereyougo.geo.orientation;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Vector;

import menion.android.whereyougo.geo.location.ILocationEventListener;
import menion.android.whereyougo.geo.location.Location;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.geo.location.SatellitePosition;
import menion.android.whereyougo.preferences.PreferenceValues;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.Logger;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class Orientation implements SensorEventListener, ILocationEventListener {

  private static final String TAG = "Orientation";
  private static float orient;
  private static float pitch;
  private static float roll;
  private static float aboveOrBelow = 0.0f;
  private static GeomagneticField gmf;
  private static long lastCompute;
  private final Vector<IOrientationEventListener> listeners;
  private SensorManager sensorManager;
  private float mLastAziGps;
  private float mLastAziSensor;
  private float mLastPitch;
  private float mLastRoll;

  public Orientation() {
    this.listeners = new Vector<>();
  }

  public static float getDeclination() {
    long actualTime = System.currentTimeMillis();
    if (gmf == null || actualTime - lastCompute > 300000) { // once per five minutes

      Location loc = LocationState.getLocation();
      // compute this only if needed
      gmf =
              new GeomagneticField((float) loc.getLatitude(), (float) loc.getLongitude(),
                      (float) loc.getAltitude(), actualTime);
      lastCompute = actualTime;
      Logger.w(TAG, "getDeclination() - dec:" + gmf.getDeclination());
    }

    return gmf.getDeclination();
  }

  public void addListener(IOrientationEventListener listener) {
    if (!listeners.contains(listener)) {
      this.listeners.add(listener);
      Logger.i(TAG, "addListener(" + listener + "), listeners.size():" + listeners.size());
      manageSensors();
    }
  }

  private float filterValue(float valueActual, float valueLast) {
    if (valueActual < valueLast - 180.0f) {
      valueLast -= 360.0f;
    } else if (valueActual > valueLast + 180.0f) {
      valueLast += 360.0f;
    }

    // Logger.d(TAG, "filterValue(" + valueActual + ", " + valueLast + ")");
    float filter = getFilter();
    return (float) ((valueActual * filter) + (valueLast * (1.0 - filter)));
  }

  private float getFilter() {
    switch (Preferences.SENSOR_ORIENT_FILTER) {
      case PreferenceValues.VALUE_SENSORS_ORIENT_FILTER_LIGHT:
        return 0.20f;
      case PreferenceValues.VALUE_SENSORS_ORIENT_FILTER_MEDIUM:
        return 0.06f;
      case PreferenceValues.VALUE_SENSORS_ORIENT_FILTER_HEAVY:
        return 0.03f;
    }
    return 1.0f;
  }

  @Override
  public String getName() {
    return TAG;
  }

  public int getPriority() {
    return ILocationEventListener.PRIORITY_MEDIUM;
    }

  @Override
  public boolean isRequired() {
    return false;
  }

  public void manageSensors() {
    // stop sensor manager
    if (sensorManager != null) {
      mLastAziGps = 0.0f;
      mLastAziSensor = 0.0f;
      sendOrientation(0.0f, 0.0f);
      sensorManager.unregisterListener(this);
      sensorManager = null;
      // remove location listener
      LocationState.removeLocationChangeListener(this);
    }

    // start new manager
    if (listeners.size() > 0) {
      if (sensorManager == null) {
        sensorManager = (SensorManager) A.getMain().getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
      }

      // get azimuth from GPS when enabled in settings or by auto-change
      if (!Preferences.SENSOR_HARDWARE_COMPASS || Preferences.SENSOR_HARDWARE_COMPASS_AUTO_CHANGE) {
        // register location listener
        LocationState.addLocationChangeListener(this);
        // set zero bearing, if previously was set by sensor
        mLastAziGps = 0.0f;
      }
    }
    }

  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

  public void onGpsStatusChanged(int event, ArrayList<SatellitePosition> sats) {
  }

  public void onLocationChanged(Location location) {
    // Logger.d(TAG, "onLocationChanged(), bear:" + location.hasBearing() + ", " +
    // location.getBearing());
    if (location.getBearing() != 0.0f) {
      this.mLastAziGps = location.getBearing();
      sendOrientation(mLastPitch, mLastRoll);
    }
    }

  public void onSensorChanged(SensorEvent event) {
    switch (event.sensor.getType()) {
      case Sensor.TYPE_MAGNETIC_FIELD:
        break;
      case Sensor.TYPE_ACCELEROMETER:
        float filter = getFilter();
        aboveOrBelow =
                (float) ((event.values[SensorManager.DATA_Z] * filter) + (aboveOrBelow * (1.0 - filter)));
        break;
      case Sensor.TYPE_ORIENTATION:
        float valueOr = event.values[SensorManager.DATA_X];
        // Logger.d(TAG, "sensorOrientation:" + valueOr + ", " + event.values[SensorManager.DATA_Y]
        // + ", " + event.values[SensorManager.DATA_Z] + ", " + getDeclination());
        // fix to true bearing
        if (Preferences.SENSOR_BEARING_TRUE) {
          valueOr += getDeclination();
        }
        orient = filterValue(valueOr, orient);
        pitch = filterValue(event.values[SensorManager.DATA_Y], pitch);

        roll = filterValue(event.values[SensorManager.DATA_Z], roll);
        float rollDef;
        if (aboveOrBelow < 0) {
          if (roll < 0) {
            rollDef = -180 - roll;
          } else {
            rollDef = 180 - roll;
          }
        } else {
          rollDef = roll;
        }
        this.mLastAziSensor = orient;

        // do some orientation change by settings
        int rotation = A.getMain().getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
          case Surface.ROTATION_0:
            // no need for change
            break;
          case Surface.ROTATION_90:
            mLastAziSensor += 90;
            break;
          case Surface.ROTATION_180:
            mLastAziSensor -= 180;
            break;
          case Surface.ROTATION_270:
            mLastAziSensor -= 90;
            break;
        }

        sendOrientation(pitch, rollDef);
        break;
    }
    }

  public void onStatusChanged(String provider, int state, Bundle extras) {
    }

  public void removeAllListeners() {
    listeners.clear();
    manageSensors();
    }

  public void removeListener(IOrientationEventListener listener) {
    if (listeners.contains(listener)) {
      this.listeners.remove(listener);
      Logger.i(TAG, "removeListener(" + listener + "), listeners.size():" + listeners.size());
      manageSensors();
        }
  }

  private void sendOrientation(float pitch, float roll) {
    float usedOrient;
    if (!Preferences.SENSOR_HARDWARE_COMPASS_AUTO_CHANGE
            || LocationState.getLocation().getSpeed() < Preferences.SENSOR_HARDWARE_COMPASS_AUTO_CHANGE_VALUE) {
      if (!Preferences.SENSOR_HARDWARE_COMPASS)
        usedOrient = mLastAziGps;
      else
        usedOrient = mLastAziSensor;
        } else {
      usedOrient = mLastAziGps;
        }

    this.mLastPitch = pitch;
    this.mLastRoll = roll;

    for (IOrientationEventListener listener : listeners) {
      listener.onOrientationChanged(usedOrient, mLastPitch, mLastRoll);
    }
    }
}
