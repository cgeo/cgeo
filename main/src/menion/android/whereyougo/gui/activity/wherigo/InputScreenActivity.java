/*
 * This file is part of WhereYouGo.
 *
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package menion.android.whereyougo.gui.activity.wherigo;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;
import cgeo.geocaching.R;
import menion.android.whereyougo.gui.extension.activity.MediaActivity;
import menion.android.whereyougo.gui.extension.dialog.CustomDialog;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.preferences.Locale;
import menion.android.whereyougo.utils.A;
import se.krka.kahlua.vm.LuaTable;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class InputScreenActivity extends MediaActivity {

    private static final String TAG = "InputScreen";
    private static EventTable input;
    private enum InputType { NONE, TEXT, MULTI }
    private InputType inputType = InputType.NONE;

    public static void setInput(EventTable input) {
        InputScreenActivity.input = input;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (A.getMain() == null || Engine.instance == null || input == null || input.table == null) {
            finish();
            return;
        }
        setContentView(R.layout.layout_input);

        Media media = (Media) input.table.rawget("Media");
        setMedia(media);

        // set question TextView
        TextView tvQuestion = (TextView) findViewById(R.id.layoutInputTextView);
        String text = (String) input.table.rawget("Text");
        tvQuestion.setText(UtilsGUI.simpleHtml(text));

        // set answer
        final EditText editText = (EditText) findViewById(R.id.layoutInputEditText);
        editText.setVisibility(View.GONE);
        final Button scanButton = (Button) findViewById(R.id.layoutInputScanButton);
        scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(InputScreenActivity.this);
            integrator.initiateScan();
        });
        scanButton.setVisibility(View.GONE);
        final Spinner spinner = (Spinner) findViewById(R.id.layoutInputSpinner);
        spinner.setVisibility(View.GONE);
        String type = (String) input.table.rawget("InputType");

        if ("Text".equals(type)) {
            editText.setText("");
            editText.setVisibility(View.VISIBLE);
            scanButton.setVisibility(View.VISIBLE);
            inputType = InputType.TEXT;
        } else if ("MultipleChoice".equals(type)) {
            LuaTable choices = (LuaTable) input.table.rawget("Choices");
            String[] data = new String[choices.len()];
            for (int i = 0; i < choices.len(); i++) {
                data[i] = (String) choices.rawget((double) (i + 1));
                if (data[i] == null)
                    data[i] = "-";
            }

            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setVisibility(View.VISIBLE);
            inputType = InputType.MULTI;
        }

        CustomDialog.setBottom(this, Locale.getString(R.string.answer), (dialog, v, btn) -> {
            if (inputType == InputType.TEXT) {
                Engine.callEvent(input, "OnGetInput", editText.getText()
                        .toString());
            } else if (inputType == InputType.MULTI) {
                String item = String.valueOf(spinner.getSelectedItem());
                Engine.callEvent(input, "OnGetInput", item);
            } else {
                Engine.callEvent(input, "OnGetInput", null);
            }
            InputScreenActivity.this.finish();
            return true;
        }, null, null, null, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            Engine.callEvent(input, "OnGetInput", null);
            InputScreenActivity.this.finish();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null && result.getContents() != null) {
            final EditText editText = (EditText) findViewById(R.id.layoutInputEditText);
            editText.setText(result.getContents());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
