package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.unifiedmap.AbstractMapFragment;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTileProvider {

    protected boolean supportsLanguages;
    protected boolean supportsThemes;
    protected boolean supportsHillshading = false;
    protected boolean supportsBackgroundMaps = false;
    protected boolean supportsThemeOptions;
    protected String tileProviderName;
    private Integer numericId;
    private static final Map<String, Integer> mapSourceIds = new HashMap<>();

    protected int zoomMin;
    protected int zoomMax;
    protected Pair<String, Boolean> mapAttribution;

    protected AbstractTileProvider(final int zoomMin, final int zoomMax, final Pair<String, Boolean> mapAttribution) {
        this.zoomMin = zoomMin;
        this.zoomMax = zoomMax;
        this.mapAttribution = mapAttribution;
    }

    protected void setMapAttribution(final Pair<String, Boolean> newAttribution) {
        mapAttribution = newAttribution;
    }

    public Pair<String, Boolean> getMapAttribution() {
        return mapAttribution;
    }

    public void setPreferredLanguage(final String language) {
        // default: do nothing
    }

    public boolean supportsThemes() {
        return supportsThemes;
    }

    public boolean supportsThemeOptions() {
        return supportsThemeOptions;
    }

    public boolean supportsHillshading() {
        return supportsHillshading;
    }

    public boolean supportsBackgroundMaps() {
        return supportsBackgroundMaps;
    }

    public String getTileProviderName() {
        return tileProviderName;
    }

    @Nullable
    public String getDisplayName(@Nullable final String defaultDisplayName) {
        return defaultDisplayName;
    }

    public String getId() {
        return this.getClass().getName();
    }

    public int getNumericalId() {
        if (numericId == null) {
            final String id = getId();
            //produce a guaranteed unique numerical id for the string id
            synchronized (mapSourceIds) {
                if (mapSourceIds.containsKey(id)) {
                    numericId = mapSourceIds.get(id);
                } else {
                    numericId = -1000000000 + mapSourceIds.size();
                    mapSourceIds.put(id, numericId);
                }
            }
        }
        return numericId;
    }

    public abstract AbstractMapFragment createMapFragment();


    public int getZoomMin() {
        return zoomMin;
    }

    public int getZoomMax() {
        return zoomMax;
    }

    // ========================================================================
    // Lifecycle methods

    public void onPause() {
        // do nothing by default
    }

    public void onResume() {
        // do nothing by default
    }

    public void onDestroy() {
        // do nothing by default
    }

}
