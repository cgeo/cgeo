package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.storage.extension.EmojiLRU;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EmojiUtils {

    private EmojiUtils() {
        // utility class
    }

    public static void selectEmojiPopup(final Activity activity, final int currentValue, @DrawableRes final int defaultRes, final Action1<Integer> setNewCacheIcon) {

        final EmojiViewAdapter gridAdapter;
        final EmojiViewAdapter lruAdapter;

        // calc sizes
        final int markerHeight = DisplayUtils.getDrawableHeight(activity.getResources(), R.drawable.marker_oc);
        final int markerAvailable = (int) (markerHeight * 0.6);
        final int markerFontsize = DisplayUtils.calculateMaxFontsize(35, 10, 100, markerAvailable);

        // data to populate the emoji selector with
        // !! those values are for testing purposes only currently !!
        // @todo: set actual list of characters to be supported
        final int[] emojiList = new int[50];
        for (int i = 0; i < 50; i++) {
            emojiList[i] = 0x1f334 + i;
        }
        final int[] lru = EmojiLRU.getLRU();

        final View dialogView = activity.getLayoutInflater().inflate(R.layout.emojiselector, null);

        final int maxCols = DisplayUtils.calculateNoOfColumns(activity, 40);
        final RecyclerView emojiGrid = dialogView.findViewById(R.id.emoji_grid);
        emojiGrid.setLayoutManager(new GridLayoutManager(activity, maxCols));
        final RecyclerView emojiLru = dialogView.findViewById(R.id.emoji_lru);
        emojiLru.setLayoutManager(new GridLayoutManager(activity, maxCols));

        final View customTitle = activity.getLayoutInflater().inflate(R.layout.dialog_title_button_button, null);
        final AlertDialog dialog = Dialogs.newBuilder(activity)
            .setView(dialogView)
            .setCustomTitle(customTitle)
            .create();

        gridAdapter = new EmojiViewAdapter(activity, emojiList, newCacheIcon -> onItemSelected(dialog, setNewCacheIcon, newCacheIcon));
        emojiGrid.setAdapter(gridAdapter);

        lruAdapter = new EmojiViewAdapter(activity, lru, newCacheIcon -> onItemSelected(dialog, setNewCacheIcon, newCacheIcon));
        emojiLru.setAdapter(lruAdapter);

        ((TextView) customTitle.findViewById(R.id.dialog_title_title)).setText(R.string.cache_menu_set_cache_icon);

        // right button displays current value; clicking simply closes the dialog
        final ImageButton button2 = customTitle.findViewById(R.id.dialog_button2);
        button2.setVisibility(View.VISIBLE);
        if (currentValue != 0) {
            button2.setImageDrawable(getEmojiDrawable(activity.getResources(), markerHeight, markerAvailable, markerFontsize, currentValue));
        } else if (defaultRes != 0) {
            button2.setImageResource(defaultRes);
        }
        button2.setOnClickListener(v -> dialog.dismiss());

        // left button displays default value (if different from current value)
        if (currentValue != 0 && defaultRes != 0) {
            final ImageButton button1 = customTitle.findViewById(R.id.dialog_button1);
            button1.setVisibility(View.VISIBLE);
            button1.setImageResource(defaultRes);
            button1.setOnClickListener(v -> setNewCacheIcon.call(0));
        }

        dialog.show();
    }

    private static void onItemSelected(final AlertDialog dialog, final Action1<Integer> setNewCacheIcon, final int newCacheIcon) {
        dialog.dismiss();
        EmojiLRU.add(newCacheIcon);
        setNewCacheIcon.call(newCacheIcon);
    }

    private static class EmojiViewAdapter extends RecyclerView.Adapter<EmojiViewAdapter.ViewHolder> {

        private final int[] data;
        private final LayoutInflater inflater;
        private final Action1<Integer> setNewCacheIcon;

        EmojiViewAdapter(final Context context, final int[] data, final Action1<Integer> setNewCacheIcon) {
            this.inflater = LayoutInflater.from(context);
            this.data = data;
            this.setNewCacheIcon = setNewCacheIcon;
        }

        @Override
        @NonNull
        public EmojiViewAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = inflater.inflate(R.layout.emojiselector_item, parent, false);
            return new EmojiViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final EmojiViewAdapter.ViewHolder holder, final int position) {
            holder.tv.setText(new String(Character.toChars(data[position])));
            holder.bind(data[position], setNewCacheIcon);
        }

        @Override
        public int getItemCount() {
            return data.length;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            protected TextView tv;

            ViewHolder(final View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.info_text);
            }

            public void bind(final int item, final Action1<Integer> setNewCacheIcon) {
                itemView.setOnClickListener(v -> setNewCacheIcon.call(item));
            }
        }
    }

    /**
     * builds a drawable the size of a marker with a given text
     * @param res - the resources to use
     * @param bitmapSize - actual size of the bitmap to place the text in
     * @param availableSize - available size
     * @param fontsize - fontsize to use
     * @param emoji codepoint of the emoji to display
     * @return drawable bitmap with text on it
     */
    public static BitmapDrawable getEmojiDrawable(final Resources res, final int bitmapSize, final int availableSize, final int fontsize, final int emoji) {
        final String text = new String(Character.toChars(emoji));
        final TextPaint tPaint = new TextPaint();
        tPaint.setTextSize(fontsize);
        final Bitmap bm = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bm);
        final StaticLayout lsLayout = new StaticLayout(text, tPaint, availableSize, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        final int deltaTopLeft = (int) (0.3 * (bitmapSize - availableSize));
        canvas.translate(deltaTopLeft, deltaTopLeft + (int) ((availableSize - lsLayout.getHeight()) / 2));
        lsLayout.draw(canvas);
        canvas.save();
        canvas.restore();
        return new BitmapDrawable(res, bm);
    }
}
