package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.EnumValueMapper;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.wherigo.openwig.EventTable;
import cgeo.geocaching.wherigo.openwig.Task;
import cgeo.geocaching.wherigo.openwig.Thing;
import cgeo.geocaching.wherigo.openwig.Zone;
import cgeo.geocaching.wherigo.openwig.platform.UIScreen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum WherigoThingType {

    LOCATION(Zone.class, R.string.wherigo_thingtype_location, R.drawable.wherigo_icon_locations, UIScreen.LOCATIONSCREEN, WherigoGame::getZones),
    ITEM(Thing.class, R.string.wherigo_thingtype_item, R.drawable.wherigo_icon_search, UIScreen.ITEMSCREEN, WherigoGame::getItems),           // surroundings, "You see"
    INVENTORY(Thing.class, R.string.wherigo_thingtype_inventory, R.drawable.wherigo_icon_inventory, UIScreen.INVENTORYSCREEN, WherigoGame::getInventory),
    TASK(Task.class, R.string.wherigo_thingtype_task, R.drawable.wherigo_icon_tasks, UIScreen.TASKSCREEN, WherigoGame::getTasks),
    THING(Thing.class, R.string.wherigo_thingtype_thing, R.drawable.ic_menu_list, null, WherigoGame::getThings);   // ALL Things in a cartridge

    private final Class<?> clazz;
    private final int displayResId;
    private final int iconId;
    private final UIScreen screen;
    private final Function<WherigoGame, List<? extends EventTable>> thingsGetter;

    private static final EnumValueMapper<UIScreen, WherigoThingType> SCREEN_TO_TYPE = new EnumValueMapper<>();

    static {
        for (WherigoThingType type : values()) {
            if (type.screen != null) {
                SCREEN_TO_TYPE.add(type, type.screen);
            }
        }
    }

    WherigoThingType(final Class<?> clazz, final int displayResId, final int iconId, final UIScreen screen, final Function<WherigoGame, List<? extends EventTable>> thingsGetter) {
        this.clazz = clazz;
        this.displayResId = displayResId;
        this.iconId = iconId;
        this.screen = screen;
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
    public static WherigoThingType getByWherigoScreen(final UIScreen screen) {
        return SCREEN_TO_TYPE.get(screen);
    }

    public static List<EventTable> getEverything() {
        final List<EventTable> result = new ArrayList<>();
        for (WherigoThingType type : values()) {
            result.addAll(type.getAllThings());
        }
        return result;
    }

    public static String getVisibleThingState() {
        return getEverything().stream()
            .filter(WherigoUtils::isVisibleToPlayer)
            .map(et -> et.getClass().getSimpleName() + ":" + et.name)
            .sorted().collect(Collectors.joining(","));
    }

}
