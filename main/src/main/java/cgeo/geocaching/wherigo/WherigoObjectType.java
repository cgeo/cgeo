package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;

import cz.matejcik.openwig.Task;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;

public enum WherigoObjectType {

    LOCATION(Zone.class, "Location/Zone", R.drawable.ic_menu_mapmode),
    ITEM(Thing.class, "You see", R.drawable.ic_menu_list),           // surroundings, "You see"
    INVENTORY(Thing.class, "Inventory", R.drawable.ic_menu_list),
    TASK(Task.class, "Task", R.drawable.ic_menu_myplaces),
    THING(Thing.class, "Thing", R.drawable.ic_menu_list);   // ALL Things in a cartridge

    private final Class<?> clazz;
    private final String displayString;
    private final int iconId;

    WherigoObjectType(final Class<?> clazz, final String displayString, final int iconId) {
        this.clazz = clazz;
        this.displayString = displayString;
        this.iconId = iconId;
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

}
