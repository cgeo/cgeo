package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.utils.RunnableWithArgument;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;

import java.util.List;

public final class PocketQueryList {

    private final String guid;
    private final int maxCaches;
    private final String name;

    public PocketQueryList(String guid, String name, int maxCaches) {
        this.guid = guid;
        this.name = name;
        this.maxCaches = maxCaches;
    }

    public static class UserInterface {

        List<PocketQueryList> pocketQueryList = null;
        RunnableWithArgument<PocketQueryList> runAfterwards;

        private Handler loadPocketQueryHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                if ((pocketQueryList == null) || (pocketQueryList.size() == 0)) {
                    if (waitDialog != null) {
                        waitDialog.dismiss();
                    }

                    ActivityMixin.showToast(activity, res.getString(R.string.warn_no_pocket_query_found));

                    return;
                }

                if (waitDialog != null) {
                    waitDialog.dismiss();
                }

                final CharSequence[] items = new CharSequence[pocketQueryList.size()];

                for (int i = 0; i < pocketQueryList.size(); i++) {
                    PocketQueryList pq = pocketQueryList.get(i);

                    items[i] = pq.name + " (" + pq.maxCaches + ")";

                }

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(res.getString(R.string.search_pocket_select));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int itemId) {
                        runAfterwards.run(pocketQueryList.get(itemId));
                    }
                });
                builder.create().show();

            }
        };

        private class LoadPocketQueryListThread extends Thread {
            final private Handler handler;

            public LoadPocketQueryListThread(Handler handlerIn) {
                handler = handlerIn;
            }

            @Override
            public void run() {
                pocketQueryList = GCParser.searchPocketQueryList();
                handler.sendMessage(Message.obtain());
            }
        }

        private final Activity activity;
        private final cgeoapplication app;
        private final Resources res;
        private ProgressDialog waitDialog = null;

        public UserInterface(final Activity activity) {
            this.activity = activity;
            app = cgeoapplication.getInstance();
            res = app.getResources();
        }

        public void promptForListSelection(final RunnableWithArgument<PocketQueryList> runAfterwards) {

            this.runAfterwards = runAfterwards;

            waitDialog = ProgressDialog.show(activity, res.getString(R.string.search_pocket_title), res.getString(R.string.search_pocket_loading), true, true);

            LoadPocketQueryListThread thread = new LoadPocketQueryListThread(loadPocketQueryHandler);
            thread.start();
        }


    }

    public String getGuid() {
        return guid;
    }

    public int getMaxCaches() {
        return maxCaches;
    }

    public String getName() {
        return name;
    }

}
