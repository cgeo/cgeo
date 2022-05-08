package cgeo.geocaching.utils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RxOkHttpUtils {

    private RxOkHttpUtils() {
        // Do not instantiate
    }

    /**
     * Create a Single for running a cancellable request.
     *
     * @param client  the client to use for this request
     * @param request the request
     * @return a Single containing the response or an IOException
     */
    public static Single<Response> request(final OkHttpClient client, final Request request) {
        return Single.create(singleEmitter -> {
            final Call call = client.newCall(request);
            final AtomicBoolean completed = new AtomicBoolean(false);
            singleEmitter.setDisposable(Disposable.fromRunnable(() -> {
                if (!completed.get()) {
                    call.cancel();
                }
            }));
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull final Call call, @NonNull final IOException e) {
                    completed.set(true);
                    singleEmitter.onError(e);
                }

                @Override
                public void onResponse(@NonNull final Call call, @NonNull final Response response) throws IOException {
                    completed.set(true);
                    singleEmitter.onSuccess(response);
                }
            });
        });
    }

}
