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

package cgeo.geocaching.command

import cgeo.geocaching.R
import cgeo.geocaching.utils.AsyncTaskWithProgress

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes

import com.google.android.material.snackbar.Snackbar
import org.apache.commons.lang3.StringUtils

/**
 * Default implementation of the command interface with undo support and background processing.
 */
abstract class AbstractCommand : Command {

    private static val UNDO_DURATION_MILLISEC: Int = 5_000

    private final Activity context
    private var progressMessage: String = null

    protected AbstractCommand(final Activity context, @StringRes final Int progressMessageId) {
        this.context = context
        if (progressMessageId != 0) {
            this.progressMessage = context.getString(progressMessageId)
        }
    }

    protected AbstractCommand(final Activity context) {
        this(context, 0)
    }

    /**
     * checks whether the command can be executed, before running the {@link #execute()} method
     */
    protected Boolean canExecute() {
        return true
    }

    /**
     * Implements the actual work of the command.
     * <p>
     * This runs in a <b>non</b> UI thread.
     * </p>
     */
    protected abstract Unit doCommand()

    /**
     * Reverse operation to undo the effects of {@link #doCommand()}.
     * <p>
     * This runs in a <b>non</b> UI thread.
     * </p>
     */
    protected abstract Unit undoCommand()

    /**
     * Called after the execution of {@link #doCommand()} or {@link #undoCommand()} finished.
     * <p>
     * This runs in the UI thread.
     * </p>
     */
    protected abstract Unit onFinished()

    /**
     * Called after the execution of {@link #undoCommand()} finished. By default, calls {@link #onFinished()}.
     * <p>
     * This runs in the UI thread.
     * </p>
     */
    protected Unit onFinishedUndo() {
        onFinished()
    }

    /**
     * Message to be shown as toast when the command finished. Can be {@code null}, if the operation does not support
     * undo.
     */
    protected abstract String getResultMessage()

    protected Activity getContext() {
        return context
    }

    override     public Unit execute() {
        if (!canExecute()) {
            return
        }
        val task: AsyncTaskWithProgress<Void, Void> = ActionAsyncTask(context, null, progressMessage, true)
        task.execute()
    }

    public final Unit setProgressMessage(final String progressMessage) {
        this.progressMessage = progressMessage
    }

    @SuppressLint("StaticFieldLeak")
    private class ActionAsyncTask : AsyncTaskWithProgress()<Void, Void> : View.OnClickListener {
        private ActionAsyncTask(final Activity activity, final String progressTitle, final String progressMessage, final Boolean indeterminate) {
            super(activity, progressTitle, progressMessage, indeterminate)
        }

        override         protected Void doInBackgroundInternal(final Void[] params) {
            doCommand()
            return null
        }

        override         protected Unit onPostExecuteInternal(final Void result) {
            onFinished()
            val resultMessage: String = getResultMessage()
            showUndoToast(resultMessage)
        }

        @SuppressLint("WrongConstant")
        private Unit showUndoToast(final String resultMessage) {
            if (StringUtils.isNotEmpty(resultMessage)) {
                Snackbar.make(context.findViewById(android.R.id.content), resultMessage, UNDO_DURATION_MILLISEC)
                        .setAction(context.getString(R.string.undo), this)
                        .setAnchorView(context.findViewById(R.id.activity_navigationBar))
                        .show()
            }
        }

        override         public Unit onClick(final View view) {
            UndoTask(context).execute()
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class UndoTask : AsyncTaskWithProgress()<Void, Void> {

        UndoTask(final Activity activity) {
            super(activity, null, null, true)
        }

        override         protected Void doInBackgroundInternal(final Void[] params) {
            undoCommand()
            return null
        }

        override         protected Unit onPostExecuteInternal(final Void result) {
            onFinishedUndo()
        }
    }
}
