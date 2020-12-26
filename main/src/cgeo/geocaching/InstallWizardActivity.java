package cgeo.geocaching;

import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.GCAuthorizationActivity;
import cgeo.geocaching.settings.MapDownloadSelectorActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.MapDownloadUtils;
import cgeo.geocaching.utils.ProcessUtils;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class InstallWizardActivity extends AppCompatActivity {

    private enum WizardStep {
        WIZARD_START,
        WIZARD_PERMISSIONS, WIZARD_STORAGE, WIZARD_LOCATION,
        WIZARD_PLATFORMS,
        WIZARD_END
    }
    private WizardStep step = WizardStep.WIZARD_START;

    private static final String BUNDLE_STEP = "step";

    private static final int REQUEST_CODE_WIZARD_GC = 0x7167;

    // dialog elements
    private ImageView logo = null;
    private TextView title = null;
    private TextView text = null;
    private Button prev = null;
    private Button skip = null;
    private Button next = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.dark);
        if (savedInstanceState != null) {
            step = WizardStep.values()[savedInstanceState.getInt(BUNDLE_STEP)];
        }
        basicConfiguration();
    }

    private void basicConfiguration() {
        setContentView(R.layout.install_wizard);
        bindTitleAndFooter();
        logo = findViewById(R.id.wizard_logo);
        text = findViewById(R.id.wizard_text);
        updateDialog();
    }

    private void advancedConfiguration() {
        setContentView(R.layout.install_wizard_advanced);
        bindTitleAndFooter();
        title.setText(R.string.wizard_welcome_advanced);
        setNavigation(this::basicConfiguration, 0, null, 0, this::finishWizard, R.string.finish);

        findViewById(R.id.wizard_offlinemaps).setOnClickListener(v -> startActivityForResult(new Intent(this, MapDownloadSelectorActivity.class), MapDownloadUtils.REQUEST_CODE));
        findViewById(R.id.wizard_brouter).setOnClickListener(v -> ProcessUtils.openMarket(this, getString(R.string.brouter_package)));
        findViewById(R.id.wizard_services).setOnClickListener(v -> SettingsActivity.openForScreen(R.string.preference_screen_services, this));
        findViewById(R.id.wizard_restore).setOnClickListener(v -> SettingsActivity.openForScreen(R.string.preference_screen_backup, this));
    }

    private void bindTitleAndFooter() {
        title = findViewById(R.id.wizard_title);
        prev = findViewById(R.id.wizard_prev);
        skip = findViewById(R.id.wizard_skip);
        next = findViewById(R.id.wizard_next);
    }

    private void updateDialog() {
        logo.setImageResource(R.mipmap.ic_launcher);
        switch (step) {
            case WIZARD_START: {
                title.setText(R.string.wizard_welcome_title);
                text.setText(R.string.wizard_intro);
                setNavigation(this::finishWizard, R.string.skip, null, 0, this::gotoNext, 0);
                break;
            }
            case WIZARD_PERMISSIONS: {
                title.setText(R.string.wizard_permissions_title);
                text.setText(R.string.wizard_permissions_intro);
                setNavigation(this::gotoPrevious, 0, null, 0, this::gotoNext, 0);
                break;
            }
            case WIZARD_STORAGE:
                title.setText(R.string.wizard_permissions_title);
                text.setText(R.string.storage_permission_request_explanation);
                setNavigation(this::gotoPrevious, 0, null, 0, this::requestStorage, 0);
                break;
            case WIZARD_LOCATION:
                title.setText(R.string.wizard_permissions_title);
                text.setText(R.string.location_permission_request_explanation);
                setNavigation(this::gotoPrevious, 0, null, 0, this::requestLocation, 0);
                break;
            case WIZARD_PLATFORMS:
                title.setText(R.string.wizard_platforms_title);
                text.setText(R.string.wizard_platforms_intro);
                setNavigation(this::gotoPrevious, 0, this::gotoNext, 0, this::authorizeGC, 0);
                break;
            case WIZARD_END: {
                title.setText(R.string.wizard_welcome_title);
                text.setText(R.string.wizard_outro);
                setNavigation(this::gotoPrevious, 0, this::advancedConfiguration, R.string.advanced, this::finishWizard, R.string.finish);
                break;
            }
            default: {
                // never should happen!
                step = WizardStep.WIZARD_START;
                updateDialog();
                break;
            }
        }
    }

    private void setNavigation(@Nullable final Runnable listenerPrev, final int prevLabelRes, @Nullable final Runnable listenerSkip, final int skipLabelRes, @Nullable final Runnable listenerNext, final int nextLabelRes) {
        if (listenerPrev == null) {
            prev.setVisibility(View.GONE);
        } else {
            prev.setVisibility(View.VISIBLE);
            prev.setText(prevLabelRes == 0 ? R.string.previous : prevLabelRes);
            prev.setOnClickListener(v -> listenerPrev.run());
        }
        if (listenerSkip == null) {
            skip.setVisibility(View.GONE);
        } else {
            skip.setVisibility(View.VISIBLE);
            skip.setText(skipLabelRes == 0 ? R.string.skip : skipLabelRes);
            skip.setOnClickListener(v -> listenerSkip.run());
        }
        if (listenerNext == null) {
            next.setVisibility(View.GONE);
        } else {
            next.setVisibility(View.VISIBLE);
            next.setText(nextLabelRes == 0 ? R.string.next : nextLabelRes);
            next.setOnClickListener(v -> listenerNext.run());
        }
    }

    private void gotoPrevious() {
        if (step.ordinal() > 0) {
            step = WizardStep.values()[step.ordinal() - 1];
            if (stepCanBeSkipped()) {
                gotoPrevious();
            } else {
                updateDialog();
            }
        }
    }

    private void gotoNext() {
        final int max = WizardStep.values().length - 1;
        if (step.ordinal() < max) {
            step = WizardStep.values()[step.ordinal() + 1];
            if (stepCanBeSkipped()) {
                gotoNext();
            } else {
                updateDialog();
            }
        }
    }

    private boolean stepCanBeSkipped() {
        return (step == WizardStep.WIZARD_PERMISSIONS && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (hasStoragePermission() && hasLocationPermission())))
            || (step == WizardStep.WIZARD_STORAGE && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasStoragePermission()))
            || (step == WizardStep.WIZARD_LOCATION && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasLocationPermission()))
            || (step == WizardStep.WIZARD_PLATFORMS && hasValidGCCredentials())
            ;
    }

    private void finishWizard() {
        // call MainActivity and finish this Activity
        final Intent main = new Intent(this, MainActivity.class);
        main.putExtras(getIntent());
        startActivity(main);
        finish();
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermission() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            || (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestStorage() {
        if (!hasStoragePermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PermissionRequestContext.InstallWizardActivity.getRequestCode());
        }
    }

    private void requestLocation() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PermissionRequestContext.InstallWizardActivity.getRequestCode());
        }
    }

    private boolean hasValidGCCredentials() {
        return Settings.getCredentials(GCConnector.getInstance()).isValid();
    }

    private void authorizeGC() {
        String username = "";
        String password = "";

        final Intent checkIntent = new Intent(this, GCAuthorizationActivity.class);
        final Credentials credentials = GCConnector.getInstance().getCredentials();
        try {
            username = credentials.getUserName();
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            password = credentials.getPassword();
        } catch (IllegalArgumentException e) {
            // ignore
        }
        checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_USERNAME, username);
        checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_PASSWORD, password);
        startActivityForResult(checkIntent, REQUEST_CODE_WIZARD_GC);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(BUNDLE_STEP, step.ordinal());
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        gotoNext();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_WIZARD_GC) {
            if (!hasValidGCCredentials()) {
                Toast.makeText(this, R.string.err_auth_process, Toast.LENGTH_SHORT).show();
            } else {
                Dialogs.confirm(this, R.string.settings_title_gc, R.string.settings_gc_legal_note, android.R.string.ok, (dialog, which) -> {
                    Settings.setGCConnectorActive(true);
                    gotoNext();
                }, dialog -> gotoNext());
            }
        } else {
            MapDownloadUtils.onActivityResult(this, requestCode, resultCode, data);
        }
    }
}
