package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.ICoordinates;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.SmileyImage;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.UnknownTagsHandler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

// TODO The suppression of this lint finding is bad. But to fix it, someone needs to rework the layout of the cache
// details also, not just only change the code here.
@SuppressLint("InflateParams")
public final class CacheDetailsCreator {
    private final Activity activity;
    private final ViewGroup parentView;
    private final Resources res;

    public CacheDetailsCreator(final Activity activity, final ViewGroup parentView) {
        this.activity = activity;
        this.res = activity.getResources();
        this.parentView = parentView;
        parentView.removeAllViews();
    }

    /**
     * Create a "name: value" line.
     *
     * @param nameId the resource of the name field
     * @param value the initial value
     * @return a pair made of the whole "name: value" line (to be able to hide it for example) and of the value (to update it)
     */
    public ImmutablePair<RelativeLayout, TextView> add(final int nameId, final CharSequence value) {
        final ImmutablePair<RelativeLayout, TextView> nameValue = createNameValueLine(nameId);
        nameValue.getRight().setText(value);
        return nameValue;
    }

    /**
     * Create a "name: value" line with html content.
     *
     * @param nameId the resource of the name field
     * @param value the initial value
     * @param geocode the geocode for image getter
     * @return a pair made of the whole "name: value" line (to be able to hide it for example) and of the value (to update it)
     */
    public ImmutablePair<RelativeLayout, TextView> addHtml(final int nameId, final CharSequence value, final String geocode) {
        final ImmutablePair<RelativeLayout, TextView> nameValue = createNameValueLine(nameId);
        final TextView valueView = nameValue.getRight();
        valueView.setText(HtmlCompat.fromHtml(value.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY, new SmileyImage(geocode, valueView), new UnknownTagsHandler()), TextView.BufferType.SPANNABLE);
        return nameValue;
    }

    @NonNull
    private ImmutablePair<RelativeLayout, TextView> createNameValueLine(final int nameId) {
        final RelativeLayout layout = (RelativeLayout) activity.getLayoutInflater().inflate(R.layout.cache_information_item, null, false);
        final TextView nameView = layout.findViewById(R.id.name);
        nameView.setText(res.getString(nameId));
        final TextView valueView = layout.findViewById(R.id.value);
        parentView.addView(layout);
        return ImmutablePair.of(layout, valueView);
    }

    public RelativeLayout addStars(final int nameId, final float value) {
        return addStars(nameId, value, 5);
    }

    private RelativeLayout addStars(final int nameId, final float value, final int max) {
        final RelativeLayout layout = (RelativeLayout) activity.getLayoutInflater().inflate(R.layout.cache_information_item, null, false);
        final TextView nameView = layout.findViewById(R.id.name);
        final TextView valueView = layout.findViewById(R.id.value);

        nameView.setText(activity.getString(nameId));
        valueView.setText(String.format(Locale.getDefault(), activity.getString(R.string.cache_rating_of_new), value, max));

        final RatingBar layoutStars = layout.findViewById(R.id.stars);
        layoutStars.setNumStars(max);
        layoutStars.setRating(value);
        layoutStars.setVisibility(View.VISIBLE);

        parentView.addView(layout);
        return layout;
    }

    public void addCacheState(final Geocache cache) {
        final List<String> states = new ArrayList<>(5);
        String date = getVisitedDate(cache);
        if (cache.isLogOffline()) {
            states.add(res.getString(R.string.cache_status_offline_log) + date);
            // reset the found date, to avoid showing it twice
            date = "";
        }
        if (cache.isFound()) {
            states.add(res.getString(R.string.cache_status_found) + date);
        } else if (cache.isDNF()) {
            states.add(res.getString(R.string.cache_not_status_found) + date);
        }
        if (cache.isEventCache() && states.isEmpty()) {
            for (final LogEntry log : cache.getLogs()) {
                if (log.getType() == LogType.WILL_ATTEND && log.isOwn()) {
                    states.add(LogType.WILL_ATTEND.getL10n());
                }
            }
        }
        if (cache.isArchived()) {
            states.add(res.getString(R.string.cache_status_archived));
        }
        if (cache.isDisabled()) {
            states.add(res.getString(R.string.cache_status_disabled));
        }
        if (cache.isPremiumMembersOnly()) {
            states.add(res.getString(R.string.cache_status_premium));
        }
        if (!states.isEmpty()) {
            add(R.string.cache_status, StringUtils.join(states, ", "));
        }
    }

    private static String getVisitedDate(final Geocache cache) {
        final long visited = cache.getVisitedDate();
        return visited != 0 ? " (" + Formatter.formatShortDate(visited) + ")" : "";
    }

    private static Float distanceNonBlocking(final ICoordinates target) {
        if (target.getCoords() == null) {
            return null;
        }
        return Sensors.getInstance().currentGeo().getCoords().distanceTo(target);
    }

    @SuppressLint("SetTextI18n")
    public void addRating(final Geocache cache) {
        if (cache.getRating() > 0) {
            final RelativeLayout itemLayout = addStars(R.string.cache_rating, cache.getRating());
            if (cache.getVotes() > 0) {
                final TextView itemAddition = itemLayout.findViewById(R.id.addition);
                itemAddition.setText(" (" + cache.getVotes() + ')');
                itemAddition.setVisibility(View.VISIBLE);
            }
        }
    }

    public void addSize(final Geocache cache) {
        if (cache.showSize()) {
            add(R.string.cache_size, cache.getSize().getL10n());
        }
    }

    public void addDifficulty(final Geocache cache) {
        if (cache.getDifficulty() > 0) {
            addStars(R.string.cache_difficulty, cache.getDifficulty());
        }
    }

    public void addTerrain(final Geocache cache) {
        if (cache.getTerrain() > 0) {
            addStars(R.string.cache_terrain, cache.getTerrain(), ConnectorFactory.getConnector(cache).getMaxTerrain());
        }
    }

    public TextView addDistance(final Geocache cache, final TextView cacheDistanceView) {
        Float distance = distanceNonBlocking(cache);
        if (distance == null && cache.getDistance() != null) {
            distance = cache.getDistance();
        }
        String text = "--";
        if (distance != null) {
            text = Units.getDistanceFromKilometers(distance);
        } else if (cacheDistanceView != null) {
            // if there is already a distance in cacheDistance, use it instead of resetting to default.
            // this prevents displaying "--" while waiting for a new position update (See bug #1468)
            text = cacheDistanceView.getText().toString();
        }
        return add(R.string.cache_distance, text).right;
    }

    public TextView addDistance(final Waypoint wpt, final TextView waypointDistanceView) {
        final Float distance = distanceNonBlocking(wpt);
        String text = "--";
        if (distance != null) {
            text = Units.getDistanceFromKilometers(distance);
        } else if (waypointDistanceView != null) {
            // if there is already a distance in waypointDistance, use it instead of resetting to default.
            // this prevents displaying "--" while waiting for a new position update (See bug #1468)
            text = waypointDistanceView.getText().toString();
        }
        return add(R.string.cache_distance, text).right;
    }

    public void addEventDate(@NonNull final Geocache cache) {
        if (!cache.isEventCache()) {
            return;
        }
        addHiddenDate(cache);
    }

    public TextView addHiddenDate(@NonNull final Geocache cache) {
        final String dateString = Formatter.formatHiddenDate(cache);
        if (StringUtils.isEmpty(dateString)) {
            return null;
        }
        final TextView view = add(cache.isEventCache() ? R.string.cache_event : R.string.cache_hidden, dateString).right;
        view.setId(R.id.date);
        return view;
    }
}
