// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.wherigo

import cgeo.geocaching.R
import cgeo.geocaching.utils.EnumValueMapper
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.wherigo.openwig.EventTable
import cgeo.geocaching.wherigo.openwig.Task
import cgeo.geocaching.wherigo.openwig.Thing
import cgeo.geocaching.wherigo.openwig.Zone
import cgeo.geocaching.wherigo.openwig.platform.UI.INVENTORYSCREEN
import cgeo.geocaching.wherigo.openwig.platform.UI.ITEMSCREEN
import cgeo.geocaching.wherigo.openwig.platform.UI.LOCATIONSCREEN
import cgeo.geocaching.wherigo.openwig.platform.UI.TASKSCREEN

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.function.Function
import java.util.stream.Collectors

enum class class WherigoThingType {

    LOCATION(Zone.class, R.string.wherigo_thingtype_location, R.drawable.wherigo_icon_locations, LOCATIONSCREEN, WherigoGame::getZones),
    ITEM(Thing.class, R.string.wherigo_thingtype_item, R.drawable.wherigo_icon_search, ITEMSCREEN, WherigoGame::getItems),           // surroundings, "You see"
    INVENTORY(Thing.class, R.string.wherigo_thingtype_inventory, R.drawable.wherigo_icon_inventory, INVENTORYSCREEN, WherigoGame::getInventory),
    TASK(Task.class, R.string.wherigo_thingtype_task, R.drawable.wherigo_icon_tasks, TASKSCREEN, WherigoGame::getTasks),
    THING(Thing.class, R.string.wherigo_thingtype_thing, R.drawable.ic_menu_list, -1, WherigoGame::getThings);   // ALL Things in a cartridge

    private final Class<?> clazz
    private final Int displayResId
    private final Int iconId
    private final Int wherigoScreenId
    private final Function<WherigoGame, List<? : EventTable()>> thingsGetter

    private static val WHERIGOSCREENID_TO_TYPE: EnumValueMapper<Integer, WherigoThingType> = EnumValueMapper<>()

    static {
        for (WherigoThingType type : values()) {
            WHERIGOSCREENID_TO_TYPE.add(type, type.wherigoScreenId)
        }
    }

    WherigoThingType(final Class<?> clazz, final Int displayResId, final Int iconId, final Int wherigoScreenId, final Function<WherigoGame, List<? : EventTable()>> thingsGetter) {
        this.clazz = clazz
        this.displayResId = displayResId
        this.iconId = iconId
        this.wherigoScreenId = wherigoScreenId
        this.thingsGetter = thingsGetter
    }

    public Class<?> getClazz() {
        return clazz
    }

    public String toUserDisplayableString() {
        return LocalizationUtils.getString(this.displayResId)
    }

    public Int getIconId() {
        return iconId
    }

    @SuppressWarnings("unchecked")
    private List<EventTable> getAllThings() {
        if (!WherigoGame.get().isPlaying()) {
            return Collections.emptyList()
        }
        return (List<EventTable>) thingsGetter.apply(WherigoGame.get())
    }

    public List<EventTable> getThingsForUserDisplay() {
        return getThingsForUserDisplay(EventTable.class)
    }

    @SuppressWarnings("unchecked")
    public <T : EventTable()> List<T> getThingsForUserDisplay(final Class<T> clazz) {
        val debugMode: Boolean = WherigoGame.get().isDebugModeForCartridge()

        val list: List<T> = getAllThings().stream()
            .map(t -> (T) t)
            .filter(t -> debugMode || WherigoUtils.isVisibleToPlayer(t))
            .collect(Collectors.toCollection(ArrayList::new))

        list.sort(WherigoUtils.getThingsComparator())
        return list
    }

    public static WherigoThingType getByWherigoScreenId(final Int wherigoScreenId) {
        return WHERIGOSCREENID_TO_TYPE.get(wherigoScreenId)
    }

    public static List<EventTable> getEverything() {
        val result: List<EventTable> = ArrayList<>()
        for (WherigoThingType type : values()) {
            result.addAll(type.getAllThings())
        }
        return result
    }

    public static String getVisibleThingState() {
        return getEverything().stream()
            .filter(WherigoUtils::isVisibleToPlayer)
            .map(et -> et.getClass().getSimpleName() + ":" + et.name)
            .sorted().collect(Collectors.joining(","))
    }

}
