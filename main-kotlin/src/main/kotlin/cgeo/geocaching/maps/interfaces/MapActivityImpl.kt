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

package cgeo.geocaching.maps.interfaces

import cgeo.geocaching.activity.FilteredActivity
import cgeo.geocaching.maps.RouteTrackUtils
import cgeo.geocaching.maps.Tracks

import android.content.res.Resources
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

import androidx.appcompat.app.AppCompatActivity

/**
 * Defines the common functions of the provider-specific
 * MapActivity implementations.
 */
interface MapActivityImpl : FilteredActivity() {

    Resources getResources()

    AppCompatActivity getActivity()

    Unit superOnCreate(Bundle savedInstanceState)

    Unit superOnResume()

    Unit superOnDestroy()

    Unit superOnStart()

    Unit superOnStop()

    Unit superOnPause()

    Boolean superOnCreateOptionsMenu(Menu menu)

    Boolean superOnPrepareOptionsMenu(Menu menu)

    Boolean superOnOptionsItemSelected(MenuItem item)

    RouteTrackUtils getRouteTrackUtils()

    Tracks getTracks()

}
