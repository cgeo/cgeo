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
import android.media.MediaPlayer;
import android.net.Uri;

import menion.android.whereyougo.utils.Logger;

public class AudioClip {

    private static final String TAG = "AudioClip";
    private MediaPlayer mPlayer;
    private String name;
    private boolean mPlaying = false;
    private boolean mLoop = false;
    private int playCount = 0;
    private AudioListener listener;

    public AudioClip(Context ctx, int resID) {
        try {
            name = ctx.getResources().getResourceName(resID);
        } catch (Exception e) {
            // name is used only for debug purposes
        }
        mPlayer = MediaPlayer.create(ctx, resID);
        initMediaPlayer();
    }

    public AudioClip(Context ctx, Uri soundUri) {
        mPlayer = MediaPlayer.create(ctx, soundUri);
        initMediaPlayer();
    }

    public static void destroyAudio(AudioClip mAudio) {
        try {
            if (mAudio != null) {
                mAudio.stop();
                mAudio.release();
            }
        } catch (Exception e) {
            Logger.e(TAG, "destroyAudio()", e);
        }
    }

    private void initMediaPlayer() {
        mPlayer.setVolume(1000, 1000);
        mPlayer.setOnCompletionListener(mp -> {
            mPlaying = false;
            if (mLoop) {
                System.out.println("AudioClip loop " + name);
                mp.start();
            } else if (playCount > 0) {
                playCount--;
                mp.start();
            } else {
                // really finished
                if (listener != null)
                    listener.playCompleted();
            }
        });
    }

    public synchronized void loop() {
        mLoop = true;
        mPlaying = true;
        mPlayer.start();
    }

    public synchronized void play() {
        if (mPlaying)
            return;

        if (mPlayer != null) {
            mPlaying = true;
            mPlayer.start();
        }
    }

    public synchronized void play(int count) {
        if (mPlaying)
            return;

        this.playCount = count - 1;
        if (mPlayer != null) {
            mPlaying = true;
            mPlayer.start();
        }
    }

    public void registerListener(AudioListener listener) {
        this.listener = listener;
    }

    public void release() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    public synchronized void stop() {
        try {
            mLoop = false;
            if (mPlaying) {
                mPlaying = false;
                mPlayer.pause();
            }
        } catch (Exception e) {
            System.err.println("AudioClip::stop " + name + " " + e.toString());
        }
    }

    public interface AudioListener {
        void playCompleted();
    }
}
