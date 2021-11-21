package cgeo.geocaching.utils;

import cgeo.geocaching.BuildConfig;

public class BranchDetectionHelper {

    // should contain the version name of the last feature release
    public static final String FEATURE_VERSION_NAME = "2021.11.21";

    private BranchDetectionHelper() {
        // utility class
    }

    /**
     * @return true, if BUILD_TYPE is not a nightly or debug build (e. g.: release, rc, legacy)
     */
    @SuppressWarnings("ConstantConditions") // BUILD_TYPE is detected as constant but can change depending on the build configuration
    public static boolean isProductionBuild() {
        return !(BuildConfig.BUILD_TYPE.equals("debug") || BuildConfig.BUILD_TYPE.equals("nightly"));
    }
}
