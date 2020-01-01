package cgeo.geocaching.gcvote;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * TODO: convert to fragment
 *
 */
public final class GCVoteRatingBarUtil {
    public interface OnRatingChangeListener {
        void onRatingChanged(float stars);
    }

    private GCVoteRatingBarUtil() {
        // utility class
    }

    public static void initializeRatingBar(@NonNull final Geocache cache, final View parentView, @Nullable final OnRatingChangeListener changeListener) {
        if (GCVote.isVotingPossible(cache)) {
            final RatingBar ratingBar = parentView.findViewById(R.id.gcvoteRating);
            final TextView label = parentView.findViewById(R.id.gcvoteLabel);
            ratingBar.setVisibility(View.VISIBLE);
            label.setVisibility(View.VISIBLE);
            ratingBar.setOnRatingBarChangeListener((ratingBar1, stars, fromUser) -> {
                // 0.5 is not a valid rating, therefore we must limit
                final float rating = GCVote.isValidRating(stars) ? stars : 0;
                if (rating < stars) {
                    ratingBar1.setRating(rating);
                }
                label.setText(GCVote.getDescription(rating));
                if (changeListener != null) {
                    changeListener.onRatingChanged(rating);
                }
            });
            ratingBar.setRating(cache.getMyVote());
        }
    }

    static float getRating(final View parentView) {
        final RatingBar ratingBar = parentView.findViewById(R.id.gcvoteRating);
        return ratingBar.getRating();
    }

}
