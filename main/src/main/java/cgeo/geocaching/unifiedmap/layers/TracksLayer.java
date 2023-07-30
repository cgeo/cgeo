package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.maps.Tracks;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;

import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class TracksLayer {

    final UnifiedMapViewModel viewModel;

    public TracksLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);

        viewModel.trackUpdater.observe(activity, event -> event.ifNotHandled((key -> {
            final Tracks.Track track = viewModel.getTracks().getTrack(key);
            if (track == null || track.getRoute().isHidden()) {
                layer.remove(key);
            } else {

                //Apply current chosen default color to all elements and display

                final float widthFactor = 2f;
                final float defaultWidth = track.getTrackfile().getWidth() / widthFactor;
                final int defaultStrokeColor = track.getTrackfile().getColor();
                final int defaultFillColor = Color.argb(128, Color.red(defaultStrokeColor), Color.green(defaultStrokeColor), Color.blue(defaultStrokeColor));
                final GeoStyle defaultStyle = GeoStyle.builder()
                        .setFillColor(defaultFillColor)
                        .setStrokeColor(defaultStrokeColor)
                        .setStrokeWidth(defaultWidth).build();

                layer.put(key, track.getRoute().getItem().applyDefaultStyle(defaultStyle));
             }
        })));

    }

}
