package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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

    public TextView add(final int nameId, final CharSequence value) {
        final RelativeLayout layout = (RelativeLayout) activity.getLayoutInflater().inflate(R.layout.cache_item, null);
        final TextView nameView = (TextView) layout.findViewById(R.id.name);
        nameView.setText(res.getString(nameId));
        lastValueView = (TextView) layout.findViewById(R.id.value);
        lastValueView.setText(value);
        parentView.addView(layout);
        return lastValueView;
    }

    public TextView getValueView() {
        return lastValueView;
    }

    public RelativeLayout addStars(final int nameId, final float value) {
        final RelativeLayout layout = (RelativeLayout) activity.getLayoutInflater().inflate(R.layout.cache_layout, null);
        final TextView nameView = (TextView) layout.findViewById(R.id.name);
        lastValueView = (TextView) layout.findViewById(R.id.value);
        final LinearLayout layoutStars = (LinearLayout) layout.findViewById(R.id.stars);

        nameView.setText(activity.getResources().getString(nameId));
        lastValueView.setText(String.format("%.1f", value) + ' ' + activity.getResources().getString(R.string.cache_rating_of) + " 5");
        layoutStars.addView(createStarImages(value), 1);

        parentView.addView(layout);
        return layout;
    }

    private LinearLayout createStarImages(final float value) {
        final LayoutInflater inflater = LayoutInflater.from(activity);
        final LinearLayout starsContainer = new LinearLayout(activity);
        starsContainer.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < 5; i++) {
            ImageView star = (ImageView) inflater.inflate(R.layout.star, null);
            if (value - i >= 0.75) {
                star.setImageResource(R.drawable.star_on);
            } else if (value - i >= 0.25) {
                star.setImageResource(R.drawable.star_half);
            } else {
                star.setImageResource(R.drawable.star_off);
            }
            starsContainer.addView(star);
        }

        return starsContainer;
    }

    public void addCacheState(cgCache cache) {
        if (cache.isLogOffline() || cache.isArchived() || cache.isDisabled() || cache.isPremiumMembersOnly() || cache.isFound()) {
            final List<String> states = new ArrayList<String>(5);
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

    public void addRating(cgCache cache) {
        if (cache.getRating() > 0) {
            final RelativeLayout itemLayout = addStars(R.string.cache_rating, cache.getRating());
            if (cache.getVotes() > 0) {
                final TextView itemAddition = (TextView) itemLayout.findViewById(R.id.addition);
                itemAddition.setText("(" + cache.getVotes() + ")");
                itemAddition.setVisibility(View.VISIBLE);
            }
        }
    }

    public void addSize(cgCache cache) {
        if (null != cache.getSize() && cache.showSize()) {
            add(R.string.cache_size, cache.getSize().getL10n());
        }
    }

    public void addDifficulty(cgCache cache) {
        if (cache.getDifficulty() > 0) {
            addStars(R.string.cache_difficulty, cache.getDifficulty());
        }
    }

    public void addTerrain(cgCache cache) {
        if (cache.getTerrain() > 0) {
            addStars(R.string.cache_terrain, cache.getTerrain());
        }
    }
}
