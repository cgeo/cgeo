package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.downloader.ReceiveDownloadService;
import cgeo.geocaching.files.FileType;
import cgeo.geocaching.files.FileTypeDetector;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.wherigo.GwzCompileService;
import cgeo.geocaching.wherigo.WherigoActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class HandleLocalFilesActivity extends AbstractActivity {

    private static final String LOGPRAEFIX = "HandleLocalFilesActivity: ";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Intent intent = getIntent();
        final Uri uri = intent.getData();
        boolean finished = false;

        Log.iForce(LOGPRAEFIX + "called for URI:" + uri);

        final ContentResolver contentResolver = getContentResolver();
        final FileType fileType = new FileTypeDetector(uri, contentResolver).getFileType();
        switch (fileType) {
            case GPX:
            case ZIP:
            case LOC:
                continueWith(CacheListActivity.class, intent);
                finished = true;
                break;
            case MAP:
                continueWithForegroundService(ReceiveDownloadService.class, intent);
                finished = true;
                break;
            case WHERIGO:
                final String guid = copyToWherigoFolder(uri);
                if (guid != null) {
                    WherigoActivity.startForGuid(this, guid, null, false);
                    finish();
                    finished = true;
                }
                break;
            case WHERIGO_ZIP:
                final String gwzGuid = extractAndCopyGwzToWherigoFolder(uri);
                if (gwzGuid != null) {
                    WherigoActivity.startForGuid(this, gwzGuid, null, false);
                    finish();
                    finished = true;
                }
                break;
            default:
                break;
        }
        if (!finished) {
            SimpleDialog.of(this).setTitle(R.string.localfile_title).setMessage(R.string.localfile_cannot_handle).show(this::finish);
        }
    }

    private void continueWith(@SuppressWarnings("rawtypes") final Class clazz, final Intent intent) {
        Log.iForce(LOGPRAEFIX + "continueWith:" + clazz);
        final Intent forwarder = new Intent(intent);
        forwarder.setClass(this, clazz);
        startActivity(forwarder);
        finish();
    }

    private void continueWithForegroundService(@SuppressWarnings("rawtypes") final Class clazz, final Intent intent) {
        Log.iForce(LOGPRAEFIX + "continueWithForegroundService:" + clazz);
        final Intent forwarder = new Intent(intent);
        forwarder.setClass(this, clazz);
        ContextCompat.startForegroundService(this, forwarder);
        finish();
    }

    /**
     * copy uri to wherigo folder
     * <br />
     * For "guid calculation" of WherigoActivity,
     * - capitalization of suffix ".gwc" will be normalized to ".gwc"
     * - all "_" will be replaced by "-"
     * <br />
     * returns "guid" (= filename without suffix ".gwc") on success, null otherwise
     */
    @Nullable
    private String copyToWherigoFolder(final Uri uri) {
        String filename = ContentStorage.get().getName(uri);
        Log.iForce(LOGPRAEFIX + "expect Wherigo, raw filename:" + filename);
        if (StringUtils.isBlank(filename)) {
            filename = FileNameCreator.WHERIGO.createName();
        }
        filename = filename.replace("_", "-");
        if (StringUtils.equalsAnyIgnoreCase(filename.substring(filename.length() - 4), ".gwc")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        Log.iForce(LOGPRAEFIX + "Wherigo final filename:" + filename);
        ContentStorage.get().copy(uri, PersistableFolder.WHERIGO.getFolder(), FileNameCreator.forName(filename + ".gwc"), false);
        return filename;
    }

    /**
     * Process GWZ (Wherigo ZIP) archive using compile service
     * <br />
     * GWZ files are ZIP archives containing uncompiled Lua source code and resources.
     * This method sends the GWZ file to the Wherigo Foundation compiler service.
     * <br />
     * returns "guid" (= filename without suffix ".gwc") on success, null otherwise
     */
    @Nullable
    private String extractAndCopyGwzToWherigoFolder(final Uri uri) {
        Log.iForce(LOGPRAEFIX + "Processing GWZ (Wherigo ZIP), uri:" + uri);

        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(LOGPRAEFIX + "Failed to open GWZ file");
                return null;
            }

            final String cartridgeName = generateCartridgeName(uri);
            final File tempGwcFile = ContentStorage.get().createTempFile();
            if (tempGwcFile == null) {
                Log.e(LOGPRAEFIX + "Failed to create temporary file");
                return null;
            }

            return compileAndSaveGwz(inputStream, cartridgeName, tempGwcFile);

        } catch (final Exception e) {
            Log.e(LOGPRAEFIX + "Error processing GWZ file", e);
            showErrorDialog("Error processing GWZ file: " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return null;
    }

    private String generateCartridgeName(final Uri uri) {
        String cartridgeName = ContentStorage.get().getName(uri);
        if (StringUtils.isBlank(cartridgeName)) {
            cartridgeName = FileNameCreator.WHERIGO.createName();
        } else {
            if (StringUtils.endsWithIgnoreCase(cartridgeName, ".gwz")) {
                cartridgeName = cartridgeName.substring(0, cartridgeName.length() - 4);
            }
        }
        cartridgeName = cartridgeName.replace("_", "-");
        Log.iForce(LOGPRAEFIX + "Cartridge name: " + cartridgeName);
        return cartridgeName;
    }

    @Nullable
    private String compileAndSaveGwz(final InputStream inputStream, final String cartridgeName, final File tempGwcFile) {
        Log.iForce(LOGPRAEFIX + "Sending GWZ to compile service...");
        final String errorMessage = GwzCompileService.compileGwzFile(inputStream, tempGwcFile);
        
        if (errorMessage != null) {
            Log.e(LOGPRAEFIX + "Compilation failed: " + errorMessage);
            showErrorDialog(errorMessage);
            tempGwcFile.delete();
            return null;
        }

        final Uri tempUri = Uri.fromFile(tempGwcFile);
        ContentStorage.get().copy(tempUri, PersistableFolder.WHERIGO.getFolder(), 
            FileNameCreator.forName(cartridgeName + ".gwc"), false);
        tempGwcFile.delete();

        Log.iForce(LOGPRAEFIX + "Successfully compiled and saved GWZ: " + cartridgeName);
        return cartridgeName;
    }

    private void showErrorDialog(final String message) {
        SimpleDialog.of(this)
            .setTitle(R.string.localfile_title)
            .setMessage(TextParam.text(message))
            .show();
    }

    private static class ResourceEntry {
        final String name;
        final byte[] data;

        ResourceEntry(final String name, final byte[] data) {
            this.name = name;
            this.data = data;
        }
    }
}
