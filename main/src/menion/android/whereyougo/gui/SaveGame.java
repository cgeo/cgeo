/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package menion.android.whereyougo.gui;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import cz.matejcik.openwig.Engine;
import cgeo.geocaching.R;
import menion.android.whereyougo.openwig.WUI;
import menion.android.whereyougo.utils.Logger;

public class SaveGame extends AsyncTask<Void, Void, Void> {

    protected static final String TAG = "SaveGame";
    protected ProgressDialog dialog;
    protected Context context;

    public SaveGame(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        Engine.requestSync();
        dialog = ProgressDialog.show(context, null, context.getString(R.string.working));
    }

    @Override
    protected Void doInBackground(Void... params) {
        // let thread sleep for a while to be sure that cartridge is saved!
        try {
            while (WUI.saving) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        try {
            if (dialog != null) {
                dialog.cancel();
                dialog = null;
            }
        } catch (Exception e) {
            Logger.w(TAG, "onPostExecute(), e:" + e.toString());
        }
    }
}
