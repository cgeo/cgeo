package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class FileList<T extends ArrayAdapter<File>> extends AbstractListActivity {
    private static final int MSG_SEARCH_WHOLE_SD_CARD = 1;

    private final List<File> files = new ArrayList<File>();
    private T adapter = null;
    private ProgressDialog waitDialog = null;
    private SearchFilesThread searchingThread = null;
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
                waitDialog.setMessage(searchInfo + msg.obj);
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
        searchingThread = new SearchFilesThread();
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
    protected abstract List<File> getBaseFolders();

    /**
     * Triggers the deriving class to set the title
     */
    protected abstract void setTitle();

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
                    for (final File dir : getBaseFolders())
                    {
                        if (dir.exists() && dir.isDirectory()) {
                            FileUtils.listDir(list, dir,selector,changeWaitDialogHandler);
                            if (!list.isEmpty()) {
                                loaded = true;
                                break;
                            }
                        }
                    }
                    if (!loaded) {
                        changeWaitDialogHandler.sendMessage(Message.obtain(changeWaitDialogHandler, MSG_SEARCH_WHOLE_SD_CARD, Environment.getExternalStorageDirectory().getName()));
                        listDirs(list, getStorages(), selector, changeWaitDialogHandler);
                    }
                } else {
                    Log.w("No external media mounted.");
                }
            } catch (Exception e) {
                Log.e("cgFileList.loadFiles.run: " + e.toString());
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
    }

    private void listDirs(List<File> list, List<File> directories, FileListSelector selector, Handler feedbackHandler) {
        for (final File dir : directories) {
            FileUtils.listDir(list, dir, selector, feedbackHandler);
        }
    }

    /*
     * Get all storages available on the device.
     * Will include paths like /mnt/sdcard /mnt/usbdisk /mnt/ext_card /mnt/sdcard/ext_card
     */
    protected static List<File> getStorages() {

        String extStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        List<File> storages = new ArrayList<File>();
        storages.add(new File(extStorage));
        File file = new File("/system/etc/vold.fstab");
        if (file.canRead()) {
            FileReader fr = null;
            BufferedReader br = null;
            try {
                fr = new FileReader(file);
                br = new BufferedReader(fr);
                String s = br.readLine();
                while (s != null) {
                    if (s.startsWith("dev_mount")) {
                        String[] tokens = StringUtils.split(s);
                        if (tokens.length >= 3) {
                            String path = tokens[2]; // mountpoint
                            if (!extStorage.equals(path)) {
                                File directory = new File(path);
                                if (directory.exists() && directory.isDirectory()) {
                                    storages.add(directory);
                                }
                            }
                        }
                    }
                    s = br.readLine();
                }
            } catch (IOException e) {
                Log.e("Could not get additional mount points for user content. " +
                        "Proceeding with external storage only (" + extStorage + ")");
            } finally {
                try {
                    if (fr != null) {
                        fr.close();
                    }
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException e) {
                }
            }
        }
        return storages;
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

    private class FileListSelector extends FileUtils.FileSelector {

        boolean _shouldEnd = false;

        @Override
        public boolean isSelected(File file) {
            return filenameBelongsToList(file.getName());
        }

        @Override
        public boolean shouldEnd() {
            return _shouldEnd;
        }

        public void setShouldEnd(boolean shouldEnd) {
            _shouldEnd = shouldEnd;
        }
    }
}
