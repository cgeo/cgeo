package cgeo.geocaching.test;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.InstrumentationInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.text.HtmlCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class CgeoTestsActivity extends Activity {
    private static final String TAG = CgeoTestsActivity.class.getName();

    private static final int TIMEOUT = 600 * 1000;

    private TextView logView;
    private LogcatAsyncTask logCatTask;

    private BottomAwareScrollView scrollView;

    private class LogcatAsyncTask extends AsyncTask<Integer, String, Void> {
        // TestRunner and silence others
        private static final String CMD = "logcat -v brief TestRunner:I cgeo:I *:S";
        private BufferedReader mReader;
        private Process mProc;

        LogcatAsyncTask() {
            try {
                mProc = Runtime.getRuntime().exec(CMD);
                mReader = new BufferedReader(new InputStreamReader(
                        mProc.getInputStream()));
            } catch (final Exception e) {
                Log.e(TAG, "Creating proc", e);
            }
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            final String line = values[0];
            final boolean isAtBottom = scrollView.isAtBottom();
            if (!TextUtils.isEmpty(line)) {
                logView.append(HtmlCompat.fromHtml("<font color=\"" + color(line) + "\">" + line + "</font><br/>", HtmlCompat.FROM_HTML_MODE_LEGACY));
                if (isAtBottom) {
                    scrollView.scrollTo(0, logView.getBottom());
                }
            }
        }

        private String color(final String line) {
            switch (line.charAt(0)) {
                case 'E':
                    return "red";
                case 'W':
                    return "#FFA500";
                case 'D':
                    return "blue";
                default:
                    return "white";
            }
        }

        @Override
        protected Void doInBackground(final Integer... params) {
            final long timeout = System.currentTimeMillis() + params[0];
            try {
                do {
                    Thread.sleep(50);
                    publishProgress(mReader.readLine());
                } while (System.currentTimeMillis() < timeout);
            } catch (InterruptedException | IOException e) {
                publishProgress("ERROR: " + e);
            } finally {
                publishProgress("END");
                mProc.destroy();
            }
            return null;
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cgeo_tests_activity);
        logView = findViewById(R.id.logOutput);
        scrollView = findViewById(R.id.scrollView);
    }

    @Override
    protected void onDestroy() {
        if (logCatTask != null) {
            logCatTask.cancel(true);
        }
        super.onDestroy();
    }

    private InstrumentationInfo getInstrumentationInfo(final String packageName) {
        final List<InstrumentationInfo> list =
                getPackageManager()
                        .queryInstrumentation(packageName, 0);
        return !list.isEmpty() ? list.get(0) : null;
    }

    /**
     * @param v referenced from XML layout
     */
    public void runTests(final View v) {
        final Button button = findViewById(R.id.buttonRun);
        button.setEnabled(false);
        runTestsInternally();
    }

    private void runTestsInternally() {
        final String pn = getPackageName().replaceFirst(".test$", "");
        final InstrumentationInfo info = getInstrumentationInfo(pn);
        if (info == null) {
            Toast.makeText(this,
                    "Cannot find instrumentation for " + pn, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        final ComponentName cn = new ComponentName(info.packageName,
                info.name);
        if (startInstrumentation(cn, null, null)) {
            logCatTask = new LogcatAsyncTask();
            logCatTask.execute(TIMEOUT);
        } else {
            Toast.makeText(this,
                    "Cannot run instrumentation for " + pn, Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
