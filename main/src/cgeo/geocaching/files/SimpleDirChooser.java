/**
 *
 */
package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dialog for choosing a file or directory.
 */
public class SimpleDirChooser extends ListActivity {
    public static final String START_DIR = "start_dir";
    private static final String PARENT_DIR = "..        ";
    private File currentDir;
    private FileArrayAdapter adapter;
    private CheckBox lastBoxChecked = null;
    private Button okButton = null;
    private String checkedText = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle extras = getIntent().getExtras();
        String startDir = extras.getString(START_DIR);
        if (startDir == null) {
            startDir = "/sdcard";
        } else {
            startDir = startDir.substring(0, startDir.lastIndexOf(File.separatorChar));
        }
        currentDir = new File(startDir);

        ActivityMixin.setTheme(this);
        setContentView(R.layout.simple_dir_chooser);
        setTitle(this.getResources().getString(R.string.simple_dir_chooser_title));

        fill(currentDir);

        okButton = (Button) findViewById(R.id.simple_dir_chooser_ok);
        okButton.setEnabled(false);
        okButton.setVisibility(View.INVISIBLE);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                String chosenDirName = File.separator + checkedText;
                intent.putExtra("chosenDir", currentDir.getAbsolutePath() + chosenDirName);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.simple_dir_chooser_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finish();
            }
        });
    }

    private void fill(File dir) {
        EditText path = (EditText) findViewById(R.id.simple_dir_chooser_path);
        path.setText(this.getResources().getString(R.string.simple_dir_chooser_current_path) + " " + dir.getAbsolutePath());
        final File[] dirs = dir.listFiles(new DirOnlyFilenameFilter());
        List<Option> listDirs = new ArrayList<Option>();
        try {
            for (File currentDir : dirs) {
                listDirs.add(new Option(currentDir.getName(), currentDir.getAbsolutePath()));
            }
        } catch (Exception e) {
        }
        Collections.sort(listDirs);
        if (dir.getParent() != null) {
            listDirs.add(0, new Option(PARENT_DIR, dir.getParent()));
        }
        this.adapter = new FileArrayAdapter(SimpleDirChooser.this, R.layout.simple_dir_item, listDirs);
        this.setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    public class FileArrayAdapter extends ArrayAdapter<Option> {

        private Context c;
        private int id;
        private List<Option> items;

        public FileArrayAdapter(Context context, int simpleDirItemResId, List<Option> objects) {
            super(context, simpleDirItemResId, objects);
            c = context;
            id = simpleDirItemResId;
            items = objects;
        }

        @Override
        public Option getItem(int i) {
            return items.get(i);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(id, null);
            }

            final Option o = items.get(position);
            if (o != null) {
                TextView t1 = (TextView) v.findViewById(R.id.TextView01);
                if (t1 != null) {
                    t1.setOnClickListener(new OnTextViewClickListener(position));
                    t1.setText(o.getName());
                }
                CheckBox check = (CheckBox) v.findViewById(R.id.CheckBox);
                if (check != null) {
                    check.setOnClickListener(new OnCheckBoxClickListener(o.getName()));
                }
            }
            return v;
        }
    }

    public class OnTextViewClickListener implements OnClickListener {
        private int position;

        OnTextViewClickListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View arg0) {
            Option o = adapter.getItem(position);
            if (o.getName().equals(PARENT_DIR)) {
                currentDir = new File(o.getPath());
                fill(currentDir);
            } else {
                File dir = new File(o.getPath());
                if (dir.list(new DirOnlyFilenameFilter()).length > 0) {
                    currentDir = dir;
                    fill(currentDir);
                }
            }
        }
    }

    public class OnCheckBoxClickListener implements OnClickListener {
        private String checkedText;

        OnCheckBoxClickListener(String checkedText) {
            this.checkedText = checkedText;
        }

        @Override
        public void onClick(View arg0) {
            CheckBox check = (CheckBox) arg0;
            if (lastBoxChecked == check) {
                check.setChecked(false);
                lastBoxChecked = null;
                okButton.setEnabled(false);
                okButton.setVisibility(View.INVISIBLE);
                SimpleDirChooser.this.checkedText = "";
            } else {
                if (lastBoxChecked != null) {
                    lastBoxChecked.setChecked(false);
                }
                check.setChecked(true);
                lastBoxChecked = check;
                okButton.setEnabled(true);
                okButton.setVisibility(View.VISIBLE);
                SimpleDirChooser.this.checkedText = checkedText;
            }
        }
    }

    public class Option implements Comparable<Option> {
        private String name;
        private String path;

        public Option(String n, String p) {
            name = n;
            path = p;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        @Override
        public int compareTo(Option o) {
            if (o != null && this.name != null) {
                return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
            }
            throw new IllegalArgumentException("");
        }
    }

    public class DirOnlyFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            return file.isDirectory();
        }

    }
}
