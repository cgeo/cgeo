package cgeo.geocaching;

import cgeo.geocaching.files.FileList;
import cgeo.geocaching.files.IFileSelectionView;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.ui.FileSelectionListAdapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class cgSelectMapfile extends FileList<FileSelectionListAdapter> implements IFileSelectionView {

    public cgSelectMapfile() {
        super("map");
    }

    String mapFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapFile = Settings.getMapFile();
    }

    @Override
    public void close() {

        Intent intent = new Intent();
        intent.putExtra("mapfile", mapFile);

        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    protected FileSelectionListAdapter getAdapter(List<File> files) {
        return new FileSelectionListAdapter(this, files);
    }

    @Override
    protected List<File> getBaseFolders() {
        List<File> folders = new ArrayList<File>();
        for (File dir : getStorages()) {
            folders.add(new File(dir, "mfmaps"));
            folders.add(new File(new File(dir, "Locus"), "mapsVector"));
            folders.add(new File(dir, LocalStorage.cache));
        }
        return folders;
    }

    @Override
    protected void setTitle() {
        setTitle(res.getString(R.string.map_file_select_title));
    }

    @Override
    public String getCurrentFile() {
        return mapFile;
    }

    @Override
    public void setCurrentFile(String newFile) {
        mapFile = newFile;
    }

    @Override
    public Context getContext() {
        return this;
    }

}
