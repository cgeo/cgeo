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

package cgeo.geocaching.unifiedmap.mapsforge

import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.settings.SettingsActivity

import android.os.Bundle
import android.view.MenuItem

class MapsforgeThemeSettings : AbstractActivity() {

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish()
            return true
        }
        return false
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        ActivityMixin.setDisplayHomeAsUpEnabled(this, true)
        setContentView(R.layout.layout_settings)

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_fragment_root, MapsforgeThemeSettingsFragment())
                .commit()
        SettingsActivity.hideRightColumnInLandscapeMode(this)
    }

}
