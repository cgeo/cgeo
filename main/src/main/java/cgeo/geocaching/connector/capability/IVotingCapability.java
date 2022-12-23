package cgeo.geocaching.connector.capability;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;


/**
 * Connector interface to implement for ability to vote for caches.
 */
public interface IVotingCapability extends IConnector {

    static String getDefaultFiveStarsDescription(final float rating) {
        final Application app = CgeoApplication.getInstance();
        switch (Math.round(rating * 2f)) {
            case 2:
                return app.getString(R.string.log_stars_1_description);
            case 3:
                return app.getString(R.string.log_stars_15_description);
            case 4:
                return app.getString(R.string.log_stars_2_description);
            case 5:
                return app.getString(R.string.log_stars_25_description);
            case 6:
                return app.getString(R.string.log_stars_3_description);
            case 7:
                return app.getString(R.string.log_stars_35_description);
            case 8:
                return app.getString(R.string.log_stars_4_description);
            case 9:
                return app.getString(R.string.log_stars_45_description);
            case 10:
                return app.getString(R.string.log_stars_5_description);
            default:
                return app.getString(R.string.log_no_rating);
        }
    }

    /**
     * Indicates whether voting is possible for this cache for this type of log entry
     */
    boolean canVote(@NonNull Geocache cache, @NonNull LogType logType);

    /**
     * Indicates whether voting is possible for this cache in general
     */
    boolean supportsVoting(@NonNull Geocache cache);

    /**
     * Indicates whether the given rating is acceptable (i.e. one might accept only integer values or restrict some other values)
     *
     * @param rating rating given
     * @return true if rating is acceptable, false if not
     */
    boolean isValidRating(float rating);

    /**
     * Returns string representation of the given rating
     *
     * @param rating current selected rating
     * @return short description of the value (like "awesome"/"poor" etc.)
     */
    String getDescription(float rating);


    /**
     * Posts single request to update the vote only. This is being send either while posting log
     * with rating or using menu "Vote" to post vote only.
     *
     * @param cache  cache to vote for
     * @param rating vote to set
     * @return status of the request
     */
    @WorkerThread
    boolean postVote(@NonNull Geocache cache, float rating);

}
