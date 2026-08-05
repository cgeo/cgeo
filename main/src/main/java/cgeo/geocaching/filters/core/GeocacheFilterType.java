package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.EnumValueMapper;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.StringRes;
import androidx.core.util.Supplier;

import org.apache.commons.text.WordUtils;

public enum GeocacheFilterType {

    NAME("name", R.string.cache_filter_name, R.string.cache_filtergroup_basic, NameGeocacheFilter::new),
    OWNER("owner", R.string.cache_filter_owner, R.string.cache_filtergroup_basic, OwnerGeocacheFilter::new),
    DESCRIPTION("description", R.string.cache_filter_description, R.string.cache_filtergroup_basic, DescriptionGeocacheFilter::new),
    TYPE("type", R.string.cache_filter_type, R.string.cache_filtergroup_basic, TypeGeocacheFilter::new),
    SIZE("size", R.string.cache_filter_size, R.string.cache_filtergroup_details, SizeGeocacheFilter::new),
    PERSONAL_NOTE("note", R.string.cache_filter_personalnote, R.string.cache_filtergroup_userspecific, PersonalNoteGeocacheFilter::new),
    DIFFICULTY("difficulty", R.string.cache_filter_difficulty, R.string.cache_filtergroup_details, DifficultyGeocacheFilter::new),
    TERRAIN("terrain", R.string.cache_filter_terrain, R.string.cache_filtergroup_details, TerrainGeocacheFilter::new),
    DIFFICULTY_TERRAIN("difficulty_terrain", R.string.cache_filter_difficulty_terrain, R.string.cache_filtergroup_details, DifficultyAndTerrainGeocacheFilter::new),
    DIFFICULTY_TERRAIN_MATRIX("difficulty_terrain_matrix", R.string.cache_filter_difficulty_terrain_matrix, R.string.cache_filtergroup_details, DifficultyTerrainMatrixGeocacheFilter::new),
    RATING("rating", R.string.cache_filter_rating, R.string.cache_filtergroup_details, RatingGeocacheFilter::new),
    STATUS("status", R.string.cache_filter_status, R.string.cache_filtergroup_basic, StatusGeocacheFilter::new),
    INDIVIDUAL_ROUTE("individualroute", R.string.cache_filter_individualroute, R.string.cache_filtergroup_userspecific, IndividualRouteGeocacheFilter::new),
    ATTRIBUTES("attributes", R.string.cache_filter_attributes, R.string.cache_filtergroup_details, AttributesGeocacheFilter::new),
    INVENTORY_COUNT("inventory", R.string.cache_filter_inventory_count, R.string.cache_filtergroup_details, InventoryCountFilter::new),
    OFFLINE_LOG("offlinelog", R.string.cache_filter_offlinelog, R.string.cache_filtergroup_userspecific, OfflineLogGeocacheFilter::new),
    FAVORITES("favorites", R.string.cache_filter_favorites, R.string.cache_filtergroup_details, FavoritesGeocacheFilter::new),
    DISTANCE("distance", R.string.cache_filter_distance, R.string.cache_filtergroup_userspecific, DistanceGeocacheFilter::new),
    HIDDEN("hidden", R.string.cache_filter_hidden, R.string.cache_filtergroup_basic, HiddenGeocacheFilter::new),
    EVENT_DATE("eventdate", R.string.cache_filter_eventdate, R.string.cache_filtergroup_basic, HiddenGeocacheFilter::new),
    LOGS_COUNT("logs_count", R.string.cache_filter_logs_count, R.string.cache_filtergroup_details, LogsCountGeocacheFilter::new),
    LAST_FOUND("last_found", R.string.cache_filter_last_found, R.string.cache_filtergroup_details, LastFoundGeocacheFilter::new),
    LOG_ENTRY("log_entry", R.string.cache_filter_log_entry, R.string.cache_filtergroup_details, LogEntryGeocacheFilter::new),
    LOCATION("location", R.string.cache_filter_location, R.string.cache_filtergroup_details, LocationGeocacheFilter::new),
    STORED_LISTS("stored_list", R.string.cache_filter_stored_lists, R.string.cache_filtergroup_userspecific, StoredListGeocacheFilter::new),
    LIST_ID("list_id", 0, 0, ListIdGeocacheFilter::new),
    VIEWPORT("viewport", 0, 0, ViewportGeocacheFilter::new),
    ORIGIN("origin", R.string.cache_filter_origin, R.string.cache_filtergroup_details, OriginGeocacheFilter::new),
    STORED_SINCE("stored_since", R.string.cache_filter_stored_since, R.string.cache_filtergroup_userspecific, StoredSinceGeocacheFilter::new),
    CATEGORY("category", R.string.cache_filter_category, R.string.cache_filtergroup_details, CategoryGeocacheFilter::new),
    TIER("tier", R.string.cache_filter_tier, R.string.cache_filtergroup_details, TierGeocacheFilter::new),
    NAMED_FILTER("named_filter", R.string.cache_filter_named_filter, R.string.cache_filtergroup_special, NamedFilterGeocacheFilter::new),
    LOGICAL_FILTER_GROUP(null, R.string.cache_filter_logical_filter_group, R.string.cache_filtergroup_special, AndGeocacheFilter::new);


    private final String typeId;
    private final Supplier<BaseGeocacheFilter> supplier;
    @StringRes
    private final int nameId;
    @StringRes
    private final int groupId;

    private static final EnumValueMapper<String, GeocacheFilterType> TYPEID_TO_TYPE = new EnumValueMapper<>();

    static {
        for (GeocacheFilterType type : values()) {
            TYPEID_TO_TYPE.add(type, type.typeId);
        }
    }

    GeocacheFilterType(final String typeId, @StringRes final int nameId, @StringRes final int groupId, final Supplier<BaseGeocacheFilter> supplier) {
        this.supplier = supplier;
        this.nameId = nameId;
        this.typeId = typeId;
        this.groupId = groupId;
    }

    public String getTypeId() {
        return this.typeId;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseGeocacheFilter> T create() {
        final T gcf = (T) supplier.get();
        gcf.setType(this);
        return gcf;
    }

    public static GeocacheFilterType getByTypeId(final String typeId) {
        return TYPEID_TO_TYPE.get(typeId, null);
    }

    public String getUserDisplayableName() {
        return LocalizationUtils.getStringWithFallback(this.nameId, WordUtils.capitalizeFully(name().replace('_', ' ')));
    }

    public String getUserDisplayableGroup() {
        return LocalizationUtils.getStringWithFallback(this.groupId, null);
    }

    public boolean displayToUser() {
        return this.groupId != 0;
    }
}
