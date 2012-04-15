package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractFilter implements IFilter {
    private final String name;

    public AbstractFilter(String name) {
        this.name = name;
    }

    public void filter(List<cgCache> list) {
        final List<cgCache> itemsToRemove = new ArrayList<cgCache>();
        for (cgCache item : list) {
            if (!accepts(item)) {
                itemsToRemove.add(item);
            }
        }
        list.removeAll(itemsToRemove);
    }

    @Override
    public String getName() {
        return name;
    }

    /*
     * show name in array adapter
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName();
    }
}
