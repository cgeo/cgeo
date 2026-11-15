package cgeo.geocaching.wherigo;

import android.app.Activity;
import android.app.Dialog;

import java.util.Map;

public interface IWherigoDialogProvider {

    Dialog createAndShowDialog(Activity activity, IWherigoDialogControl control, Map<String, Object> state);

    /** return TRUE if a currentl visible provider can "take over" what the given provider is shown. In that case
     * no new dialog is created, just a refresh event is triggered */
    default boolean canRefresh(final IWherigoDialogProvider otherDialog) {
        return false;
    }

}
