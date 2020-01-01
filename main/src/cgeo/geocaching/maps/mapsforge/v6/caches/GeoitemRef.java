package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.utils.TextUtils;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

public class GeoitemRef {

    public static final Comparator<? super GeoitemRef> NAME_COMPARATOR = (Comparator<GeoitemRef>) (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName());

    private final String itemCode;
    private final CoordinatesType type;
    private final String geocode;
    private final int id;
    private final String name;
    private final int markerId;

    public GeoitemRef(final String itemCode, final CoordinatesType type, final String geocode, final int id, final String name, final int markerId) {
        this.itemCode = itemCode;
        this.type = type;
        this.geocode = geocode;
        this.id = id;
        this.name = name;
        this.markerId = markerId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeoitemRef)) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(this.itemCode, ((GeoitemRef) o).itemCode);
    }

    @Override
    public int hashCode() {
        return StringUtils.defaultString(itemCode).hashCode();
    }

    @Override
    public String toString() {
        if (StringUtils.isEmpty(name)) {
            return itemCode;
        }

        return String.format("%s: %s", itemCode, name);
    }

    public String getItemCode() {
        return itemCode;
    }

    public CoordinatesType getType() {
        return type;
    }

    public String getGeocode() {
        return geocode;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMarkerId() {
        return markerId;
    }
}
