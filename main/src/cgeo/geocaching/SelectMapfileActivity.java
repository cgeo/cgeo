package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.files.AbstractFileListActivity;
import cgeo.geocaching.files.IFileSelectionView;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.files.SimpleDirChooser;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.FileSelectionListAdapter;

import org.openintents.intents.FileManagerIntents;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectMapfileActivity extends AbstractFileListActivity<FileSelectionListAdapter> implements IFileSelectionView {

    public SelectMapfileActivity() {
        super("map");
    }

    @InjectView(R.id.select_dir) protected Button selectDirectory;

    private String mapFile;

    private static int REQUEST_DIRECTORY = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.inject(this);

        mapFile = Settings.getMapFile();

        selectDirectory.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    final Intent dirChooser = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);
                    dirChooser.putExtra(FileManagerIntents.EXTRA_TITLE,
                            getString(R.string.simple_dir_chooser_title));
                    dirChooser.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT,
                            getString(android.R.string.ok));
                    startActivityForResult(dirChooser, REQUEST_DIRECTORY);
                } catch (android.content.ActivityNotFoundException ex) {
                    // OI file manager not available
                    final Intent dirChooser = new Intent(SelectMapfileActivity.this, SimpleDirChooser.class);
                    dirChooser.putExtra(Intents.EXTRA_START_DIR, LocalStorage.getStorage().getAbsolutePath());
                    startActivityForResult(dirChooser, REQUEST_DIRECTORY);
                }
            }
        });
        selectDirectory.setText(getResources().getString(R.string.simple_dir_chooser_title));
        selectDirectory.setVisibility(View.VISIBLE);
    }

    @Override
    public void close() {

        Intent intent = new Intent();
        intent.putExtra(Intents.EXTRA_MAP_FILE, mapFile);

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
        for (File dir : LocalStorage.getStorages()) {
            folders.add(new File(dir, "mfmaps"));
            folders.add(new File(new File(dir, "Locus"), "mapsVector"));
            folders.add(new File(dir, LocalStorage.cache));
        }
        return folders;
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

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_DIRECTORY) {
            final String directory = new File(data.getData().getPath()).getAbsolutePath();
            mapFile = directory;
            close();
        }
    }

    @Override
    protected boolean requireFiles() {
        return false;
    }
}
