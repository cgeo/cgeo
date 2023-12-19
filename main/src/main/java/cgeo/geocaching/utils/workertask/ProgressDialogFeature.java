package cgeo.geocaching.utils.workertask;

import cgeo.geocaching.activity.Progress;

import android.app.Activity;

import java.util.function.Consumer;
import java.util.function.Function;

/** Feature to surround execution of an activity worker task visually with a progress dialog */
public class ProgressDialogFeature<P> implements Consumer<WorkerTaskConfiguration<?, ?, ?>> {

    private final Activity activity;

    private String title = null;
    private String initialMessage = null;
    private boolean allowCancel = false;
    private boolean allowCloseWithoutCancel = false;
    private boolean indeterminate = true;

    private Function<P, String> messageMapper = p -> p == null ? null : p.toString();

    private int maxValue = 100;
    private Function<P, Integer> progressMapper = null;

    private ProgressDialogFeature(final Activity activity) {
        this.activity = activity;
    }

    public static <P> ProgressDialogFeature<P> of(final Activity activity, final Class<P> ignore) {
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
    @SuppressWarnings("unchecked")
    public void accept(final WorkerTaskConfiguration<?, ?, ?> taskConfiguration) {
        acceptInternal((WorkerTaskConfiguration<?, P, ?>) taskConfiguration);
    }


    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    private void acceptInternal(final WorkerTaskConfiguration<?, P, ?> taskConfig) {
        if (activity == null) {
            return;
        }
        final Progress progress = new Progress();

        //remember the task we associated the dialog with
        @SuppressWarnings("rawtypes")
        final WorkerTaskLogic[] taskStore = new WorkerTaskLogic[1];

        taskConfig.addTaskListener(event -> {
            final WorkerTaskLogic<?, P, ?> workerTask = event.task;

            switch (event.type) {
                case STARTED:
                case RECONNECTED:
                    if (allowCancel) {
                        progress.setOnCancelListener((dialog, which) -> {
                            if (allowCloseWithoutCancel) {
                                taskConfig.getTaskControl().cancel();
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
                            taskConfig.getTaskControl().cancel();
                        }
                    });

                    if (!indeterminate) {
                        progress.setMaxProgressAndReset(Math.max(0, maxValue));
                    }
                    taskStore[0] = workerTask;
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
                case DISCONNECTED:
                default:
                    if (progress.isShowing() && taskStore[0] == workerTask) {
                        progress.dismiss();
                        taskStore[0] = null;
                    }
                    break;
            }
        });

    }

}
