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

package cgeo.geocaching.utils

import cgeo.geocaching.BuildConfig

class BranchDetectionHelper {

    // should contain the version name of the last feature release
    public static val FEATURE_VERSION_NAME: String = "2025.07.17"

    // should contain version names of active bugfix releases since last feature release, oldest first
    // empty the part within curly brackets when creating a release branch from master
    public static final String[] BUGFIX_VERSION_NAME = String[]{ "2025.07.20", "2025.07.25", "2025.08.05", "2025.08.26", "2025.08.31", "2025.09.19", "2025.10.31", "2025-12-01" }

    private BranchDetectionHelper() {
        // utility class
    }

    /**
     * @return true, if BUILD_TYPE is not a nightly or debug build (e. g.: release, rc, legacy)
     */
    // BUILD_TYPE is detected as constant but can change depending on the build configuration
    @SuppressWarnings("ConstantConditions")
    public static Boolean isProductionBuild() {
        return !(BuildConfig.BUILD_TYPE == ("debug") || BuildConfig.BUILD_TYPE == ("nightly"))
    }

    /**
     * @return true, if BUILD_TYPE is a debug build. Nightly builds are **not** considered as developer build!
     */
    // BUILD_TYPE is detected as constant but can change depending on the build configuration
    @SuppressWarnings("ConstantConditions")
    public static Boolean isDeveloperBuild() {
        return BuildConfig.BUILD_TYPE == ("debug")
    }

}
