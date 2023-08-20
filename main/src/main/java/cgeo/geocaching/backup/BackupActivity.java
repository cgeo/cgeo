package cgeo.geocaching.backup;

import cgeo.geocaching.databinding.InstallWizardBinding;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.BackupUtils;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.settings.Settings.EXCLUSIVEDBACTION.EDBA_NONE;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class BackupActivity extends AppCompatActivity {

    public static final String STATE_BACKUPUTILS = "backuputils";
    public static final String STATE_EDBA = "EDBA_requested";

    private BackupUtils backupUtils = null;
    private Settings.EXCLUSIVEDBACTION requestedEDBA = EDBA_NONE;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final InstallWizardBinding binding = InstallWizardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestedEDBA = savedInstanceState != null ? Settings.EXCLUSIVEDBACTION.values()[savedInstanceState.getInt(STATE_EDBA)] : Settings.getEDBARequested();
        Settings.setEDBARequested(EDBA_NONE);

        if (requestedEDBA == EDBA_NONE) {
            finish();
        }

        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "BackupActivity.onCreate")) {
            backupUtils = new BackupUtils(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_BACKUPUTILS));
            binding.wizardPrev.setVisibility(View.GONE);
            binding.wizardSkip.setVisibility(View.GONE);

            binding.wizardNext.setVisibility(View.VISIBLE);
            binding.wizardNext.setText("Cancel");
            binding.wizardNext.setEnabled(true);
            binding.wizardNext.setOnClickListener(v -> {
                // prepare restart of c:geo
                final Intent intent = Settings.getStartscreenIntent(this);
                intent.putExtras(getIntent());
                startActivity(intent);
                cLog.add("fin");
                finish();
            });

            switch (requestedEDBA) {
                case EDBA_BACKUP_AUTO: {
                    binding.wizardTitle.setText("Automatic backup");
                    backupUtils.backup(() -> {
                        cLog.add("abackup");
                        Settings.setAutomaticBackupLastCheck(false);
                    }, true);
                    break;
                }
                case EDBA_BACKUP_MANUAL: {
                    binding.wizardTitle.setText("Manual backup");
                    backupUtils.backup(() -> {
                        cLog.add("mbackup");
                    }, false);
                    break;
                }
                case EDBA_RESTORE_NEWEST: {
                    binding.wizardTitle.setText("Restore");
                    backupUtils.restore(BackupUtils.newestBackupFolder(false));
                    cLog.add("restoreN");
                    break;
                }
                case EDBA_RESTORE_SELECT: {
                    binding.wizardTitle.setText("Restore");
                    backupUtils.selectBackupDirIntent();
                    cLog.add("restoreS");
                    break;
                }
                /*
                case EDBA_SETSIZE: {
                    // @todo: set max allowed number of backups; delete superfluous backups
                    break;
                }
                case EDBA_DBMOVE: {
                    // @todo: move database
                    break;
                }
                */
                default: {
                    throw new IllegalStateException("unknown value for requested EDBA");
                }
            }
            binding.wizardText.setText("Tap button to continue to c:geo");
            binding.wizardNext.setText("Continue");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBundle(STATE_BACKUPUTILS, backupUtils.getState());
        savedInstanceState.putInt(STATE_EDBA, requestedEDBA.ordinal());
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        backupUtils.onActivityResult(requestCode, resultCode, data);
    }

}
