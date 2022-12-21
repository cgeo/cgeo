package cgeo.geocaching.gcvote;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.CacheVotingBar;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

/**
 * Small dialog showing only a rating bar to vote for the cache. Confirming the dialog will send the vote over the
 * network (in the background).
 */
public class VoteDialog {

    private VoteDialog() {
        // prevents calls from subclass throw new UnsupportedOperationException();
    }

    public static void show(final Activity context, @NonNull final Geocache cache, @Nullable final Runnable afterVoteSent) {
        final View votingLayout = View.inflate(context, R.layout.vote_dialog, null);

        final CacheVotingBar votingBar = new CacheVotingBar();

        final AlertDialog.Builder builder = Dialogs.newBuilder(context);
        builder.setView(votingLayout);
        builder.setPositiveButton(R.string.cache_menu_vote, (dialog, which) -> vote(cache, votingBar.getRating(), afterVoteSent));
        builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.dismiss());
        final AlertDialog dialog = builder.create();

        votingBar.initialize(cache, votingLayout, stars -> {
            final Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            // this listener might be fired already while the dialog is not yet shown
            if (button != null) {
                final IConnector connector = ConnectorFactory.getConnector(cache);

                button.setEnabled(connector instanceof IVotingCapability && ((IVotingCapability) connector).isValidRating(stars));
            }
        });
        dialog.show();
        final IConnector connector = ConnectorFactory.getConnector(cache);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(connector instanceof IVotingCapability && ((IVotingCapability) connector).isValidRating(cache.getMyVote()));
    }

    private static void vote(@NonNull final Geocache cache, final float rating, @Nullable final Runnable afterVoteSent) {
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {
            try {
                final IConnector connector = ConnectorFactory.getConnector(cache);
                if (connector instanceof IVotingCapability && ((IVotingCapability) connector).isValidRating(rating) && ((IVotingCapability) connector).supportsVoting(cache)) {
                    // send over network
                    if (((IVotingCapability) connector).postVote(cache, rating)) {
                        // store locally
                        cache.setMyVote(rating);
                        DataStore.saveChangedCache(cache);
                        return true;
                    }
                    Log.w("VoteDialog.vote: could not send vote");
                }
            } catch (final RuntimeException e) {
                Log.e("VoteDialog.vote: could not send vote", e);
            }

            return false;
        }, status -> {
            final Application context = CgeoApplication.getInstance();
            final String text = context.getString(status ? R.string.vote_sent : R.string.err_vote_send_rating);
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            if (afterVoteSent != null) {
                afterVoteSent.run();
            }
        });
    }

}
