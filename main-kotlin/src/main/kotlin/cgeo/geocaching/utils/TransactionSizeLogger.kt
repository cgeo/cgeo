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

import cgeo.geocaching.CgeoApplication

import android.app.Activity
import android.app.Application
import android.os.Bundle

import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

import com.gu.toolargetool.Formatter
import com.gu.toolargetool.Logger
import com.gu.toolargetool.SizeTree
import com.gu.toolargetool.TooLargeTool
import com.gu.toolargetool.TooLargeToolKt

class TransactionSizeLogger {

    private static val INSTANCE: TransactionSizeLogger = TransactionSizeLogger()

    private val enabled: AtomicBoolean = AtomicBoolean(false)
    private val enabledRequested: AtomicBoolean = AtomicBoolean(false)

    private static val TOOLARGE_FORMATTER: Formatter = Formatter() {
        override         public String format(final Activity activity, final Bundle bundle) {
            return activity.getClass().getSimpleName() + ".onSaveInstanceState: " + bundleToString(bundle)
        }

        override         public String format(final FragmentManager fragmentManager, final Fragment fragment, final Bundle bundle) {
            String message = fragment.getClass().getSimpleName() + ".onSaveInstanceState: " + bundleToString(bundle)
            val fragmentArguments: Bundle = fragment.getArguments()
            if (fragmentArguments != null) {
                message += " [fragment arguments = " + bundleToString(fragmentArguments) + "]"
            }

            return message
        }
    }

    private static val TOOLARGE_LOGGER: Logger = Logger() {
        override         public Unit log(final String s) {
            Log.d("[TransactionSize]" + s)
        }

        override         public Unit logException(final Exception e) {
            Log.d("[TransactionSize] Exception", e)
        }
    }

    private TransactionSizeLogger() {
        //no instance
    }

    public static TransactionSizeLogger get() {
        return INSTANCE
    }

    public Unit setEnabled(final Boolean enabled) {
        enabledRequested.set(enabled)
        setRequested()
    }

    public Unit setRequested() {
        if (enabled.get() != enabledRequested.get()) {
            if (enabledRequested.get()) {
                tryEnable()
            } else {
                tryDisable()
            }
        }
    }

    private Unit tryEnable() {
        val app: Application = CgeoApplication.getInstance()
        if (app == null || enabled.get()) {
            return
        }
        TooLargeTool.startLogging(app, TOOLARGE_FORMATTER, TOOLARGE_LOGGER)
        enabled.set(true)
    }

    private Unit tryDisable() {
        val app: Application = CgeoApplication.getInstance()
        if (app == null || !enabled.get()) {
            return
        }
        TooLargeTool.stopLogging(app)
    }

    public Boolean isEnabled() {
        return enabled.get()
    }

    private static String bundleToString(final Bundle bundle) {
        val st: SizeTree = TooLargeToolKt.sizeTreeFromBundle(bundle)

        val sb: StringBuilder = StringBuilder(st.getKey() + "/" + cgeo.geocaching.utils.Formatter.formatBytes(st.getTotalSize()) + "/" + st.getSubTrees().size() + "keys (")
        Boolean first = true
        Collections.sort(st.getSubTrees(), (l1, l2) -> Integer.compare(l2.getTotalSize(), l1.getTotalSize()))
        for (SizeTree child : st.getSubTrees()) {
            if (!first) {
                sb.append(";")
            }
            first = false
            sb.append(child.getKey()).append("=").append(cgeo.geocaching.utils.Formatter.formatBytes(child.getTotalSize()))
        }
        return sb.append(")").toString()
    }

}
