package cgeo.geocaching.filters.core;

import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.StringRes;
import androidx.core.util.Supplier;

import org.apache.commons.text.WordUtils;

public enum GeocacheFilterType {

    NAME("name", 0, NameGeocacheFilter::new),
    OWNER("owner", 0, OwnerGeocacheFilter::new),
    DESCRIPTION("description", 0, DescriptionGeocacheFilter::new),
    TYPE("type", 0, TypeGeocacheFilter::new),
    SIZE("size", 0, SizeGeocacheFilter::new),
    PERSONAL_NOTE("note", 0, PersonalNoteGeocacheFilter::new),
    OFFLINE_LOG_FILTER("offlinelog", 0, OfflineLogGeocacheFilter::new);

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
