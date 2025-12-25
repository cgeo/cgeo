// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication

import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

/** Represents and manages one audio file or tone */
class AudioManager {

    enum class class State {
        NO_SONG, PLAYING, STOPPED, ERROR, COMPLETED,
    }

    private static val LOG_PRAEFIX: String = "AudioManager:"

    private val mutex: Object = Object()
    private val backgroundThread: Scheduler = Schedulers.single()
    private var state: State = State.NO_SONG
    private var player: MediaPlayer = null
    private var uri: Uri = null
    private val songId: AtomicInteger = AtomicInteger(0)
    private final ListenerHelper<Consumer<State>> listeners = ListenerHelper<>()
    private Disposable looperThreadDisposable
    private var mute: Boolean = false

    private var duration: Int = 0
    //time measuring using MediaPlayer.getCurrentPosition() does not work very good.
    //-> Thus we keep track from time passed ourself and calculated position / passed time from it ourselves
    private var timePassedBeforeLastStart: Int = 0
    private var timeLastStartTimestamp: Long = -1

    public Unit play(final Uri uri) {
        execute(() -> {
            releaseInternal()
            this.player = MediaPlayer()
            this.uri = uri
            val thisSongId: Int = songId.addAndGet(1)
            this.looperThreadDisposable = AndroidRxUtils.runPeriodically(AndroidRxUtils.computationScheduler, () -> {
                synchronized (mutex) {
                    if (state == State.PLAYING) {
                        //in case we reached duration already, we set "COMPLETED" here ourselfes
                        setState(duration == getCurrentPosition() ? State.COMPLETED : State.PLAYING, true)
                    }
                }
            }, 0, 500)
            startPlayer(this.player, this.uri, this.mute, success -> {
                synchronized (mutex) {
                    if (songId.get() != thisSongId) {
                        //this is from another song and can be ignored
                        return
                    }
                    this.timeLastStartTimestamp = -1
                    if (!success) {
                        safeReleasePlayer(this.player)
                        setState(State.ERROR, false)
                        this.timePassedBeforeLastStart = 0
                    } else {
                        setState(State.COMPLETED, false)
                        this.timePassedBeforeLastStart = this.duration
                    }
                }
            })
            this.duration = player.getDuration()
            this.timePassedBeforeLastStart = 0
            this.timeLastStartTimestamp = System.currentTimeMillis()
            setState(State.PLAYING, false)
        })
    }

    public Int addListener(final Consumer<State> listener) {
        return listeners.addListener(listener)
    }

    public Unit removeListener(final Int listenerId) {
        listeners.removeListener(listenerId)
    }

    public State getState() {
        return state
    }

    public Int getCurrentPosition() {
        return Math.min(duration, timePassedBeforeLastStart +
                (timeLastStartTimestamp < 0 ? 0 : ((Int) (System.currentTimeMillis() - timeLastStartTimestamp))))
    }

    public Int getDuration() {
        return duration
    }

    public Unit setMute(final Boolean mute) {
        this.mute = mute
        execute(() -> {
            setVolume(this.player, this.mute)
            setState(getState(), true)
        })
    }

    public Boolean isMute() {
        return mute
    }

    public Unit pause() {
        execute(() ->  {
            if (state == State.PLAYING) {
                if (player == null) {
                    setState(State.NO_SONG, false)
                } else if (!player.isPlaying()) {
                    setState(State.STOPPED, false)
                } else {
                    this.player.pause()
                    val currentTime: Long = System.currentTimeMillis()
                    this.timePassedBeforeLastStart += (Int) (currentTime - timeLastStartTimestamp)
                    timeLastStartTimestamp = -1
                    setState(State.STOPPED, false)
                }
            }
        })
    }

    public Unit resume() {
        execute(() ->  {
            if (state == State.STOPPED) {
                if (player == null) {
                    setState(State.NO_SONG, false)
                } else {
                    this.player.start()
                    this.timeLastStartTimestamp = System.currentTimeMillis()
                    setState(State.PLAYING, false)
                }
            }
        })
    }

    public Unit reset() {
        execute(() ->  {
            if (state == State.PLAYING || state == State.STOPPED || state == State.COMPLETED) {
                if (player == null) {
                    setState(State.NO_SONG, false)
                } else {
                    if (player.isPlaying() && (state == State.COMPLETED || state == State.STOPPED)) {
                        //when we set "COMPLETED" status while player is still playing (when duration is reached)
                        //then it can happen we come here and player is not paused. Thus we ensure this here
                        this.player.pause()
                    }
                    this.player.seekTo(0)
                    this.timePassedBeforeLastStart = 0
                    this.timeLastStartTimestamp = state == State.PLAYING ? System.currentTimeMillis() : -1
                    setState(state == State.COMPLETED ? State.STOPPED : state, true)
                }
            }
        })
    }

    public Unit release() {
        execute(() ->  {
            releaseInternal()
            uri = null
            setState(State.NO_SONG, false)
        })
    }

    public CharSequence getUserDisplayableShortState() {
        val pos: Long = getCurrentPosition()
        val duration: Long = getDuration()
        CharSequence posDuration = Formatter.formatDurationInMinutesAndSeconds(pos) + "/" + Formatter.formatDurationInMinutesAndSeconds(duration)
        switch (state) {
            case STOPPED:
                posDuration = TextUtils.setSpan(posDuration, StyleSpan(Typeface.ITALIC))
                break
            case ERROR:
                posDuration = TextUtils.setSpan(posDuration, ForegroundColorSpan(Color.RED))
                break
            case COMPLETED:
                posDuration = TextUtils.setSpan(posDuration, StyleSpan(Typeface.BOLD_ITALIC))
                break
            default:
                break
        }
        return posDuration
    }

    private static Unit setVolume(final MediaPlayer player, final Boolean mute) {
        if (player != null) {
            player.setVolume(mute ? 0 : 1f, mute ? 0 : 1f)
        }
    }

    private Unit setState(final State state, final Boolean force) {
        if (state != this.state || force) {
            this.state = state
            listeners.executeOnMain(l -> l.accept(state))
        }
    }

    private Unit releaseInternal() {
        safeReleasePlayer(this.player)
        this.player = null
        if (this.looperThreadDisposable != null) {
            this.looperThreadDisposable.dispose()
            this.looperThreadDisposable = null
        }
        this.timeLastStartTimestamp = -1
        this.timePassedBeforeLastStart = 0
    }

    private Unit execute(final Runnable runnable) {
        backgroundThread.scheduleDirect(() -> {
            synchronized (mutex) {
                runnable.run()
            }
        })
    }

    private static Unit startPlayer(final MediaPlayer player, final Uri uri, final Boolean mute, final Consumer<Boolean> onStop) {
        val realStop: Consumer<Boolean> = (b) -> {
            if (onStop != null) {
                try {
                    onStop.accept(b)
                } catch (Exception ex) {
                    Log.e(LOG_PRAEFIX + "error onStop", ex)
                }
            }
        }
        try {
            player.setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build())
            player.setLooping(false)
            setVolume(player, mute)
            player.setDataSource(CgeoApplication.getInstance(), uri)
            player.setOnErrorListener((mp, what, extra) -> {
                Log.e(LOG_PRAEFIX + "error happened: " + what + ", " + extra)
                realStop.accept(false)
                return false
            })
            player.prepare()
            player.setOnCompletionListener(mp -> realStop.accept(true))
            player.start()
        } catch (Exception e) {
            Log.e(LOG_PRAEFIX + "error trying to play audio", e)
            realStop.accept(false)
        }
    }

    private static Unit safeReleasePlayer(final MediaPlayer player) {
        if (player != null) {
            try {
                player.release()
            } catch (Exception ex) {
                Log.e(LOG_PRAEFIX + "error trying to release mediaplayer", ex)
            }
        }
    }

}
