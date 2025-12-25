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

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.databinding.ActivityCrashBinding
import cgeo.geocaching.utils.DebugUtils

import android.os.Bundle
import android.view.Window

class CrashActivity : AbstractActivity() {

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        // window without actionbar for a cleaner look
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setTheme(R.style.NoActionbarTheme)

        val binding: ActivityCrashBinding = ActivityCrashBinding.inflate(getLayoutInflater())
        setContentView(binding.getRoot())

        val error: String = getIntent().getStringExtra("Error")
        binding.crashMessage.setText(error)

        binding.reportButton.setOnClickListener(v -> DebugUtils.askUserToReportProblem(CrashActivity.this, error, false))

    }
}
