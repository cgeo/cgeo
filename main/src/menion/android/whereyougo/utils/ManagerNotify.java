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

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import cgeo.geocaching.R;
import menion.android.whereyougo.preferences.Locale;
import menion.android.whereyougo.preferences.PreferenceValues;

/**
 * @author menion
 * @since 10.2.2010 2010
 */
public class ManagerNotify {

    private static final String TAG = "ManagerNotify";

    public static void toastInternetProblem() {
        toastLongMessage(Locale.getString(R.string.problem_with_internet_connection));
    }

    public static void toastLongMessage(final Context context, final String msg) {
        toastMessage(context, msg, Toast.LENGTH_LONG);
    }

    public static void toastLongMessage(final int msg) {
        toastLongMessage(Locale.getString(msg));
    }

    public static void toastLongMessage(final String msg) {
        toastLongMessage(PreferenceValues.getCurrentActivity(), msg);
    }

    private static void toastMessage(final Context context, final String msg, final int time) {
        Logger.d(TAG, "toastMessage(" + context + ", " + msg + ", " + time + ")");
        if (context == null || msg == null || msg.length() == 0)
            return;

        try {
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(context, msg, time).show();
                    }
                });
            } else {
                Toast.makeText(context, msg, time).show();
            }
        } catch (Exception e) {
            Logger.e(TAG, "toastMessage(" + context + ", " + msg + ", " + time + ")", e);
        }
    }

    public static void toastShortMessage(final Context context, final String msg) {
        toastMessage(context, msg, Toast.LENGTH_SHORT);
    }

    public static void toastShortMessage(final int msg) {
        toastShortMessage(Locale.getString(msg));
    }

    public static void toastShortMessage(final String msg) {
        toastShortMessage(PreferenceValues.getCurrentActivity(), msg);
    }

    public static void toastUnexpectedProblem() {
        toastLongMessage(Locale.getString(R.string.unexpected_problem));
    }
}
