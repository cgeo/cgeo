package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.address.AndroidGeocoder;
import cgeo.geocaching.databinding.LocationStatusViewBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.permission.PermissionContext;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GnssStatusProvider;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.GeoHeightUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Address;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Single;
import org.apache.commons.lang3.StringUtils;

/** Displays location information */
public class LocationStatusView extends LinearLayout {

    private Context context;
    private Activity activity;

    private LocationStatusViewBinding binding;
    private Geopoint currentCoords;
    private Geopoint currentAddressCoords = null;

    private Runnable permissionRequestCallback;

    private boolean showAddress = false;

    @Override
    protected void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    public LocationStatusView(final Context context) {
        super(context);
        init();
    }

    public LocationStatusView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LocationStatusView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        this.context = new ContextThemeWrapper(getContext(), R.style.cgeo);
        this.activity = ViewUtils.toActivity(this.context);

        inflate(this.context, R.layout.location_status_view, this);
        binding = LocationStatusViewBinding.bind(this);

        setOnClickListener(c -> {
            if (permissionRequestCallback != null && !PermissionContext.LOCATION.hasAllPermissions()) {
                permissionRequestCallback.run();
            } else {
                openLocationSettings();
            }
        });

        setOnLongClickListener(v -> {
            ClipboardUtils.copyToClipboard(GeopointFormatter.reformatForClipboard(currentCoords.toString()));
            ActivityMixin.showToast(context, R.string.loc_copied_clipboard);
            return true;
        });

        updateGeoData(null);
    }

    public void setShowAddress(final boolean showAddress) {
        this.showAddress = showAddress;
    }

    @SuppressLint("SetTextI18n")
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    public void updateGeoData(@Nullable final GeoData geo) {

        this.currentCoords = geo == null ? null : geo.getCoords();

        binding.locationType.setText(geo == null ? "" : LocalizationUtils.getString(geo.getLocationProvider().resourceId));

        if (geo != null && geo.getAccuracy() >= 0) {
            final int speed = Math.round(geo.getSpeed()) * 60 * 60 / 1000;
            binding.locationAccuracy.setText("±" + Units.getDistanceFromMeters(geo.getAccuracy()) + Formatter.SEPARATOR + Units.getSpeed(speed));
        } else {
            binding.locationAccuracy.setText(null);
        }

        final String averageHeight = geo == null ? "" : GeoHeightUtils.getAverageHeight(geo, true);
        if (currentCoords != null && showAddress && activity != null) {
            if (currentAddressCoords == null) {
                binding.locationText.setText(R.string.loc_no_addr);
            }
            if (currentAddressCoords == null || currentCoords.distanceTo(currentAddressCoords) > 0.5) {
                currentAddressCoords = currentCoords;
                final Single<String> address = (new AndroidGeocoder(context).getFromLocation(currentCoords)).map(LocationStatusView::formatAddress).onErrorResumeWith(Single.just(currentCoords.toString()));
                AndroidRxUtils.bindActivity(activity, address)
                        .subscribeOn(AndroidRxUtils.networkScheduler)
                        .subscribe(address12 -> binding.locationText.setText(address12 + averageHeight));
            }
        } else {
            binding.locationText.setText((currentCoords == null ? "" : currentCoords.toString()) + averageHeight);
        }

        updatePermissions();
    }

    @SuppressLint("SetTextI18n")
    public void updateSatelliteStatus(final GnssStatusProvider.Status gnssStatus) {
        if (gnssStatus.gnssEnabled) {
            binding.locationSatellites.setText(LocalizationUtils.getString(R.string.loc_sat) + ": " + gnssStatus.satellitesFixed + '/' + gnssStatus.satellitesVisible);
        } else {
            binding.locationSatellites.setText(LocalizationUtils.getString(R.string.loc_gps_disabled));
        }
    }

    public void updatePermissions() {
        final Set<String> notGranted = PermissionContext.LOCATION.getNotGrantedPermissions();
        binding.locationPermissionStatus.setVisibility(notGranted.isEmpty() ? View.GONE : View.VISIBLE);
        if (notGranted.size() >= PermissionContext.LOCATION.getPermissions().length) {
            binding.locationPermissionStatus.setText(R.string.location_no_permission);
        } else {
            binding.locationPermissionStatus.setText(R.string.location_only_coarse_permission);
        }
    }

    public void setPermissionRequestCallback(final Runnable callback) {
        this.permissionRequestCallback = callback;
    }

    private static String formatAddress(final Address address) {
        final List<String> addressParts = new ArrayList<>();

        final String countryName = address.getCountryName();
        if (countryName != null) {
            addressParts.add(countryName);
        }
        final String locality = address.getLocality();
        if (locality != null) {
            addressParts.add(locality);
        } else {
            final String adminArea = address.getAdminArea();
            if (adminArea != null) {
                addressParts.add(adminArea);
            }
        }
        return StringUtils.join(addressParts, ", ");
    }

    private void openLocationSettings() {
        if (activity != null) {
            activity.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }

}
