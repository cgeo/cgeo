package cgeo.geocaching;

import cgeo.geocaching.files.FileList;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.ui.MapfileListAdapter;

import android.content.Intent;
import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class cgSelectMapfile extends FileList<MapfileListAdapter> {

    public cgSelectMapfile() {
        super("map");
    }

    String mapFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapFile = Settings.getMapFile();
    }

    public void close() {

        Intent intent = new Intent();
        intent.putExtra("mapfile", mapFile);

        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    protected MapfileListAdapter getAdapter(List<File> files) {
        return new MapfileListAdapter(this, files);
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

    public String getCurrentMapfile() {
        return mapFile;
    }

    public void setMapfile(String newMapfile) {
        mapFile = newMapfile;
    }

}
