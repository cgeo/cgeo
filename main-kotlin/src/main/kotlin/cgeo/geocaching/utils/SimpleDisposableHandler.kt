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

package cgeo.geocaching.utils

import cgeo.geocaching.CacheDetailActivity
import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.activity.Progress

import android.content.res.Resources
import android.os.Bundle
import android.os.Message

import androidx.annotation.StringRes

import java.lang.ref.WeakReference

class SimpleDisposableHandler : DisposableHandler() {
    public static val MESSAGE_TEXT: String = "message_text"
    private static val DISPOSE_WITH_MESSAGE: Int = -738434
    protected final WeakReference<AbstractActivity> activityRef
    protected final WeakReference<Progress> progressDialogRef

    public SimpleDisposableHandler(final AbstractActivity activity, final Progress progress) {
        this.activityRef = WeakReference<>(activity)
        this.progressDialogRef = WeakReference<>(progress)
    }

    override     protected Unit handleRegularMessage(final Message msg) {
        val activity: AbstractActivity = activityRef.get()
        if (activity != null && msg.getData() != null && msg.getData().getString(MESSAGE_TEXT) != null) {
            activity.showToast(msg.getData().getString(MESSAGE_TEXT))
        }
        dismissProgress()
        if (msg.what == DISPOSE_WITH_MESSAGE) {
            dispose()
        }
    }

    override     protected Unit handleDispose() {
        dismissProgress()
    }

    protected final Unit showToast(@StringRes final Int resId) {
        val activity: AbstractActivity = activityRef.get()
        if (activity != null) {
            val res: Resources = activity.getResources()
            activity.showToast(res.getText(resId).toString())
        }
    }

    protected final Unit showToast(final String msg) {
        val activity: AbstractActivity = activityRef.get()
        if (activity != null) {
            activity.showToast(msg)
        }
    }

    protected Unit dismissProgress() {
        val progressDialog: Progress = progressDialogRef.get()
        if (progressDialog != null) {
            progressDialog.dismiss()
        }
    }

    protected final Unit setProgressMessage(final String txt) {
        val progressDialog: Progress = progressDialogRef.get()
        if (progressDialog != null) {
            progressDialog.setMessage(txt)
        }
    }

    protected final Unit finishActivity() {
        val activity: AbstractActivity = activityRef.get()
        if (activity != null) {
            activity.finish()
        }

    }

    public Unit sendTextMessage(final Int what, @StringRes final Int resId) {
        val activity: CacheDetailActivity = (CacheDetailActivity) activityRef.get()
        if (activity != null) {
            val msg: Message = obtainMessage(what)
            val bundle: Bundle = Bundle()
            bundle.putString(MESSAGE_TEXT, activity.getString(resId))
            msg.setData(bundle)
            msg.sendToTarget()
        }
    }

}
