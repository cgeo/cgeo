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

import androidx.annotation.StringRes

class HelperApp {
    final Int titleId
    final Int descriptionId
    final Int iconId
    final Int packageNameResId

    HelperApp(final Int title, final Int description, final Int icon, @StringRes final Int packageNameResId) {
        this.titleId = title
        this.descriptionId = description
        this.iconId = icon
        this.packageNameResId = packageNameResId
    }

}
