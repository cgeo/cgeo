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

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Hashtable;

import cgeo.geocaching.R;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.FileSystem;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.Utils;

/**
 * @author menion
 * @since 15.3.2010 2010
 */
public class ManagerAudio {

    private static final int SOUND_POOL_BEEP = 1001;
    private static final String TAG = "ManagerAudio";
    private final SoundPool soundPool;
    private final Hashtable<Integer, Integer> soundPoolMap;

    private long lastVolumeCheck;

    private float volume;

    public ManagerAudio() {
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        soundPoolMap = new Hashtable<>();
        soundPoolMap.put(SOUND_POOL_BEEP, soundPool.load(A.getApp(), R.raw.sound_beep_01, 1));
    }

    public void playMp3File(String fileName, String fileEnd, InputStream is) {
        try {
            // load data
            byte[] data = new byte[is.available()];
            is.read(data);
            Utils.closeStream(is);

            // write data
            File fileMp3 = new File(FileSystem.CACHE_AUDIO + fileName + fileEnd);
            if (fileMp3.exists())
                fileMp3.delete();
            fileMp3.getParentFile().mkdirs();

            FileOutputStream fos = new FileOutputStream(fileMp3);
            fos.write(data);
            fos.flush();
            Utils.closeStream(fos);
            // Logger.d(TAG, "playMp3File(), file:" + fileMp3.getAbsolutePath() + ", " +
            // fileMp3.exists());

            // version 01 - good for MP3, wrong on WAV for unknown reason
            // new AudioClip(fileMp3.getAbsolutePath(), false).play();
            // version 02 - as service
            Intent intent = new Intent(A.getMain(), AudioPlayService.class);
            intent.putExtra(AudioPlayService.EXTRA_FILEPATHS, fileMp3.getAbsolutePath());
            intent.putExtra(AudioPlayService.EXTRA_DELETE_FILE, false);
            intent.putExtra(AudioPlayService.EXTRA_PLAY_AS_NOTIFICATION, false);
            A.getMain().startService(intent);
        } catch (Exception e) {
            Logger.e(TAG, "playMp3File(" + fileName + ", " + fileEnd + ", " + is + ")", e);
        }
    }

    public void playSound(int sound) {
        if (volume == 0.0f || (System.currentTimeMillis() - lastVolumeCheck) > 1000) {
      /* The next 4 lines calculate the current volume in a scale of 0.0 to 1.0 */
            AudioManager mgr = (AudioManager) A.getMain().getSystemService(Context.AUDIO_SERVICE);
            float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
            float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            volume = streamVolumeCurrent / streamVolumeMax;
            lastVolumeCheck = System.currentTimeMillis();
        }

    /* Play the sound with the correct volume */
        soundPool.play(soundPoolMap.get(sound), volume, volume, 1, 0, 1f);
    }

    public void putAudio(int audioId, String filePath) {
        try {
            soundPoolMap.put(audioId, soundPool.load(filePath, 1));
        } catch (Exception e) {
            Logger.e(TAG, "putAudio(" + audioId + ")", e);
        }
    }

    public void putAudio(int audioId, String fileName, String fileEnd, InputStream is) {
        try {
            byte[] data = new byte[is.available()];
            is.read(data);
            Utils.closeStream(is);
            String filePath =
                    FileSystem.CACHE_AUDIO + Utils.hashString(fileName) + "." + fileEnd;
            FileSystem.saveBytes(filePath, data);
            soundPoolMap.put(audioId, soundPool.load(filePath, 1));
        } catch (Exception e) {
            Logger.e(TAG, "putAudio(" + audioId + ", " + fileName + ", " + is + ")", e);
        }
    }

    public void removeAudio(int audioId) {
        try {
            soundPool.unload(audioId);
            soundPoolMap.remove(audioId);
        } catch (Exception e) {
            Logger.e(TAG, "removeAudio(" + audioId + ")", e);
        }
    }

    public void stopSound() {
        if (A.getMain() != null) {
            Intent intent = new Intent(A.getMain(), AudioPlayService.class);
            A.getMain().stopService(intent);
        }
    }
}
