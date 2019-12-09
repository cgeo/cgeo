package cgeo.geocaching.gcvote;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.gcvote.GCVoteRatingBarUtil.OnRatingChangeListener;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Callable;

import io.reactivex.functions.Consumer;

/**
 * Small dialog showing only a rating bar to vote on GCVote.com. Confirming the dialog will send the vote over the
 * network (in the background).
 */
public class GCVoteDialog {

    private GCVoteDialog() {
        // prevents calls from subclass throw new UnsupportedOperationException();
    }

    public static void show(final Activity context, @NonNull final Geocache cache, @Nullable final Runnable afterVoteSent) {
        final View votingLayout = View.inflate(context, R.layout.gcvote_dialog, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(votingLayout);
        builder.setPositiveButton(R.string.cache_menu_vote, (dialog, which) -> vote(cache, GCVoteRatingBarUtil.getRating(votingLayout), afterVoteSent));
        builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.dismiss());
        final AlertDialog dialog = builder.create();

        GCVoteRatingBarUtil.initializeRatingBar(cache, votingLayout, new OnRatingChangeListener() {

            @Override
            public void onRatingChanged(final float stars) {
                final Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                // this listener might be fired already while the dialog is not yet shown
                if (button != null) {
                    button.setEnabled(GCVote.isValidRating(stars));
                }
            }
        });
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(GCVote.isValidRating(cache.getMyVote()));
    }

    private static void vote(@NonNull final Geocache cache, final float rating, @Nullable final Runnable afterVoteSent) {
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    if (GCVote.isValidRating(rating) && GCVote.isVotingPossible(cache)) {
                        // send over network
                        if (GCVote.setRating(cache, rating)) {
                            // store locally
                            cache.setMyVote(rating);
                            DataStore.saveChangedCache(cache);
                            return true;
                        }
                        Log.w("GCVoteDialog.vote: could not send vote");
                    }
                } catch (final RuntimeException e) {
                    Log.e("GCVoteDialog.vote: could not send vote", e);
                }

                return false;
            }
        }, new Consumer<Boolean>() {
            @Override
            public void accept(final Boolean status) {
                final Application context = CgeoApplication.getInstance();
                final String text = context.getString(status ? R.string.gcvote_sent : R.string.err_gcvote_send_rating);
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                if (afterVoteSent != null) {
                    afterVoteSent.run();
                }
            }
        });
    }

}
