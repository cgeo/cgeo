package menion.android.whereyougo.guide;

import cz.matejcik.openwig.Zone;
import menion.android.whereyougo.geo.location.Location;

public class ZoneGuide extends Guide {

    // private static final String TAG = "ZoneGuide";

    private final Zone mZone;

    private boolean mAlreadyEntered = false;

    public ZoneGuide(Zone zone) {
        super(zone.name, new Location("Guidance: " + zone.name, zone.bbCenter.latitude,
                zone.bbCenter.longitude));
        mZone = zone;
        mAlreadyEntered = false;
    }

  /*
   * public void actualizeState(Location location) { super.actualizeState(location); if
   * (mAlreadyEntered == false && mZone.contain == Zone.INSIDE) { mAlreadyEntered = true;
   * 
   * // issue #54 - acoustical switch (Preferences.GUIDING_WAYPOINT_SOUND) { case
   * PreferenceValues.VALUE_GUIDING_WAYPOINT_SOUND_INCREASE_CLOSER: case
   * PreferenceValues.VALUE_GUIDING_WAYPOINT_SOUND_BEEP_ON_DISTANCE: playSingleBeep(); break; case
   * PreferenceValues.VALUE_GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND: playCustomSound(); break; }
   * 
   * // issue #54 - visual //ManagerNotify.toastShortMessage(R.string.guidance_zone_entered);
   * 
   * // issue #54 - vibration Vibrator v = (Vibrator)
   * A.getMain().getSystemService(Context.VIBRATOR_SERVICE); v.vibrate(50); } }
   */

}
