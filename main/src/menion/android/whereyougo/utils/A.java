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

import android.app.Application;

import cgeo.geocaching.BuildConfig;
import cgeo.geocaching.CgeoApplication;
import menion.android.whereyougo.MainApplication;
import menion.android.whereyougo.audio.ManagerAudio;
import menion.android.whereyougo.geo.orientation.Orientation;
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;
import menion.android.whereyougo.guide.GuideContent;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class A {

    private static WhereYouGoActivity main;
    private static final String TAG = "A";
    private static CgeoApplication app;
    private static GuideContent guidingContent;
    private static ManagerAudio managerAudio;
    private static Orientation rotator;

    public static void destroy() {
        guidingContent = null;
        managerAudio = null;
        main = null;
        if (rotator != null) {
            rotator.removeAllListeners();
            rotator = null;
        }
        // finally destroy app
//        if (app != null)
//            app.destroy();
        app = null;
    }

    public static Application getApp() {
        return app;
    }

    public static GuideContent getGuidingContent() {
        if (guidingContent == null) {
            guidingContent = new GuideContent();
        }
        return guidingContent;
    }

    public static WhereYouGoActivity getMain() {
        return main;
    }

    public static ManagerAudio getManagerAudio() {
        if (managerAudio == null) {
            managerAudio = new ManagerAudio();
        }
        return managerAudio;
    }

    public static Orientation getRotator() {
        if (rotator == null) {
            rotator = new Orientation();
        }
        return rotator;
    }

    public static void printState() {
        Logger.i(TAG, "printState() - STATIC VARIABLES");
        Logger.i(TAG, "app:" + app);
        Logger.i(TAG, "managerAudio:" + managerAudio);
        Logger.i(TAG, "main:" + main);
        Logger.i(TAG, "guidingContent:" + guidingContent);
        Logger.i(TAG, "rotator:" + rotator);
    }

    public static void registerApp(CgeoApplication app) {
        A.app = app;
    }

    public static void registerMain(WhereYouGoActivity main) {
        A.main = main;
    }

    public static String getAppName() {
        try {
            return app.getPackageManager().getApplicationLabel(app.getApplicationInfo()).toString();
        } catch (Exception e) {
            return "WhereYouGo";
        }
    }

    public static String getAppVersion() {
        try {
            return app.getPackageManager().getPackageInfo(app.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return BuildConfig.VERSION_NAME;
        }
    }
}
