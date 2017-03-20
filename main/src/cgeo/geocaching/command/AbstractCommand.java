package cgeo.geocaching.command;

import cgeo.geocaching.utils.AsyncTaskWithProgress;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of the command interface with undo support and background processing.
 */
public abstract class AbstractCommand implements Command {

    @NonNull
    private final Activity context;
    private String progressMessage = null;

    protected AbstractCommand(@NonNull final Activity context, @StringRes final int progressMessageId) {
        this.context = context;
        if (progressMessageId != 0) {
            this.progressMessage = context.getString(progressMessageId);
        }
    }

    protected AbstractCommand(final Activity context) {
        this(context, 0);
    }

    /**
     * checks whether the command can be executed, before running the {@link #execute()} method
     */
    protected boolean canExecute() {
        return true;
    }

    /**
     * Implements the actual work of the command.
     * <p>
     * This runs in a <b>non</b> UI thread.
     * </p>
     */
    protected abstract void doCommand();

    /**
     * Reverse operation to undo the effects of {@link #doCommand()}.
     * <p>
     * This runs in a <b>non</b> UI thread.
     * </p>
     */
    protected abstract void undoCommand();

    /**
     * Called after the execution of {@link #doCommand()} or {@link #undoCommand()} finished.
     * <p>
     * This runs in the UI thread.
     * </p>
     */
    protected abstract void onFinished();

    /**
     * Called after the execution of {@link #undoCommand()} finished. By default, calls {@link #onFinished()}.
     * <p>
     * This runs in the UI thread.
     * </p>
     */
    protected void onFinishedUndo() {
        onFinished();
    }

    /**
     * Message to be shown as toast when the command finished. Can be {@code null}, if the operation does not support
     * undo.
     */
    @Nullable
    protected abstract String getResultMessage();

    @NonNull
    protected Activity getContext() {
        return context;
    }

    @Override
    public void execute() {
        if (!canExecute()) {
            return;
        }
        final AsyncTaskWithProgress<Void, Void> task = new ActionAsyncTask(context, null, progressMessage, true);
        task.execute();
    }

    public final void setProgressMessage(final String progressMessage) {
        this.progressMessage = progressMessage;
    }

    private final class ActionAsyncTask extends AsyncTaskWithProgress<Void, Void> implements View.OnClickListener {
        private ActionAsyncTask(final Activity activity, final String progressTitle, final String progressMessage, final boolean indeterminate) {
            super(activity, progressTitle, progressMessage, indeterminate);
        }

        @Override
        protected Void doInBackgroundInternal(final Void[] params) {
            doCommand();
            return null;
        }

        @Override
        protected void onPostExecuteInternal(final Void result) {
            onFinished();
            final String resultMessage = getResultMessage();
            showUndoToast(resultMessage);
        }

        private void showUndoToast(final String resultMessage) {
            if (StringUtils.isNotEmpty(resultMessage)) {
                Snackbar.make(context.findViewById(android.R.id.content), resultMessage, Snackbar.LENGTH_LONG).setAction("Undo", this).show();
            }
        }

        @Override
        public void onClick(final View view) {
            new UndoTask(context).execute();
        }
    }

    private final class UndoTask extends AsyncTaskWithProgress<Void, Void> {

        UndoTask(final Activity activity) {
            super(activity, null, null, true);
        }

        @Override
        protected Void doInBackgroundInternal(final Void[] params) {
            undoCommand();
            return null;
        }

        @Override
        protected void onPostExecuteInternal(final Void result) {
            onFinishedUndo();
        }

    }

}
