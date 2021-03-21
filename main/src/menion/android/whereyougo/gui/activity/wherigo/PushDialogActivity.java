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

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.Media;
import cgeo.geocaching.R;
import menion.android.whereyougo.gui.extension.activity.MediaActivity;
import menion.android.whereyougo.gui.extension.dialog.CustomDialog;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.preferences.Locale;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.Logger;
import se.krka.kahlua.vm.LuaClosure;

public class PushDialogActivity extends MediaActivity {

    private static final String TAG = "PushDialog";

    private static String menu01Text = null;
    private static String menu02Text = null;

    // STATIC CONTENT
    private static String[] texts;
    private static Media[] media;
    private static LuaClosure callback;
    private static int page = -1;
    private TextView tvText;

    public static void setDialog(String[] texts, Media[] media, String button1, String button2,
                                 LuaClosure callback) {
        synchronized (PushDialogActivity.class) {
            PushDialogActivity.texts = texts;
            PushDialogActivity.media = media;
            PushDialogActivity.callback = callback;
            PushDialogActivity.page = -1;

            if (button1 == null)
                button1 = Locale.getString(R.string.ok);

            menu01Text = button1;
            menu02Text = button2;
            Logger.d(TAG, "setDialog() - finish, callBack:" + (callback != null));
        }
    }

    private void nextPage() {
        synchronized (PushDialogActivity.class) {
            Logger.d(TAG, "nextpage() - page:" + page + ", texts:" + texts.length + ", callback:"
                    + (callback != null));
            page++;
            if (page >= texts.length) {
                if (callback != null) {
                    LuaClosure call = callback;
                    callback = null;
                    Engine.invokeCallback(call, "Button1");
                }
                PushDialogActivity.this.finish();
                return;
            }

            setMedia(media[page]);

            tvText.setText(UtilsGUI.simpleHtml(texts[page]));
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (A.getMain() == null || Engine.instance == null) {
            finish();
            return;
        }
        setContentView(R.layout.layout_details);
        findViewById(R.id.layoutDetailsTextViewName).setVisibility(View.GONE);
        findViewById(R.id.layoutDetailsTextViewState).setVisibility(View.GONE);
        findViewById(R.id.layoutDetailsTextViewDistance).setVisibility(View.GONE);
        tvText = (TextView) findViewById(R.id.layoutDetailsTextViewDescription);

        if (menu02Text == null || menu02Text.length() == 0) {
            menu02Text = null;
        }

        CustomDialog.setBottom(this, menu01Text, (dialog, v, btn) -> {
            nextPage();
            return true;
        }, null, null, menu02Text, (dialog, v, btn) -> {
            if (callback != null)
                Engine.invokeCallback(callback, "Button2");
            callback = null;
            PushDialogActivity.this.finish();
            return true;
        });

        if (page == -1) {
            nextPage();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Logger.d(TAG, "onKeyDown(" + keyCode + ", " + event + ")");
        return event.getKeyCode() == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }
}
