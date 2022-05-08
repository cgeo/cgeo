package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * TODO: convert to fragment
 */
public class CacheVotingBar {

    private RatingBar ratingBar;
    private TextView label;
    private float rating;
    private IVotingCapability votingConnector;
    private OnRatingChangeListener changeListener;

    public void initialize(@NonNull final Geocache cache, final View parentView, @Nullable final OnRatingChangeListener changeListener) {
        this.changeListener = changeListener;
        this.ratingBar = parentView.findViewById(R.id.gcvoteRating);
        this.label = parentView.findViewById(R.id.voteLabel);

        this.votingConnector = null;
        final IConnector connector = ConnectorFactory.getConnector(cache);
        if (connector instanceof IVotingCapability && ((IVotingCapability) connector).supportsVoting(cache)) {
            this.votingConnector = (IVotingCapability) connector;
            ratingBar.setVisibility(View.VISIBLE);
            label.setVisibility(View.VISIBLE);
            ratingBar.setOnRatingBarChangeListener((ratingBar1, stars, fromUser) -> setRating(stars));
            //initialize setting
            this.rating = cache.getMyVote();
            ratingBar.setRating(this.rating);
        }
    }

    public float getRating() {
        return rating;
    }

    public void setRating(final float stars) {
        final float rating = votingConnector != null && votingConnector.isValidRating(stars) ? stars : 0;
        //don't continue if rating has not changed. See #9227
        if (Math.abs(this.rating - rating) < 0.1f) {
            return;
        }
        ratingBar.setRating(rating);
        label.setText(votingConnector == null ? "--" : votingConnector.getDescription(rating));
        this.rating = rating;
        if (changeListener != null) {
            changeListener.onRatingChanged(rating);
        }
    }

    public void validateVisibility(final Geocache cache, final LogType logType) {
        final int visible = (this.votingConnector != null && this.votingConnector.canVote(cache, logType)) ? View.VISIBLE : View.GONE;
        ratingBar.setVisibility(visible);
        label.setVisibility(visible);
    }


    public interface OnRatingChangeListener {
        void onRatingChanged(float stars);
    }

}
