package cgeo.geocaching.utils.buildconfig;

import cgeo.geocaching.BuildConfig;
import cgeo.geocaching.utils.Log;

public final class BuildConfigHooks {

    private static final IBuildConfigHooks hooks;

    static {
        IBuildConfigHooks h = null;
        if (BuildConfig.DEBUG) {
            try {
                h = (IBuildConfigHooks) Class.forName("cgeo.geocaching.utils.buildconfig.DebugBuildConfigHooks").newInstance();
            } catch (Exception e) {
                Log.e("Could not initialize debug hook class", e);
            }
        }
        hooks = h != null ? h : new NoOpBuildConfigHooks();
    }

    private BuildConfigHooks() {
        // utility class
    }

    public static IBuildConfigHooks get() {
        return hooks;
    }


}
