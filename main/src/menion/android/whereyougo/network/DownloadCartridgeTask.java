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

package menion.android.whereyougo.network;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import menion.android.whereyougo.utils.FileSystem;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.Utils;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DownloadCartridgeTask extends
        AsyncTask<String, DownloadCartridgeTask.Progress, Boolean> {
    private static final String TAG = "DownloadCartridgeTask";
    private static final String LOGIN = "https://www.wherigo.com/login/default.aspx";
    private static final String DOWNLOAD = "https://www.wherigo.com/cartridge/download.aspx";

    private final String username;
    private final String password;
    private OkHttpClient httpClient;
    private String errorMessage;

    public DownloadCartridgeTask(Context context, String username, String password) {
        super();
        this.username = username;
        this.password = password;
    }

    @Override
    protected Boolean doInBackground(String... arg0) {
        return init() && ping() && login() && download(arg0) && logout();
    }

    private boolean init() {
        try {
            System.setProperty("http.keepAlive", "false");
            httpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(new TLSSocketFactory())
                    .cookieJar(new NonPersistentCookieJar())
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            Logger.e(TAG, "init()", e);
            errorMessage = e.getMessage();
        }
        if (httpClient == null)
            publishProgress(new Progress(Task.INIT, State.FAIL, errorMessage));
        return httpClient != null;
    }

    private boolean ping() {
        Request request = new Request.Builder()
                .url(LOGIN)
                .build();
        return handleRequest(request, Task.PING) != null;
    }

    private boolean login() {
        RequestBody formBody = new FormBody.Builder()
                .add("__EVENTTARGET", "")
                .add("__EVENTARGUMENT", "")
                .add("ctl00$ContentPlaceHolder1$Login1$Login1$UserName", username)
                .add("ctl00$ContentPlaceHolder1$Login1$Login1$Password", password)
                .add("ctl00$ContentPlaceHolder1$Login1$Login1$LoginButton", "Sign In")
                .build();
        Request request = new Request.Builder()
                .url(LOGIN)
                .post(formBody)
                .build();
        publishProgress(new Progress(Task.LOGIN, State.WORKING));
        Response response = handleRequest(request);
        if (response != null && !LOGIN.equals(response.request().url().toString())) {
            publishProgress(new Progress(Task.LOGIN, State.SUCCESS));
            return true;
        } else {
            publishProgress(new Progress(Task.LOGIN, State.FAIL, errorMessage));
            return false;
        }
    }

    private boolean logout() {
        RequestBody formBody = new FormBody.Builder()
                .add("__EVENTTARGET", "ctl00$ProfileWidget$LoginStatus1$ctl00")
                .add("__EVENTARGUMENT", "")
                .build();
        Request request = new Request.Builder()
                .url(LOGIN)
                .post(formBody)
                .build();
        return handleRequest(request, Task.LOGOUT) != null;
    }

    private boolean download(String[] cguid) {
        publishProgress(new Progress(Task.DOWNLOAD, State.WORKING));
        for (int i = 0; i < cguid.length; ++i) {
            if (download(cguid[i])) {
                publishProgress(new Progress(Task.DOWNLOAD, i, cguid.length));
            } else {
                publishProgress(new Progress(Task.DOWNLOAD, State.FAIL, errorMessage));
                return false;
            }
        }
        return true;
    }

    private boolean download(String cguid) {
        RequestBody formBody = new FormBody.Builder()
                .add("__EVENTTARGET", "")
                .add("__EVENTARGUMENT", "")
                .add("ctl00$ContentPlaceHolder1$uxDeviceList", "4")
                .add("ctl00$ContentPlaceHolder1$btnDownload", "Download Now")
                .build();
        Request request = new Request.Builder()
                .url(DOWNLOAD + "?CGUID=" + cguid)
                .post(formBody)
                .build();
        Response response = handleRequest(request);
        if (response != null) {
            String type = response.body().contentType().toString();
            if ("application/octet-stream".equals(type)) {
                String contentDisposition = response.header("Content-Disposition", "");
                String pattern = "(?i)^ *attachment *; *filename *= *(.*) *$";
                String filename;
                if (contentDisposition.matches(pattern)) {
                    filename = cguid + "_" + contentDisposition.replaceFirst(pattern, "$1");
                } else {
                    filename = cguid + ".gwc";
                }
                long length = Long.parseLong(response.header("Content-Length", "0"));
                return download(filename, response.body().byteStream(), length);
            }
        }
        return false;
    }

    private boolean download(String filename, InputStream input, long total) {
        File file = new File(FileSystem.ROOT + filename);
        long completed = 0;
        int length;
        byte[] buffer = new byte[1024];
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(input);
            bos = new BufferedOutputStream(new FileOutputStream(file));
            publishProgress(new Progress(Task.DOWNLOAD_SINGLE, completed, total));
            while ((length = bis.read(buffer)) > 0 && !isCancelled()) {
                bos.write(buffer, 0, length);
                completed += length;
                publishProgress(new Progress(Task.DOWNLOAD_SINGLE, completed, total));
            }
        } catch (IOException e) {
            Logger.e(TAG, "download(" + filename + ")", e);
            errorMessage = e.getMessage();
        } finally {
            Utils.closeStream(bis);
            Utils.closeStream(bos);
            if (completed != total) {
                file.delete();
            }
        }
        return completed == total;
    }

    private Response handleRequest(Request request, Task task) {
        publishProgress(new Progress(task, State.WORKING));
        Response response = handleRequest(request);
        if (response != null)
            publishProgress(new Progress(task, State.SUCCESS));
        else
            publishProgress(new Progress(task, State.FAIL, errorMessage));
        return response;
    }

    private Response handleRequest(Request request) {
        if (isCancelled())
            return null;
        Response response;
        try {
            response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Request " + request.toString() + " failed: " + response);
            }
        } catch (Exception e) {
            Logger.e(TAG, "handleRequest(" + request.toString() + ")", e);
            errorMessage = e.getMessage();
            return null;
        }
        return response;
    }

    public enum Task {
        INIT, PING, LOGIN, DOWNLOAD, DOWNLOAD_SINGLE, LOGOUT
    }

    public enum State {
        WORKING, SUCCESS, FAIL
    }

    public static class Progress {
        final Task task;
        final State state;
        long total;
        long completed;
        String message;

        public Progress(Task task, State state) {
            this.task = task;
            this.state = state;
        }

        public Progress(Task task, State state, String message) {
            this.task = task;
            this.state = state;
            this.message = message;
        }

        public Progress(Task task, long completed, long total) {
            this.state = State.WORKING;
            this.task = task;
            this.total = total;
            this.completed = completed;
        }

        public Task getTask() {
            return task;
        }

        public State getState() {
            return state;
        }

        public String getMessage() {
            return message;
        }

        public long getTotal() {
            return total;
        }

        public long getCompleted() {
            return completed;
        }
    }
}
