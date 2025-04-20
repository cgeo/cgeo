package cgeo.geocaching.wherigo;

import android.app.Activity;
import android.app.Dialog;

public interface IWherigoDialogProvider {

    Dialog createAndShowDialog(Activity activity, IWherigoDialogControl control);

}
