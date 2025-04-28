package cgeo.geocaching.utils.workertask;

import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.utils.Log;

import android.annotation.TargetApi;

import androidx.activity.ComponentActivity;

import java.util.function.Function;

/** Feature to surround execution of a worker task visually with a progress dialog */
@TargetApi(24)
public class ProgressDialogFeature<P> implements WorkerTask.TaskFeature<Object, P, Object> {

    private final ComponentActivity activity;

    private String title = null;
    private String initialMessage = "…";
    private boolean allowCancel = false;
    private boolean allowCloseWithoutCancel = false;
    private boolean indeterminate = true;

    private Function<P, String> messageMapper = p -> p == null ? null : p.toString();

    private int maxValue = 100;
    private Function<P, Integer> progressMapper = null;

    private ProgressDialogFeature(final ComponentActivity activity) {
        this.activity = activity;
    }

    public static <P> ProgressDialogFeature<P> of(final ComponentActivity activity) {
        return new ProgressDialogFeature<>(activity);
    }

    public ProgressDialogFeature<P> setTitle(final String title) {
        this.title = title;
        return this;
    }

    public ProgressDialogFeature<P> setInitialMessage(final String initialMessage) {
        this.initialMessage = initialMessage;
        return this;
    }

    public ProgressDialogFeature<P> setAllowCancel(final boolean allowCancel) {
        this.allowCancel = allowCancel;
        return this;
    }

    public ProgressDialogFeature<P> setAllowCloseWithoutCancel(final boolean allowCloseWithoutCancel) {
        this.allowCloseWithoutCancel = allowCloseWithoutCancel;
        return this;
    }

    public ProgressDialogFeature<P> setMessageMapper(final Function<P, String> messageMapper) {
        if (messageMapper != null) {
            this.messageMapper = messageMapper;
        }
        return this;
    }

    public ProgressDialogFeature<P> setProgressIndeterminate() {
        this.indeterminate = true;
        return this;
    }

    public ProgressDialogFeature<P> setProgressParameter(final int maxValue, final Function<P, Integer> progressMapper) {
        this.indeterminate = false;
        this.maxValue = maxValue;
        this.progressMapper = progressMapper;
        return this;
    }

    @Override
    public void accept(final WorkerTask<?, ? extends P, ?> task) {

        final Progress[] progressStore = new Progress[1];

        task.observe(activity, event -> {
            Log.d("WORKERTASK:PROGRESSDIALOG (progress = " + (progressStore[0] != null) + ": received " + event);

            switch (event.type) {
                case STARTED:
                    if (progressStore[0] == null) {
                        progressStore[0] = createAndShowProgress(task::cancel);
                    }
                    break;
                case PROGRESS:
                    if (progressStore[0] == null) {
                        progressStore[0] = createAndShowProgress(task::cancel);
                    }
                    setProgress(progressStore[0], event.progress);
                    break;
                default:
                    cancelProgress(progressStore[0]);
                    progressStore[0] = null;
                    break;
            }
        });
    }

    private Progress createAndShowProgress(final Runnable taskCancelAction) {
        final Progress progress = new Progress();
        if (allowCancel) {
            progress.setOnCancelListener((dialog, which) -> {
                if (allowCloseWithoutCancel) {
                    taskCancelAction.run();
                }
            });
        }
        if (allowCloseWithoutCancel) {
            progress.setOnCloseListener((dialog, which) -> dialog.dismiss());
        }
        progress.setOnDismissListener(d -> {
            if (!allowCloseWithoutCancel) {
                taskCancelAction.run();
            }
        });


        progress.show(activity, title == null ? "" : title, initialMessage == null ? "" : initialMessage, indeterminate, null);
        if (!indeterminate) {
            progress.setMaxProgressAndReset(Math.max(0, maxValue));
        }

        return progress;
    }

    private void setProgress(final Progress progress, final P message) {
        if (progress == null) {
            return;
        }
        progress.setMessage(messageMapper.apply(message));
        if (!indeterminate && progressMapper != null) {
            final Integer progressValue = progressMapper.apply(message);
            if (progressValue != null) {
                progress.setProgress(Math.min(maxValue, Math.max(0, progressValue)));
            }
        }
    }

    private void cancelProgress(final Progress progress) {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
    }

}
