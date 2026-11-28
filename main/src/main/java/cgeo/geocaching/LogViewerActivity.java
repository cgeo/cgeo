package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractGridActivity;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.movingbits.grid.CellRef;
import com.movingbits.grid.DatabaseGrid;
import com.movingbits.grid.GridColumn;
import com.movingbits.grid.LogcatGrid;
import com.movingbits.grid.SearchRequest;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Shows a logcat file as a sortable and searchable grid.
 *
 * <p>The file is selected via SAF (starting in {@link PersistableFolder#LOGFILES}); the most recently
 * viewed file of that folder is reopened directly. Its content is imported into a temporary SQLite
 * database and displayed by a {@link LogcatGrid}, so paging, sorting and searching run as SQL and even
 * large files need not be held in memory.</p>
 *
 * <p>Temporary databases are kept in a subfolder of the cache directory. They are deleted when the
 * viewer is closed or another file is loaded, and leftovers (crash, process kill) on every start
 * of the viewer.</p>
 */
public class LogViewerActivity extends AbstractGridActivity {

    /** settings key for column configuration, search and last file, stored as JSON */
    private static final String CONFIG_KEY = "logviewconfig";
    private static final String CONFIG_CONFIGURATION = "configuration";
    private static final String CONFIG_SEARCH = "search";
    private static final String CONFIG_LAST_FILE = "lastFile";

    private static final String STATE_URI = "logfileUri";
    private static final String STATE_DB = "logfileDb";
    private static final String STATE_CSAH = "csah";

    private ContentStorageActivityHelper csah;
    private DatabaseGrid databaseGrid;
    private SQLiteDatabase database;
    private File databaseFile;
    /** file currently shown, or currently being imported */
    private Uri currentUri;
    /** file whose content is in {@link #database} */
    private Uri shownUri;
    private final Progress progress = new Progress();

    @Override
    public void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.string.view_logfile);

        csah = new ContentStorageActivityHelper(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_CSAH))
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE, Uri.class, this::onFileSelected);

        // start with an empty grid, a database is attached once a file has been imported
        databaseGrid = createGrid();
        attachGrid(databaseGrid, null);
        configureFileButton();

        final Uri restoredUri = savedInstanceState == null ? null : savedInstanceState.getParcelable(STATE_URI);
        final String restoredDb = savedInstanceState == null ? null : savedInstanceState.getString(STATE_DB);
        final File restoredFile = restoredDb == null ? null : new File(restoredDb);

        // remove leftovers of earlier sessions (crash, process kill), but keep a database still in use after rotation
        // (done synchronously - the folder holds only a few files - so it cannot interfere with an import started below)
        final File keep = restoredFile != null && restoredFile.isFile() ? restoredFile : null;
        LogcatGrid.cleanup(getTempDir(), keep);

        if (restoredUri != null && keep != null) {
            showDatabase(restoredUri, keep, savedInstanceState);
        } else if (restoredUri != null) {
            loadFile(restoredUri, savedInstanceState);
        } else if (savedInstanceState == null) {
            openInitialFile();
        }
    }

    /** reuse "select table" button of control bar for file selection */
    private void configureFileButton() {
        binding.buttonSelectTable.setVisibility(View.VISIBLE);
        // the file icon is defined with 48dp, scale it to the size of the other icons of the control bar
        final Drawable reference = binding.buttonSearch.getIcon();
        if (reference != null && reference.getIntrinsicWidth() > 0) {
            binding.buttonSelectTable.setIconSize(reference.getIntrinsicWidth());
        }
        binding.buttonSelectTable.setIconResource(R.drawable.ic_menu_file);
        binding.buttonSelectTable.setContentDescription(LocalizationUtils.getString(R.string.logview_select_file));
        binding.buttonSelectTable.setTooltipText(LocalizationUtils.getString(R.string.logview_select_file));
        binding.buttonSelectTable.setOnClickListener(v -> selectFile());
    }

    // -------------------------------------------------------------- file selection & loading

    /** reopen last file if it still exists in logfile folder, otherwise ask user */
    private void openInitialFile() {
        final String lastFile = readConfig().optString(CONFIG_LAST_FILE, "");
        final ContentStorage.FileInformation info = lastFile.isEmpty() ? null : ContentStorage.get().getFileInfo(PersistableFolder.LOGFILES.getFolder(), lastFile);
        if (info != null && !info.isDirectory && info.uri != null) {
            loadFile(info.uri, null);
        } else {
            selectFile();
        }
    }

    private void selectFile() {
        csah.selectFile(null, PersistableFolder.LOGFILES.getUri());
    }

    private void onFileSelected(@Nullable final Uri uri) {
        if (uri != null) {
            loadFile(uri, null);
        } else if (database == null) {
            // nothing to show
            finish();
        }
    }

    /** imports file into a new temporary database in background, then displays it */
    private void loadFile(@NonNull final Uri uri, @Nullable final Bundle state) {
        final String name = displayName(uri);
        currentUri = uri;
        progress.show(this, LocalizationUtils.getString(R.string.view_logfile), LocalizationUtils.getString(R.string.logview_loading, name), true, null);
        final File tempDir = getTempDir();
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> importFile(uri, tempDir), file -> {
            if (isFinishing() || isDestroyed()) {
                // activity is gone (e.g. rotated during import - new instance imports again)
                LogcatGrid.deleteDatabase(file);
                return;
            }
            progress.dismiss();
            if (file == null) {
                showHint(LocalizationUtils.getString(R.string.logview_error_reading, name));
                currentUri = shownUri;
                if (database == null) {
                    finish();
                }
                return;
            }
            showDatabase(uri, file, state);
        });
    }

    @Nullable
    private static File importFile(@NonNull final Uri uri, @NonNull final File tempDir) {
        try (InputStream is = ContentStorage.get().openForRead(uri)) {
            if (is == null) {
                return null;
            }
            return LogcatGrid.importLogcat(tempDir, is);
        } catch (IOException | RuntimeException e) {
            Log.w("LogViewer: problem reading " + uri, e);
            return null;
        }
    }

    /** switches display to given database file, releasing the previous one */
    private void showDatabase(@NonNull final Uri uri, @NonNull final File file, @Nullable final Bundle state) {
        final SQLiteDatabase newDatabase;
        try {
            newDatabase = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch (RuntimeException e) {
            Log.w("LogViewer: cannot open " + file, e);
            LogcatGrid.deleteDatabase(file);
            showHint(LocalizationUtils.getString(R.string.logview_error_reading, displayName(uri)));
            return;
        }
        closeDatabase(true);
        database = newDatabase;
        databaseFile = file;
        currentUri = uri;
        shownUri = uri;

        // LogcatGrid selects the log table and builds its columns by itself
        databaseGrid = createGrid().setDatabase(database);
        applyStoredConfig(databaseGrid);
        // after rotation: restore grid state of this session (overrides stored config)
        databaseGrid.readState(state);
        gridView.setGrid(databaseGrid);
        gridView.readState(state);
        refreshButtonHighlights();

        final String name = displayName(uri);
        setTitle(name);
        storeLastFile(name);
    }

    private void closeDatabase(final boolean deleteFile) {
        if (databaseGrid != null) {
            databaseGrid.close();
        }
        if (database != null) {
            database.close();
            database = null;
        }
        if (deleteFile) {
            LogcatGrid.deleteDatabase(databaseFile);
            databaseFile = null;
        }
    }

    /** folder for temporary logfile databases */
    @NonNull
    private File getTempDir() {
        return new File(getCacheDir(), "logviewer");
    }

    @NonNull
    private static String displayName(@NonNull final Uri uri) {
        final String name = ContentStorage.get().getName(uri);
        return name != null ? name : UriUtils.getLastPathSegment(uri);
    }

    // -------------------------------------------------------------- grid

    private DatabaseGrid createGrid() {
        return new LogcatGrid(this)
                .rowsPerPage(50)
                .alternatingRowColors(true)
                .adjustableFixedBoundary(true)
                .onConfigurationChanged(json -> {
                    storeConfig();
                    highlightButton(binding.buttonConfigColumns, grid().hasHiddenColumns());
                })
                .onSearchChanged(search -> {
                    storeConfig();
                    highlightButton(binding.buttonSearch, !search.isEmpty());
                })
                .onCellLongClick(this::showCell)
                .onSortChanged(order -> showHint(addSortIndicator(order)))
                .wrappableText(true);
    }

    /** long tap on cell shows its full content */
    private void showCell(final CellRef cell, final String[] row) {
        final GridColumn column = grid().getColumn(cell.columnIndex());
        final String text = cell.dataIndex() < row.length && row[cell.dataIndex()] != null ? row[cell.dataIndex()] : "";
        SimpleDialog.of(this)
                .setTitle(TextParam.text(LocalizationUtils.getString(R.string.logview_cell_title, column.getTitle(), cell.rowIndex() + 1)))
                .setMessage(TextParam.text(text)).show();
    }

    // -------------------------------------------------------------- config

    @NonNull
    private static JSONObject readConfig() {
        try {
            return new JSONObject(Settings.getStringDirect(CONFIG_KEY, "{}"));
        } catch (JSONException ignore) {
            // nothing stored yet, or unreadable => use defaults
            return new JSONObject();
        }
    }

    private static void writeConfig(@NonNull final JSONObject config) {
        Settings.putStringDirect(CONFIG_KEY, config.toString());
    }

    /** stores column configuration and search in settings */
    private void storeConfig() {
        try {
            final JSONObject root = readConfig();
            root.put(CONFIG_CONFIGURATION, new JSONObject(databaseGrid.toConfigurationJson()));
            root.put(CONFIG_SEARCH, new JSONObject(databaseGrid.getSearchJson()));
            writeConfig(root);
        } catch (JSONException e) {
            Log.w("LogViewer: cannot store config", e);
        }
    }

    /** remembers file name for direct reopening - only for files in c:geo's logfile folder */
    private static void storeLastFile(@NonNull final String name) {
        final boolean inLogfolder = ContentStorage.get().getFileInfo(PersistableFolder.LOGFILES.getFolder(), name) != null;
        try {
            final JSONObject root = readConfig();
            if (inLogfolder) {
                root.put(CONFIG_LAST_FILE, name);
            } else {
                root.remove(CONFIG_LAST_FILE);
            }
            writeConfig(root);
        } catch (JSONException e) {
            Log.w("LogViewer: cannot store last file", e);
        }
    }

    /** applies stored column configuration and search (data order is not stored, as extra columns vary per file) */
    private static void applyStoredConfig(final DatabaseGrid grid) {
        final JSONObject root = readConfig();
        final JSONObject configuration = root.optJSONObject(CONFIG_CONFIGURATION);
        if (configuration != null) {
            grid.configuration(configuration.toString());
        }
        final JSONObject search = root.optJSONObject(CONFIG_SEARCH);
        if (search != null) {
            grid.search(SearchRequest.parse(search.toString()));
        }
    }

    // -------------------------------------------------------------- lifecycle

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (csah.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_CSAH, csah.getState());
        if (currentUri != null) {
            outState.putParcelable(STATE_URI, currentUri);
        }
        // only a database matching currentUri may be reused (not the old one while another file is imported)
        if (databaseFile != null && database != null && currentUri != null && currentUri.equals(shownUri)) {
            outState.putString(STATE_DB, databaseFile.getPath());
        }
    }

    @Override
    protected void onDestroy() {
        progress.dismiss();
        // keep database file on configuration change, it is reused by the new instance
        closeDatabase(isFinishing());
        super.onDestroy();
    }
}
