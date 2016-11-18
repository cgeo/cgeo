package cgeo.geocaching.utils;

import java.io.IOException;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
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
                // A disposible should be set on singleEmitter to cancel the request. However,
                // canceling seems to kill the application because of an IOException not caught
                // at the thread top-level.
                // See https://github.com/cgeo/cgeo/issues/6114 for more details.
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(final Call call, final IOException e) {
                        singleEmitter.onError(e);
                    }

                    @Override
                    public void onResponse(final Call call, final Response response) throws IOException {
                        singleEmitter.onSuccess(response);
                    }
                });
            }
        });
    }

}
