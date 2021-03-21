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
 * Copyright (C) 2017 biylda <biylda@gmail.com>
 */

package menion.android.whereyougo.gui.utils;

import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Zone;
import menion.android.whereyougo.geo.location.Location;
import menion.android.whereyougo.preferences.PreferenceValues;
import menion.android.whereyougo.preferences.Preferences;

public class UtilsWherigo {

    public static Location extractLocation(EventTable et) {
        if (et == null || !et.isLocated())
            return null;

        Location loc = new Location();
        if (et instanceof Zone) {
            Zone z = ((Zone) et);
            if (Preferences.GUIDING_ZONE_NAVIGATION_POINT == PreferenceValues.VALUE_GUIDING_ZONE_POINT_NEAREST) {
                loc.setLatitude(z.nearestPoint.latitude);
                loc.setLongitude(z.nearestPoint.longitude);
            } else if (et.position != null) {
                loc.setLatitude(z.position.latitude);
                loc.setLongitude(z.position.longitude);
            } else {
                loc.setLatitude(z.bbCenter.latitude);
                loc.setLongitude(z.bbCenter.longitude);
            }
        } else {
            loc.setLatitude(et.position.latitude);
            loc.setLongitude(et.position.longitude);
        }
        return loc;
    }
}
