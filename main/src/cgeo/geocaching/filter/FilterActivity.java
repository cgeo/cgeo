package cgeo.geocaching.filter;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.filter.FilterRegistry.FactoryEntry;
import cgeo.geocaching.utils.Log;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Show a filter selection using an {@code ExpandableListView}.
 */
@OptionsMenu(R.menu.filter_options)
@EActivity
public class FilterActivity extends AbstractActionBarActivity {

    public static final String EXTRA_FILTER_RESULT = null;
    public static final int REQUEST_SELECT_FILTER = 1234;

    private static final String KEY_FILTER_NAME = "filterName";
    private static final String KEY_FILTER_GROUP_NAME = "filterGroupName";

    @InjectView(R.id.filterList) protected ExpandableListView filterList;
    @InjectView(R.id.filters) protected LinearLayout filtersContainer;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.filter_activity);
        ButterKnife.inject(this);

        createListAdapter();
    }

    private void createListAdapter() {
        final SimpleExpandableListAdapter adapter =
                new SimpleExpandableListAdapter(
                        this,
                        // top level entries in the next 4 lines
                        createFilterTopLevelGroups(),
                        android.R.layout.simple_expandable_list_item_1,
                        new String[] { KEY_FILTER_GROUP_NAME },
                        new int[] { android.R.id.text1 },

                        // child level entries in the next 4 lines
                        createFilterChildren(),
                        android.R.layout.simple_expandable_list_item_2,
                        new String[] { KEY_FILTER_NAME, "CHILD_NAME" },
                        new int[] { android.R.id.text1 }
                );
        filterList.setAdapter(adapter);
        filterList.setOnChildClickListener(new OnChildClickListener() {

            @Override
            public boolean onChildClick(final ExpandableListView parent, final View v, final int groupPosition, final int childPosition, final long id) {
                setFilterResult(groupPosition, childPosition);
                return true;
            }

        });
    }

    public static @Nullable IFilter getFilterFromPosition(final int groupPosition, final int childPosition) {
        if (groupPosition < 0 || childPosition < 0) {
            return null;
        }
        final FactoryEntry factoryEntry = FilterRegistry.getInstance().getFactories().get(groupPosition);
        return createFilterFactory(factoryEntry.getFactory()).getFilters().get(childPosition);
    }

    /**
     * Creates the group list with the mapped properties.
     */
    private static List<Map<String, String>> createFilterTopLevelGroups() {
        final List<Map<String, String>> groups = new ArrayList<>();
        for (final FactoryEntry factoryEntry : FilterRegistry.getInstance().getFactories()) {
            final Map<String, String> map = new HashMap<>();
            map.put(KEY_FILTER_GROUP_NAME, factoryEntry.getName());
            groups.add(map);
        }
        return groups;
    }

    private static List<List<Map<String, String>>> createFilterChildren() {
        final List<List<Map<String, String>>> listOfChildGroups = new ArrayList<>();

        for (final FactoryEntry factoryEntry : FilterRegistry.getInstance().getFactories()) {
            final IFilterFactory factory = createFilterFactory(factoryEntry.getFactory());
            final List<? extends IFilter> filters = factory.getFilters();

            final List<Map<String, String>> childGroups = new ArrayList<>(filters.size());

            for (final IFilter filter : filters) {
                final Map<String, String> hashMap = new HashMap<>(1);
                hashMap.put(KEY_FILTER_NAME, filter.getName());
                hashMap.put("CHILD_NAME", filter.getName());
                childGroups.add(hashMap);
            }
            listOfChildGroups.add(childGroups);
        }
        return listOfChildGroups;
    }

    private static IFilterFactory createFilterFactory(final Class<? extends IFilterFactory> class1) {
        try {
            return class1.newInstance();
        } catch (final InstantiationException e) {
            Log.e("createFilterFactory", e);
        } catch (final IllegalAccessException e) {
            Log.e("createFilterFactory", e);
        }
        return null;
    }

    /**
     * After calling this method, the calling activity must implement onActivityResult, and check the
     * {@link #EXTRA_FILTER_RESULT}.
     */
    public static void selectFilter(@NonNull final Activity context) {
        context.startActivityForResult(new Intent(context, FilterActivity_.class), REQUEST_SELECT_FILTER);
    }

    @OptionsItem(R.id.menu_reset_filter)
    void resetFilter() {
        setFilterResult(-1, -1);
    }

    private void setFilterResult(final int groupPosition, final int childPosition) {
        final Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_FILTER_RESULT, new int[] { groupPosition, childPosition });
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
