package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
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
import android.content.Context;
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

    public static final String BUNDLE_RETURNING = "returning";
    private static final String BUNDLE_STEP = "step";

    private enum WizardStep {
        WIZARD_START,
        WIZARD_PERMISSIONS, WIZARD_PERMISSIONS_STORAGE, WIZARD_PERMISSIONS_LOCATION,
        WIZARD_PLATFORMS,
        WIZARD_ADVANCED,
        WIZARD_END
    }
    private WizardStep step = WizardStep.WIZARD_START;
    private boolean returning = false;

    private static final int REQUEST_CODE_WIZARD_GC = 0x7167;

    // dialog elements
    private ImageView logo = null;
    private TextView title = null;
    private TextView text = null;

    private TextView button1Info = null;
    private Button button1 = null;
    private TextView button2Info = null;
    private Button button2 = null;
    private TextView button3Info = null;
    private Button button3 = null;

    private Button prev = null;
    private Button skip = null;
    private Button next = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.dark);
        if (savedInstanceState != null) {
            returning = savedInstanceState.getBoolean(BUNDLE_RETURNING);
            step = WizardStep.values()[savedInstanceState.getInt(BUNDLE_STEP)];
        } else {
            returning = getIntent().getBooleanExtra(BUNDLE_RETURNING, false);
        }
        setContentView(R.layout.install_wizard);

        logo = findViewById(R.id.wizard_logo);
        title = findViewById(R.id.wizard_title);
        text = findViewById(R.id.wizard_text);

        button1Info = findViewById(R.id.wizard_button1_info);
        button1 = findViewById(R.id.wizard_button1);
        button2Info = findViewById(R.id.wizard_button2_info);
        button2 = findViewById(R.id.wizard_button2);
        button3Info = findViewById(R.id.wizard_button3_info);
        button3 = findViewById(R.id.wizard_button3);

        prev = findViewById(R.id.wizard_prev);
        skip = findViewById(R.id.wizard_skip);
        next = findViewById(R.id.wizard_next);

        updateDialog();
    }

    private void updateDialog() {
        logo.setImageResource(R.mipmap.ic_launcher);
        text.setVisibility(View.VISIBLE);
        setButton(button1, 0, null, button1Info, 0);
        setButton(button2, 0, null, button2Info, 0);
        setButton(button3, 0, null, button3Info, 0);
        switch (step) {
            case WIZARD_START: {
                title.setText(R.string.wizard_welcome_title);
                text.setText(returning ? R.string.wizard_intro2 : R.string.wizard_intro);
                setNavigation(this::finishWizard, R.string.skip, null, 0, this::gotoNext, 0);
                break;
            }
            case WIZARD_PERMISSIONS: {
                title.setText(R.string.wizard_permissions_title);
                text.setText(R.string.wizard_permissions_intro);
                setNavigation(this::gotoPrevious, 0, null, 0, this::gotoNext, 0);
                break;
            }
            case WIZARD_PERMISSIONS_STORAGE:
                title.setText(R.string.wizard_permissions_title);
                text.setText(R.string.storage_permission_request_explanation);
                setNavigation(this::gotoPrevious, 0, null, 0, this::requestStorage, 0);
                break;
            case WIZARD_PERMISSIONS_LOCATION:
                title.setText(R.string.wizard_permissions_title);
                text.setText(R.string.location_permission_request_explanation);
                setNavigation(this::gotoPrevious, 0, null, 0, this::requestLocation, 0);
                break;
            case WIZARD_PLATFORMS:
                title.setText(R.string.wizard_platforms_title);
                text.setText(R.string.wizard_platforms_intro);
                setNavigation(this::gotoPrevious, 0, null, 0, this::gotoNext, 0);
                setButton(button1, R.string.wizard_platforms_gc, v -> authorizeGC(), button1Info, 0);
                setButton(button2, R.string.wizard_platforms_others, v -> SettingsActivity.openForScreen(R.string.preference_screen_services, this), button2Info, 0);
                break;
            case WIZARD_ADVANCED:
                title.setText(R.string.wizard_welcome_advanced);
                text.setVisibility(View.GONE);
                setNavigation(this::gotoPrevious, 0, null, 0, this::gotoNext, 0);
                setButton(button1, R.string.wizard_advanced_offlinemaps_label, v -> startActivityForResult(new Intent(this, MapDownloadSelectorActivity.class), MapDownloadUtils.REQUEST_CODE), button1Info, R.string.wizard_advanced_offlinemaps_info);
                setButton(button2, R.string.wizard_advanced_brouter_label, v -> ProcessUtils.openMarket(this, getString(R.string.package_brouter)), button2Info, R.string.wizard_advanced_brouter_info);
                setButton(button3, R.string.wizard_advanced_restore_label, v -> SettingsActivity.openForScreen(R.string.preference_screen_backup, this), button3Info, R.string.wizard_advanced_restore_info);
                break;
            case WIZARD_END: {
                title.setText(R.string.wizard_welcome_title);
                final StringBuilder info = new StringBuilder();
                info.append(getString(R.string.wizard_status_title)).append(":\n")
                    .append(getString(R.string.wizard_status_storage_permission)).append(": ").append(hasStoragePermission(this) ? getString(android.R.string.ok) : getString(R.string.status_not_ok)).append("\n")
                    .append(getString(R.string.wizard_status_location_permission)).append(": ").append(hasLocationPermission(this) ? getString(android.R.string.ok) : getString(R.string.status_not_ok)).append("\n")
                    .append(getString(R.string.wizard_status_platform));
                boolean platformConfigured = false;
                final StringBuilder platforms = new StringBuilder();
                for (final IConnector conn : ConnectorFactory.getActiveConnectorsWithValidCredentials()) {
                    if (platformConfigured) {
                        platforms.append(", ");
                    }
                    platforms.append(conn.getName());
                    platformConfigured = true;
                }
                if (platformConfigured) {
                    info.append(": ").append(getString(android.R.string.ok)).append("\n(").append(platforms).append(")\n");
                } else {
                    info.append(": ").append(getString(R.string.status_not_ok)).append("\n");
                }
                button1Info.setVisibility(View.VISIBLE);
                button1Info.setText(info);

                text.setText(isConfigurationOk(this) ? R.string.wizard_outro_ok : R.string.wizard_outro_error);

                setNavigation(this::gotoPrevious, 0, null, 0, this::finishWizard, R.string.finish);
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

    private void setButton(final Button button, final int labelResId, @Nullable final View.OnClickListener listener, final TextView buttonInfo, final int infoResId) {
        if (button != null) {
            if (listener == null) {
                button.setVisibility(View.GONE);
            } else {
                button.setVisibility(View.VISIBLE);
                button.setText(labelResId);
                button.setOnClickListener(listener);
            }
        }
        if (buttonInfo != null) {
            if (infoResId == 0) {
                buttonInfo.setVisibility(View.GONE);
            } else {
                buttonInfo.setVisibility(View.VISIBLE);
                buttonInfo.setText(infoResId);
            }
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
        return (step == WizardStep.WIZARD_PERMISSIONS && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (hasStoragePermission(this) && hasLocationPermission(this))))
            || (step == WizardStep.WIZARD_PERMISSIONS_STORAGE && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasStoragePermission(this)))
            || (step == WizardStep.WIZARD_PERMISSIONS_LOCATION && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasLocationPermission(this)))
            ;
    }

    private void finishWizard() {
        // call MainActivity (if not returning) and finish this Activity
        if (!returning) {
            final Intent main = new Intent(this, MainActivity.class);
            main.putExtras(getIntent());
            startActivity(main);
        }
        finish();
    }

    private static boolean hasStoragePermission(final Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasLocationPermission(final Context context) {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            || (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isConfigurationOk(final Context context) {
        final boolean isPlatformConfigured = ConnectorFactory.getActiveConnectorsWithValidCredentials().length > 0;
        return hasStoragePermission(context) && hasLocationPermission(context) && isPlatformConfigured;
    }

    private void requestStorage() {
        if (!hasStoragePermission(this)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PermissionRequestContext.InstallWizardActivity.getRequestCode());
        }
    }

    private void requestLocation() {
        if (!hasLocationPermission(this)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PermissionRequestContext.InstallWizardActivity.getRequestCode());
        }
    }

    private boolean hasValidGCCredentials() {
        return Settings.getCredentials(GCConnector.getInstance()).isValid();
    }

    private void authorizeGC() {
        final Intent checkIntent = new Intent(this, GCAuthorizationActivity.class);
        final Credentials credentials = GCConnector.getInstance().getCredentials();
        checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_USERNAME, credentials.getUsernameRaw());
        checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_PASSWORD, credentials.getPasswordRaw());
        startActivityForResult(checkIntent, REQUEST_CODE_WIZARD_GC);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(BUNDLE_RETURNING, returning);
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
                }, dialog -> { });
            }
        } else {
            MapDownloadUtils.onActivityResult(this, requestCode, resultCode, data);
        }
    }
}
