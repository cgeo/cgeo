/**
 *
 */
package cgeo.geocaching.maps;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * @author keith.paterson
 *
 */
public class ProgressDialogWithThread extends ProgressDialog {

    /** Thread this class owns and can cancel. */
    private StoppableThread thread = null;

    public ProgressDialogWithThread(Context context, StoppableThread t) {
        super(context, ProgressDialog.STYLE_HORIZONTAL);
        setCancelable(true);
    }

    /**
     * Requests the owned Thread to stop.
     */

    protected void stopIt()
    {
        thread.stopIt();
    }

}
