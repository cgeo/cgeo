package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.IConversion;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ContinuousRangeSlider;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.NewCoordinateInputDialog;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class DistanceFilterViewHolder extends BaseFilterViewHolder<DistanceGeocacheFilter> {


    private final int maxDistance = Settings.useImperialUnits() ? Math.round(500f / IConversion.MILES_TO_KILOMETER) : 500;
    private final float conversion = Settings.useImperialUnits() ? IConversion.MILES_TO_KILOMETER : 1f;

    private ContinuousRangeSlider slider;
    private CheckBox useCurrentPosition;
    private Geopoint location;
    private Button setCoordsButton;

    @Override
    public View createView() {

        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        useCurrentPosition = ViewUtils.addCheckboxItem(getActivity(), ll, TextParam.id(R.string.cache_filter_distance_use_current_position), R.drawable.ic_menu_mylocation, null);
        useCurrentPosition.setChecked(true);
        useCurrentPosition.setOnClickListener(v -> toggleCurrent());
        location = LocationDataProvider.getInstance().currentGeo().getCoords();

        setCoordsButton = ViewUtils.createButton(getActivity(), ll, TextParam.id(R.string.cache_filter_distance_coordinates), R.layout.button_coordinate_view);
        setCoordsButton.setEnabled(false);
        final ViewGroup.LayoutParams ll1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setCoordsButton.setLayoutParams(ll1);
        ViewUtils.setCoordinates(location, setCoordsButton);
        setCoordsButton.setOnClickListener(v -> setCoordinates());
        ll.addView(setCoordsButton);

        slider = new ContinuousRangeSlider(getActivity());
        slider.setScale(-0.2f, maxDistance + 0.2f, f -> {
            if (f <= 0) {
                return "0";
            }
            if (f > maxDistance) {
                return ">" + maxDistance;
            }
            return "" + Math.round(f);
        }, 6, 1);
        slider.setRange(-0.2f, maxDistance + 0.2f);

        final LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(20));
        ll.addView(slider, llp);

        return ll;
    }

    @Override
    public void setViewFromFilter(final DistanceGeocacheFilter filter) {
        useCurrentPosition.setChecked(filter.isUseCurrentPosition());
        if (filter.getCoordinate() != null) {
            location = filter.getCoordinate();
        }
        setCoordsButton.setEnabled(!filter.isUseCurrentPosition());
        ViewUtils.setCoordinates(location, setCoordsButton);
        slider.setRange(
                filter.getMinRangeValue() == null ? -10f : filter.getMinRangeValue() / conversion,
                filter.getMaxRangeValue() == null ? maxDistance + 500f : filter.getMaxRangeValue() / conversion);
    }

    @Override
    public DistanceGeocacheFilter createFilterFromView() {
        final DistanceGeocacheFilter filter = createFilter();
        filter.setUseCurrentPosition(useCurrentPosition.isChecked());
        filter.setCoordinate(location);
        final ImmutablePair<Float, Float> range = slider.getRange();
        filter.setMinMaxRange(range.left, range.right , 0f, (float) maxDistance, value -> (float) Math.round(value * conversion));
        return filter;
    }

    private void toggleCurrent() {
        if (useCurrentPosition.isChecked()) {
            location = LocationDataProvider.getInstance().currentGeo().getCoords();
            ViewUtils.setCoordinates(location, setCoordsButton);
            setCoordsButton.setEnabled(false);
        } else {
            setCoordsButton.setEnabled(true);
        }
    }

    private void setCoordinates() {
        final NewCoordinateInputDialog dialog = new NewCoordinateInputDialog(getActivity(), this::onDialogClosed);
        dialog.show(location);
    }

    public void onDialogClosed(final Geopoint input) {
        // Handle the data from the dialog
        if (input.isValid()) {
            this.location = input;
            ViewUtils.setCoordinates(location, setCoordsButton);
        }
    }
}
