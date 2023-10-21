package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gu.toolargetool.Formatter;
import com.gu.toolargetool.Logger;
import com.gu.toolargetool.SizeTree;
import com.gu.toolargetool.TooLargeTool;
import com.gu.toolargetool.TooLargeToolKt;

public class TransactionSizeLogger {

    private static final TransactionSizeLogger INSTANCE = new TransactionSizeLogger();

    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicBoolean enabledRequested = new AtomicBoolean(false);

    private static final Formatter TOOLARGE_FORMATTER = new Formatter() {
        @NonNull
        @Override
        public String format(@NonNull final Activity activity, @NonNull final Bundle bundle) {
            return activity.getClass().getSimpleName() + ".onSaveInstanceState: " + bundleToString(bundle);
        }

        @NonNull
        @Override
        public String format(@NonNull final FragmentManager fragmentManager, @NonNull final Fragment fragment, @NonNull final Bundle bundle) {
            String message = fragment.getClass().getSimpleName() + ".onSaveInstanceState: " + bundleToString(bundle);
            final Bundle fragmentArguments = fragment.getArguments();
            if (fragmentArguments != null) {
                message += " [fragment arguments = " + bundleToString(fragmentArguments) + "]";
            }

            return message;
        }
    };

    private static final Logger TOOLARGE_LOGGER = new Logger() {
        @Override
        public void log(@NonNull final String s) {
            Log.d("[TransactionSize]" + s);
        }

        @Override
        public void logException(@NonNull final Exception e) {
            Log.d("[TransactionSize] Exception", e);
        }
    };

    private TransactionSizeLogger() {
        //no instance
    }

    public static TransactionSizeLogger get() {
        return INSTANCE;
    }

    public void setEnabled(final boolean enabled) {
        enabledRequested.set(enabled);
        setRequested();
    }

    public void setRequested() {
        if (enabled.get() != enabledRequested.get()) {
            if (enabledRequested.get()) {
                tryEnable();
            } else {
                tryDisable();
            }
        }
    }

    private void tryEnable() {
        final Application app = CgeoApplication.getInstance();
        if (app == null || enabled.get()) {
            return;
        }
        TooLargeTool.startLogging(app, TOOLARGE_FORMATTER, TOOLARGE_LOGGER);
        enabled.set(true);
    }

    private void tryDisable() {
        final Application app = CgeoApplication.getInstance();
        if (app == null || !enabled.get()) {
            return;
        }
        TooLargeTool.stopLogging(app);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    private static String bundleToString(final Bundle bundle) {
        final SizeTree st = TooLargeToolKt.sizeTreeFromBundle(bundle);

        final StringBuilder sb = new StringBuilder(st.getKey() + "/" + cgeo.geocaching.utils.Formatter.formatBytes(st.getTotalSize()) + "/" + st.getSubTrees().size() + "keys (");
        boolean first = true;
        Collections.sort(st.getSubTrees(), (l1, l2) -> Integer.compare(l2.getTotalSize(), l1.getTotalSize()));
        for (SizeTree child : st.getSubTrees()) {
            if (!first) {
                sb.append(";");
            }
            first = false;
            sb.append(child.getKey()).append("=").append(cgeo.geocaching.utils.Formatter.formatBytes(child.getTotalSize()));
        }
        return sb.append(")").toString();
    }

}
