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

package menion.android.whereyougo.geo.location;

/**
 * Representing one satellite
 *
 * @author menion
 */
public class SatellitePosition {

    Integer prn;
    float azimuth;
    float elevation;
    /**
     * signal to noise ratio
     */
    int snr;
    /**
     * is satellite fixed
     */
    boolean fixed;

    public SatellitePosition() {
        this.prn = 0;
        this.azimuth = 0.0f;
        this.elevation = 0.0f;
        this.snr = 0;
    }

    public float getAzimuth() {
        return azimuth;
    }

    public float getElevation() {
        return elevation;
    }

    public Integer getPrn() {
        return prn;
    }

    public int getSnr() {
        return snr;
    }

    public boolean isFixed() {
        return fixed;
    }
}
