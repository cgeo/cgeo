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

package cgeo.geocaching.wherigo

import android.app.Activity
import android.app.Dialog

interface IWherigoDialogProvider {

    Dialog createAndShowDialog(Activity activity, IWherigoDialogControl control)

    /** return TRUE if a currentl visible provider can "take over" what the given provider is shown. In that case
     * no dialog is created, just a refresh event is triggered */
    default Boolean canRefresh(IWherigoDialogProvider otherDialog) {
        return false
    }

}
