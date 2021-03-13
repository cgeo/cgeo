package cgeo.geocaching.filters.core;

import java.util.ArrayList;
import java.util.List;

public abstract class LogicalGeocacheFilter implements IGeocacheFilter {

    private final List<IGeocacheFilter> children = new ArrayList<>();

    public GeocacheFilterType getType() {
        return null;
    }

    @Override
    public void setConfig(final String[] value) {
        //logical filter has no config
    }

    @Override
    public String[] getConfig() {
        return null;
    }

    @Override
    public void addChild(final IGeocacheFilter child) {
        children.add(child);
    }

    @Override
    public List<IGeocacheFilter> getChildren() {
        return children;
    }
}
