package cgeo.geocaching.wherigo;

import android.app.Activity;
import android.app.Dialog;

public interface IWherigoDialogProvider {

    Dialog createAndShowDialog(Activity activity, IWherigoDialogControl control);

    /** return TRUE if a currentl visible provider can "take over" what the given provider is shown. In that case
     * no new dialog is created, just a refresh event is triggered */
    default boolean canRefresh(IWherigoDialogProvider otherDialog) {
        return false;
    }

}
