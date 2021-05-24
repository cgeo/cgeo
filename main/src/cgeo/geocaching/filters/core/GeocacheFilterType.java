package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.StringRes;
import androidx.core.util.Supplier;

import org.apache.commons.text.WordUtils;

public enum GeocacheFilterType {

    NAME("name", R.string.cache_filter_name, R.string.cache_filtergroup_basic, NameGeocacheFilter::new),
    OWNER("owner", R.string.cache_filter_owner, R.string.cache_filtergroup_basic, OwnerGeocacheFilter::new),
    DESCRIPTION("description", R.string.cache_filter_description, R.string.cache_filtergroup_basic, DescriptionGeocacheFilter::new),
    TYPE("type", R.string.cache_filter_type, R.string.cache_filtergroup_basic, TypeGeocacheFilter::new),
    SIZE("size", R.string.cache_filter_size, R.string.cache_filtergroup_detailed, SizeGeocacheFilter::new),
    PERSONAL_NOTE("note", R.string.cache_filter_personalnote, R.string.cache_filtergroup_userspecific, PersonalNoteGeocacheFilter::new),
    DIFFICULTY("difficulty", R.string.cache_filter_difficulty, R.string.cache_filtergroup_detailed,  DifficultyGeocacheFilter::new),
    TERRAIN("terrain", R.string.cache_filter_terrain, R.string.cache_filtergroup_detailed, TerrainGeocacheFilter::new),
    STATUS("status", R.string.cache_filter_status, R.string.cache_filtergroup_basic, StatusGeocacheFilter::new),
    ATTRIBUTES("attributes", R.string.cache_filter_attributes, R.string.cache_filtergroup_detailed, AttributesGeocacheFilter::new),
    OFFLINE_LOG("offlinelog", R.string.cache_filter_offlinelog, R.string.cache_filtergroup_userspecific, OfflineLogGeocacheFilter::new),
    FAVORITES("favorites", R.string.cache_filter_favorites, R.string.cache_filtergroup_detailed, FavoritesGeocacheFilter::new),
    DISTANCE("distance", R.string.cache_filter_distance, R.string.cache_filtergroup_userspecific, DistanceGeocacheFilter::new),
    HIDDEN("hidden", R.string.cache_filter_hidden, R.string.cache_filtergroup_basic, HiddenGeocacheFilter::new);

    private final String typeId;
    private final Supplier<BaseGeocacheFilter> supplier;
    @StringRes
    private final int nameId;
    @StringRes
    private final int groupId;

    GeocacheFilterType(final String typeId, @StringRes final int nameId, @StringRes final int groupId, final Supplier<BaseGeocacheFilter> supplier) {
        this.supplier = supplier;
        this.nameId = nameId;
        this.typeId = typeId;
        this.groupId = groupId;
    }

    public String getTypeId() {
        return this.typeId;
    }

    public BaseGeocacheFilter create() {
        final BaseGeocacheFilter gcf = supplier.get();
        gcf.setType(this);
        return gcf;
    }

    public String getUserDisplayableName() {
        return LocalizationUtils.getStringWithFallback(this.nameId, WordUtils.capitalizeFully(name().replace('_', ' ')));
    }

    public String getUserDisplayableGroup() {
        return LocalizationUtils.getStringWithFallback(this.groupId, null);
    }
}
