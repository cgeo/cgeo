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

package menion.android.whereyougo.audio;

import java.io.ByteArrayInputStream;

import cgeo.geocaching.R;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.Logger;

public class UtilsAudio {

    private static final String TAG = "UtilsAudio";

    public static void playBeep(int count) {
        try {
            if (A.getApp() != null)
                new AudioClip(A.getApp(), R.raw.sound_beep_01).play(count);
            else if (A.getMain() != null)
                new AudioClip(A.getMain(), R.raw.sound_beep_01).play(count);
        } catch (Exception e) {
            Logger.e(TAG, "playBeep(" + count + ")", e);
        }
    }

    public static void playSound(byte[] data, String mime) {
        Logger.i(TAG, "playSound(" + (data != null ? data.length : 0) + ", " + mime + ")");

        if (data == null || data.length == 0 || mime == null) {
            Logger.e(TAG, "playSound(): invalid parameters");
            return;
        }

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            if ("audio/x-wav".equals(mime)) {
                A.getManagerAudio().playMp3File("audio", ".wav", bis);
            } else if ("audio/mpeg".equals(mime)) {
                A.getManagerAudio().playMp3File("audio", ".mp3", bis);
            } else {
                Logger.e(TAG, "playSound(): unsupported mime-type");
            }
        } catch (Exception e) {
            Logger.e(TAG, "playSound() failed", e);
        }
    }

    public static void stopSound() {
        A.getManagerAudio().stopSound();
    }
}
