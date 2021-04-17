package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.StringRes;
import androidx.core.util.Supplier;

import org.apache.commons.text.WordUtils;

public enum GeocacheFilterType {

    NAME("name", R.string.cache_filter_name, NameGeocacheFilter::new),
    OWNER("owner", R.string.cache_filter_owner, OwnerGeocacheFilter::new),
    DESCRIPTION("description", R.string.cache_filter_description, DescriptionGeocacheFilter::new),
    TYPE("type", R.string.cache_filter_type, TypeGeocacheFilter::new),
    SIZE("size", R.string.cache_filter_size, SizeGeocacheFilter::new),
    PERSONAL_NOTE("note", R.string.cache_filter_personalnote, PersonalNoteGeocacheFilter::new),
    OFFLINE_LOG("offlinelog", R.string.cache_filter_offlinelog, OfflineLogGeocacheFilter::new);

    private final String typeId;
    private final Supplier<BaseGeocacheFilter> supplier;
    @StringRes
    private final int nameId;

    GeocacheFilterType(final String typeId, @StringRes final int nameId, final Supplier<BaseGeocacheFilter> supplier) {
        this.supplier = supplier;
        this.nameId = nameId;
        this.typeId = typeId;
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
}
