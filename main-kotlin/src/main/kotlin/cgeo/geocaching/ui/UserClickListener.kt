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
import cgeo.geocaching.connector.UserAction
import cgeo.geocaching.connector.UserAction.UAContext
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.ui.dialog.ContextMenuDialog

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.View.OnClickListener

import androidx.annotation.NonNull

import java.util.List

abstract class UserClickListener : View.OnClickListener {

    private final UAContext user

    private UserClickListener(final UserAction.UAContext user) {
        this.user = user
    }

    override     public Unit onClick(final View view) {
        if (view == null) {
            return
        }

        showUserActionsDialog(view)
    }

    protected abstract List<UserAction> createUserActions(UserAction.UAContext user)

    /**
     * Opens a dialog to do actions on a user name
     */
    private Unit showUserActionsDialog(final View view) {
        val context: Context = view.getContext()
        user.setContext(context)

        val userActions: List<UserAction> = createUserActions(user)
        if (userActions.isEmpty()) {
            return
        }

        val res: Resources = context.getResources()

        val dialog: ContextMenuDialog = ContextMenuDialog((Activity) context)
        dialog.setTitle(res.getString(R.string.user_menu_title) + " " + user.userName)

        for (UserAction action : userActions) {
            dialog.addItem(action.displayResourceId, action.iconId, null)
        }
        dialog.setOnClickAction(pos -> userActions.get(pos).run(user))
        dialog.show()
    }

    public static UserClickListener forOwnerOf(final Geocache cache) {
        return forUser(cache, cache.getOwnerDisplayName(), cache.getOwnerUserId(), cache.getOwnerGuid())
    }

    public static OnClickListener forOwnerOf(final Trackable trackable) {
        return forUser(trackable, trackable.getOwner(), trackable.getOwnerGuid())
    }

    public static OnClickListener forUser(final Trackable trackable, final String userName, final String userGuid) {
        return UserClickListener(UAContext(userName, userName, userGuid, null)) {

            override             protected List<UserAction> createUserActions(final UserAction.UAContext user) {
                return ConnectorFactory.getConnector(trackable).getUserActions(user)
            }
        }
    }

    public static UserClickListener forUser(final Geocache cache, final String userName, final String userId, final String userGuid) {
        return UserClickListener(UAContext(userName, userId, userGuid, cache.getGeocode())) {

            override             protected List<UserAction> createUserActions(final UserAction.UAContext user) {
                return ConnectorFactory.getConnector(cache).getUserActions(user)
            }
        }
    }

}
