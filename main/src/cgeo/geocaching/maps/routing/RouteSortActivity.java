package cgeo.geocaching.maps.routing;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class RouteSortActivity extends AbstractActivity {

    private ArrayAdapter<RouteItem> routeItemAdapter;
    private ArrayList<RouteItem> routeItems;
    private ListView listView;
    private boolean changed = false;
    private int lastActivatedPosition = -1;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setTitle(getString(R.string.map_sort_individual_route));

        routeItems = DataStore.loadRoute();

        routeItemAdapter = new ArrayAdapter<RouteItem>(this, 0, routeItems) {
            @NonNull
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                View v = convertView;
                if (null == convertView) {
                    v = getLayoutInflater().inflate(R.layout.twotexts_twobuttons_item, null, false);
                    ((ImageButton) v.findViewById(R.id.button_left)).setImageResource(R.drawable.ic_menu_up);
                    ((ImageButton) v.findViewById(R.id.button_right)).setImageResource(R.drawable.ic_menu_down);
                }
                setItem(v, position);
                return v;
            }
        };
        listView = new ListView(this);
        setContentView(listView);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(final AbsListView view, final int scrollState) {
                // nothing to do here
            }

            @Override
            public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
                setItemRestoreButtons();
            }
        });
        listView.setAdapter(routeItemAdapter);
    }

    @SuppressLint("SetTextI18n")
    private void setItem(final View v, final int position) {
        final RouteItem routeItem = routeItems.get(position);
        final IWaypoint data = routeItem.getType() == CoordinatesType.CACHE ? DataStore.loadCache(routeItem.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB) : DataStore.loadWaypoint(routeItem.getId());

        final TextView title = v.findViewById(R.id.title);
        final TextView detail = v.findViewById(R.id.detail);
        if (null == data) {
            title.setText(routeItem.getGeocode());
            detail.setText(R.string.route_item_not_yet_loaded);
        } else {
            title.setText(data.getName());
            if (routeItem.getType() == CoordinatesType.CACHE) {
                assert data instanceof Geocache;
                detail.setText(Formatter.formatCacheInfoLong((Geocache) data));
                title.setCompoundDrawablesWithIntrinsicBounds(MapMarkerUtils.getCacheMarker(res, (Geocache) data, CacheListType.OFFLINE).getDrawable(), null, null, null);
            } else {
                assert data instanceof Waypoint;
                final Geocache cache = DataStore.loadCache(data.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                detail.setText(data.getGeocode() + Formatter.SEPARATOR + cache.getName());
                title.setCompoundDrawablesWithIntrinsicBounds(data.getWaypointType().markerId, 0, 0, 0);
            }
        }

        final View buttonUp = v.findViewById(R.id.button_left);
        buttonUp.setVisibility(position > 0 ? View.VISIBLE : View.INVISIBLE);
        buttonUp.setOnClickListener(vUp -> swap(position, position - 1));

        final ImageButton buttonDown = v.findViewById(R.id.button_right);
        buttonDown.setImageResource(R.drawable.ic_menu_down);
        buttonDown.setVisibility(position < routeItems.size() - 1 ? View.VISIBLE : View.INVISIBLE);
        buttonDown.setOnClickListener(vDown -> swap(position, position + 1));

        v.setOnTouchListener(new HorizontalSwipeDetector(position));
    };

    private void setItemOnLeftSwipe(final int position) {
        setItemRestoreButtons();

        final View v = listView.getChildAt(position - listView.getFirstVisiblePosition());
        if (null != v) {
            v.findViewById(R.id.button_left).setVisibility(View.INVISIBLE);

            final ImageButton buttonDelete = v.findViewById(R.id.button_right);
            buttonDelete.setImageResource(R.drawable.ic_menu_delete);
            buttonDelete.setVisibility(View.VISIBLE);
            buttonDelete.setOnClickListener(vDelete -> delete(position));

            lastActivatedPosition = position;
        }
    }

    private void setItemRestoreButtons() {
        if (lastActivatedPosition >= 0) {
            final int firstVisiblePosition = listView.getFirstVisiblePosition();
            if (lastActivatedPosition >= firstVisiblePosition && lastActivatedPosition <= listView.getLastVisiblePosition()) {
                setItem(listView.getChildAt(lastActivatedPosition - firstVisiblePosition), lastActivatedPosition);
            }
            lastActivatedPosition = -1;
        }
    }

    private void swap(final int position1, final int position2) {
        Collections.swap(routeItems, position1, position2);
        routeItemAdapter.notifyDataSetChanged();
        changed = true;
        invalidateOptionsMenu();
    }

    private void delete(final int position) {
        routeItems.remove(position);
        routeItemAdapter.notifyDataSetChanged();
        changed = true;
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.route_sort, menu);
        menu.findItem(R.id.save_sorted_route).setVisible(changed);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.save_sorted_route) {
            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.saveRoute(routeItems), () -> {
                changed = false;
                invalidateOptionsMenu();
                Toast.makeText(this, R.string.sorted_route_saved, Toast.LENGTH_SHORT).show();
            });
            return true;
        }
        return false;
    }

    private class HorizontalSwipeDetector implements View.OnClickListener, View.OnTouchListener {
        private GestureDetector gestureDetector;

        HorizontalSwipeDetector(final int position) {
            gestureDetector = new GestureDetector(routeItemAdapter.getContext(), new HorizontalSwipe(position));
        }

        // Tap on item
        @Override
        public void onClick(final View view) {
            setItemRestoreButtons();
        }

        // Swipe on item
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(final View view, final MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private class HorizontalSwipe extends GestureDetector.SimpleOnGestureListener {
            private static final int SWIPE_MIN_DISTANCE = 60;
            private static final int SWIPE_MAX_OFF_PATH = 100;
            private int position;

            HorizontalSwipe(final int position) {
                this.position = position;
            }

            @Override
            public boolean onDown(final MotionEvent event) {
                return true;
            }

            @Override
            public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
                try {
                    if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                        return false;
                    }
                    if (routeItemAdapter == null) {
                        return false;
                    }

                    // horizontal swipe
                    if (Math.abs(velocityX) > Math.abs(velocityY)) {
                        // left to right swipe
                        if ((e2.getX() - e1.getX()) > SWIPE_MIN_DISTANCE) {
                            setItemRestoreButtons();
                            return true;
                        }
                        // right to left swipe
                        if ((e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE) {
                            setItemOnLeftSwipe(position);
                            return true;
                        }
                    }
                } catch (final Exception e) {
                    // ignore
                }
                return false;
            }
        }

    }

}
