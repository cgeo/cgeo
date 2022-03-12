package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.unifiedmap.AbstractUnifiedMap;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTileProvider {

    protected boolean supportsLanguages;
    protected boolean supportsThemes;
    protected String tileProviderName;
    private Integer numericId;
    private static final Map<String, Integer> mapSourceIds = new HashMap<>();

    protected final int zoomMin;
    protected final int zoomMax;

    protected AbstractTileProvider (final int zoomMin, final int zoomMax) {
        this.zoomMin = zoomMin;
        this.zoomMax = zoomMax;
    }

    public boolean supportsLanguages() {
        return supportsLanguages;
    }

    public void setPreferredLanguage(final String language) {
        // default: do nothing
    }

    public boolean supportsThemes() {
        return supportsThemes;
    }

    public String getTileProviderName() {
        return tileProviderName;
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

    public abstract AbstractUnifiedMap getMap();

    public int getZoomMin() {
        return zoomMin;
    }

    public int getZoomMax() {
        return zoomMax;
    }

}
