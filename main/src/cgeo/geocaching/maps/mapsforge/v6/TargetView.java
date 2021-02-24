package cgeo.geocaching.maps.mapsforge.v6;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class TargetView {

    private final TextView targetView;
    private final TextView targetViewSupersize;

    public TargetView(@NonNull final TextView targetView, @NonNull final TextView targetViewSupersize, @Nullable final String geocode, @Nullable final String name) {
        this.targetView = targetView;
        this.targetViewSupersize = targetViewSupersize;

        setTarget(geocode, name);
    }

    public void setTarget(@Nullable final String geocode, @Nullable final String name) {
        if (StringUtils.isNotEmpty(geocode)) {
            targetView.setText(String.format("%s: %s", geocode, StringUtils.isNotEmpty(name) ? name : StringUtils.EMPTY));
            targetView.setVisibility(View.VISIBLE);
        } else {
            targetView.setText("");
            targetView.setVisibility(View.GONE);
            targetViewSupersize.setVisibility(View.GONE);
        }
    }
}
