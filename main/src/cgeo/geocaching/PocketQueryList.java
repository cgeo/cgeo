package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.utils.RunnableWithArgument;

import org.apache.commons.collections4.CollectionUtils;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.concurrency.Schedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;

import java.util.List;

public final class PocketQueryList {

    private final String guid;
    private final int maxCaches;
    private final String name;

    public PocketQueryList(final String guid, final String name, final int maxCaches) {
        this.guid = guid;
        this.name = name;
        this.maxCaches = maxCaches;
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

    public static void promptForListSelection(final Activity activity, final RunnableWithArgument<PocketQueryList> runAfterwards) {
        final Dialog waitDialog = ProgressDialog.show(activity, activity.getString(R.string.search_pocket_title), activity.getString(R.string.search_pocket_loading), true, true);

        AndroidObservable.fromActivity(activity, Observable.create(new OnSubscribeFunc<List<PocketQueryList>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super List<PocketQueryList>> observer) {
                observer.onNext(GCParser.searchPocketQueryList());
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }).subscribeOn(Schedulers.threadPoolForIO())).subscribe(new Action1<List<PocketQueryList>>() {
            @Override
            public void call(final List<PocketQueryList> pocketQueryLists) {
                waitDialog.dismiss();
                selectFromPocketQueries(activity, pocketQueryLists, runAfterwards);
            }
        });
    }
    private static void selectFromPocketQueries(final Activity activity, final List<PocketQueryList> pocketQueryList, final RunnableWithArgument<PocketQueryList> runAfterwards) {
        if (CollectionUtils.isEmpty(pocketQueryList)) {
            ActivityMixin.showToast(activity, activity.getString(R.string.warn_no_pocket_query_found));
            return;
        }

        final CharSequence[] items = new CharSequence[pocketQueryList.size()];

        for (int i = 0; i < pocketQueryList.size(); i++) {
            items[i] = pocketQueryList.get(i).name;
        }

        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.search_pocket_select))
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, final int itemId) {
                        dialogInterface.dismiss();
                        runAfterwards.run(pocketQueryList.get(itemId));
                    }
                }).create().show();
    }

}
