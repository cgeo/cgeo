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

package cgeo.geocaching.about

import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.SystemInformation

import android.app.Application

import androidx.annotation.NonNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * Stores system information for the about page.
 */
class SystemInformationViewModel : AndroidViewModel() {

    private final LiveData<String> systemInformation

    public SystemInformationViewModel(final Application application) {
        super(application)

        systemInformation = LiveDataReactiveStreams.fromPublisher(
            Flowable.fromCallable(() -> SystemInformation.getSystemInformation(application))
                    .subscribeOn(Schedulers.io())
                    .onErrorReturn(throwable -> {
                        Log.e("Could not load system information", throwable)
                        return null
                    })
        )
    }

    public LiveData<String> getSystemInformation() {
        return systemInformation
    }
}
