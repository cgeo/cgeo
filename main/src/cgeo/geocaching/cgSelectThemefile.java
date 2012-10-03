package cgeo.geocaching;

import cgeo.geocaching.files.FileList;
import cgeo.geocaching.files.IFileSelectionView;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.ui.FileSelectionListAdapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.util.List;

public class cgSelectThemefile extends FileList<FileSelectionListAdapter> implements IFileSelectionView {

    public cgSelectThemefile() {
        super("xml");
    }

    String themeFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeFile = Settings.getCustomRenderThemePath();
    }

    @Override
    public void close() {

        Intent intent = new Intent();
        intent.putExtra("themefile", themeFile);

        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    protected FileSelectionListAdapter getAdapter(List<File> files) {
        return new FileSelectionListAdapter(this, files);
    }

    @Override
    protected File[] getBaseFolders() {
        final File base = Environment.getExternalStorageDirectory();
        return new File[] {
                new File(new File(base, "mfmaps"), "_themes"),
                new File(new File(new File(base, "Locus"), "mapsVector"), "_themes"),
                new File(base, LocalStorage.cache)
        };
    }

    @Override
    protected void setTitle() {
        setTitle(res.getString(R.string.theme_file_select_title));
    }

    @Override
    public String getCurrentFile() {
        return themeFile;
    }

    @Override
    public void setCurrentFile(String newFile) {
        themeFile = newFile;
    }

    @Override
    public Context getContext() {
        return this;
    }

}
