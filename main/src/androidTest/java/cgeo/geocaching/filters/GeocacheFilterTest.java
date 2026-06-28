package cgeo.geocaching.filters;

import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.AttributesGeocacheFilter;
import cgeo.geocaching.filters.core.CategoryGeocacheFilter;
import cgeo.geocaching.filters.core.DescriptionGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyAndTerrainGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyTerrainMatrixGeocacheFilter;
import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.FavoritesGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.HiddenGeocacheFilter;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.InventoryCountFilter;
import cgeo.geocaching.filters.core.LastFoundGeocacheFilter;
import cgeo.geocaching.filters.core.LocationGeocacheFilter;
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter;
import cgeo.geocaching.filters.core.LogsCountGeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;
import cgeo.geocaching.filters.core.OfflineLogGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.filters.core.PersonalNoteGeocacheFilter;
import cgeo.geocaching.filters.core.RatingGeocacheFilter;
import cgeo.geocaching.filters.core.SizeGeocacheFilter;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.filters.core.StoredListGeocacheFilter;
import cgeo.geocaching.filters.core.StoredSinceGeocacheFilter;
import cgeo.geocaching.filters.core.StringFilter;
import cgeo.geocaching.filters.core.TierGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.bettercacher.Category;
import cgeo.geocaching.models.bettercacher.Tier;
import cgeo.geocaching.storage.DataStore;

import androidx.core.util.Pair;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GeocacheFilterTest {

    @Test
    public void emptyFilter() {
        assertFilterFromConfig("", null);
        assertFilterFromConfig(null, null);
        assertFilterFromConfig("   ", null);
    }

    @Test
    public void emptyFilterWithName() {
        assertFilterFromConfig("[myname]", null);
        assertFilterFromConfig("[myname", null);
        assertFilterFromConfig("[]", null);
        assertFilterFromConfig("[test\\]\\:test]", null);
    }

    @Test
    public void emptyNameWithFilter() {
        assertFilterFromConfig("{\"tree\": { \"type\":\"name\" } }", NameGeocacheFilter.class);
    }

    @Test
    public void checkInconclusive() {
        GeocacheFilter filter = assertFilterFromConfig("{\"tree\": { \"type\":\"name\" } }", NameGeocacheFilter.class);
        assertThat(filter.isIncludeInconclusive()).isFalse();
        filter = assertFilterFromConfig("{\"inconclusive\":true, \"tree\": { \"type\":\"name\" } }", NameGeocacheFilter.class);
        assertThat(filter.isIncludeInconclusive()).isTrue();
    }

    @Test
    public void checkAdvancedView() {
        GeocacheFilter filter = assertFilterFromConfig("{\"tree\": { \"type\":\"name\" } }", NameGeocacheFilter.class);
        assertThat(filter.isOpenInAdvancedMode()).isFalse();
        filter = assertFilterFromConfig("{\"advanced\":true, \"tree\": { \"type\":\"name\" } }", NameGeocacheFilter.class);
        assertThat(filter.isOpenInAdvancedMode()).isTrue();
    }

    private GeocacheFilter assertFilterFromConfig(final String config, final Class<? extends IGeocacheFilter> expectedFilterClass) {
        final GeocacheFilter filter = GeocacheFilter.createFromConfig(config);
        assertThat(filter.getTree() == null ? null : filter.getTree().getClass()).as("treeclass for ' " + config + "'").isEqualTo(expectedFilterClass);
        return filter;
    }

    @Test
    public void genericEmptyFilter() throws ParseException {

        final IGeocacheFilter filterTree = AndGeocacheFilter.create();
        for (GeocacheFilterType type : GeocacheFilterType.values()) {
            filterTree.addChild(type.create());
        }

        final GeocacheFilter filter = GeocacheFilter.create(false, false, filterTree);
        assertFilterConfig(filter);
    }

    @Test
    public void genericFilledFilter() throws ParseException {
        
        final IGeocacheFilter filterTree = AndGeocacheFilter.create();
        for (GeocacheFilterType type : GeocacheFilterType.values()) {
            filterTree.addChild(getFilledInstance(type));
        }

        final GeocacheFilter filter = GeocacheFilter.create(true, true, filterTree);
        assertFilterConfig(filter);
    }


    private static void assertFilterConfig(final GeocacheFilter filter) {
        final String filterJson = filter.toConfig();

        final GeocacheFilter filterFromJson = GeocacheFilter.createFromConfig(filterJson);
        assertThat(filterFromJson.toConfig()).isEqualTo(filterJson);
        assertThat(filterFromJson.isIncludeInconclusive()).isEqualTo(filter.isIncludeInconclusive());
        assertThat(filterFromJson.isOpenInAdvancedMode()).isEqualTo(filter.isOpenInAdvancedMode());
        assertThat(filterFromJson.isFiltering()).isEqualTo(filter.isFiltering());
        assertThat(filterFromJson.toUserDisplayableString()).isEqualTo(filter.toUserDisplayableString());
    }

    private static IGeocacheFilter getFilledInstance(final GeocacheFilterType type) {
        switch (type) {
            case TYPE:
                return TypeGeocacheFilter.create(CacheType.TRADITIONAL, CacheType.MYSTERY);
            case NAME:
                return NameGeocacheFilter.create("name", false, StringFilter.StringFilterType.CONTAINS);
            case OWNER:
                return OwnerGeocacheFilter.create("owner", true, StringFilter.StringFilterType.PATTERN);
            case DESCRIPTION:
                return DescriptionGeocacheFilter.create(null, false, StringFilter.StringFilterType.CONTAINS);
            case SIZE:
                return SizeGeocacheFilter.create(CacheSize.LARGE);
            case PERSONAL_NOTE:
                return PersonalNoteGeocacheFilter.create(null, true, StringFilter.StringFilterType.CONTAINS);
            case DIFFICULTY_TERRAIN:
                final DifficultyAndTerrainGeocacheFilter dtFilter = DifficultyAndTerrainGeocacheFilter.create(
                        Pair.create(1f, 3f),
                        Pair.create(null, 4f));
                dtFilter.difficultyGeocacheFilter.setSpecialNumber(0f, true);
                return dtFilter;
            case DIFFICULTY_TERRAIN_MATRIX:
                final DifficultyTerrainMatrixGeocacheFilter dtmFilter = DifficultyTerrainMatrixGeocacheFilter.create(
                        List.of(Pair.create(1f, 4f), Pair.create(2f, 3f)));
                dtmFilter.setIncludeCachesWoDt(false);
                return dtmFilter;
            case RATING:
                return RatingGeocacheFilter.create(1f, null);
            case STATUS:
                final StatusGeocacheFilter statusFilter = StatusGeocacheFilter.create(
                        Collections.singletonList(StatusGeocacheFilter.StatusType.FOUND),
                        Collections.singletonList(StatusGeocacheFilter.StatusType.HAS_TRACKABLE));
                statusFilter.setExcludeActive(true);
                return statusFilter;
            case ATTRIBUTES:
                final AttributesGeocacheFilter attribFilter = AttributesGeocacheFilter.create(
                        Collections.singletonList(CacheAttribute.AIRCRAFT),
                        Collections.singletonList(CacheAttribute.WADING));
                attribFilter.setInverse(true);
                attribFilter.setSources(5);
                return attribFilter;
            case OFFLINE_LOG:
                return OfflineLogGeocacheFilter.create("abc", false, StringFilter.StringFilterType.CONTAINS);
            case FAVORITES:
                final FavoritesGeocacheFilter favFilter = type.create();
                favFilter.setPercentage(true);
                return favFilter;
            case DISTANCE:
                final DistanceGeocacheFilter distFilter = DistanceGeocacheFilter.create(1f, null);
                distFilter.setCoordinate(Geopoint.ZERO);
                distFilter.setUseCurrentPosition(true);
                return distFilter;
            case HIDDEN:
            case EVENT_DATE:
                final HiddenGeocacheFilter eventDateFilter = HiddenGeocacheFilter.create(new Date(123), new Date(456));
                eventDateFilter.setRelativeMinMaxDays(-5, 5);
                return eventDateFilter;
            case LOGS_COUNT:
                return LogsCountGeocacheFilter.create(LogType.FOUND_IT, 1, 500);
            case LAST_FOUND:
                final LastFoundGeocacheFilter lastFoundFilter = LastFoundGeocacheFilter.create(new Date(123), new Date(456));
                lastFoundFilter.setRelativeMinMaxDays(-5, 5);
                return lastFoundFilter;
            case LOG_ENTRY:
                final LogEntryGeocacheFilter logEntryFilter = LogEntryGeocacheFilter.create("logtext", "user");
                logEntryFilter.setInverse(true);
                return logEntryFilter;
            case LOCATION:
                return LocationGeocacheFilter.create("abc", false, StringFilter.StringFilterType.CONTAINS);
            case STORED_LISTS:
                return StoredListGeocacheFilter.create(DataStore.getLists().get(0));
            case ORIGIN:
                return OriginGeocacheFilter.create(GCConnector.getInstance());
            case STORED_SINCE:
                return StoredSinceGeocacheFilter.create(null, new Date(1000));
            case CATEGORY:
                return CategoryGeocacheFilter.create(Category.BC_GADGET);
            case TIER:
                return TierGeocacheFilter.create(Tier.BC_BLUE, Tier.BC_GOLD);
            case INVENTORY_COUNT:
                return InventoryCountFilter.create(Collections.emptyList(), 1, 10);
            case DIFFICULTY:
            case TERRAIN:
            case INDIVIDUAL_ROUTE:
            case NAMED_FILTER:
            case LOGICAL_FILTER_GROUP:
            case LIST_ID:
            case VIEWPORT:
            case HEALTH_SCORE:
                return type.create();
            default:
                Assert.fail(String.format("Filter %s missing", type.getTypeId()));
                break;
        }
        return type.create();
    }
}
