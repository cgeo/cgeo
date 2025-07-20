package cgeo.geocaching;

import cgeo.geocaching.databinding.ActivityCrashBinding;
import cgeo.geocaching.utils.DebugUtils;

import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

public class CrashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // window without actionbar for a cleaner look
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(R.style.NoActionbarTheme);

        final ActivityCrashBinding binding = ActivityCrashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final String error = getIntent().getStringExtra("Error");
        binding.crashMessage.setText(error);

        binding.reportButton.setOnClickListener(v -> DebugUtils.askUserToReportProblem(CrashActivity.this, error, false));

    }
}
