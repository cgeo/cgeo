package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/** Represents and manages one audio file or tone */
public class AudioManager {

    public enum State {
        NO_SONG, PLAYING, STOPPED, ERROR, COMPLETED,
    }

    private static final String LOG_PRAEFIX = "AudioManager:";

    private final Object mutex = new Object();
    private final Scheduler backgroundThread = Schedulers.single();
    private State state = State.NO_SONG;
    private MediaPlayer player = null;
    private Uri uri = null;
    private final AtomicInteger songId = new AtomicInteger(0);
    private final ListenerHelper<Consumer<State>> listeners = new ListenerHelper<>();
    private Disposable looperThreadDisposable;
    private boolean mute = false;

    private int duration = 0;
    //time measuring using MediaPlayer.getCurrentPosition() does not work very good.
    //-> Thus we keep track from time passed ourself and calculated position / passed time from it ourselves
    private int timePassedBeforeLastStart = 0;
    private long timeLastStartTimestamp = -1;

    public void play(final Uri uri) {
        execute(() -> {
            releaseInternal();
            this.player = new MediaPlayer();
            this.uri = uri;
            final int thisSongId = songId.addAndGet(1);
            this.looperThreadDisposable = AndroidRxUtils.runPeriodically(AndroidRxUtils.computationScheduler, () -> {
                synchronized (mutex) {
                    if (state == State.PLAYING) {
                        //in case we reached duration already, we set "COMPLETED" here ourselfes
                        setState(duration == getCurrentPosition() ? State.COMPLETED : State.PLAYING, true);
                    }
                }
            }, 0, 500);
            startPlayer(this.player, this.uri, this.mute, success -> {
                synchronized (mutex) {
                    if (songId.get() != thisSongId) {
                        //this is from another song and can be ignored
                        return;
                    }
                    this.timeLastStartTimestamp = -1;
                    if (!success) {
                        safeReleasePlayer(this.player);
                        setState(State.ERROR, false);
                        this.timePassedBeforeLastStart = 0;
                    } else {
                        setState(State.COMPLETED, false);
                        this.timePassedBeforeLastStart = this.duration;
                    }
                }
            });
            this.duration = player.getDuration();
            this.timePassedBeforeLastStart = 0;
            this.timeLastStartTimestamp = System.currentTimeMillis();
            setState(State.PLAYING, false);
        });
    }

    public int addListener(final Consumer<State> listener) {
        return listeners.addListener(listener);
    }

    public void removeListener(final int listenerId) {
        listeners.removeListener(listenerId);
    }

    public State getState() {
        return state;
    }

    public int getCurrentPosition() {
        return Math.min(duration, timePassedBeforeLastStart +
                (timeLastStartTimestamp < 0 ? 0 : ((int) (System.currentTimeMillis() - timeLastStartTimestamp))));
    }

    public int getDuration() {
        return duration;
    }

    public void setMute(final boolean mute) {
        this.mute = mute;
        execute(() -> {
            setVolume(this.player, this.mute);
            setState(getState(), true);
        });
    }

    public boolean isMute() {
        return mute;
    }

    public void pause() {
        execute(() ->  {
            if (state == State.PLAYING) {
                if (player == null) {
                    setState(State.NO_SONG, false);
                } else if (!player.isPlaying()) {
                    setState(State.STOPPED, false);
                } else {
                    this.player.pause();
                    final long currentTime = System.currentTimeMillis();
                    this.timePassedBeforeLastStart += (int) (currentTime - timeLastStartTimestamp);
                    timeLastStartTimestamp = -1;
                    setState(State.STOPPED, false);
                }
            }
        });
    }

    public void resume() {
        execute(() ->  {
            if (state == State.STOPPED) {
                if (player == null) {
                    setState(State.NO_SONG, false);
                } else {
                    this.player.start();
                    this.timeLastStartTimestamp = System.currentTimeMillis();
                    setState(State.PLAYING, false);
                }
            }
        });
    }

    public void reset() {
        execute(() ->  {
            if (state == State.PLAYING || state == State.STOPPED || state == State.COMPLETED) {
                if (player == null) {
                    setState(State.NO_SONG, false);
                } else {
                    if (player.isPlaying() && (state == State.COMPLETED || state == State.STOPPED)) {
                        //when we set "COMPLETED" status while player is still playing (when duration is reached)
                        //then it can happen we come here and player is not paused. Thus we ensure this here
                        this.player.pause();
                    }
                    this.player.seekTo(0);
                    this.timePassedBeforeLastStart = 0;
                    this.timeLastStartTimestamp = state == State.PLAYING ? System.currentTimeMillis() : -1;
                    setState(state == State.COMPLETED ? State.STOPPED : state, true);
                }
            }
        });
    }

    public void release() {
        execute(() ->  {
            releaseInternal();
            uri = null;
            setState(State.NO_SONG, false);
        });
    }

    public CharSequence getUserDisplayableShortState() {
        final long pos = getCurrentPosition();
        final long duration = getDuration();
        CharSequence posDuration = Formatter.formatDurationInMinutesAndSeconds(pos) + "/" + Formatter.formatDurationInMinutesAndSeconds(duration);
        switch (state) {
            case STOPPED:
                posDuration = TextUtils.setSpan(posDuration, new StyleSpan(Typeface.ITALIC));
                break;
            case ERROR:
                posDuration = TextUtils.setSpan(posDuration, new ForegroundColorSpan(Color.RED));
                break;
            case COMPLETED:
                posDuration = TextUtils.setSpan(posDuration, new StyleSpan(Typeface.BOLD_ITALIC));
                break;
            default:
                break;
        }
        return posDuration;
    }

    private static void setVolume(final MediaPlayer player, final boolean mute) {
        if (player != null) {
            player.setVolume(mute ? 0 : 1f, mute ? 0 : 1f);
        }
    }

    private void setState(final State state, final boolean force) {
        if (state != this.state || force) {
            this.state = state;
            listeners.executeOnMain(l -> l.accept(state));
        }
    }

    private void releaseInternal() {
        safeReleasePlayer(this.player);
        this.player = null;
        if (this.looperThreadDisposable != null) {
            this.looperThreadDisposable.dispose();
            this.looperThreadDisposable = null;
        }
        this.timeLastStartTimestamp = -1;
        this.timePassedBeforeLastStart = 0;
    }

    private void execute(final Runnable runnable) {
        backgroundThread.scheduleDirect(() -> {
            synchronized (mutex) {
                runnable.run();
            }
        });
    }

    private static void startPlayer(final MediaPlayer player, final Uri uri, final boolean mute, final Consumer<Boolean> onStop) {
        final  Consumer<Boolean> realStop = (b) -> {
            if (onStop != null) {
                try {
                    onStop.accept(b);
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
            setVolume(player, mute);
            player.setDataSource(CgeoApplication.getInstance(), uri);
            player.setOnErrorListener((mp, what, extra) -> {
                Log.e(LOG_PRAEFIX + "error happened: " + what + ", " + extra);
                realStop.accept(false);
                return false;
            });
            player.prepare();
            player.setOnCompletionListener(mp -> realStop.accept(true));
            player.start();
        } catch (Exception e) {
            Log.e(LOG_PRAEFIX + "error trying to play audio", e);
            realStop.accept(false);
        }
    }

    private static void safeReleasePlayer(final MediaPlayer player) {
        if (player != null) {
            try {
                player.release();
            } catch (Exception ex) {
                Log.e(LOG_PRAEFIX + "error trying to release mediaplayer", ex);
            }
        }
    }

}
