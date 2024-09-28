package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import io.reactivex.rxjava3.disposables.Disposable;

/** Represents and manages one audio file or tone */
public class AudioClip implements Disposable {

    private static final String LOG_PRAEFIX = "AudioClip:";

    private MediaPlayer player = new MediaPlayer();
    private boolean isReleased = false;
    private Uri uri;

    private AudioClip() {
        //instances only via static creators
    }

    public static AudioClip play(final Uri audiofile) {
        final AudioClip clip = new AudioClip();
        clip.uri = audiofile;
        startPlayer(clip.player, clip.uri, clip::safeDestroy);
        return clip;
    }

    private static void startPlayer(final MediaPlayer player, final Uri uri, final Runnable onStop) {
        final Runnable realStop = () -> {
            if (onStop != null) {
                try {
                    onStop.run();
                } catch (Exception ex) {
                    Log.e(LOG_PRAEFIX + "error onStop", ex);
                }
            }
        };
        try {
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
            player.setLooping(false);
            player.setVolume(1f, 1f);
            player.setDataSource(CgeoApplication.getInstance(), uri);
            player.setOnErrorListener((mp, what, extra) -> {
                Log.e(LOG_PRAEFIX + "error happened: " + what + ", " + extra);
                return false;
            });
            player.prepare();
            player.start();
            player.setOnCompletionListener(mp -> realStop.run());
        } catch (Exception e) {
            Log.e(LOG_PRAEFIX + "error trying to play audio", e);
            realStop.run();
        }
    }

    private static void safeReleasePlayer(final MediaPlayer player) {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (Exception ex) {
                Log.w(LOG_PRAEFIX + "problem trying to stop player", ex);
            }
            try {
                player.reset();
            } catch (Exception ex) {
                Log.w(LOG_PRAEFIX + "problem trying to reset player", ex);
            }
            try {
                player.release();
            } catch (Exception ex) {
                Log.e(LOG_PRAEFIX + "error trying to release mediaplayer", ex);
            }
        }
    }

    private void safeDestroy() {
        if (!isReleased) {
            safeReleasePlayer(player);
        }
        isReleased = true;
        player = null;
        uri = null;
    }

    @Override
    public void dispose() {
        safeDestroy();
    }

    @Override
    public boolean isDisposed() {
        return isReleased;
    }
}
