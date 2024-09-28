package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Task;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;

public enum WherigoThingType {

    LOCATION(Zone.class, "Location/Zone", R.drawable.wherigo_icon_locations, WherigoGame::getZones),
    ITEM(Thing.class, "You see", R.drawable.wherigo_icon_search, WherigoGame::getItems),           // surroundings, "You see"
    INVENTORY(Thing.class, "Inventory", R.drawable.wherigo_icon_inventory, WherigoGame::getInventory),
    TASK(Task.class, "Task", R.drawable.wherigo_icon_tasks, WherigoGame::getTasks),
    THING(Thing.class, "Thing", R.drawable.ic_menu_list, WherigoGame::getThings);   // ALL Things in a cartridge

    private final Class<?> clazz;
    private final String displayString;
    private final int iconId;
    private final Function<WherigoGame, List<? extends EventTable>> thingsGetter;

    WherigoThingType(final Class<?> clazz, final String displayString, final int iconId, final Function<WherigoGame, List<? extends EventTable>> thingsGetter) {
        this.clazz = clazz;
        this.displayString = displayString;
        this.iconId = iconId;
        this.thingsGetter = thingsGetter;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String toUserDisplayableString() {
        return displayString;
    }

    public int getIconId() {
        return iconId;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public List<EventTable> getThings() {
        if (!WherigoGame.get().isPlaying()) {
            return Collections.emptyList();
        }
        return (List<EventTable>) thingsGetter.apply(WherigoGame.get());
    }

    public List<EventTable> getThingsForUserDisplay() {

        final List<EventTable> list;
        if (Settings.enableFeatureWherigoDebug()) {
            list = new ArrayList<>(getThings());
        } else {
            list = getThings().stream().filter(WherigoUtils::isVisibleToPlayer).collect(Collectors.toCollection(ArrayList::new));
        }
        list.sort(WherigoUtils.getThingsComparator());
        return list;
    }

}
