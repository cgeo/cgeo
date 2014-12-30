package cgeo.geocaching.gcvote;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.gcvote.GCVoteRatingBarUtil.OnRatingChangeListener;
import cgeo.geocaching.settings.Settings;

import org.eclipse.jdt.annotation.Nullable;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;

/**
 * Small dialog showing only a rating bar to vote on GCVote.com. Confirming the dialog will send the vote over the
 * network (in the background).
 */
public class GCVoteDialog {

    public static void show(final Activity context, final Geocache cache, final @Nullable Runnable afterVoteSent) {
        final Context themedContext;

        if (Settings.isLightSkin() && VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            themedContext = new ContextThemeWrapper(context, R.style.dark);
        } else {
            themedContext = context;
        }

        final View votingLayout = View.inflate(themedContext, R.layout.gcvote_dialog, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
        builder.setView(votingLayout);
        builder.setPositiveButton(R.string.cache_menu_vote, new OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                vote(cache, GCVoteRatingBarUtil.getRating(votingLayout), afterVoteSent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int whichButton) {
                dialog.dismiss();
            }
        });
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

    protected static void vote(final Geocache cache, final float rating, final @Nullable Runnable afterVoteSent) {
        new GCVotePoster(cache, rating, afterVoteSent).execute();
    }

}
