package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
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
import java.util.List;

public abstract class FileList<T extends ArrayAdapter<File>> extends AbstractListActivity {
    private static final int MSG_SEARCH_WHOLE_SD_CARD = 1;

    private List<File> files = new ArrayList<File>();
    private T adapter = null;
    private ProgressDialog waitDialog = null;
    private loadFiles searchingThread = null;
    private boolean endSearching = false;
    private int listId = StoredList.STANDARD_LIST_ID;
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
                waitDialog.setMessage(searchInfo + (String) msg.obj);
            }
        }

        private String getDefaultFolders() {
            StringBuilder sb = new StringBuilder();
            for (File f : getBaseFolders()) {
                String fName = f.getPath();
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(fName);
            }
            return sb.toString();
        }
    };

    final private Handler loadFilesHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (waitDialog != null) {
                waitDialog.dismiss();
            }
            if (CollectionUtils.isEmpty(files)) {
                showToast(res.getString(R.string.file_list_no_files));
                finish();
            } else if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };
    private String[] extensions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.gpx);
        setTitle();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            listId = extras.getInt("list");
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
                        if (files.isEmpty()) {
                            finish();
                        }
                    }
                }
                );

        endSearching = false;
        searchingThread = new loadFiles();
        searchingThread.start();
    }

    @Override
    public void onResume() {
        super.onResume();

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
    protected abstract File[] getBaseFolders();

    /**
     * Triggers the deriving class to set the title
     */
    protected abstract void setTitle();

    private class loadFiles extends Thread {
        public void notifyEnd() {
            endSearching = true;
        }

        @Override
        public void run() {
            List<File> list = new ArrayList<File>();

            try {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    boolean loaded = false;
                    for (final File dir : getBaseFolders())
                    {
                        if (dir.exists() && dir.isDirectory()) {
                            listDir(list, dir);
                            if (list.size() > 0) {
                                loaded = true;
                                break;
                            }
                        }
                    }
                    if (!loaded) {
                        changeWaitDialogHandler.sendMessage(Message.obtain(changeWaitDialogHandler, MSG_SEARCH_WHOLE_SD_CARD, Environment.getExternalStorageDirectory().getName()));
                        listDir(list, Environment.getExternalStorageDirectory());
                    }
                } else {
                    Log.w("No external media mounted.");
                }
            } catch (Exception e) {
                Log.e("cgFileList.loadFiles.run: " + e.toString());
            }

            changeWaitDialogHandler.sendMessage(Message.obtain(changeWaitDialogHandler, 0, "loaded directories"));

            files.addAll(list);
            list.clear();

            loadFilesHandler.sendMessage(Message.obtain(loadFilesHandler));
        }
    }

    private void listDir(List<File> result, File directory) {
        if (directory == null || !directory.isDirectory() || !directory.canRead()) {
            return;
        }

        final File[] files = directory.listFiles();

        if (ArrayUtils.isNotEmpty(files)) {
            for (File file : files) {
                if (endSearching) {
                    return;
                }
                if (!file.canRead()) {
                    continue;
                }
                String name = file.getName();
                if (file.isFile()) {
                    if (filenameBelongsToList(name)) {
                        result.add(file); // add file to list
                    }
                } else if (file.isDirectory()) {
                    if (name.charAt(0) == '.') {
                        continue; // skip hidden directories
                    }
                    if (name.length() > 16) {
                        name = name.substring(0, 14) + '…';
                    }
                    changeWaitDialogHandler.sendMessage(Message.obtain(changeWaitDialogHandler, 0, name));

                    listDir(result, file); // go deeper
                }
            }
        }

    }

    /**
     * Check if a filename belongs to the FileList. This implementation checks for file extensions.
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

    protected FileList(final String extension) {
        setExtensions(new String[] { extension });
    }

    protected FileList(final String[] extensions) {
        setExtensions(extensions);
    }

    private void setExtensions(final String[] extensionsIn) {
        extensions = extensionsIn;
        for (int i = 0; i < extensions.length; i++) {
            String extension = extensions[i];
            if (extension.length() == 0 || extension.charAt(0) != '.') {
                extensions[i] = "." + extension;
            }
        }
    }
}
