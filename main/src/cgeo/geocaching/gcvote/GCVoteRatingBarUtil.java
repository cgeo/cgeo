package cgeo.geocaching.gcvote;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.view.View;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;

import butterknife.ButterKnife;

/**
 * TODO: convert to fragment
 *
 */
public final class GCVoteRatingBarUtil {
    public interface OnRatingChangeListener {
        void onRatingChanged(final float stars);
    }

    private GCVoteRatingBarUtil() {
        // utility class
    }

    public static void initializeRatingBar(@NonNull final Geocache cache, final View parentView, @Nullable final OnRatingChangeListener changeListener) {
        if (GCVote.isVotingPossible(cache)) {
            final RatingBar ratingBar = ButterKnife.findById(parentView, R.id.gcvoteRating);
            final TextView label = ButterKnife.findById(parentView, R.id.gcvoteLabel);
            ratingBar.setVisibility(View.VISIBLE);
            label.setVisibility(View.VISIBLE);
            ratingBar.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {

                @Override
                public void onRatingChanged(final RatingBar ratingBar, final float stars, final boolean fromUser) {
                    // 0.5 is not a valid rating, therefore we must limit
                    final float rating = GCVote.isValidRating(stars) ? stars : 0;
                    if (rating < stars) {
                        ratingBar.setRating(rating);
                    }
                    label.setText(GCVote.getDescription(rating));
                    if (changeListener != null) {
                        changeListener.onRatingChanged(rating);
                    }
                }
            });
            ratingBar.setRating(cache.getMyVote());
        }
    }

    static float getRating(final View parentView) {
        final RatingBar ratingBar = ButterKnife.findById(parentView, R.id.gcvoteRating);
        return ratingBar.getRating();
    }

}
