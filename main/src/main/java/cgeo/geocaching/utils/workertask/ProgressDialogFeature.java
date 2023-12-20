package cgeo.geocaching.utils.workertask;

import cgeo.geocaching.activity.Progress;

import android.annotation.TargetApi;
import android.content.Context;

import java.util.function.Function;

/** Feature to surround execution of a worker task visually with a progress dialog */
@TargetApi(24)
public class ProgressDialogFeature<P> implements WorkerTask.TaskFeature<Object, P, Object> { //WorkerTask.TaskFeature<Object, P, Object> {

    private final Context activity;

    private String title = null;
    private String initialMessage = null;
    private boolean allowCancel = false;
    private boolean allowCloseWithoutCancel = false;
    private boolean indeterminate = true;

    private Function<P, String> messageMapper = p -> p == null ? null : p.toString();

    private int maxValue = 100;
    private Function<P, Integer> progressMapper = null;

    private ProgressDialogFeature(final Context activity) {
        this.activity = activity;
    }

    public static <P> ProgressDialogFeature<P> of(final Context activity) {
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

        final Progress progress = new Progress();

        task.addTaskListener(event -> {

            switch (event.type) {
                case STARTED:
                    if (allowCancel) {
                        progress.setOnCancelListener((dialog, which) -> {
                            if (allowCloseWithoutCancel) {
                                task.cancel();
                            }
                        });
                    }
                    if (allowCloseWithoutCancel) {
                        progress.setOnCloseListener((dialog, which) -> {
                            dialog.dismiss();
                        });
                    }
                    progress.setOnDismissListener(d -> {
                        if (!allowCloseWithoutCancel) {
                            task.cancel();
                        }
                    });

                    if (!indeterminate) {
                        progress.setMaxProgressAndReset(Math.max(0, maxValue));
                    }
                    progress.show(activity, title == null ? "" : title, initialMessage == null ? "" : initialMessage, indeterminate, null);
                    break;
                case PROGRESS:
                    progress.setMessage(messageMapper.apply(event.progress));
                    if (!indeterminate && progressMapper != null) {
                        final Integer progressValue = progressMapper.apply(event.progress);
                        if (progressValue != null) {
                            progress.setProgress(Math.min(maxValue, Math.max(0, progressValue)));
                        }
                    }
                    break;
                case FINISHED:
                case CANCELLED:
                default:
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    break;
            }
        });
    }

}
