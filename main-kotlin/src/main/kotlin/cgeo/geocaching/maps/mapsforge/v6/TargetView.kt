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

package cgeo.geocaching.maps.mapsforge.v6

import android.view.View
import android.widget.TextView

import org.apache.commons.lang3.StringUtils

class TargetView {

    private final TextView targetView

    public TargetView(final TextView targetView, final String geocode, final String name) {
        this.targetView = targetView
        setTarget(geocode, name)
    }

    public Unit setTarget(final String geocode, final String name) {
        if (StringUtils.isNotEmpty(geocode)) {
            targetView.setText(String.format("%s: %s", geocode, name))
            targetView.setVisibility(View.VISIBLE)
        } else {
            targetView.setText("")
            targetView.setVisibility(View.GONE)
        }
    }
}
