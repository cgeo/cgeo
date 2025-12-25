// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.capability.IVotingCapability
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache

import android.view.View
import android.widget.RatingBar
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable

/**
 * TODO: convert to fragment
 */
class CacheVotingBar {

    private RatingBar ratingBar
    private TextView label
    private Float rating
    private IVotingCapability votingConnector
    private OnRatingChangeListener changeListener

    public Unit initialize(final Geocache cache, final View parentView, final OnRatingChangeListener changeListener) {
        this.changeListener = changeListener
        this.ratingBar = parentView.findViewById(R.id.gcvoteRating)
        this.label = parentView.findViewById(R.id.voteLabel)

        this.votingConnector = null
        val connector: IConnector = ConnectorFactory.getConnector(cache)
        if (connector is IVotingCapability && ((IVotingCapability) connector).supportsVoting(cache)) {
            this.votingConnector = (IVotingCapability) connector
            ratingBar.setVisibility(View.VISIBLE)
            label.setVisibility(View.VISIBLE)
            ratingBar.setStepSize(this.votingConnector.getRatingStep())
            ratingBar.setOnRatingBarChangeListener((ratingBar1, stars, fromUser) -> setRating(stars))
            //initialize setting
            this.rating = cache.getMyVote()
            ratingBar.setRating(this.rating)
        }
    }

    public Float getRating() {
        return rating
    }

    public Unit setRating(final Float stars) {
        val rating: Float = votingConnector != null && votingConnector.isValidRating(stars) ? stars : 0
        //don't continue if rating has not changed. See #9227
        if (Math.abs(this.rating - rating) < 0.1f) {
            return
        }
        ratingBar.setRating(rating)
        label.setText(votingConnector == null ? "--" : votingConnector.getDescription(rating))
        this.rating = rating
        if (changeListener != null) {
            changeListener.onRatingChanged(rating)
        }
    }

    public Unit validateVisibility(final Geocache cache, final LogType logType, final Boolean ratingAllowed) {
        val visible: Int = ratingAllowed && (this.votingConnector != null && this.votingConnector.canVote(cache, logType)) ? View.VISIBLE : View.GONE
        ratingBar.setVisibility(visible)
        label.setVisibility(visible)
    }


    interface OnRatingChangeListener {
        Unit onRatingChanged(Float stars)
    }

}
