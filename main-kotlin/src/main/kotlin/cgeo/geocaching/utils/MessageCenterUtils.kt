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
import cgeo.geocaching.R
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.connector.gc.GCLogin
import cgeo.geocaching.network.Network
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.functions.Action1
import cgeo.geocaching.Intents.ACTION_MESSAGE_CENTER_UPDATE
import cgeo.geocaching.Intents.EXTRA_MESSAGE_CENTER_COUNTER

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Objects
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.JsonNode
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Response

class MessageCenterUtils {

    private static Long lastMCTime = 0L
    private static Int lastCount = 0

    private MessageCenterUtils() {
        // utility class
    }

    /** configures polling of message center - to be called by CGeoApplication */
    public static Unit configureMessageCenterPolling() {
        val pollingObservable: Observable<JsonNode> = Observable.interval(10, 300, TimeUnit.SECONDS)
                .flatMap(tick -> {
                    if (Settings.getBoolean(R.string.pref_pollMessageCenter, false)) {
                        val temp: JsonNode = getMessageCenterStatus()
                        return temp != null ? Observable.just(temp) : Observable.empty()
                    } else {
                        return Observable.empty()
                    }
                })

        val mcpoll: Disposable = pollingObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe(data -> {
                    @SuppressLint("SimpleDateFormat")
                    val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    val time: Long = Objects.requireNonNull(df.parse(data.at("/lastConversationActivityDateUtc").textValue())).getTime()
                    lastCount = Integer.parseInt(data.at("/unreadConversationCount").toString())
                    if (time != lastMCTime) {
                        lastMCTime = time
                        Log.d("received message center update, count=" + lastCount)
                        notifyIfMessagesPending()
                    }
                }, throwable -> Log.e("Error occurred while polling message center: " + throwable.getMessage()))
    }

    public static Unit notifyIfMessagesPending() {
        Log.d("message center notify check, count=" + lastCount)
        if (lastCount > 0) {
            LocalBroadcastManager.getInstance(CgeoApplication.getInstance()).sendBroadcast(Intent(ACTION_MESSAGE_CENTER_UPDATE).putExtra(EXTRA_MESSAGE_CENTER_COUNTER, lastCount))
        }
    }

    public static Unit setReceiver(final AppCompatActivity activity, final Action1<Intent> onReceive) {
        activity.getLifecycle().addObserver(MessageCenterUpdateReceiver() {
            override             public Unit onReceive(final Context context, final Intent intent) {
                onReceive.call(intent)
            }

            override             public Unit onCreate(final LifecycleOwner owner) {
                super.onCreate(owner)
                notifyIfMessagesPending()
            }
        })
    }

    private static JsonNode getMessageCenterStatus() {
        if (!GCConnector.getInstance().isLoggedIn()) {
            return null
        }
        val response: Response = Network.getRequest("https://www.geocaching.com/api/communication-service/participant/" + GCLogin.getInstance().getPublicGuid() + "/summary/", Parameters()).blockingGet()
        if (!response.isSuccessful()) {
            return null
        }
        val jsonString: String = Network.getResponseData(response)
        try {
            return JsonUtils.reader.readTree(jsonString)
        } catch (Exception ignore) {
            return null
        }
    }

    private abstract static class MessageCenterUpdateReceiver : BroadcastReceiver() : DefaultLifecycleObserver {
        override         public Unit onCreate(final LifecycleOwner owner) {
            LocalBroadcastManager.getInstance(CgeoApplication.getInstance()).registerReceiver(this, IntentFilter(ACTION_MESSAGE_CENTER_UPDATE))
        }

        override         public Unit onDestroy(final LifecycleOwner owner) {
            LocalBroadcastManager.getInstance(CgeoApplication.getInstance()).unregisterReceiver(this)
        }
    }

}
