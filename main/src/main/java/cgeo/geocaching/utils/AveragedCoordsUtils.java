package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.AveragingBoxBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.ui.NotifyOnTouchOutsideElement;
import cgeo.geocaching.utils.functions.Action1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.disposables.Disposable;

public class AveragedCoordsUtils extends GeoDirHandler {
    private final WeakReference<Context> activityRef;
    private final FrameLayout container;
    private final Action1<Geopoint> averagingCallback;
    private final Action1<Geopoint> resultCallback;
    final AtomicBoolean isAveraging = new AtomicBoolean(true);
    private long latitude;
    private long longitude;
    private long divisor;
    private int counter;
    final AveragingBoxBinding binding;

    public AveragedCoordsUtils(@NonNull final Context activity, @NonNull final NotifyOnTouchOutsideElement touchInterceptor, @NonNull final Geopoint initialCoords, @NonNull final FrameLayout container, @NonNull final Action1<Geopoint> averagingCallback, @NonNull final Action1<Geopoint> resultCallback) {
        this.activityRef = new WeakReference<>(activity);
        this.container = container;
        this.averagingCallback = averagingCallback;
        this.resultCallback = resultCallback;
        counter = 0;

        divisor = 1000;
        latitude = initialCoords.getLatitudeE6() * divisor;
        longitude = initialCoords.getLongitudeE6() * divisor;

        // create layout
        if (container.getChildCount() > 0) {
            container.removeAllViews();
        }
        container.setVisibility(View.VISIBLE);
        binding = AveragingBoxBinding.inflate(LayoutInflater.from(activity), container, true);

        // inject layout at given container
        binding.averagingCancel.setOnClickListener(v1 -> returnToCaller(false));
        binding.averagingPause.setOnClickListener(v2 -> toggleTo(!isAveraging.get()));
        binding.averagingOk.setOnClickListener(v3 -> returnToCaller(true));

        touchInterceptor.setExceptionElement(binding.averagingBox, () -> toggleTo(false));
    }

    private void toggleTo(final boolean newIsAveraging) {
        isAveraging.set(newIsAveraging);
        binding.averagingPause.setText(newIsAveraging ? R.string.pause : R.string.resume);
        setCounter();

    }

    private void setCounter() {
        if (isAveraging.get()) {
            binding.averagingInfo.setText(LocalizationUtils.getString(R.string.averaging_my_coordinates2, NumberFormat.getIntegerInstance().format(counter)));
        } else {
            binding.averagingInfo.setText(LocalizationUtils.getString(R.string.averaging_my_coordinates2, LocalizationUtils.getString(R.string.averaging_paused)));
        }
    }

    private void returnToCaller(final boolean success) {
        isAveraging.set(false);
        container.setVisibility(View.GONE);
        resultCallback.call(success ? getGeopoint() : null);
    }

    private Geopoint getGeopoint() {
        return Geopoint.forE6((int) (latitude / divisor), (int) (longitude / divisor));
    }

    @Override
    public Disposable start(final int flags, final long windowDuration, final TimeUnit unit) {
        return super.start(flags, windowDuration, unit);
    }

    @Override
    public void updateGeoData(final GeoData geoData) {
        if (!isAveraging.get()) {
            return;
        }
        final Context activity = activityRef.get();
        if (activity == null) {
            return;
        }

        long factor = 1000;
        final float accuracy = geoData.getAccuracy();
        if (accuracy > 0) {
            factor = (long) (1000f / accuracy);
            if (factor > 1000) {
                factor = 1000;
            }
        }

        divisor += factor;
        latitude += (long) (geoData.getLatitude() * 1e6) * factor;
        longitude += (long) (geoData.getLongitude() * 1e6) * factor;

        averagingCallback.call(getGeopoint());
        counter++;
        setCounter();
    }
}
