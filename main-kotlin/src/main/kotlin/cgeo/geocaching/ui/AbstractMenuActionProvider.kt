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

package cgeo.geocaching.ui

import android.content.Context
import android.view.View

import androidx.core.view.ActionProvider

/**
 * abstract super class for all our action providers showing sub menus
 */
abstract class AbstractMenuActionProvider : ActionProvider() {

    /**
     * Creates a instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
    protected AbstractMenuActionProvider(final Context context) {
        super(context)
    }

    override     public Boolean hasSubMenu() {
        return true
    }

    override     public View onCreateActionView() {
        // must return null, otherwise the menu will not work
        return null
    }

}
