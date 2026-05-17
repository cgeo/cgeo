package cgeo.geocaching.utils.buildconfig;

import leakcanary.LeakCanary;

public final class DebugBuildConfigHooks implements IBuildConfigHooks {

    @Override
    public void initializeApp() {
        //LeakCanary: deactivate heap dumps
        final LeakCanary.Config config = LeakCanary.getConfig()
                .newBuilder()
                .dumpHeap(false) // Heap-Dumps deaktivieren
                .build();
        LeakCanary.setConfig(config);
    }
}
