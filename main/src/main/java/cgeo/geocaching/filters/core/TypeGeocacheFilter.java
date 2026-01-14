package cgeo.geocaching.filters.core;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

import org.apache.commons.lang3.EnumUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class TypeGeocacheFilter extends ValueGroupGeocacheFilter<CacheType, CacheType> {

    // All special event types that are part of the COMMUN_CELEBRATION group
    private static final Set<CacheType> ALL_SPECIAL_EVENT_TYPES = Collections.unmodifiableSet(EnumSet.of(
            CacheType.MEGA_EVENT,
            CacheType.GIGA_EVENT,
            CacheType.COMMUN_CELEBRATION,
            CacheType.GCHQ_CELEBRATION,
            CacheType.GPS_EXHIBIT,
            CacheType.BLOCK_PARTY
    ));
    
    // Pre-computed array for efficient use in constructor
    private static final CacheType[] ALL_SPECIAL_EVENT_TYPES_ARRAY = ALL_SPECIAL_EVENT_TYPES.toArray(new CacheType[0]);

    // Special handling for partial special event type selection
    private Set<CacheType> selectedSpecialEventTypes = null;

    public TypeGeocacheFilter() {
        //gc.com groups in their search their cache types as follows:
        //* "Unknown" is displayed as "Others" and includes: Unknown, GCHQ, PROJECT_APE
        //* "Celebration Event" is displayed as "Specials" and includes: mega,giga, gps_exhibit,commun_celeb, gchq_celeb, block_party
        addDisplayValues(CacheType.UNKNOWN, CacheType.UNKNOWN, CacheType.GCHQ, CacheType.PROJECT_APE);
        addDisplayValues(CacheType.COMMUN_CELEBRATION, ALL_SPECIAL_EVENT_TYPES_ARRAY);
        addDisplayValues(CacheType.VIRTUAL, CacheType.VIRTUAL, CacheType.LOCATIONLESS);
    }

    /**
     * Get all special event types that are part of the COMMUN_CELEBRATION group.
     */
    public static Set<CacheType> getAllSpecialEventTypes() {
        return ALL_SPECIAL_EVENT_TYPES;
    }

    /**
     * Set a specific subset of special event types to filter by.
     * If null or contains all special types, COMMUN_CELEBRATION group will be used.
     * If empty, no special events will be filtered.
     * Otherwise, only the specified special event types will be filtered.
     */
    public void setSelectedSpecialEventTypes(final Set<CacheType> specialTypes) {
        final Set<CacheType> allSpecialTypes = getAllSpecialEventTypes();
        
        // Update the main values set
        final Set<CacheType> currentValues = new HashSet<>(getValues());
        currentValues.remove(CacheType.COMMUN_CELEBRATION);
        
        if (specialTypes == null || specialTypes.isEmpty()) {
            // No special events selected
            this.selectedSpecialEventTypes = null;
        } else if (specialTypes.size() == allSpecialTypes.size() && specialTypes.containsAll(allSpecialTypes)) {
            // All special events selected - use the group
            this.selectedSpecialEventTypes = null;
            currentValues.add(CacheType.COMMUN_CELEBRATION);
        } else {
            // Partial selection - store for custom filtering
            this.selectedSpecialEventTypes = new HashSet<>(specialTypes);
        }
        
        setValues(currentValues);
    }

    /**
     * Get the currently selected special event types.
     * Returns null if using the COMMUN_CELEBRATION group (all types) or no special types selected.
     */
    public Set<CacheType> getSelectedSpecialEventTypes() {
        if (selectedSpecialEventTypes != null) {
            return new HashSet<>(selectedSpecialEventTypes);
        }
        if (getValues().contains(CacheType.COMMUN_CELEBRATION)) {
            return getAllSpecialEventTypes();
        }
        return new HashSet<>();
    }

    @Override
    public CacheType getRawCacheValue(final Geocache cache) {
        return cache.getType();
    }

    @Override
    public CacheType valueFromString(final String stringValue) {
        return EnumUtils.getEnum(CacheType.class, stringValue);
    }

    @Override
    public String valueToString(final CacheType value) {
        return value.name();
    }

    @Override
    public String valueToUserDisplayableValue(final CacheType value) {
        return valueDisplayTextGetter(value);
    }

    @Override
    public String getSqlColumnName() {
        return "type";
    }

    @Override
    public String valueToSqlValue(final CacheType value) {
        return value.id;
    }

    public static String valueDisplayTextGetter(final CacheType value) {
        if (CacheType.UNKNOWN == value) {
            return CgeoApplication.getInstance().getString(R.string.other);
        } else if (CacheType.COMMUN_CELEBRATION == value) {
            return CgeoApplication.getInstance().getString(R.string.event_special);
        }
        return value.getShortL10n();
    }

    @Override
    public Boolean filter(final Geocache cache) {
        final CacheType cacheType = getRawCacheValue(cache);
        if (cacheType == null) {
            return null;
        }
        
        // Custom handling for partial special event type selection
        if (selectedSpecialEventTypes != null) {
            final Set<CacheType> allSpecialTypes = getAllSpecialEventTypes();
            if (allSpecialTypes.contains(cacheType)) {
                // For special event types with partial selection, only check against selected special types
                return selectedSpecialEventTypes.contains(cacheType);
            }
        }
        
        // Default filtering logic for non-special types or when using COMMUN_CELEBRATION group
        return super.filter(cache);
    }

    @Override
    public Set<CacheType> getRawValues() {
        final Set<CacheType> rawValues = new HashSet<>(super.getRawValues());
        
        // If partial special event types are selected, replace COMMUN_CELEBRATION group with individual selections
        if (selectedSpecialEventTypes != null) {
            final Set<CacheType> allSpecialTypes = getAllSpecialEventTypes();
            // Remove all special event types first
            rawValues.removeAll(allSpecialTypes);
            // Then add only the selected ones
            rawValues.addAll(selectedSpecialEventTypes);
        }
        
        return rawValues;
    }

}
