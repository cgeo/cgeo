package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.EnumValueMapper;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Task;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;
import static cz.matejcik.openwig.platform.UI.INVENTORYSCREEN;
import static cz.matejcik.openwig.platform.UI.ITEMSCREEN;
import static cz.matejcik.openwig.platform.UI.LOCATIONSCREEN;
import static cz.matejcik.openwig.platform.UI.TASKSCREEN;

public enum WherigoThingType {

    LOCATION(Zone.class, R.string.wherigo_thingtype_location, R.drawable.wherigo_icon_locations, LOCATIONSCREEN, WherigoGame::getZones),
    ITEM(Thing.class, R.string.wherigo_thingtype_item, R.drawable.wherigo_icon_search, ITEMSCREEN, WherigoGame::getItems),           // surroundings, "You see"
    INVENTORY(Thing.class, R.string.wherigo_thingtype_inventory, R.drawable.wherigo_icon_inventory, INVENTORYSCREEN, WherigoGame::getInventory),
    TASK(Task.class, R.string.wherigo_thingtype_task, R.drawable.wherigo_icon_tasks, TASKSCREEN, WherigoGame::getTasks),
    THING(Thing.class, R.string.wherigo_thingtype_thing, R.drawable.ic_menu_list, -1, WherigoGame::getThings);   // ALL Things in a cartridge

    private final Class<?> clazz;
    private final int displayResId;
    private final int iconId;
    private final int wherigoScreenId;
    private final Function<WherigoGame, List<? extends EventTable>> thingsGetter;

    private static final EnumValueMapper<Integer, WherigoThingType> WHERIGOSCREENID_TO_TYPE = new EnumValueMapper<>();

    static {
        for (WherigoThingType type : values()) {
            WHERIGOSCREENID_TO_TYPE.add(type, type.wherigoScreenId);
        }
    }

    WherigoThingType(final Class<?> clazz, final int displayResId, final int iconId, final int wherigoScreenId, final Function<WherigoGame, List<? extends EventTable>> thingsGetter) {
        this.clazz = clazz;
        this.displayResId = displayResId;
        this.iconId = iconId;
        this.wherigoScreenId = wherigoScreenId;
        this.thingsGetter = thingsGetter;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String toUserDisplayableString() {
        return LocalizationUtils.getString(this.displayResId);
    }

    public int getIconId() {
        return iconId;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private List<EventTable> getAllThings() {
        if (!WherigoGame.get().isPlaying()) {
            return Collections.emptyList();
        }
        return (List<EventTable>) thingsGetter.apply(WherigoGame.get());
    }

    public List<EventTable> getThingsForUserDisplay() {
        return getThingsForUserDisplay(EventTable.class);
    }

    @SuppressWarnings("unchecked")
    public <T extends EventTable> List<T> getThingsForUserDisplay(final Class<T> clazz) {
        final boolean debugMode = WherigoGame.get().isDebugModeForCartridge();

        final List<T> list = getAllThings().stream()
            .map(t -> (T) t)
            .filter(t -> debugMode || WherigoUtils.isVisibleToPlayer(t))
            .collect(Collectors.toCollection(ArrayList::new));

        list.sort(WherigoUtils.getThingsComparator());
        return list;
    }

    @Nullable
    public static WherigoThingType getByWherigoScreenId(final int wherigoScreenId) {
        return WHERIGOSCREENID_TO_TYPE.get(wherigoScreenId);
    }

}
