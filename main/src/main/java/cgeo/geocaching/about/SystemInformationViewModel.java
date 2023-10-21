package cgeo.geocaching.about;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SystemInformation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Stores system information for the about page.
 */
public class SystemInformationViewModel extends AndroidViewModel {

    private final LiveData<String> systemInformation;

    public SystemInformationViewModel(@NonNull final Application application) {
        super(application);

        systemInformation = LiveDataReactiveStreams.fromPublisher(
            Flowable.fromCallable(() -> SystemInformation.getSystemInformation(application))
                    .subscribeOn(Schedulers.io())
                    .onErrorReturn(throwable -> {
                        Log.e("Could not load system information", throwable);
                        return null;
                    })
        );
    }

    @NonNull
    public LiveData<String> getSystemInformation() {
        return systemInformation;
    }
}
