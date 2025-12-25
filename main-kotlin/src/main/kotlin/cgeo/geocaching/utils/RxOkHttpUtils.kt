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

import androidx.annotation.NonNull

import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class RxOkHttpUtils {

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
            val call: Call = client.newCall(request)
            val completed: AtomicBoolean = AtomicBoolean(false)
            singleEmitter.setDisposable(Disposable.fromRunnable(() -> {
                if (!completed.get()) {
                    call.cancel()
                }
            }))
            call.enqueue(Callback() {
                override                 public Unit onFailure(final Call call, final IOException e) {
                    completed.set(true)
                    singleEmitter.onError(e)
                }

                override                 public Unit onResponse(final Call call, final Response response) {
                    completed.set(true)
                    singleEmitter.onSuccess(response)
                }
            })
        })
    }

}
