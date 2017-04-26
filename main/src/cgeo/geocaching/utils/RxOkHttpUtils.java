package cgeo.geocaching.utils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
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
     * @param client the client to use for this request
     * @param request the request
     * @return a Single containing the response or an IOException
     */
    public static Single<Response> request(final OkHttpClient client, final Request request) {
        return Single.create(new SingleOnSubscribe<Response>() {
            @Override
            public void subscribe(final SingleEmitter<Response> singleEmitter) throws Exception {
                final Call call = client.newCall(request);
                final AtomicBoolean completed = new AtomicBoolean(false);
                singleEmitter.setDisposable(Disposables.fromRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (!completed.getAndSet(true)) {
                            // Cancel request on a non-UI thread as operations such as close()
                            // may trigger strict-mode errors even though they are executed
                            // very quickly and without blocking.
                            Schedulers.io().scheduleDirect(new Runnable() {
                                @Override
                                public void run() {
                                    call.cancel();
                                }
                            });
                        }
                    }
                }));
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(final Call call, final IOException e) {
                        completed.set(true);
                        singleEmitter.onError(e);
                    }

                    @Override
                    public void onResponse(final Call call, final Response response) throws IOException {
                        completed.set(true);
                        singleEmitter.onSuccess(response);
                    }
                });
            }
        });
    }

}
