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
import cgeo.geocaching.utils.config.LegacyFilterConfig;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeocacheFilterTest {

    @Test
    public void emptyFilter() {
        assertFilterFromConfig("", "", null);
        assertFilterFromConfig(null, "", null);
        assertFilterFromConfig("   ", "", null);
    }

    @Test
    public void emptyFilterWithName() {
        assertFilterFromConfig("[myname]", "myname", null);
        assertFilterFromConfig("[myname", "myname", null);
        assertFilterFromConfig("[]", "", null);
        assertFilterFromConfig("[test\\]\\:test]", "test]:test", null);
    }

    @Test
    public void emptyNameWithFilter() {
        assertFilterFromConfig("name", "", NameGeocacheFilter.class);
    }

    @Test
    public void bothfilled() {
        assertFilterFromConfig("[myfilter]name", "myfilter", NameGeocacheFilter.class);
        assertFilterFromConfig("[myfilter] name", "myfilter", NameGeocacheFilter.class);
        assertFilterFromConfig("[myfilter] AND(name)", "myfilter", AndGeocacheFilter.class);
    }

    @Test
    public void checkInconclusive() {
        GeocacheFilter filter = assertFilterFromConfig("[myfilter]name", "myfilter", NameGeocacheFilter.class);
        assertThat(filter.isIncludeInconclusive()).isFalse();
        filter = assertFilterFromConfig("[inconclusive=true:myfilter]name", "myfilter", NameGeocacheFilter.class);
        assertThat(filter.isIncludeInconclusive()).isTrue();
    }

    @Test
    public void checkAdvancedView() {
        GeocacheFilter filter = assertFilterFromConfig("[myfilter]name", "myfilter", NameGeocacheFilter.class);
        assertThat(filter.isOpenInAdvancedMode()).isFalse();
        filter = assertFilterFromConfig("[advanced=true:myfilter]name", "myfilter", NameGeocacheFilter.class);
        assertThat(filter.isOpenInAdvancedMode()).isTrue();
    }

    private GeocacheFilter assertFilterFromConfig(final String config, final String expectedName, final Class<? extends IGeocacheFilter> expectedFilterClass) {
        final GeocacheFilter filter;
        try {
            filter = LegacyFilterConfig.parseLegacy(null, config, true);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        assertThat(filter.getName()).as("name for ' " + config + "'").isEqualTo(expectedName);
        assertThat(filter.getTree() == null ? null : filter.getTree().getClass()).as("treeclass for ' " + config + "'").isEqualTo(expectedFilterClass);
        return filter;
    }

    @Test
    public void legacyParsing() throws ParseException {
        final String filterConfig = "[=:inconclusive=false:advanced=true]AND(rating:2.0:-;NOT(OR(status:has_user_defined_waypoints_yes:solved_mystery_yes;name:test:contains));difficulty_terrain_matrix:1.0-1.0:1.5-2.5:2.0-2.0:1.5-2.0:include-wo-dt=true)";
        final GeocacheFilter filter = LegacyFilterConfig.parseLegacy(null, filterConfig, true);

        final String json = filter.toConfig();
        final GeocacheFilter filter2 = GeocacheFilter.createFromConfig(json);
        final String config2 = filter2.toConfig();

        assertThat(config2).isEqualTo(json);
        //assertThat(json).isEqualTo("nojson");
        //assertThat(json.length()).isEqualTo(2);
    }

    @Test
    public void genericEmptyFilter() throws ParseException {

        final IGeocacheFilter filterTree = new AndGeocacheFilter();
        for (GeocacheFilterType type : GeocacheFilterType.values()) {
            filterTree.addChild(type.create());
        }

        final GeocacheFilter filter = GeocacheFilter.create("test", false, false, filterTree);
        assertFilterConfig(filter);
        assertThat(filter.getName()).isEqualTo("test");
    }

    @Test
    public void genericFilledFilter() throws ParseException {

        final IGeocacheFilter filterTree = new AndGeocacheFilter();
        for (GeocacheFilterType type : GeocacheFilterType.values()) {
            filterTree.addChild(getFilledInstance(type));
        }

        final GeocacheFilter filter = GeocacheFilter.create("test", true, true, filterTree);
        assertFilterConfig(filter);
        assertThat(filter.getName()).isEqualTo("test");
    }


    private static void assertFilterConfig(final GeocacheFilter filter) throws ParseException {
        final String filterJson = filter.toConfig();

        final GeocacheFilter filterFromJson = GeocacheFilter.createFromConfig(filterJson);
        assertThat(filterFromJson.toConfig()).isEqualTo(filterJson);
        assertThat(filterFromJson.getName()).isEqualTo(filter.getName());
        assertThat(filterFromJson.isIncludeInconclusive()).isEqualTo(filter.isIncludeInconclusive());
        assertThat(filterFromJson.isOpenInAdvancedMode()).isEqualTo(filter.isOpenInAdvancedMode());
        assertThat(filterFromJson.isFiltering()).isEqualTo(filter.isFiltering());
        assertThat(filterFromJson.toUserDisplayableString()).isEqualTo(filter.toUserDisplayableString());

        //legacy
        assertThat(LegacyFilterConfig.toLegacyConfig(filterFromJson)).isEqualTo(LegacyFilterConfig.toLegacyConfig(filter));
    }

    private static IGeocacheFilter getFilledInstance(final GeocacheFilterType type) {
        final IGeocacheFilter filter = type.create();
        switch (type) {
            case TYPE:
                ((TypeGeocacheFilter) filter).setValues(Arrays.asList(CacheType.TRADITIONAL, CacheType.MYSTERY));
                break;
            case NAME:
                ((NameGeocacheFilter) filter).getStringFilter().setTextValue("name");
                break;
            case OWNER:
                ((OwnerGeocacheFilter) filter).getStringFilter().setTextValue("owner");
                ((OwnerGeocacheFilter) filter).getStringFilter().setMatchCase(true);
                ((OwnerGeocacheFilter) filter).getStringFilter().setFilterType(StringFilter.StringFilterType.PATTERN);
                break;
            case DESCRIPTION:
                ((DescriptionGeocacheFilter) filter).getStringFilter().setTextValue(null);
                break;
            case SIZE:
                ((SizeGeocacheFilter) filter).setValues(Collections.singletonList(CacheSize.LARGE));
                break;
            case PERSONAL_NOTE:
                ((PersonalNoteGeocacheFilter) filter).getStringFilter().setMatchCase(true);
                break;
            case DIFFICULTY_TERRAIN:
                ((DifficultyAndTerrainGeocacheFilter) filter).difficultyGeocacheFilter.setMinMaxRange(1f, 3f);
                ((DifficultyAndTerrainGeocacheFilter) filter).difficultyGeocacheFilter.setSpecialNumber(0f, true);
                ((DifficultyAndTerrainGeocacheFilter) filter).terrainGeocacheFilter.setMinMaxRange(null, 4f);
                break;
            case DIFFICULTY_TERRAIN_MATRIX:
                ((DifficultyTerrainMatrixGeocacheFilter) filter).addDtCombi(1f, 4f);
                ((DifficultyTerrainMatrixGeocacheFilter) filter).addDtCombi(2f, 3f);
                ((DifficultyTerrainMatrixGeocacheFilter) filter).setIncludeCachesWoDt(false);
                break;
            case RATING:
                ((RatingGeocacheFilter) filter).setMinMaxRange(1f, null);
                break;
            case STATUS:
                ((StatusGeocacheFilter) filter).setStatusFound(true);
                ((StatusGeocacheFilter) filter).setExcludeActive(true);
                ((StatusGeocacheFilter) filter).setStatusHasTrackable(false);
                break;
            case ATTRIBUTES:
                final Map<CacheAttribute, Boolean> atts = new HashMap<>();
                atts.put(CacheAttribute.AIRCRAFT, true);
                atts.put(CacheAttribute.WADING, false);
                ((AttributesGeocacheFilter) filter).setAttributes(atts);
                ((AttributesGeocacheFilter) filter).setInverse(true);
                ((AttributesGeocacheFilter) filter).setSources(5);
                break;
            case OFFLINE_LOG:
                ((OfflineLogGeocacheFilter) filter).getStringFilter().setTextValue("abc");
                break;
            case FAVORITES:
                ((FavoritesGeocacheFilter) filter).setPercentage(true);
                break;
            case DISTANCE:
                ((DistanceGeocacheFilter) filter).setCoordinate(Geopoint.ZERO);
                ((DistanceGeocacheFilter) filter).setUseCurrentPosition(true);
                ((DistanceGeocacheFilter) filter).setMinMaxRange(1f, 5f);
                break;
            case HIDDEN:
            case EVENT_DATE:
                ((HiddenGeocacheFilter) filter).setMinMaxDate(new Date(123), new Date(456));
                ((HiddenGeocacheFilter) filter).setRelativeMinMaxDays(-5, 5);
                break;
            case LOGS_COUNT:
                ((LogsCountGeocacheFilter) filter).setLogType(LogType.FOUND_IT);
                ((LogsCountGeocacheFilter) filter).setMinMaxRange(1, 500);
                break;
            case LAST_FOUND:
                ((LastFoundGeocacheFilter) filter).setRelativeMinMaxDays(-5, 5);
                ((LastFoundGeocacheFilter) filter).setMinMaxDate(new Date(123), new Date(456));
                break;
            case LOG_ENTRY:
                ((LogEntryGeocacheFilter) filter).setInverse(true);
                ((LogEntryGeocacheFilter) filter).setFoundByUser("user");
                ((LogEntryGeocacheFilter) filter).setLogText("logtext");
                break;
            case LOCATION:
                ((LocationGeocacheFilter) filter).getStringFilter().setTextValue("abc");
                break;
            case STORED_LISTS:
                ((StoredListGeocacheFilter) filter).setFilterLists(Collections.singletonList(DataStore.getLists().get(0)));
                break;
            case ORIGIN:
                ((OriginGeocacheFilter) filter).setValues(Collections.singletonList(GCConnector.getInstance()));
                break;
            case STORED_SINCE:
                ((StoredSinceGeocacheFilter) filter).setMinMaxDate(null, new Date(1000));
                break;
            case CATEGORY:
                ((CategoryGeocacheFilter) filter).setCategories(Collections.singletonList(Category.BC_GADGET));
                break;
            case TIER:
                ((TierGeocacheFilter) filter).setValues(Arrays.asList(Tier.BC_BLUE, Tier.BC_GOLD));
                break;
            case INVENTORY_COUNT:
                ((InventoryCountFilter) filter).setRangeFromValues(Arrays.asList(), 1, 10);
                break;
            case DIFFICULTY:
            case TERRAIN:
            case INDIVIDUAL_ROUTE:
            case NAMED_FILTER:
            case LOGICAL_FILTER_GROUP:
            case LIST_ID:
            case VIEWPORT:
                // nothing to do
                break;
            default:
                Assert.fail(String.format("Filter %s missing", type.getTypeId()));
                break;
        }
        return filter;
    }
}
