package cgeo.geocaching.maps.mapsforge.v6;

import org.apache.commons.lang3.StringUtils;

import android.view.View;
import android.widget.TextView;

public class TargetView {

    private final TextView targetView;

    public TargetView(final TextView targetView, final String geocode, final String name) {
        this.targetView = targetView;

        setTarget(geocode, name);
    }

    public void setTarget(final String geocode, final String name) {
        if (StringUtils.isNotEmpty(geocode)) {
            targetView.setText(String.format("%s: %s", geocode, name));
            targetView.setVisibility(View.VISIBLE);
        } else {
            targetView.setVisibility(View.GONE);
        }
    }
}
