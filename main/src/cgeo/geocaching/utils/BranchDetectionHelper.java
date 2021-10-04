package cgeo.geocaching.utils;

public class BranchDetectionHelper {
    // should be "1" if on branch "release", and "0" else
    public static final int FROM_BRANCH_RELEASE = 0;

    // should contain the version name of the last feature release
    public static final String FEATURE_VERSION_NAME = "2021.09.27";

    private BranchDetectionHelper() {
        // utility class
    }
}
