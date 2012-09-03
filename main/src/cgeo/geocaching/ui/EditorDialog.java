package cgeo.geocaching.ui;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

public class EditorDialog extends Dialog {

    private CharSequence editorText;
    private EditorUpdate editorUpdate;

    public EditorDialog(CacheDetailActivity cacheDetailActivity, CharSequence editable) {
        super(cacheDetailActivity);
        this.editorText = editable;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.editor);

        final EditText editText = (EditText) findViewById(R.id.editorEditText);
        editText.setText(editorText);

        final Button buttonSave = (Button) findViewById(R.id.editorSave);
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorUpdate.update(editText.getEditableText());
                EditorDialog.this.hide();
            }
        });
    }

    public interface EditorUpdate {
        public void update(CharSequence editorText);
    }

    public void setOnEditorUpdate(EditorUpdate editorUpdate) {
        this.editorUpdate = editorUpdate;

    }

    @Override
    public void show() {
        super.show();
        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
    }

}
