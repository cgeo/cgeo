package cgeo.junit;

import com.google.android.apps.common.testing.testrunner.GoogleInstrumentationTestRunner;
import com.zutubi.android.junitreport.JUnitReportListener;
import com.zutubi.android.junitreport.JUnitReportTestRunner;

import android.os.Bundle;
import android.test.AndroidTestRunner;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Test runner which derives from the newer Google instrumentation test runner used by the Espresso test framework. It
 * adds junit report functionality by cloning the behaviour of the {@link JUnitReportTestRunner}.
 *
 */
public class CgeoTestRunner extends GoogleInstrumentationTestRunner {

    /**
     * Name of the report file(s) to write, may contain __suite__ in multiFile mode.
     */
    private static final String ARG_REPORT_FILE = "reportFile";
    /**
     * If specified, path of the directory to write report files to. May start with __external__.
     * If not set files are written to the internal storage directory of the app under test.
     */
    private static final String ARG_REPORT_DIR = "reportDir";
    /**
     * If true, stack traces in the report will be filtered to remove common noise (e.g. framework
     * methods).
     */
    private static final String ARG_FILTER_TRACES = "filterTraces";
    /**
     * If true, produce a separate file for each test suite. By default a single report is created
     * for all suites.
     */
    private static final String ARG_MULTI_FILE = "multiFile";
    /**
     * Default name of the single report file.
     */
    private static final String DEFAULT_SINGLE_REPORT_FILE = "junit-report.xml";
    /**
     * Default name pattern for multiple report files.
     */
    private static final String DEFAULT_MULTI_REPORT_FILE = "junit-report-" + JUnitReportListener.TOKEN_SUITE + ".xml";

    private static final String LOG_TAG = CgeoTestRunner.class.getSimpleName();

    private JUnitReportListener mListener;
    private String mReportFile;
    private String mReportDir;
    private boolean mFilterTraces = true;
    private boolean mMultiFile = false;

    @Override
    public void onCreate(Bundle arguments) {
        if (arguments != null) {
            Log.i(LOG_TAG, "Created with arguments: " + arguments.keySet());
            mReportFile = arguments.getString(ARG_REPORT_FILE);
            mReportDir = arguments.getString(ARG_REPORT_DIR);
            mFilterTraces = getBooleanArgument(arguments, ARG_FILTER_TRACES, true);
            mMultiFile = getBooleanArgument(arguments, ARG_MULTI_FILE, false);
        } else {
            Log.i(LOG_TAG, "No arguments provided");
        }

        if (mReportFile == null) {
            mReportFile = mMultiFile ? DEFAULT_MULTI_REPORT_FILE : DEFAULT_SINGLE_REPORT_FILE;
        }
        Log.i(LOG_TAG, "report directory '" + mReportDir + "'");
        Log.i(LOG_TAG, "report file '" + mReportFile + "'");

        super.onCreate(arguments);
    }

    private static boolean getBooleanArgument(Bundle arguments, String name, boolean defaultValue) {
        String value = arguments.getString(name);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public void start() {
        mListener = new JUnitReportListener(getContext(), getTargetContext(), mReportFile, mReportDir, mFilterTraces, mMultiFile);
        try {
            Class<?> c = getClass();
            Field bridgeTestRunner = c.getSuperclass().getDeclaredField("bridgeTestRunner");
            bridgeTestRunner.setAccessible(true);
            Object obj = bridgeTestRunner.get(this);
            Method m = obj.getClass().getDeclaredMethod("getAndroidTestRunner", (Class[]) null);
            AndroidTestRunner androidTestRunner = (AndroidTestRunner) m.invoke(obj);
            androidTestRunner.addTestListener(mListener);
        } catch (NoSuchFieldException | InvocationTargetException | IllegalAccessException | NoSuchMethodException | SecurityException x) {
            Log.e(LOG_TAG, x.toString());
        }
        super.start();
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        if (mListener != null) {
            mListener.close();
        }

        super.finish(resultCode, results);
    }
}
