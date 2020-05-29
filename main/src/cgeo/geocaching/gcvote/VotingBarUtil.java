package cgeo.geocaching.gcvote;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.models.Geocache;

import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * TODO: convert to fragment
 */
public final class VotingBarUtil {
    private VotingBarUtil() {
        // utility class
    }

    public static void initializeRatingBar(@NonNull final Geocache cache, final View parentView, @Nullable final OnRatingChangeListener changeListener) {
        final IConnector connector = ConnectorFactory.getConnector(cache);
        if (connector instanceof IVotingCapability && ((IVotingCapability) connector).supportsVoting(cache)) {
            final IVotingCapability votingConnector = (IVotingCapability) connector;
            final RatingBar ratingBar = parentView.findViewById(R.id.gcvoteRating);
            final TextView label = parentView.findViewById(R.id.voteLabel);
            ratingBar.setVisibility(View.VISIBLE);
            label.setVisibility(View.VISIBLE);
            ratingBar.setOnRatingBarChangeListener((ratingBar1, stars, fromUser) -> {
                final float rating = votingConnector.isValidRating(stars) ? stars : 0;
                if (rating < stars) {
                    ratingBar1.setRating(rating);
                }
                label.setText(votingConnector.getDescription(rating));
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

    public interface OnRatingChangeListener {
        void onRatingChanged(float stars);
    }

}
