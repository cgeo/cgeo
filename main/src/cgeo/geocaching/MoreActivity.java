package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.gc.BookmarkListActivity;
import cgeo.geocaching.connector.gc.PocketQueryListActivity;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.databinding.MoreActivityBinding;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.helper.UsefulAppsActivity;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.DebugUtils;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import com.google.zxing.integration.android.IntentIntegrator;

public class MoreActivity extends AbstractBottomNavigationActivity {

    private MoreActivityBinding binding;
    private final ArrayList<MenuItem> menuItems = new ArrayList<>();

    @Override
    public int getSelectedBottomItemId() {
        return MENU_MORE;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        // don't call the super implementation with the layout argument, as that would set the wrong theme
        super.onCreate(savedInstanceState);

        setTitle(R.string.more_button);

        binding = MoreActivityBinding.inflate(getLayoutInflater());

        // init BottomNavigationController to add the bottom navigation to the layout
        setContentView(binding.getRoot());

        final PopupMenu p = new PopupMenu(this, null);
        final Menu menu = p.getMenu();
        getMenuInflater().inflate(R.menu.more_activity_popup, menu);

        // initialize menu items
        menu.findItem(R.id.menu_wizard).setVisible(!InstallWizardActivity.isConfigurationOk(this));
        menu.findItem(R.id.menu_update_routingdata).setEnabled(Settings.useInternalRouting());

        final boolean isPremiumActive = Settings.isGCConnectorActive() && Settings.isGCPremiumMember();
        menu.findItem(R.id.menu_pocket_queries).setVisible(isPremiumActive);
        menu.findItem(R.id.menu_bookmarklists).setVisible(isPremiumActive);



        for (int pos = 0; pos < menu.size(); pos++) {
            final MenuItem menuItem = menu.getItem(pos);
            if (menuItem.isVisible()) {
                menuItems.add(menuItem);
            }
        }

        final ArrayAdapter<MenuItem> menuItemAdapter = new ArrayAdapter<MenuItem>(this, 0, menuItems) {
            @NonNull
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                View v = convertView;
                if (null == convertView) {
                    v = getLayoutInflater().inflate(R.layout.page_more_item, parent, false);
                }

                final MenuItem menuItem = menuItems.get(position);
                final ImageView icon = v.findViewById(R.id.item_icon);
                final TextView text = v.findViewById(R.id.item_text);

                icon.setImageDrawable(menuItem.getIcon());
                text.setText(menuItem.getTitle());

                v.setOnClickListener(v1 -> onOptionsItemSelected(menuItem));

                return v;
            }
        };

        binding.moreActions.setAdapter(menuItemAdapter);
    }

    @Override
    protected void onLoginIssue(final boolean issue) {
        new Handler().postDelayed(() -> {
            for (int i = 0; i < menuItems.size(); i++) {
                if (menuItems.get(i).getItemId() == R.id.menu_dashboard) {
                    final ImageView itemIcon = binding.moreActions.getChildAt(i).findViewById(R.id.item_icon);
                    if (issue) {
                        itemIcon.setColorFilter(Color.rgb(191, 7, 7)); // red color
                        itemIcon.setImageResource(R.drawable.ic_warning);
                    } else {
                        itemIcon.clearColorFilter();
                        itemIcon.setImageDrawable(menuItems.get(i).getIcon());
                    }
                    return;
                }
            }
        }, 250); // menuItems might not be available if executed too early
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.more_activity_overflow, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.hasSubMenu()) {
            final SubMenu subMenu = item.getSubMenu();
            final ArrayList<CharSequence> subMenuItems = new ArrayList<>();

            for (int pos = 0; pos < subMenu.size(); pos++) {
                final MenuItem subMenuItem = subMenu.getItem(pos);
                subMenuItems.add(subMenuItem.getTitle());
            }

            Dialogs.newBuilder(this)
                    .setTitle(item.getTitle())
                    .setItems(subMenuItems.toArray(new CharSequence[0]), (dialog, which) -> onOptionsItemSelected(subMenu.getItem(which)))
                    .show();
            return true;
        }

        final int id = item.getItemId();
        if (id == R.id.menu_about) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.menu_dashboard) {
            startActivity(new Intent(this, MainActivity.class));
            ActivityMixin.overrideTransitionToFade(this);
            finish();
        } else if (id == R.id.menu_report_problem) {
            DebugUtils.askUserToReportProblem(this, null);
        } else if (id == R.id.menu_helpers) {
            startActivity(new Intent(this, UsefulAppsActivity.class));
        } else if (id == R.id.menu_wizard) {
            final Intent wizard = new Intent(this, InstallWizardActivity.class);
            wizard.putExtra(InstallWizardActivity.BUNDLE_MODE, InstallWizardActivity.needsFolderMigration() ? InstallWizardActivity.WizardMode.WIZARDMODE_MIGRATION.id : InstallWizardActivity.WizardMode.WIZARDMODE_RETURNING.id);
            startActivity(wizard);
        } else if (id == R.id.menu_settings) {
            startActivityForResult(new Intent(this, SettingsActivity.class), Intents.SETTINGS_ACTIVITY_REQUEST_CODE);
        } else if (id == R.id.menu_backup) {
            SettingsActivity.openForScreen(R.string.preference_screen_backup, this);
        } else if (id == R.id.menu_history) {
            startActivity(CacheListActivity.getHistoryIntent(this));
            ActivityMixin.overrideTransitionToFade(this);
            finish();
        } else if (id == R.id.menu_goto) {
            InternalConnector.assertHistoryCacheExists(this);
            CacheDetailActivity.startActivity(this, InternalConnector.GEOCODE_HISTORY_CACHE, true);
        } else if (id == R.id.menu_scan) {
            startScannerApplication();
        } else if (id == R.id.menu_pocket_queries) {
            if (Settings.isGCPremiumMember()) {
                startActivity(new Intent(this, PocketQueryListActivity.class));
            }
        } else if (id == R.id.menu_bookmarklists) {
            if (Settings.isGCPremiumMember()) {
                startActivity(new Intent(this, BookmarkListActivity.class));
            }
        } else if (id == R.id.menu_update_routingdata) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(this, Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES, R.string.updates_check, DownloaderUtils::returnFromTileUpdateCheck);
        } else if (id == R.id.menu_update_mapdata) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(this, Download.DownloadType.DOWNLOADTYPE_ALL_MAPRELATED, R.string.updates_check, DownloaderUtils::returnFromMapUpdateCheck);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void startScannerApplication() {
        final IntentIntegrator integrator = new IntentIntegrator(this);
        // integrator dialog is English only, therefore localize it
        integrator.setButtonYesByID(android.R.string.yes);
        integrator.setButtonNoByID(android.R.string.no);
        integrator.setTitleByID(R.string.menu_scan_geo);
        integrator.setMessageByID(R.string.menu_scan_description);
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }
}
