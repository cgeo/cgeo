package cgeo.geocaching.apps.navi;

import cgeo.geocaching.models.Geocache;

import android.content.Context;

import androidx.annotation.NonNull;

public interface TargetSelectorNavigationApp extends CacheNavigationApp {

    /*
     * same as navigate(...), might be called from navigateWithTargetSelector to navigate to Geocache
     */
    void navigateWithoutTargetSelector(@NonNull Context context, @NonNull Geocache cache);

}
