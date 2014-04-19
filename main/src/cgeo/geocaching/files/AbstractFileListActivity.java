package cgeo.geocaching.files;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractFileListActivity<T extends ArrayAdapter<File>> extends AbstractListActivity {
    private static final int MSG_SEARCH_WHOLE_SD_CARD = 1;

    private final List<File> files = new ArrayList<File>();
    private T adapter = null;
    private ProgressDialog waitDialog = null;
    private SearchFilesThread searchingThread = null;
    protected int listId = StoredList.STANDARD_LIST_ID;
    private String[] extensions;

    final private Handler changeWaitDialogHandler = new Handler() {
        private String searchInfo;

        @Override
        public void handleMessage(Message msg) {
            if (msg.obj != null && waitDialog != null) {
                if (searchInfo == null) {
                    searchInfo = res.getString(R.string.file_searching_in) + " ";
                }
                if (msg.what == MSG_SEARCH_WHOLE_SD_CARD) {
                    searchInfo = String.format(res.getString(R.string.file_searching_sdcard_in), getDefaultFolders());
                }
                waitDialog.setMessage(searchInfo + msg.obj);
            }
        }

        private String getDefaultFolders() {
            final ArrayList<String> names = new ArrayList<String>();
            for (File dir : getExistingBaseFolders()) {
                names.add(dir.getPath());
            }
            return StringUtils.join(names, '\n');
        }
    };

    final private Handler loadFilesHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (waitDialog != null) {
                waitDialog.dismiss();
            }
            if (CollectionUtils.isEmpty(files) && requireFiles()) {
                showToast(res.getString(R.string.file_list_no_files));
                finish();
            } else if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.gpx);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            listId = extras.getInt(Intents.EXTRA_LIST_ID);
        }
        if (listId <= StoredList.TEMPORARY_LIST_ID) {
            listId = StoredList.STANDARD_LIST_ID;
        }

        setAdapter();

        waitDialog = ProgressDialog.show(
                this,
                res.getString(R.string.file_title_searching),
                res.getString(R.string.file_searching),
                true,
                true,
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
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

    @SuppressWarnings("static-method")
    protected boolean requireFiles() {
        return true;
    }

    protected abstract T getAdapter(List<File> files);

    private void setAdapter() {
        if (adapter == null) {
            adapter = getAdapter(files);
            setListAdapter(adapter);
        }
    }

    /**
     * Gets the base folder for file searches
     *
     * @return The folder to start the recursive search in
     */
    protected abstract List<File> getBaseFolders();

    private class SearchFilesThread extends Thread {

        private final FileListSelector selector = new FileListSelector();

        public void notifyEnd() {
            selector.setShouldEnd(true);
        }

        @Override
        public void run() {
            final List<File> list = new ArrayList<File>();

            try {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
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
            } catch (Exception e) {
                Log.e("AbstractFileListActivity.loadFiles.run", e);
            }

            changeWaitDialogHandler.sendMessage(Message.obtain(changeWaitDialogHandler, 0, "loaded directories"));

            files.addAll(list);
            Collections.sort(files, new Comparator<File>() {

                @Override
                public int compare(File lhs, File rhs) {
                    return lhs.getName().compareToIgnoreCase(rhs.getName());
                }
            });

            loadFilesHandler.sendMessage(Message.obtain(loadFilesHandler));
        }

        private void listDirs(List<File> list, List<File> directories, FileListSelector selector, Handler feedbackHandler) {
            for (final File dir : directories) {
                FileUtils.listDir(list, dir, selector, feedbackHandler);
            }
        }
    }

    /**
     * Check if a filename belongs to the AbstractFileListActivity. This implementation checks for file extensions.
     * Subclasses may override this method to filter out specific files.
     *
     * @param filename
     * @return <code>true</code> if the filename belongs to the list
     */
    protected boolean filenameBelongsToList(final String filename) {
        for (String ext : extensions) {
            if (StringUtils.endsWithIgnoreCase(filename, ext)) {
                return true;
            }
        }
        return false;
    }

    protected List<File> getExistingBaseFolders() {
        ArrayList<File> result = new ArrayList<File>();
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

    private class FileListSelector extends FileUtils.FileSelector {

        boolean shouldEnd = false;

        @Override
        public boolean isSelected(File file) {
            return filenameBelongsToList(file.getName());
        }

        @Override
        public synchronized boolean shouldEnd() {
            return shouldEnd;
        }

        public synchronized void setShouldEnd(boolean shouldEnd) {
            this.shouldEnd = shouldEnd;
        }
    }
}
