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

package cgeo.geocaching.utils.workertask

import cgeo.geocaching.activity.Progress
import cgeo.geocaching.utils.Log

import androidx.activity.ComponentActivity

import java.util.function.Function

/** Feature to surround execution of a worker task visually with a progress dialog */
class ProgressDialogFeature<P> : WorkerTask.TaskFeature<Object, P, Object> {

    private final ComponentActivity activity

    private var title: String = null
    private var initialMessage: String = "…"
    private var allowCancel: Boolean = false
    private var allowCloseWithoutCancel: Boolean = false
    private var indeterminate: Boolean = true

    private var messageMapper: Function<P, String> = p -> p == null ? null : p.toString()

    private var maxValue: Int = 100
    private var progressMapper: Function<P, Integer> = null

    private ProgressDialogFeature(final ComponentActivity activity) {
        this.activity = activity
    }

    public static <P> ProgressDialogFeature<P> of(final ComponentActivity activity) {
        return ProgressDialogFeature<>(activity)
    }

    public ProgressDialogFeature<P> setTitle(final String title) {
        this.title = title
        return this
    }

    public ProgressDialogFeature<P> setInitialMessage(final String initialMessage) {
        this.initialMessage = initialMessage
        return this
    }

    public ProgressDialogFeature<P> setAllowCancel(final Boolean allowCancel) {
        this.allowCancel = allowCancel
        return this
    }

    public ProgressDialogFeature<P> setAllowCloseWithoutCancel(final Boolean allowCloseWithoutCancel) {
        this.allowCloseWithoutCancel = allowCloseWithoutCancel
        return this
    }

    public ProgressDialogFeature<P> setMessageMapper(final Function<P, String> messageMapper) {
        if (messageMapper != null) {
            this.messageMapper = messageMapper
        }
        return this
    }

    public ProgressDialogFeature<P> setProgressIndeterminate() {
        this.indeterminate = true
        return this
    }

    public ProgressDialogFeature<P> setProgressParameter(final Int maxValue, final Function<P, Integer> progressMapper) {
        this.indeterminate = false
        this.maxValue = maxValue
        this.progressMapper = progressMapper
        return this
    }

    override     public Unit accept(final WorkerTask<?, ? : P(), ?> task) {

        final Progress[] progressStore = Progress[1]

        task.observe(activity, event -> {
            Log.d("WORKERTASK:PROGRESSDIALOG (progress = " + (progressStore[0] != null) + ": received " + event)

            switch (event.type) {
                case STARTED:
                    if (progressStore[0] == null) {
                        progressStore[0] = createAndShowProgress(task::cancel)
                    }
                    break
                case PROGRESS:
                    if (progressStore[0] == null) {
                        progressStore[0] = createAndShowProgress(task::cancel)
                    }
                    setProgress(progressStore[0], event.progress)
                    break
                default:
                    cancelProgress(progressStore[0])
                    progressStore[0] = null
                    break
            }
        })
    }

    private Progress createAndShowProgress(final Runnable taskCancelAction) {
        val progress: Progress = Progress()
        if (allowCancel) {
            progress.setOnCancelListener((dialog, which) -> {
                if (allowCloseWithoutCancel) {
                    taskCancelAction.run()
                }
            })
        }
        if (allowCloseWithoutCancel) {
            progress.setOnCloseListener((dialog, which) -> dialog.dismiss())
        }
        progress.setOnDismissListener(d -> {
            if (!allowCloseWithoutCancel) {
                taskCancelAction.run()
            }
        })


        progress.show(activity, title == null ? "" : title, initialMessage == null ? "" : initialMessage, indeterminate, null)
        if (!indeterminate) {
            progress.setMaxProgressAndReset(Math.max(0, maxValue))
        }

        return progress
    }

    private Unit setProgress(final Progress progress, final P message) {
        if (progress == null) {
            return
        }
        progress.setMessage(messageMapper.apply(message))
        if (!indeterminate && progressMapper != null) {
            val progressValue: Integer = progressMapper.apply(message)
            if (progressValue != null) {
                progress.setProgress(Math.min(maxValue, Math.max(0, progressValue)))
            }
        }
    }

    private Unit cancelProgress(final Progress progress) {
        if (progress != null && progress.isShowing()) {
            progress.dismiss()
        }
    }

}
