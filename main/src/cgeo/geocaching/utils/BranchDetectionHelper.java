package cgeo.geocaching.utils;

public class BranchDetectionHelper {
    // should be "1" if on branch "release", and "0" else
    public static final int FROM_BRANCH_RELEASE = 1;

    // should contain the version name of the last feature release
    public static final String FEATURE_VERSION_NAME = "2021.10-11-RC";

    private BranchDetectionHelper() {
        // utility class
    }
}
