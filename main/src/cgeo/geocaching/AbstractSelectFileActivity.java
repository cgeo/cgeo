package cgeo.geocaching;

import cgeo.geocaching.files.AbstractFileListActivity;
import cgeo.geocaching.files.FileSelectionListAdapter;
import cgeo.geocaching.files.IFileSelectionView;
import cgeo.geocaching.files.SimpleDirChooser;
import cgeo.geocaching.storage.LocalStorage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import org.openintents.intents.FileManagerIntents;

abstract class AbstractSelectFileActivity extends AbstractFileListActivity<FileSelectionListAdapter>  implements IFileSelectionView {

    protected static final int REQUEST_DIRECTORY = 1;

    @BindView(R.id.select_dir) protected Button selectElement;

    protected String filename;
    protected final String defaultFilename;
    private String outIntent = "";
    private boolean selectDir = false;
    private Context context = null;

    AbstractSelectFileActivity(final String extension, final String outIntent, final String defaultFilename, final boolean selectDir) {
        super(extension);
        this.outIntent = outIntent;
        this.defaultFilename = defaultFilename;
        this.selectDir = selectDir;
        this.context = context;
    }

    protected void setContext(final Context context) {
        this.context = context;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        filename = defaultFilename;

        selectElement.setOnClickListener(v -> {
            try {
                final Intent dirChooser = new Intent(selectDir ? FileManagerIntents.ACTION_PICK_DIRECTORY : FileManagerIntents.ACTION_PICK_FILE);
                dirChooser.putExtra(FileManagerIntents.EXTRA_TITLE, getString(selectDir ? R.string.simple_dir_chooser_title : R.string.simple_file_chooser_title));
                dirChooser.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getString(android.R.string.ok));
                startActivityForResult(dirChooser, REQUEST_DIRECTORY);
            } catch (final RuntimeException ignored) {
                // No file manager supporting open intents available - use our own SimpleDirChooser then
                final Intent dirChooser = new Intent(context, SimpleDirChooser.class);
                dirChooser.putExtra(Intents.EXTRA_START_DIR, LocalStorage.getExternalPublicCgeoDirectory().getAbsolutePath());
                dirChooser.putExtra(Intents.EXTRA_SELECTDIR, selectDir);
                startActivityForResult(dirChooser, REQUEST_DIRECTORY);
            }
        });
        selectElement.setText(getString(selectDir ? R.string.simple_dir_chooser_title : R.string.simple_file_chooser_title));
        selectElement.setVisibility(View.VISIBLE);
    }

    @Override
    public void close() {
        final Intent intent = new Intent();
        intent.putExtra(outIntent, filename);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected FileSelectionListAdapter getAdapter(final List<File> files) {
        return new FileSelectionListAdapter(this, files);
    }

    @Override
    public String getCurrentFile() {
        return filename;
    }

    @Override
    public void setCurrentFile(final String name) {
        filename = name;
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
            filename = new File(data.getData().getPath()).getAbsolutePath();
            close();
        }
    }

    @Override
    protected boolean requireFiles() {
        return false;
    }

}
