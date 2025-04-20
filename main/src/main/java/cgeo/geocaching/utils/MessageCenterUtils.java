package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.functions.Action1;
import static cgeo.geocaching.Intents.ACTION_MESSAGE_CENTER_UPDATE;
import static cgeo.geocaching.Intents.EXTRA_MESSAGE_CENTER_COUNTER;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Response;

public class MessageCenterUtils {

    private static long lastMCTime = 0L;
    private static int lastCount = 0;

    private MessageCenterUtils() {
        // utility class
    }

    /** configures polling of message center - to be called by CGeoApplication */
    public static void configureMessageCenterPolling() {
        final Observable<JsonNode> pollingObservable = Observable.interval(10, 300, TimeUnit.SECONDS)
                .flatMap(tick -> {
                    if (Settings.getBoolean(R.string.pref_pollMessageCenter, false)) {
                        final JsonNode temp = getMessageCenterStatus();
                        return temp != null ? Observable.just(temp) : Observable.empty();
                    } else {
                        return Observable.empty();
                    }
                });

        final Disposable mcpoll = pollingObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe(data -> {
                    @SuppressLint("SimpleDateFormat")
                    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    final long time = Objects.requireNonNull(df.parse(data.at("/lastConversationActivityDateUtc").textValue())).getTime();
                    lastCount = Integer.parseInt(data.at("/unreadConversationCount").toString());
                    if (time != lastMCTime) {
                        lastMCTime = time;
                        Log.d("received message center update, count=" + lastCount);
                        notifyIfMessagesPending();
                    }
                }, throwable -> Log.e("Error occurred while polling message center: " + throwable.getMessage()));
    }

    public static void notifyIfMessagesPending() {
        Log.d("message center notify check, count=" + lastCount);
        if (lastCount > 0) {
            LocalBroadcastManager.getInstance(CgeoApplication.getInstance()).sendBroadcast(new Intent(ACTION_MESSAGE_CENTER_UPDATE).putExtra(EXTRA_MESSAGE_CENTER_COUNTER, lastCount));
        }
    }

    public static void setReceiver(@NonNull final AppCompatActivity activity, @NonNull final Action1<Intent> onReceive) {
        activity.getLifecycle().addObserver(new MessageCenterUpdateReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                onReceive.call(intent);
            }

            @Override
            public void onCreate(@NonNull final LifecycleOwner owner) {
                super.onCreate(owner);
                notifyIfMessagesPending();
            }
        });
    }

    @Nullable
    private static JsonNode getMessageCenterStatus() {
        if (!GCConnector.getInstance().isLoggedIn()) {
            return null;
        }
        final Response response = Network.getRequest("https://www.geocaching.com/api/communication-service/participant/" + GCLogin.getInstance().getPublicGuid() + "/summary/", new Parameters()).blockingGet();
        if (!response.isSuccessful()) {
            return null;
        }
        final String jsonString = Network.getResponseData(response);
        try {
            return JsonUtils.reader.readTree(jsonString);
        } catch (Exception ignore) {
            return null;
        }
    }

    private abstract static class MessageCenterUpdateReceiver extends BroadcastReceiver implements DefaultLifecycleObserver {
        @Override
        public void onCreate(@NonNull final LifecycleOwner owner) {
            LocalBroadcastManager.getInstance(CgeoApplication.getInstance()).registerReceiver(this, new IntentFilter(ACTION_MESSAGE_CENTER_UPDATE));
        }

        @Override
        public void onDestroy(@NonNull final LifecycleOwner owner) {
            LocalBroadcastManager.getInstance(CgeoApplication.getInstance()).unregisterReceiver(this);
        }
    }

}
