package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

import java.util.ArrayList;
import java.util.List;

public abstract class cgFilter {
    private String name;

    public cgFilter(String name) {
        this.name = name;
    }

    abstract boolean applyFilter(cgCache cache);

    public void filter(List<cgCache> list) {
        List<cgCache> itemsToRemove = new ArrayList<cgCache>();
        for (cgCache item : list) {
            if (!applyFilter(item)) {
                itemsToRemove.add(item);
            }
        }
        list.removeAll(itemsToRemove);
    }

    public String getFilterName() {
        return name;
    }
}
