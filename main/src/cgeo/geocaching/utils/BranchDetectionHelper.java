package cgeo.geocaching.utils;

import cgeo.geocaching.BuildConfig;

public class BranchDetectionHelper {

    // should contain the version name of the last feature release
    public static final String FEATURE_VERSION_NAME = "2022.10.16";

    // should contain version names of active bugfix releases since last feature release, oldest first
    // empty the part within curly brackets when creating a new release branch from master
    public static final String[] BUGFIX_VERSION_NAME = new String[]{ };

    private BranchDetectionHelper() {
        // utility class
    }

    /**
     * @return true, if BUILD_TYPE is not a nightly or debug build (e. g.: release, rc, legacy)
     */
    // BUILD_TYPE is detected as constant but can change depending on the build configuration
    @SuppressWarnings("ConstantConditions")
    public static boolean isProductionBuild() {
        return !(BuildConfig.BUILD_TYPE.equals("debug") || BuildConfig.BUILD_TYPE.equals("nightly"));
    }

    /**
     * @return true, if BUILD_TYPE is a debug build. Nightly builds are **not** considered as developer build!
     */
    // BUILD_TYPE is detected as constant but can change depending on the build configuration
    @SuppressWarnings("ConstantConditions")
    public static boolean isDeveloperBuild() {
        return BuildConfig.BUILD_TYPE.equals("debug");
    }

}
