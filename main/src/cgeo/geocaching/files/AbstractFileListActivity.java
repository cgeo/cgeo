package cgeo.geocaching.files;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.EnvironmentUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractFileListActivity<T extends RecyclerView.Adapter<? extends RecyclerView.ViewHolder>> extends AbstractActionBarActivity {
    private static final int MSG_SEARCH_WHOLE_SD_CARD = 1;

    private final List<File> files = new ArrayList<>();
    private T adapter = null;
    private ProgressDialog waitDialog = null;
    private SearchFilesThread searchingThread = null;
    protected int listId = StoredList.STANDARD_LIST_ID;
    private String[] extensions;

    private final Handler changeWaitDialogHandler = new ChangeWaitDialogHandler<>(this);
    private final Handler loadFilesHandler = new LoadFilesHandler<>(this);

    private static final class ChangeWaitDialogHandler<T extends RecyclerView.Adapter<? extends RecyclerView.ViewHolder>> extends WeakReferenceHandler<AbstractFileListActivity<T>> {
        private String searchInfo;

        ChangeWaitDialogHandler(final AbstractFileListActivity<T> activity) {
            super(activity);
        }

        @Override
        public void handleMessage(final Message msg) {
            final AbstractFileListActivity<T> activity = getReference();
            if (activity != null && msg.obj != null && activity.waitDialog != null) {
                if (searchInfo == null) {
                    searchInfo = activity.res.getString(R.string.file_searching_in) + " ";
                }
                if (msg.what == MSG_SEARCH_WHOLE_SD_CARD) {
                    searchInfo = String.format(activity.res.getString(R.string.file_searching_sdcard_in), getDefaultFolders(activity));
                }
                activity.waitDialog.setMessage(searchInfo + msg.obj);
            }
        }

        private String getDefaultFolders(@NonNull final AbstractFileListActivity<T> activity) {
            final List<String> names = new ArrayList<>();
            for (final File dir : activity.getExistingBaseFolders()) {
                names.add(dir.getPath());
            }
            return StringUtils.join(names, '\n');
        }
    }

    private static final class LoadFilesHandler<T extends RecyclerView.Adapter<? extends RecyclerView.ViewHolder>> extends WeakReferenceHandler<AbstractFileListActivity<T>> {
        LoadFilesHandler(final AbstractFileListActivity<T> activity) {
            super(activity);
        }

        @Override
        public void handleMessage(final Message msg) {
            final AbstractFileListActivity<T> activity = getReference();
            if (activity != null) {
                Dialogs.dismiss(activity.waitDialog);
                if (CollectionUtils.isEmpty(activity.files) && activity.requireFiles()) {
                    activity.showToast(activity.res.getString(R.string.file_list_no_files));
                    activity.finish();
                } else if (activity.adapter != null) {
                    activity.adapter.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.gpx);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            listId = extras.getInt(Intents.EXTRA_LIST_ID);
        }
        if (listId <= StoredList.TEMPORARY_LIST.id) {
            listId = StoredList.STANDARD_LIST_ID;
        }

        adapter = getAdapter(files);
        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.list, true, true);
        view.setAdapter(adapter);

        waitDialog = ProgressDialog.show(
                this,
                res.getString(R.string.file_title_searching),
                res.getString(R.string.file_searching),
                true,
                true,
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(final DialogInterface arg0) {
                        if (searchingThread != null && searchingThread.isAlive()) {
                            searchingThread.notifyEnd();
                        }
                        if (files.isEmpty() && requireFiles()) {
                            finish();
                        }
                    }
                }
                );

        searchingThread = new SearchFilesThread();
        searchingThread.start();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    protected boolean requireFiles() {
        return true;
    }

    protected abstract T getAdapter(List<File> files);

    /**
     * Gets the base folder for file searches
     *
     * @return The folder to start the recursive search in
     */
    protected abstract List<File> getBaseFolders();

    private class SearchFilesThread extends Thread {

        private final FileListSelector selector = new FileListSelector();

        public void notifyEnd() {
            selector.setShouldEnd();
        }

        @Override
        public void run() {
            final List<File> list = new ArrayList<>();

            try {
                if (EnvironmentUtils.isExternalStorageAvailable()) {
                    boolean loaded = false;
                    for (final File dir : getExistingBaseFolders()) {
                        FileUtils.listDir(list, dir, selector, changeWaitDialogHandler);
                        if (!list.isEmpty()) {
                            loaded = true;
                            break;
                        }
                    }
                    if (!loaded) {
                        changeWaitDialogHandler.sendMessage(Message.obtain(changeWaitDialogHandler, MSG_SEARCH_WHOLE_SD_CARD, Environment.getExternalStorageDirectory().getName()));
                        listDirs(list, LocalStorage.getStorages(), selector, changeWaitDialogHandler);
                    }
                } else {
                    Log.w("No external media mounted.");
                }
            } catch (final Exception e) {
                Log.e("AbstractFileListActivity.loadFiles.run", e);
            }

            changeWaitDialogHandler.sendMessage(Message.obtain(changeWaitDialogHandler, 0, "loaded directories"));

            files.addAll(list);
            Collections.sort(files, (lhs, rhs) -> TextUtils.COLLATOR.compare(lhs.getName(), rhs.getName()));

            runOnUiThread(() -> adapter.notifyDataSetChanged());

            loadFilesHandler.sendMessage(Message.obtain(loadFilesHandler));
        }

        private void listDirs(final List<File> list, final List<File> directories, final FileListSelector selector, final Handler feedbackHandler) {
            for (final File dir : directories) {
                FileUtils.listDir(list, dir, selector, feedbackHandler);
            }
        }
    }

    /**
     * Check if a filename belongs to the AbstractFileListActivity. This implementation checks for file extensions.
     * Subclasses may override this method to filter out specific files.
     *
     * @return {@code true} if the filename belongs to the list
     */
    protected boolean filenameBelongsToList(@NonNull final String filename) {
        for (final String ext : extensions) {
            if (StringUtils.endsWithIgnoreCase(filename, ext)) {
                return true;
            }
        }
        return false;
    }

    protected List<File> getExistingBaseFolders() {
        final List<File> result = new ArrayList<>();
        for (final File dir : getBaseFolders()) {
            if (dir.exists() && dir.isDirectory()) {
                result.add(dir);
            }
        }
        return result;
    }

    protected AbstractFileListActivity(final String extension) {
        setExtensions(new String[] { extension });
    }

    protected AbstractFileListActivity(final String[] extensions) {
        setExtensions(extensions);
    }

    private void setExtensions(final String[] extensionsIn) {
        extensions = extensionsIn;
        for (int i = 0; i < extensions.length; i++) {
            final String extension = extensions[i];
            if (StringUtils.isEmpty(extension) || extension.charAt(0) != '.') {
                extensions[i] = "." + extension;
            }
        }
    }

    private class FileListSelector implements FileUtils.FileSelector {

        boolean shouldEnd = false;

        @Override
        public boolean isSelected(final File file) {
            return filenameBelongsToList(file.getName());
        }

        @Override
        public synchronized boolean shouldEnd() {
            return shouldEnd;
        }

        public synchronized void setShouldEnd() {
            this.shouldEnd = true;
        }
    }

    @Override
    public void finish() {
        Dialogs.dismiss(waitDialog);
        super.finish();
    }
}
