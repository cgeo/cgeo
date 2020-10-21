package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

public abstract class AbstractMapSource implements MapSource {

    private final String name;
    @NonNull
    private final MapProvider mapProvider;
    private final String id;

    protected AbstractMapSource(final String id, @NonNull final MapProvider mapProvider, final String name) {
        this.id = id;
        this.mapProvider = mapProvider;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    @NonNull
    public String toString() {
        // needed for adapter in selection lists
        return getName();
    }

    @Override
    public int getNumericalId() {
        return id.hashCode();
    }

    @Override
    @NonNull
    public MapProvider getMapProvider() {
        return mapProvider;
    }

    @Override
    public void setMapAttributionTo(final TextView textView) {
        if (textView == null) {
            return;
        }

        final CharSequence mapAttribution = getMapAttribution(textView.getContext());
        if (mapAttribution == null) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
            textView.setText(mapAttribution);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    protected CharSequence getMapAttribution(final Context ctx) {
        return null;
    }
}
