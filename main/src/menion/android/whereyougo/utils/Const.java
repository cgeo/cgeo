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

package menion.android.whereyougo.utils;


/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class Const {

    /**
     * 180 / PI
     */
    public static final float RHO = (float) (180 / Math.PI);
    /**
     * precision value for some rounding functions
     */
    public static final int PRECISION = 5;

    /***********************************/
    /* QUICK SWITCH FOR VERSIONS */
    public static final int SCREEN_SIZE_SMALL = 0;
    public static final int SCREEN_SIZE_MEDIUM = 1;
    public static final int SCREEN_SIZE_LARGE = 2;

    /***********************************/
   /* BUILDS VERSION TYPES */
    public static final int SCREEN_SIZE_XLARGE = 3;
    public static final int TEXT_SIZE_SMALL = 0;
    public static final int TEXT_SIZE_MEDIUM = 1;
    public static final int TEXT_SIZE_BIG = 2;
    public static final int TEXT_SIZE_HUGE = 3;
    /***********************************/

    private static final int PUBLISH_RELEASE = 0;
    private static final int PUBLISH_PRIVATE = 2;
    private static final int RELEASE = PUBLISH_PRIVATE;
    /**
     * state variable - disable some un-publicable tweaks
     */
    public static boolean STATE_RELEASE;
    /**
     * state variable - show all logs in LogCat (if false, show only 'e'
     */
    public static boolean STATE_DEBUG_LOGS;
    /**
     * screen width from various sources
     */
    public static int SCREEN_WIDTH = 0;
    /**
     * screen height from various sources
     */
    public static int SCREEN_HEIGHT = 0;

    /***********************************/

    static {
        switch (RELEASE) {
            case PUBLISH_RELEASE:
                STATE_RELEASE = true;
                STATE_DEBUG_LOGS = false;
                break;
            case PUBLISH_PRIVATE:
                STATE_RELEASE = false;
                STATE_DEBUG_LOGS = true;
                break;
        }
    }
}
