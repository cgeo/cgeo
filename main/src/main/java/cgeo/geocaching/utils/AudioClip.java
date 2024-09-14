package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.LocalStorage;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.File;

import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.commons.io.FileUtils;

/** Represents and manages one audio file or tone */
public class AudioClip implements Disposable {

    private static final String LOG_PRAEFIX = "AudioClip:";
    private static final AudioClip DISPOSED_CLIP = new AudioClip();


    private MediaPlayer player = new MediaPlayer();
    private boolean isReleased = false;
    private Uri uri;
    private boolean uriToDelete = false;

    private AudioClip() {
        //instances only via static creators
    }

    public static AudioClip play(final byte[] data) {
        final AudioClip clip = new AudioClip();
        final File newTempFile = new File(LocalStorage.getInternalCgeoCacheDirectory(), "audio-" + System.currentTimeMillis());
        try {
            FileUtils.writeByteArrayToFile(newTempFile, data);
        } catch (Exception e) {
            Log.e(LOG_PRAEFIX + "Problem extracting/storing audio data", e);
            return DISPOSED_CLIP;
        }
        clip.uri = Uri.fromFile(newTempFile);
        Log.iForce("AUDIO: Creating file: " + clip.uri);
        clip.uriToDelete = true;
        startPlayer(clip.player, clip.uri, clip::safeDestroy);
        return clip;
    }

    public static AudioClip play(final Uri audiofile) {
        final AudioClip clip = new AudioClip();
        clip.uri = audiofile;
        clip.uriToDelete = false;
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
        if (uriToDelete && uri != null) {
            ContentStorage.get().delete(uri);
            Log.iForce(LOG_PRAEFIX + "deleting file: " + uri);
        }
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
