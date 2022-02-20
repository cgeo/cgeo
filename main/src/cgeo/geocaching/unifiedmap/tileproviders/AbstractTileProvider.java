package cgeo.geocaching.unifiedmap.tileproviders;

import java.util.HashMap;
import java.util.Map;

import cgeo.geocaching.unifiedmap.AbstractUnifiedMap;

public abstract class AbstractTileProvider {

    protected boolean supportsLanguages;
    protected boolean supportsThemes;
    protected String tileProviderName;
    private Integer numericId;
    private static final Map<String, Integer> mapSourceIds = new HashMap<>();

    public boolean supportsLanguages() {
        return supportsLanguages;
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
}
