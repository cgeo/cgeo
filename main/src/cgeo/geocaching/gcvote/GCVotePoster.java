package cgeo.geocaching.gcvote;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;

import org.eclipse.jdt.annotation.Nullable;

import android.os.AsyncTask;
import android.widget.Toast;

class GCVotePoster extends AsyncTask<Void, Void, Boolean> {

    private final Geocache cache;
    private final float rating;
    private final @Nullable Runnable afterVoteSent;

    public GCVotePoster(final Geocache cache, final float rating, final @Nullable Runnable afterVoteSent) {
        this.cache = cache;
        this.rating = rating;
        this.afterVoteSent = afterVoteSent;
    }

    @Override
    protected Boolean doInBackground(final Void... inputs) {
        try {
            if (GCVote.isValidRating(rating) && GCVote.isVotingPossible(cache)) {
                // store locally
                cache.setMyVote(rating);
                DataStore.saveChangedCache(cache);

                // send over network
                return GCVote.setRating(cache, rating);
            }
        } catch (final RuntimeException e) {
            Log.e("GCVoteAsyncTask.doInBackground", e);
        }

        return false;
    }

    @Override
    protected void onPostExecute(final Boolean status) {
        final CgeoApplication context = CgeoApplication.getInstance();
        final String text = context.getString(status.booleanValue() ? R.string.gcvote_sent : R.string.err_gcvote_send_rating);
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        if (afterVoteSent != null) {
            afterVoteSent.run();
        }
    }
}