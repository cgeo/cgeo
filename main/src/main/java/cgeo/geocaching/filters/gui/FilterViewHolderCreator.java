package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.CategoryGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.HiddenGeocacheFilter;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.LastFoundGeocacheFilter;
import cgeo.geocaching.filters.core.NumberRangeGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.SizeGeocacheFilter;
import cgeo.geocaching.filters.core.StoredListGeocacheFilter;
import cgeo.geocaching.filters.core.TierGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.bettercacher.Category;
import cgeo.geocaching.models.bettercacher.Tier;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.app.Activity;
import android.widget.CheckBox;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FilterViewHolderCreator {

    private static boolean listInfoFilled = false;
    private static Collection<Geocache> listInfoFilteredList = Collections.emptyList();
    private static boolean listInfoIsComplete = false;

    // Store partial special event type selections per holder to persist across view updates
    private static final Map<CheckboxFilterViewHolder<?, ?>, Set<CacheType>> specialEventTypeSelections = new HashMap<>();

    private FilterViewHolderCreator() {
        //no instance
    }

    public static IFilterViewHolder<?> createFor(final IGeocacheFilter filter, final Activity activity) {
        return createFor(filter.getType(), activity, filter);
    }

    public static IFilterViewHolder<?> createFor(final GeocacheFilterType type, final Activity activity) {
        return createFor(type, activity, null);
    }

    private static IFilterViewHolder<?> createFor(final GeocacheFilterType type, final Activity activity, final IGeocacheFilter filter) {
        final IFilterViewHolder<?> result;
        switch (type) {
            case NAME:
            case OWNER:
            case DESCRIPTION:
            case PERSONAL_NOTE:
            case OFFLINE_LOG:
                result = new StringFilterViewHolder<>();
                break;
            case INVENTORY_COUNT:
                result = new NumberCountFilterViewHolder(0, 100);
                break;
            case TYPE:
                result = createTypeFilterViewHolder(activity);
                break;
            case SIZE:
                result = new ChipChoiceFilterViewHolder<>(
                        ValueGroupFilterAccessor.<CacheSize, SizeGeocacheFilter>createForValueGroupFilter()
                                .setSelectableValues(CacheSize.values())
                                .setValueDisplayTextGetter(CacheSize::getL10n));
                break;
            case DIFFICULTY:
            case TERRAIN:
            case RATING:
                result = create1to5ItemRangeSelectorViewHolder();
                break;
            case DIFFICULTY_TERRAIN:
                result = new DifficultyAndTerrainFilterViewHolder();
                break;
            case DIFFICULTY_TERRAIN_MATRIX:
                result = new DifficultyTerrainMatrixFilterViewHolder();
                break;
            case STATUS:
                result = new StatusFilterViewHolder();
                break;
            case INDIVIDUAL_ROUTE:
                result = new BooleanFilterViewHolder(R.string.cache_filter_boolean_individualroute_contained, ImageParam.id(R.drawable.map_quick_route));
                break;
            case ATTRIBUTES:
                result = new AttributesFilterViewHolder();
                break;
            case FAVORITES:
                result = new FavoritesFilterViewHolder();
                break;
            case DISTANCE:
                result = new DistanceFilterViewHolder();
                break;
            case HIDDEN:
                result = new DateRangeFilterViewHolder<HiddenGeocacheFilter>(true,
                        LocalizationUtils.getIntArray(R.array.cache_filter_hidden_since_stored_values_d),
                        LocalizationUtils.getStringArray(R.array.cache_filter_hidden_since_stored_values_label),
                        LocalizationUtils.getStringArray(R.array.cache_filter_hidden_since_stored_values_label_short));
                break;
            case EVENT_DATE:
                result = new DateRangeFilterViewHolder<HiddenGeocacheFilter>(true,
                        LocalizationUtils.getIntArray(R.array.cache_filter_event_date_stored_values_d),
                        LocalizationUtils.getStringArray(R.array.cache_filter_event_date_stored_values_label),
                        LocalizationUtils.getStringArray(R.array.cache_filter_event_date_stored_values_label_short));
                break;
            case LAST_FOUND:
                final int[] values = LocalizationUtils.getIntArray(R.array.cache_filter_hidden_since_stored_values_d);
                result = new DateRangeFilterViewHolder<LastFoundGeocacheFilter>(true,
                        values,
                        LocalizationUtils.getStringArray(R.array.cache_filter_hidden_since_stored_values_label),
                        LocalizationUtils.getStringArray(R.array.cache_filter_hidden_since_stored_values_label_short));
                break;
            case LOGS_COUNT:
                result = new LogsCountFilterViewHolder();
                break;
            case LOG_ENTRY:
                result = new LogEntryFilterViewHolder();
                break;
            case LOCATION:
                result = new StringFilterViewHolder<>(DataStore::getSuggestionsLocation);
                break;
            case STORED_LISTS:
                result = createStoredListFilterViewHolder();
                break;
            case NAMED_FILTER:
                result = new NamedFilterFilterViewHolder();
                break;
            case ORIGIN:
                result = new CheckboxFilterViewHolder<>(
                        ValueGroupFilterAccessor.<IConnector, OriginGeocacheFilter>createForValueGroupFilter()
                                .setSelectableValues(ConnectorFactory.getConnectors())
                                .setValueDisplayTextGetter(IConnector::getDisplayName)
                                .setValueDrawableGetter(ct -> ImageParam.id(R.drawable.ic_menu_upload)), 1,
                        new HashSet<>(ConnectorFactory.getActiveConnectors()));
                break;
            case STORED_SINCE:
                result = new DateRangeFilterViewHolder<HiddenGeocacheFilter>(true,
                        LocalizationUtils.getIntArray(R.array.cache_filter_stored_since_stored_values_d),
                        LocalizationUtils.getStringArray(R.array.cache_filter_stored_since_stored_values_label),
                        LocalizationUtils.getStringArray(R.array.cache_filter_stored_since_stored_values_label_short));
                break;
            case CATEGORY:
                result = new CheckboxFilterViewHolder<>(
                        new ValueGroupFilterAccessor<Category, CategoryGeocacheFilter>()
                                .setFilterValueGetter(CategoryGeocacheFilter::getCategories)
                                .setFilterValueSetter(CategoryGeocacheFilter::setCategories)
                                .setGeocacheValueGetter((f, c) -> new HashSet<>(c.getCategories()))
                                .setSelectableValues(Category.getAllCategoriesExceptUnknown())
                                .setValueDisplayTextGetter(Category::getI18nText)
                                .setValueDrawableGetter(c -> ImageParam.id(c.getIconId())),
                        2, null);
                break;
            case TIER:
                result = new CheckboxFilterViewHolder<>(
                        ValueGroupFilterAccessor.<Tier, TierGeocacheFilter>createForValueGroupFilter()
                                .setSelectableValues(Tier.values())
                                .setValueDisplayTextGetter(Tier::getI18nText)
                                .setValueDrawableGetter(t -> ImageParam.id(t.getIconId())),
                        2, null);
                break;
            case LOGICAL_FILTER_GROUP:
                result = new LogicalFilterViewHolder();
                break;
            default:
                result = null;
                break;
        }

        if (result == null) {
            return null;
        }

        result.init(type, activity);
        if (filter != null) {
            fillViewFrom(result, filter);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends IGeocacheFilter> void fillViewFrom(final IFilterViewHolder<T> viewHolder, final IGeocacheFilter filter) {
        if (viewHolder != null && filter != null) {
            viewHolder.setViewFromFilter((T) filter);
        }
    }

    public static <T extends IGeocacheFilter> IGeocacheFilter createFrom(final IFilterViewHolder<T> holder) {
        final IGeocacheFilter filter = holder.createFilterFromView();
        if (filter == null || filter.getType() == null) {
            throw new IllegalStateException("ViewHolder did not create valid filter: " + holder.getClass().getName());
        }
        return filter;
    }

    public static boolean isListInfoFilled() {
        return listInfoFilled;
    }

    public static boolean isListInfoComplete() {
        return listInfoIsComplete;
    }

    public static Collection<Geocache> getListInfoFilteredList() {
        return listInfoFilteredList;
    }

    public static void clearListInfo() {
        listInfoFilled = false;
        listInfoFilteredList = null;
        listInfoIsComplete = false;
    }

    public static void setListInfo(final Collection<Geocache> filteredList, final boolean isComplete) {
        listInfoFilled = filteredList != null && !filteredList.isEmpty();
        listInfoFilteredList = filteredList == null ? Collections.emptyList() : filteredList;
        listInfoIsComplete = isComplete;
    }

    private static IFilterViewHolder<?> create1to5ItemRangeSelectorViewHolder() {
        final Float[] range = new Float[]{1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f};
        return new ItemRangeSelectorViewHolder<>(
                new ValueGroupFilterAccessor<Float, NumberRangeGeocacheFilter<Float>>()
                        .setSelectableValues(range)
                        .setFilterValueGetter(f -> f.getValuesInRange(range))
                        .setFilterValueSetter((f, v) -> f.setRangeFromValues(v, 1f, 5f))
                        .setValueDisplayTextGetter(f -> String.format(Locale.getDefault(), "%.1f", f)),
                (i, f) -> i % 2 == 0 ? String.format(Locale.getDefault(), "%.1f", f) : null);
    }

    private static IFilterViewHolder<?> createStoredListFilterViewHolder() {

        final List<StoredList> allLists = DataStore.getLists();
        final Map<Integer, StoredList> allListsById = new HashMap<>();
        for (StoredList list : allLists) {
            allListsById.put(list.id, list);
        }

        final ValueGroupFilterAccessor<StoredList, StoredListGeocacheFilter> vgfa =
                new ValueGroupFilterAccessor<StoredList, StoredListGeocacheFilter>()
                        .setSelectableValues(allLists)
                        .setFilterValueGetter(StoredListGeocacheFilter::getFilterLists)
                        .setFilterValueSetter(StoredListGeocacheFilter::setFilterLists)
                        .setValueDrawableGetter(f -> f.markerId > 0 ? ImageParam.emoji(f.markerId) : ImageParam.id(R.drawable.ic_menu_list))
                        .setValueDisplayTextGetter(f -> f.title)
                        .setGeocacheValueGetter((f, c) -> CollectionStream.of(c.getLists()).map(allListsById::get).toSet());

        return new StoredListsFilterViewHolder<>(vgfa, 1, Collections.emptySet());
    }

    private static IFilterViewHolder<?> createTypeFilterViewHolder(final Activity activity) {
        final CheckboxFilterViewHolder<CacheType, TypeGeocacheFilter> holder = new CheckboxFilterViewHolder<CacheType, TypeGeocacheFilter>(
                ValueGroupFilterAccessor.<CacheType, TypeGeocacheFilter>createForValueGroupFilter()
                        .setSelectableValues(Arrays.asList(CacheType.TRADITIONAL, CacheType.MULTI, CacheType.MYSTERY, CacheType.LETTERBOX, CacheType.EVENT,
                                CacheType.EARTH, CacheType.CITO, CacheType.WEBCAM, CacheType.COMMUN_CELEBRATION, CacheType.VIRTUAL, CacheType.WHERIGO, CacheType.UNKNOWN, CacheType.ADVLAB, CacheType.USER_DEFINED))
                        .setValueDisplayTextGetter(TypeGeocacheFilter::valueDisplayTextGetter)
                        .setValueDrawableGetter(ct -> ImageParam.drawable(MapMarkerUtils.getCacheTypeMarker(activity.getResources(), ct))),
                2, null) {
            @Override
            public TypeGeocacheFilter createFilterFromView() {
                final TypeGeocacheFilter filter = super.createFilterFromView();
                // Apply stored partial special event type selection if it exists
                final Set<CacheType> storedSelection = specialEventTypeSelections.get(this);
                if (storedSelection != null) {
                    filter.setSelectedSpecialEventTypes(storedSelection);
                }
                return filter;
            }

            @Override
            public void setViewFromFilter(final TypeGeocacheFilter filter) {
                // Store the partial selection from the filter before setting view
                final Set<CacheType> selection = filter.getSelectedSpecialEventTypes();
                if (selection != null && !selection.isEmpty()) {
                    specialEventTypeSelections.put(this, new HashSet<>(selection));
                } else {
                    specialEventTypeSelections.remove(this);
                }
                
                super.setViewFromFilter(filter);
                
                // Update the Specials checkbox visual state based on selection
                final CheckBox specialsCheckbox = getValueCheckbox(CacheType.COMMUN_CELEBRATION).right;
                updateSpecialsCheckboxState(specialsCheckbox, TypeGeocacheFilter.getAllSpecialEventTypes().size(), 
                                           selection != null ? selection.size() : 0);
            }
        };

        // Set up custom click handler ONLY for the "Specials" checkbox (COMMUN_CELEBRATION)
        holder.setCustomCheckboxClickHandler(
            (cacheType, checkbox) -> handleSpecialsCheckboxClick(activity, holder, checkbox),
            cacheType -> cacheType == CacheType.COMMUN_CELEBRATION
        );

        return holder;
    }

    private static void handleSpecialsCheckboxClick(final Activity activity, 
                                                     final CheckboxFilterViewHolder<CacheType, TypeGeocacheFilter> holder,
                                                     final CheckBox specialsCheckbox) {
        // Define the special event types that can be individually selected
        final Collection<CacheType> specialEventTypes = TypeGeocacheFilter.getAllSpecialEventTypes();

        // Get current selection from storage, or default based on checkbox state
        Set<CacheType> currentlySelected = specialEventTypeSelections.get(holder);
        if (currentlySelected == null) {
            // If no stored selection, check if Specials checkbox is checked
            if (specialsCheckbox.isChecked()) {
                // Default to all special events if checkbox is checked
                currentlySelected = new HashSet<>(specialEventTypes);
            } else {
                // Default to none if checkbox is unchecked
                currentlySelected = new HashSet<>();
            }
        }

        // Create and show selection dialog
        final SimpleDialog.ItemSelectModel<CacheType> model = new SimpleDialog.ItemSelectModel<>();
        model.setItems(specialEventTypes)
                .setDisplayMapper(ct -> TextParam.text(ct.getShortL10n()))
                .setChoiceMode(SimpleDialog.ItemSelectModel.ChoiceMode.MULTI_CHECKBOX)
                .setSelectedItems(currentlySelected);

        SimpleDialog.of(activity)
                .setTitle(TextParam.id(R.string.event_special))
                .selectMultiple(model, selectedTypes -> {
                    // Update the filter with the selected special event types
                    updateSpecialEventTypesInFilter(holder, specialEventTypes, selectedTypes, specialsCheckbox);
                });
    }

    private static void updateSpecialEventTypesInFilter(final CheckboxFilterViewHolder<CacheType, TypeGeocacheFilter> holder,
                                                        final Collection<CacheType> allSpecialTypes,
                                                        final Collection<CacheType> selectedSpecialTypes,
                                                        final CheckBox specialsCheckbox) {
        // Store the partial selection for this holder
        final Set<CacheType> selectedSet = new HashSet<>(selectedSpecialTypes);
        if (!selectedSet.isEmpty()) {
            specialEventTypeSelections.put(holder, selectedSet);
        } else {
            specialEventTypeSelections.remove(holder);
        }
        
        // Update the "Specials" checkbox state based on selection
        updateSpecialsCheckboxState(specialsCheckbox, allSpecialTypes.size(), selectedSpecialTypes.size());
        
        // Update the select all/none checkbox
        holder.checkAndSetAllNoneValue();
    }

    private static void updateSpecialsCheckboxState(final CheckBox specialsCheckbox,
                                                     final int totalSpecialTypes,
                                                     final int selectedCount) {
        if (selectedCount == 0) {
            // No special types selected - unchecked
            specialsCheckbox.setChecked(false);
            specialsCheckbox.setAlpha(1.0f);
        } else if (selectedCount == totalSpecialTypes) {
            // All special types selected - checked
            specialsCheckbox.setChecked(true);
            specialsCheckbox.setAlpha(1.0f);
        } else {
            // Partial selection - indeterminate state
            // Use checked state with reduced alpha to indicate partial selection
            specialsCheckbox.setChecked(true);
            specialsCheckbox.setAlpha(0.5f);
        }
    }
}

