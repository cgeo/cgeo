/*
 * Copyright 2014 biylda <biylda@gmail.com>
 *
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

package menion.android.whereyougo.network.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import java.io.File;

import cgeo.geocaching.R;
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;
import menion.android.whereyougo.gui.activity.XmlSettingsActivity;
import menion.android.whereyougo.gui.dialog.PositiveButtonActionCustomizableDialogFragment;
import menion.android.whereyougo.gui.extension.activity.CustomActivity;
import menion.android.whereyougo.gui.extension.dialog.CustomDialog;
import menion.android.whereyougo.network.DownloadCartridgeTask;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.utils.FileSystem;
import menion.android.whereyougo.utils.Images;
import menion.android.whereyougo.utils.ManagerNotify;
import menion.android.whereyougo.utils.UtilsFormat;

public class DownloadCartridgeActivity extends CustomActivity
        implements PositiveButtonActionCustomizableDialogFragment.PositiveButtonActionCustomizableDialogListener {
    private DownloadCartridgeTask downloadTask;
    private String cguid;
    private String username;
    private String password;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Uri uri = getIntent().getData();
            cguid = uri.getQueryParameter("CGUID");
        } catch (Exception e) {
        }
        if (cguid == null) {
            finish();
            return;
        }

        username = Preferences.GC_USERNAME;
        password = Preferences.GC_PASSWORD;

        setContentView(R.layout.layout_details);

        TextView tvName = (TextView) findViewById(R.id.layoutDetailsTextViewName);
        tvName.setText(R.string.download_cartridge);
        Button buttonStart = (Button) findViewById(R.id.button_negative);
        adjustDownloadButtonToUsernamePasswordState();

        TextView tvDescription = (TextView) findViewById(R.id.layoutDetailsTextViewDescription);
        TextView tvState = (TextView) findViewById(R.id.layoutDetailsTextViewState);

        File cartridgeFile = FileSystem.findFile(cguid);
        if (cartridgeFile != null) {
            tvDescription.setText(
                    String.format("CGUID:\n%s\n%s",
                            cguid,
                            cartridgeFile.getName().replace(cguid + "_", "")
                    ));
            tvState.setText(
                    String.format("%s\n%s",
                            getString(R.string.download_successful),
                            UtilsFormat.formatDatetime(cartridgeFile.lastModified())
                    ));
        } else {
            tvDescription.setText(String.format("CGUID:\n%s", cguid));
        }

        ImageView ivImage = (ImageView) findViewById(R.id.mediaImageView);
        ivImage.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
        try {
            Bitmap icon = Images.getImageB(R.drawable.icon_gc_wherigo);
            ivImage.setImageBitmap(icon);
        } catch (Exception e) {
        }

        CustomDialog.setBottom(this, getString(R.string.download), new CustomDialog.OnClickListener() {
            @Override
            public boolean onClick(CustomDialog dialog, View v, int btn) {
                if (downloadTask != null && downloadTask.getStatus() != Status.FINISHED) {
                    downloadTask.cancel(true);
                    downloadTask = null;
                } else {
                    downloadTask = new DownloadTask(DownloadCartridgeActivity.this, username, password);
                    downloadTask.execute(cguid);
                }
                return true;

            }
        }, null, null, getString(R.string.start), new CustomDialog.OnClickListener() {
            @Override
            public boolean onClick(CustomDialog dialog, View v, int btn) {
                Intent intent = new Intent(DownloadCartridgeActivity.this, WhereYouGoActivity.class);
                intent.putExtra("cguid", cguid);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                DownloadCartridgeActivity.this.finish();
                return true;
            }
        });
        buttonStart.setEnabled(cartridgeFile != null);

        if (checkEmptyUsernamePassword()) {
            Bundle args = new Bundle();
            args.putInt("message", R.string.dialog_no_password);
            PositiveButtonActionCustomizableDialogFragment positiveButtonActionCustomizableDialogFragment = new PositiveButtonActionCustomizableDialogFragment();
            positiveButtonActionCustomizableDialogFragment.setArguments(args);
            positiveButtonActionCustomizableDialogFragment.show(getSupportFragmentManager(), "NoUsernameOrPassword");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        username = Preferences.GC_USERNAME;
        password = Preferences.GC_PASSWORD;
        adjustDownloadButtonToUsernamePasswordState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (downloadTask != null && downloadTask.getStatus() != Status.FINISHED) {
            downloadTask.cancel(true);
            downloadTask = null;
        }
    }

    /**
     * This method implements the Interface "PositiveButtonActionCustomizableDialogListener".
     */
    @Override
    public void onPositiveClick(DialogFragment dialog) {
        Intent loginPreferenceIntent = new Intent(DownloadCartridgeActivity.this, XmlSettingsActivity.class);
        loginPreferenceIntent.putExtra(getString(R.string.pref_KEY_X_LOGIN_PREFERENCES), true);
        startActivity(loginPreferenceIntent);
    }

    private void adjustDownloadButtonToUsernamePasswordState(){
        Button buttonDownload = (Button) findViewById(R.id.button_positive);
        // If one of the variables is empty the inner condition is true which get's negated because
        // the button get's enabled on true and disabled on false.
        buttonDownload.setEnabled(!checkEmptyUsernamePassword());
    }

    private boolean checkEmptyUsernamePassword() {
        return username.isEmpty() || password.isEmpty();
    }

    class DownloadTask extends DownloadCartridgeTask {
        final ProgressDialog progressDialog;

        public DownloadTask(final Context context, String username, String password) {
            super(context, username, password);
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(1);
            progressDialog.setIndeterminate(true);
            progressDialog.setCanceledOnTouchOutside(false);

            progressDialog.setCancelable(true);
            progressDialog.setOnCancelListener(arg0 -> {
                if (downloadTask != null && downloadTask.getStatus() != Status.FINISHED) {
                    downloadTask.cancel(false);
                    downloadTask = null;
                    Log.i("down", "cancel");
                    ManagerNotify.toastShortMessage(context, getString(R.string.cancelled));
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            super.onPostExecute(result);
            if (result) {
                progressDialog.dismiss();
                WhereYouGoActivity.refreshCartridges();
                DownloadCartridgeActivity.this.finish();
                DownloadCartridgeActivity.this.startActivity(DownloadCartridgeActivity.this.getIntent());
            } else {
                progressDialog.setIndeterminate(false);
            }
            downloadTask = null;
        }

        @Override
        protected void onProgressUpdate(Progress... values) {
            super.onProgressUpdate(values);
            Progress progress = values[0];
            String suffix = "";
            if (progress.getState() == State.SUCCESS) {
                suffix = String.format(": %s", getString(R.string.ok));
            } else if (progress.getState() == State.FAIL) {
                if (progress.getMessage() == null) {
                    suffix = String.format(": %s", getString(R.string.error));
                } else {
                    suffix = String.format(": %s(%s)", getString(R.string.error), progress.getMessage());
                }
            }
            switch (progress.getTask()) {
                case INIT:
                case PING:
                    progressDialog.setIndeterminate(true);
                    progressDialog.setMessage(Html.fromHtml(getString(R.string.download_state_connect) + suffix));
                    break;
                case LOGIN:
                    progressDialog.setIndeterminate(true);
                    progressDialog.setMessage(Html.fromHtml(getString(R.string.download_state_login) + suffix));
                    if (progress.getState() == State.FAIL) {
                        Bundle args = new Bundle();
                        args.putInt("message", R.string.dialog_wrong_credentials);
                        PositiveButtonActionCustomizableDialogFragment positiveButtonActionCustomizableDialogFragment = new PositiveButtonActionCustomizableDialogFragment();
                        positiveButtonActionCustomizableDialogFragment.setArguments(args);
                        positiveButtonActionCustomizableDialogFragment.show(getSupportFragmentManager(), "NoUsernameOrPassword");
                        progressDialog.cancel();
                    }
                    break;
                case LOGOUT:
                    progressDialog.setIndeterminate(true);
                    progressDialog.setMessage(Html.fromHtml(getString(R.string.download_state_logout) + suffix));
                    break;
                case DOWNLOAD:
                    progressDialog.setIndeterminate(true);
                    progressDialog.setMessage(Html.fromHtml(getString(R.string.download_state_download) + suffix));
                    break;
                case DOWNLOAD_SINGLE:
                    progressDialog.setIndeterminate(false);
                    progressDialog.setMax((int) progress.getTotal());
                    progressDialog.setProgress((int) progress.getCompleted());
                    progressDialog.setMessage(Html.fromHtml(getString(R.string.download_state_download) + suffix));
                    break;
            }
        }

    }
}

