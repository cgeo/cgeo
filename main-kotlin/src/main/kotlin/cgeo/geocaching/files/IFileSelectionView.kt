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

package cgeo.geocaching.files

import android.content.Context

interface IFileSelectionView {

    Context getContext()

    String getCurrentFile()

    Unit setCurrentFile(String name)

    Unit close()

}
