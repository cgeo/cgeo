package cgeo.geocaching.ui;

import butterknife.ButterKnife;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Units;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// TODO The suppression of this lint finding is bad. But to fix it, someone needs to rework the layout of the cache
// details also, not just only change the code here.
@SuppressLint("InflateParams")
public final class CacheDetailsCreator {
    private final Activity activity;
    private final ViewGroup parentView;
    private TextView lastValueView;
    private final Resources res;

    public CacheDetailsCreator(final Activity activity, final ViewGroup parentView) {
        this.activity = activity;
        this.res = activity.getResources();
        this.parentView = parentView;
        parentView.removeAllViews();
    }

    /**
     * @param nameId
     * @param value
     * @return the view containing the displayed string (i.e. the right side one from the pair of "label": "value")
     */
    public TextView add(final int nameId, final CharSequence value) {
        final RelativeLayout layout = (RelativeLayout) activity.getLayoutInflater().inflate(R.layout.cache_information_item, null, false);
        final TextView nameView = ButterKnife.findById(layout, R.id.name);
        nameView.setText(res.getString(nameId));
        lastValueView = ButterKnife.findById(layout, R.id.value);
        lastValueView.setText(value);
        parentView.addView(layout);
        return lastValueView;
    }

    public TextView getValueView() {
        return lastValueView;
    }

    public RelativeLayout addStars(final int nameId, final float value) {
        return addStars(nameId, value, 5);
    }

    public RelativeLayout addStars(final int nameId, final float value, final int max) {
        final RelativeLayout layout = (RelativeLayout) activity.getLayoutInflater().inflate(R.layout.cache_information_item, null, false);
        final TextView nameView = ButterKnife.findById(layout, R.id.name);
        lastValueView = ButterKnife.findById(layout, R.id.value);
        final LinearLayout layoutStars = ButterKnife.findById(layout, R.id.stars);

        nameView.setText(activity.getResources().getString(nameId));
        lastValueView.setText(String.format("%.1f", value) + ' ' + activity.getResources().getString(R.string.cache_rating_of) + " " + String.format("%d", max));
        createStarImages(layoutStars, value, max);
        layoutStars.setVisibility(View.VISIBLE);

        parentView.addView(layout);
        return layout;
    }

    private void createStarImages(final ViewGroup starsContainer, final float value, final int max) {
        final LayoutInflater inflater = LayoutInflater.from(activity);

        for (int i = 0; i < max; i++) {
            final ImageView star = (ImageView) inflater.inflate(R.layout.star_image, starsContainer, false);
            if (value - i >= 0.75) {
                star.setImageResource(R.drawable.star_on);
            } else if (value - i >= 0.25) {
                star.setImageResource(R.drawable.star_half);
            } else {
                star.setImageResource(R.drawable.star_off);
            }
            starsContainer.addView(star);
        }
    }

    public void addCacheState(final Geocache cache) {
        if (cache.isLogOffline() || cache.isArchived() || cache.isDisabled() || cache.isPremiumMembersOnly() || cache.isFound()) {
            final List<String> states = new ArrayList<>(5);
            if (cache.isLogOffline()) {
                states.add(res.getString(R.string.cache_status_offline_log));
            }
            if (cache.isFound()) {
                states.add(res.getString(R.string.cache_status_found));
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
            add(R.string.cache_status, StringUtils.join(states, ", "));
        }
    }

    public void addRating(final Geocache cache) {
        if (cache.getRating() > 0) {
            final RelativeLayout itemLayout = addStars(R.string.cache_rating, cache.getRating());
            if (cache.getVotes() > 0) {
                final TextView itemAddition = ButterKnife.findById(itemLayout, R.id.addition);
                itemAddition.setText("(" + cache.getVotes() + ")");
                itemAddition.setVisibility(View.VISIBLE);
            }
        }
    }

    public void addSize(final Geocache cache) {
        if (null != cache.getSize() && cache.showSize()) {
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

    public void addDistance(final Geocache cache, final TextView cacheDistanceView) {
        Float distance = null;
        if (cache.getCoords() != null) {
            final Geopoint currentCoords = CgeoApplication.getInstance().currentGeo().getCoords();
            if (currentCoords != null) {
                distance = currentCoords.distanceTo(cache);
            }
        }
        if (distance == null) {
            if (cache.getDistance() != null) {
                distance = cache.getDistance();
            }
        }
        String text = "--";
        if (distance != null) {
            text = Units.getDistanceFromKilometers(distance);
        }
        else if (cacheDistanceView != null) {
            // if there is already a distance in cacheDistance, use it instead of resetting to default.
            // this prevents displaying "--" while waiting for a new position update (See bug #1468)
            text = cacheDistanceView.getText().toString();
        }
        add(R.string.cache_distance, text);
    }

    public void addDistance(final Waypoint wpt, final TextView waypointDistanceView) {
        Float distance = null;
        if (wpt.getCoords() != null) {
            final Geopoint currentCoords = CgeoApplication.getInstance().currentGeo().getCoords();
            if (currentCoords != null) {
                distance = currentCoords.distanceTo(wpt);
            }
        }
        String text = "--";
        if (distance != null) {
            text = Units.getDistanceFromKilometers(distance);
        }
        else if (waypointDistanceView != null) {
            // if there is already a distance in waypointDistance, use it instead of resetting to default.
            // this prevents displaying "--" while waiting for a new position update (See bug #1468)
            text = waypointDistanceView.getText().toString();
        }
        add(R.string.cache_distance, text);
    }

    public void addEventDate(@NonNull final Geocache cache) {
        if (!cache.isEventCache()) {
            return;
        }
        addHiddenDate(cache);
    }

    public TextView addHiddenDate(final @NonNull Geocache cache) {
        final Date hiddenDate = cache.getHiddenDate();
        if (hiddenDate == null) {
            return null;
        }
        final long time = hiddenDate.getTime();
        if (time > 0) {
            String dateString = Formatter.formatFullDate(time);
            if (cache.isEventCache()) {
                dateString = DateUtils.formatDateTime(CgeoApplication.getInstance().getBaseContext(), time, DateUtils.FORMAT_SHOW_WEEKDAY) + ", " + dateString;
            }
            final TextView view = add(cache.isEventCache() ? R.string.cache_event : R.string.cache_hidden, dateString);
            view.setId(R.id.date);
            return view;
        }
        return null;
    }
}
