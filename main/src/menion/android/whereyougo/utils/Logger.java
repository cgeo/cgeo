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

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class Logger {

    private static final String TAG = "Logger";
    private static final String logFileName = "error.log";

    public static void d(String tag, String msg) {
        if (Const.STATE_DEBUG_LOGS) {
            Log.d(tag, msg != null ? msg : "null");
        }
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        LogWriter.log(logFileName, "[" + tag + "] " + msg);
    }

    public static void e(String tag, String msg, Exception e) {
        Log.e(tag, msg != null ? msg : "", e);
        LogWriter.log(logFileName, "[" + tag + "] " + msg, e);
    }

    public static void e(String tag, String msg, Throwable t) {
        Log.e(tag, msg, new Exception(t.toString()));
        LogWriter.log(logFileName, "[" + tag + "] " + msg, t);
    }

    public static void i(String tag, String msg) {
        if (Const.STATE_DEBUG_LOGS) {
            Log.i(tag, msg != null ? msg : "null");
        }
    }

    public static void printParserState(XmlPullParser parser) {
        try {
            Logger.d(TAG, "event:" + parser.getEventType() + ", attCount:" + parser.getAttributeCount()
                    + ", columnNum:" + parser.getColumnNumber() + ", depth:" + parser.getDepth() + ", ln:"
                    + parser.getLineNumber() + ", " + parser.getPositionDescription());
        } catch (Exception e) {
            Logger.e(TAG, "printParserState()");
        }
    }

    public static void v(String tag, String msg) {
        if (Const.STATE_DEBUG_LOGS) {
            Log.v(tag, msg != null ? msg : "null");
        }
    }

    public static void w(String tag, String msg) {
        if (Const.STATE_DEBUG_LOGS) {
            Log.w(tag, msg != null ? msg : "null");
        }
    }
}
