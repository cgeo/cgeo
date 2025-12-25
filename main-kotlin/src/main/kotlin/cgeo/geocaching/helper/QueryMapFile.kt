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

package cgeo.geocaching.helper

import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.storage.PersistableFolder

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle

import org.apache.commons.lang3.StringUtils

/**
 * Helper activity for WhereYouGo to request the current mf map dir
 * Only returns if there is a map file set or forceAndFeedback=true
 */
class QueryMapFile : AbstractActivity() {
    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        val bundle: Bundle = getIntent().getExtras()
        val forceAndFeedback: Boolean = null != bundle && bundle.getBoolean(getString(R.string.cgeo_queryMapFile_actionParam))

        String mapFile
        try {
            mapFile = PersistableFolder.OFFLINE_MAPS.getUri().toString()
        } catch (NullPointerException e) {
            mapFile = null
        }
        if (forceAndFeedback || StringUtils.isNotEmpty(mapFile)) {
            try {
                val intent: Intent = Intent(Intent.ACTION_SENDTO)
                intent.setComponent(ComponentName(getString(R.string.package_whereyougo), getString(R.string.whereyougo_action_Mapsforge)))
                intent.putExtra(getString(R.string.cgeo_queryMapFile_resultParam), mapFile)
                intent.putExtra(getString(R.string.cgeo_queryMapFile_actionParam), forceAndFeedback)
                startActivity(intent)
            } catch (ActivityNotFoundException e) {
                // oops? shouldn't happen, as we have been called from WhereYouGo
            }
        }

        finish()
    }
}
