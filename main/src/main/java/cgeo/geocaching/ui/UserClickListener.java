package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.connector.UserAction.UAContext;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.ui.dialog.ContextMenuDialog;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;

import java.util.List;

public abstract class UserClickListener implements View.OnClickListener {

    @NonNull private final UAContext user;

    private UserClickListener(@NonNull final UserAction.UAContext user) {
        this.user = user;
    }

    @Override
    public void onClick(final View view) {
        if (view == null) {
            return;
        }

        showUserActionsDialog(view);
    }

    @NonNull
    protected abstract List<UserAction> createUserActions(UserAction.UAContext user);

    /**
     * Opens a dialog to do actions on a user name
     */
    private void showUserActionsDialog(final View view) {
        final Context context = view.getContext();
        user.setContext(context);

        final List<UserAction> userActions = createUserActions(user);
        if (userActions.isEmpty()) {
            return;
        }

        final Resources res = context.getResources();

        final ContextMenuDialog dialog = new ContextMenuDialog((Activity) context);
        dialog.setTitle(res.getString(R.string.user_menu_title) + " " + user.userName);

        for (UserAction action : userActions) {
            dialog.addItem(action.displayResourceId, action.iconId, null);
        }
        dialog.setOnClickAction(pos -> userActions.get(pos).run(user));
        dialog.show();
    }

    public static UserClickListener forOwnerOf(final Geocache cache) {
        return forUser(cache, cache.getOwnerDisplayName(), cache.getOwnerUserId(), cache.getOwnerGuid());
    }

    public static OnClickListener forOwnerOf(final Trackable trackable) {
        return forUser(trackable, trackable.getOwner(), trackable.getOwnerGuid());
    }

    public static OnClickListener forUser(final Trackable trackable, final String userName, final String userGuid) {
        return new UserClickListener(new UAContext(userName, userName, userGuid, null)) {

            @Override
            @NonNull
            protected List<UserAction> createUserActions(final UserAction.UAContext user) {
                return ConnectorFactory.getConnector(trackable).getUserActions(user);
            }
        };
    }

    public static UserClickListener forUser(final Geocache cache, final String userName, final String userId, final String userGuid) {
        return new UserClickListener(new UAContext(userName, userId, userGuid, cache.getGeocode())) {

            @Override
            @NonNull
            protected List<UserAction> createUserActions(final UserAction.UAContext user) {
                return ConnectorFactory.getConnector(cache).getUserActions(user);
            }
        };
    }

}
