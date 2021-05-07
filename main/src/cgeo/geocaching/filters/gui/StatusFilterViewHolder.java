package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.LocalizationUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

public class StatusFilterViewHolder extends CheckboxFilterViewHolder<Integer, StatusGeocacheFilter> {

    private static final int OWN_FOUND = 1;
    private static final int DISABLED = 2;
    private static final int ARCHIVED = 3;

    public StatusFilterViewHolder() {
        super(new ValueGroupFilterAccessor<Integer, StatusGeocacheFilter>()
            .setSelectableValues(new Integer[]{OWN_FOUND, DISABLED, ARCHIVED})
            .setValueGetter(StatusFilterViewHolder::getFilterValues)
            .setValueSetter(StatusFilterViewHolder::setFilterValues)
            .setValueDisplayTextGetter(s -> LocalizationUtils.getString(s == ARCHIVED ? R.string.map_showc_archived : (s == OWN_FOUND ? R.string.map_showc_ownfound : R.string.map_showc_disabled)))
            .setValueDrawableGetter(s -> s == ARCHIVED ? R.drawable.ic_menu_archived : (s == OWN_FOUND ? R.drawable.ic_menu_myplaces : R.drawable.ic_menu_disabled))
            .setGeocacheValueGetter((f, c) -> getGeocacheValues(c)));
    }

    @NotNull
    private static Set<Integer> getGeocacheValues(final Geocache c) {
        final Set<Integer> set = new HashSet<>();
        if (c.isArchived()) {
            set.add(ARCHIVED);
        }
        if (c.isDisabled()) {
            set.add(DISABLED);
        }
        if (c.isFound() || c.getOwnerUserId().equals(Settings.getUserName())) {
            set.add(OWN_FOUND);
        }
        return set;
    }

    private static void setFilterValues(final StatusGeocacheFilter f, final Collection<Integer> s) {
        f.setShowOwnFound(s.contains(OWN_FOUND));
        f.setShowDisabled(s.contains(DISABLED));
        f.setShowArchived(s.contains(ARCHIVED));
    }

    @NotNull
    private static Collection<Integer> getFilterValues(final StatusGeocacheFilter f) {

            final Set<Integer> set = new HashSet<>();
            if (f.isShowArchived()) {
                set.add(ARCHIVED);
            }
            if (f.isShowOwnFound()) {
                set.add(OWN_FOUND);
            }
            if (f.isShowDisabled()) {
                set.add(DISABLED);
            }
            return set;
    }

}
