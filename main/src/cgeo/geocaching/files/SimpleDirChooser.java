package cgeo.geocaching.files;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.TextUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Dialog for choosing a file or directory.
 */
public class SimpleDirChooser extends AbstractListActivity {
    public static final String EXTRA_CHOOSE_FOR_WRITING = "chooseForWriting";
    private static final String PARENT_DIR = "..        ";
    private File currentDir;
    private FileArrayAdapter adapter;
    private Button okButton = null;
    private int lastPosition = -1;
    private boolean chooseForWriting = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle extras = getIntent().getExtras();
        currentDir = dirContaining(extras.getString(Intents.EXTRA_START_DIR));
        chooseForWriting = extras.getBoolean(EXTRA_CHOOSE_FOR_WRITING, false);

        ActivityMixin.setTheme(this);
        setContentView(R.layout.simple_dir_chooser);

        fill(currentDir);

        okButton = findViewById(R.id.simple_dir_chooser_ok);
        resetOkButton();
        okButton.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent()
                    .setData(Uri.fromFile(new File(currentDir, adapter.getItem(lastPosition).getName()))));
            finish();
        });

        final Button cancelButton = (Button) findViewById(R.id.simple_dir_chooser_cancel);
        cancelButton.setOnClickListener(v -> {
            final Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        });

        final EditText pathField = findViewById(R.id.simple_dir_chooser_path);
        pathField.setOnClickListener(v -> editPath());
    }

    public void editPath() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.simple_dir_chooser_current_path);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentDir.getPath());
        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            final String pathText = input.getText().toString();
            final File newPathDir = new File(pathText);
            if (newPathDir.exists() && newPathDir.isDirectory()) {
                currentDir = newPathDir;
                fill(currentDir);
            } else {
                showToast(SimpleDirChooser.this.getString(R.string.simple_dir_chooser_invalid_path));
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Return the directory containing a given path, or a sensible default.
     *
     * @param path the path to get the enclosing directory from, can be null or empty
     * @return the directory containing {@code path}, or a sensible default if none
     */
    private static File dirContaining(final String path) {
        return StringUtils.contains(path, File.separatorChar) ?
                new File(StringUtils.substringBeforeLast(path, Character.toString(File.separatorChar))) :
                Environment.getExternalStorageDirectory();
    }

    private void fill(final File dir) {
        lastPosition = -1;
        resetOkButton();
        final EditText path = findViewById(R.id.simple_dir_chooser_path);
        path.setText(this.getString(R.string.simple_dir_chooser_current_path) + " " + dir.getAbsolutePath());
        final File[] dirs = dir.listFiles(new DirOnlyFilenameFilter());
        final List<Option> listDirs = new ArrayList<>();
        if (dirs != null) {
            for (final File currentDir : dirs) {
                listDirs.add(new Option(currentDir.getName(), currentDir.getAbsolutePath(), currentDir.canWrite()));
            }
        }
        Collections.sort(listDirs, Option.NAME_COMPARATOR);
        if (dir.getParent() != null) {
            listDirs.add(0, new Option(PARENT_DIR, dir.getParent(), false));
        }
        this.adapter = new FileArrayAdapter(this, R.layout.simple_dir_item, listDirs);
        this.setListAdapter(adapter);
    }

    private void resetOkButton() {
        if (okButton != null) {
            okButton.setEnabled(false);
            okButton.setVisibility(View.INVISIBLE);
        }
    }

    public class FileArrayAdapter extends ArrayAdapter<Option> {

        private final Context context;
        @LayoutRes
        private final int id;
        private final List<Option> items;

        public FileArrayAdapter(final Context context, @LayoutRes final int simpleDirItemResId, final List<Option> objects) {
            super(context, simpleDirItemResId, objects);
            this.context = context;
            this.id = simpleDirItemResId;
            this.items = objects;
        }

        @Override
        public Option getItem(final int index) {
            return items.get(index);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                final LayoutInflater vi = LayoutInflater.from(context);
                v = vi.inflate(id, null);
            }

            final Option option = items.get(position);
            if (option != null) {
                final TextView t1 = v.findViewById(R.id.TextView01);
                if (t1 != null) {
                    t1.setOnClickListener(new OnTextViewClickListener(position));
                    t1.setText(option.getName());
                }
                final CheckBox check = v.findViewById(R.id.CheckBox);
                if (check != null) {
                    if (!chooseForWriting || option.isWriteable()) {
                        check.setOnClickListener(new OnCheckBoxClickListener(position));
                        check.setChecked(option.isChecked());
                        check.setEnabled(true);
                    } else {
                        check.setEnabled(false);
                    }
                }
            }
            return v;
        }
    }

    public class OnTextViewClickListener implements OnClickListener {
        private final int position;

        OnTextViewClickListener(final int position) {
            this.position = position;
        }

        @Override
        public void onClick(final View arg0) {
            final Option option = adapter.getItem(position);
            if (option.getName().equals(PARENT_DIR)) {
                currentDir = new File(option.getPath());
                fill(currentDir);
            } else {
                final File dir = new File(option.getPath());
                final String[] subDirs = dir.list(new DirOnlyFilenameFilter());
                if (ArrayUtils.isNotEmpty(subDirs)) {
                    currentDir = dir;
                    fill(currentDir);
                }
            }
        }
    }

    public class OnCheckBoxClickListener implements OnClickListener {
        private final int position;

        OnCheckBoxClickListener(final int position) {
            this.position = position;
        }

        @Override
        public void onClick(final View arg0) {
            final Option lastOption = lastPosition > -1 ? adapter.getItem(lastPosition) : null;
            final Option currentOption = adapter.getItem(position);
            if (lastOption != null) {
                lastOption.setChecked(false);
            }
            if (currentOption != lastOption) {
                currentOption.setChecked(true);
                lastPosition = position;
            } else {
                lastPosition = -1;
            }
            final boolean enabled = currentOption.isChecked() && !currentOption.getName().equals(PARENT_DIR);
            okButton.setEnabled(enabled);
            okButton.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    public static class Option {
        private final String name;
        private final String path;
        private boolean checked = false;
        private boolean writeable = false;

        private static final Comparator<Option> NAME_COMPARATOR = (lhs, rhs) -> TextUtils.COLLATOR.compare(lhs.name, rhs.name);

        public Option(final String name, final String path, final boolean writeable) {
            this.name = name;
            this.path = path;
            this.writeable = writeable;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public boolean isChecked() {
            return this.checked;
        }

        public void setChecked(final boolean checked) {
            this.checked = checked;
        }

        public boolean isWriteable() {
            return writeable;
        }
    }

    public static class DirOnlyFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(final File dir, final String filename) {
            final File file = new File(dir, filename);
            return file.isDirectory() && file.canRead();
        }

    }
}
