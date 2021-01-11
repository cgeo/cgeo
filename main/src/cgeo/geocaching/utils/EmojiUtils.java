package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.storage.extension.EmojiLRU;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class EmojiUtils {

    // internal consts for calculated circles
    private static final int COLOR_VALUES = 3;  // supported # of different values per RGB
    private static final int COLOR_SPREAD = 127;
    private static final int COLOR_OFFSET = 0;
    private static final int OPACITY_VALUES = 5;
    private static final int OPACITY_SPREAD = 51;
    private static final int CUSTOM_SET_SIZE_PER_OPACITY = COLOR_VALUES * COLOR_VALUES * COLOR_VALUES;
    private static final int CUSTOM_SET_SIZE = CUSTOM_SET_SIZE_PER_OPACITY * OPACITY_VALUES;

    // Unicode custom glyph area needed/supported by this class
    private static final int CUSTOM_ICONS_START = 0xe000;
    private static final int CUSTOM_ICONS_END = CUSTOM_ICONS_START + CUSTOM_SET_SIZE - 1; // max. possible value by Unicode definition: 0xf8ff;
    private static final int CUSTOM_ICONS_START_CIRCLES = CUSTOM_ICONS_START;

    // list of emojis supported by the EmojiPopup
    // should ideally be supported by the Android API level we have set as minimum (currently API 21 = Android 5),
    // but starting with API 23 (Android 6) the app automatically filters out characters not supported by their fonts
    // for a list of supported Unicode standards by API level see https://developer.android.com/guide/topics/resources/internationalization
    // for characters by Unicode version see https://unicode.org/emoji/charts-5.0/full-emoji-list.html (v5.0)

    private static final EmojiSet[] symbols = {
        // category symbols
        new EmojiSet(0x2764, new int[]{
            /* hearts */        0x2764, 0x1f9e1, 0x1f49b, 0x1f49a, 0x1f499, 0x1f49c, 0x1f90e, 0x1f5a4, 0x1f90d,
            /* geometric */     0x1f7e5, 0x1f7e7, 0x1f7e8, 0x1f7e9, 0x1f7e6, 0x1f7ea, 0x1f7eb, 0x2b1b, 0x2b1c,
            /* geometric */     0x1f536, 0x1f537,
            /* events */        0x1f383, 0x1f380,
            /* office */        0x1f4c6,
            /* warning */       0x26a0, 0x26d4, 0x1f6ab, 0x1f6d1, 0x2622,
            /* av-symbol */     0x1f505, 0x1f506,
            /* other-symbol */  0x2b55, 0x2705, 0x2611, 0x2714, 0x2716, 0x2795, 0x2796, 0x274c, 0x274e, 0x2733, 0x2734, 0x2747, 0x203c, 0x2049, 0x2753, 0x2757,
            /* flags */         0x1f3c1, 0x1f6a9, 0x1f3f4, 0x1f3f3
        }),
        // category custom symbols - will be filled dynamically below; has to be at position CUSTOM_GLYPHS_ID within EmojiSet[]
        new EmojiSet(CUSTOM_ICONS_START_CIRCLES + 18 + CUSTOM_SET_SIZE_PER_OPACITY, new int[CUSTOM_SET_SIZE_PER_OPACITY]),    // "18" to pick red circle instead of black
        // category places
        new EmojiSet(0x1f30d, new int[]{
            /* globe */         0x1f30d, 0x1f30e, 0x1f30f,
            /* geographic */    0x26f0, 0x1f3d4, 0x1f3d6, 0x1f3dc, 0x1f3dd, 0x1f3de,
            /* buildings */     0x1f3e0, 0x1f3e2, 0x1f3da, 0x1f3e5, 0x1f3f0, 0x16cf, 0x1f5fd,
            /* other */         0x26f2, 0x2668, 0x1f3ad, 0x1f3a8,
            /* plants */        0x1f332, 0x1f333, 0x1f334, 0x1f335, 0x1f340,
            /* transport */     0x1f682, 0x1f683, 0x1f686, 0x1f687, 0x1f68d, 0x1f695, 0x1f6b2, 0x1f697, 0x1f699, 0x2693, 0x26f5, 0x2708, 0x1f680,
            /* transp.-sign */  0x267f, 0x1f6bb
        }),
        // category food
        new EmojiSet(0x2615, new int[]{
            /* fruits */        0x1f34a, 0x1f34b, 0x1f34d, 0x1f34e, 0x1f34f, 0x1f95d, 0x1f336, 0x1f344,
            /* other */         0x1f968, 0x1f354, 0x1f355,
            /* food-sweet */    0x1f366, 0x1f370, 0x1f36d,
            /* drink */         0x1f964, 0x2615, 0x1f37a
        }),
        // category activity
        new EmojiSet(0x1f3c3, new int[]{
            /* person-sport */  0x26f7, 0x1f3c4, 0x1f6a3, 0x1f3ca, 0x1f6b4,
            /* p.-activity */   0x1f9d7,
            /* sport */         0x26bd, 0x1f94e, 0x1f3c0, 0x1f3c8, 0x1f93f, 0x1f3bf, 0x1f3af, 0x1f9ff,
            /* tool */          0x1fa9c,
            /* science */       0x2697, 0x1f9ea,

        }),
        // category people
        new EmojiSet(0x1f600, new int[]{
            /* smileys */       0x1f600, 0x1f60d, 0x1f641, 0x1f621, 0x1f47b,
            /* people */        0x1f466, 0x1f467, 0x1f468, 0x1f469, 0x1f474, 0x1f475
        }),
    };
    private static final int CUSTOM_GLYPHS_ID = 1;

    private static boolean fontIsChecked = false;
    private static final Boolean lockGuard = false;
    private static final SparseArray<CacheMarker> emojiCache = new SparseArray<>();

    private static class EmojiSet {
        public int tabSymbol;
        public int remaining;
        public int[] symbols;

        EmojiSet(final int tabSymbol, final int[] symbols) {
            this.tabSymbol = tabSymbol;
            this.remaining = symbols.length;
            this.symbols = symbols;
        }
    }

    private EmojiUtils() {
        // utility class
    }

    public static void selectEmojiPopup(final Activity activity, final int currentValue, @DrawableRes final int defaultRes, final Action1<Integer> setNewCacheIcon) {

        // calc sizes for markers
        final Pair<Integer, Integer> markerDimensionsTemp = DisplayUtils.getDrawableDimensions(activity.getResources(), R.drawable.ic_menu_filter);
        final int markerAvailable = (int) (markerDimensionsTemp.first * 0.5);
        final Pair<Integer, Integer> markerDimensions = new Pair<>(markerAvailable, markerAvailable);
        final EmojiPaint paint = new EmojiPaint(activity.getResources(), markerDimensions, markerAvailable, 0, DisplayUtils.calculateMaxFontsize(35, 10, 150, markerAvailable));

        // fill dynamic EmojiSet
        prefillCustomCircles(0);

        // check EmojiSet for characters not supported on this device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            synchronized (lockGuard) {
                if (!fontIsChecked) {
                    final Paint checker = new TextPaint();
                    checker.setTypeface(Typeface.DEFAULT);
                    for (EmojiSet symbol : symbols) {
                        int iPosNeu = 0;
                        for (int i = 0; i < symbol.symbols.length; i++) {
                            if (i != iPosNeu) {
                                symbol.symbols[iPosNeu] = symbol.symbols[i];
                            }
                            if ((symbol.symbols[i] >= CUSTOM_ICONS_START && symbol.symbols[i] <= CUSTOM_ICONS_END) || checker.hasGlyph(new String(Character.toChars(symbol.symbols[i])))) {
                                iPosNeu++;
                            }
                        }
                        symbol.remaining = iPosNeu;
                    }
                    fontIsChecked = true;
                }
            }
        }

        // create dialog
        final View dialogView = activity.getLayoutInflater().inflate(R.layout.emojiselector, null);
        final View customTitle = activity.getLayoutInflater().inflate(R.layout.dialog_title_button_button, null);
        final AlertDialog dialog = Dialogs.newBuilder(activity)
            .setView(dialogView)
            .setCustomTitle(customTitle)
            .create();

        final int maxCols = DisplayUtils.calculateNoOfColumns(activity, 60);
        final RecyclerView emojiGridView = dialogView.findViewById(R.id.emoji_grid);
        emojiGridView.setLayoutManager(new GridLayoutManager(activity, maxCols));
        final EmojiViewAdapter gridAdapter = new EmojiViewAdapter(activity, paint, symbols[0].symbols, symbols[0].remaining, currentValue, false, newCacheIcon -> onItemSelected(dialog, setNewCacheIcon, newCacheIcon));
        emojiGridView.setAdapter(gridAdapter);

        final RecyclerView emojiOpacityView = dialogView.findViewById(R.id.emoji_opacity);
        emojiOpacityView.setLayoutManager(new GridLayoutManager(activity, OPACITY_VALUES));
        final int[] emojiOpacities = new int[OPACITY_VALUES];
        emojiOpacities[0] = CUSTOM_ICONS_START_CIRCLES + 18;
        for (int i = 1; i < OPACITY_VALUES; i++) {
            emojiOpacities[i] = emojiOpacities[i - 1] + CUSTOM_SET_SIZE_PER_OPACITY;
        }
        final EmojiViewAdapter opacityAdapter = new EmojiViewAdapter(activity, paint, emojiOpacities, emojiOpacities.length, emojiOpacities[0], true, newgroup -> {
            final int newOpacity = (newgroup - CUSTOM_ICONS_START_CIRCLES) / CUSTOM_SET_SIZE_PER_OPACITY;
            prefillCustomCircles(newOpacity);
            gridAdapter.notifyDataSetChanged();
        });
        emojiOpacityView.setAdapter(opacityAdapter);

        final RecyclerView emojiGroupView = dialogView.findViewById(R.id.emoji_groups);
        emojiGroupView.setLayoutManager(new GridLayoutManager(activity, symbols.length));
        final int[] emojiGroups = new int[symbols.length];
        for (int i = 0; i < symbols.length; i++) {
            emojiGroups[i] = symbols[i].tabSymbol;
        }
        final EmojiViewAdapter groupsAdapter = new EmojiViewAdapter(activity, paint, emojiGroups, emojiGroups.length, symbols[0].tabSymbol, true, newgroup -> {
            int newGroupId = 0;
            for (int i = 0; i < symbols.length; i++) {
                if (symbols[i].tabSymbol == newgroup) {
                    gridAdapter.setData(symbols[i].symbols, symbols[i].remaining);
                    newGroupId = i;
                }
                emojiOpacityView.setVisibility(newGroupId == CUSTOM_GLYPHS_ID ? View.VISIBLE : View.GONE);
            }
        });
        emojiGroupView.setAdapter(groupsAdapter);

        final RecyclerView emojiLruView = dialogView.findViewById(R.id.emoji_lru);
        emojiLruView.setLayoutManager(new GridLayoutManager(activity, maxCols));
        final int[] lru = EmojiLRU.getLRU();
        final EmojiViewAdapter lruAdapter = new EmojiViewAdapter(activity, paint, lru, lru.length, 0, false, newCacheIcon -> onItemSelected(dialog, setNewCacheIcon, newCacheIcon));
        emojiLruView.setAdapter(lruAdapter);

        ((TextView) customTitle.findViewById(R.id.dialog_title_title)).setText(R.string.cache_menu_set_cache_icon);

        // right button displays current value; clicking simply closes the dialog
        final ImageButton button2 = customTitle.findViewById(R.id.dialog_button2);
        button2.setVisibility(View.VISIBLE);
        if (currentValue == -1) {
            button2.setImageResource(R.drawable.ic_menu_mark);
        } else if (currentValue != 0) {
            button2.setImageDrawable(getEmojiDrawable(paint, currentValue));
        } else if (defaultRes != 0) {
            button2.setImageResource(defaultRes);
        }
        button2.setOnClickListener(v -> dialog.dismiss());

        // left button displays default value (if different from current value)
        if (currentValue != 0 && defaultRes != 0) {
            final ImageButton button1 = customTitle.findViewById(R.id.dialog_button1);
            button1.setVisibility(View.VISIBLE);
            button1.setImageResource(defaultRes);
            button1.setOnClickListener(v -> {
                setNewCacheIcon.call(0);
                dialog.dismiss();
            });
        }

        dialog.show();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Dialogs.basicOneTimeMessage(activity, OneTimeDialogs.DialogType.MISSING_UNICODE_CHARACTERS);
        }
    }

    private static void onItemSelected(final AlertDialog dialog, final Action1<Integer> callback, final int selectedValue) {
        dialog.dismiss();
        EmojiLRU.add(selectedValue);
        callback.call(selectedValue);
    }

    private static void prefillCustomCircles(final int opacity) {
        for (int i = 0; i < CUSTOM_SET_SIZE_PER_OPACITY; i++) {
            symbols[CUSTOM_GLYPHS_ID].symbols[i] = CUSTOM_ICONS_START_CIRCLES + i + opacity * CUSTOM_SET_SIZE_PER_OPACITY;
        }
    }

    private static class EmojiViewAdapter extends RecyclerView.Adapter<EmojiViewAdapter.ViewHolder> {

        private int[] data;
        private int remaining;
        private final LayoutInflater inflater;
        private final Action1<Integer> callback;
        private final EmojiPaint paint;
        private int currentValue = 0;
        private final boolean highlightCurrent;

        EmojiViewAdapter(final Context context, final EmojiPaint paint, final int[] data, final int remaining, final int currentValue, final boolean hightlightCurrent, final Action1<Integer> callback) {
            this.inflater = LayoutInflater.from(context);
            this.paint = paint;
            this.setData(data, remaining);
            this.currentValue = currentValue;
            this.highlightCurrent = hightlightCurrent;
            this.callback = callback;
        }

        public void setData(final int[] data, final int remaining) {
            this.data = data;
            this.remaining = remaining;
            notifyDataSetChanged();
        }

        @Override
        @NonNull
        public EmojiViewAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = inflater.inflate(R.layout.emojiselector_item, parent, false);
            return new EmojiViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final EmojiViewAdapter.ViewHolder holder, final int position) {
            if (data[position] >= CUSTOM_ICONS_START && data[position] <= CUSTOM_ICONS_END) {
                holder.iv.setImageDrawable(EmojiUtils.getEmojiDrawable(paint, data[position]));
                holder.iv.setVisibility(View.VISIBLE);
                holder.tv.setVisibility(View.GONE);
            } else {
                holder.tv.setText(new String(Character.toChars(data[position])));
                holder.iv.setVisibility(View.GONE);
                holder.tv.setVisibility(View.VISIBLE);
            }
            if (highlightCurrent) {
                holder.sep.setVisibility(currentValue == data[position] ? View.VISIBLE : View.INVISIBLE);
            }
            holder.itemView.setOnClickListener(v -> {
                currentValue = data[position];
                callback.call(currentValue);
                if (highlightCurrent) {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return remaining;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            protected TextView tv;
            protected ImageView iv;
            protected View sep;

            ViewHolder(final View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.info_text);
                iv = itemView.findViewById(R.id.info_drawable);
                sep = itemView.findViewById(R.id.separator);
            }
        }

    }

    /**
     * builds a drawable the size of a marker with a given text
     * @param paint - paint data structure for Emojis
     * @param emoji codepoint of the emoji to display
     * @return drawable bitmap with text on it
     */
    @NonNull
    private static BitmapDrawable getEmojiDrawableHelper(final EmojiPaint paint, final int emoji) {
        final Bitmap bm = Bitmap.createBitmap(paint.bitmapDimensions.first, paint.bitmapDimensions.second, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bm);
        if (emoji >= CUSTOM_ICONS_START && emoji <= CUSTOM_ICONS_END) {
            final int radius = paint.availableSize / 2;
            final Paint cPaint = new Paint();

            // calculate circle details
            final int v = emoji - CUSTOM_ICONS_START_CIRCLES;
            final int aIndex = v / (COLOR_VALUES * COLOR_VALUES * COLOR_VALUES);
            final int rIndex = (v / (COLOR_VALUES * COLOR_VALUES)) % COLOR_VALUES;
            final int gIndex = (v / COLOR_VALUES) % COLOR_VALUES;
            final int bIndex = v % COLOR_VALUES;

            final int color = Color.argb(
                255 - (aIndex * OPACITY_SPREAD),
                COLOR_OFFSET + rIndex * COLOR_SPREAD,
                COLOR_OFFSET + gIndex * COLOR_SPREAD,
                COLOR_OFFSET + bIndex * COLOR_SPREAD);
            cPaint.setColor(color);
            cPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle((int) (paint.bitmapDimensions.first / 2), (int) (paint.bitmapDimensions.second / 2) - paint.offsetTop, radius, cPaint);
        } else {
            final String text = new String(Character.toChars(emoji));
            final TextPaint tPaint = new TextPaint();
            tPaint.setTextSize(paint.fontsize);
            final StaticLayout lsLayout = new StaticLayout(text, tPaint, paint.availableSize, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
            canvas.translate((int) ((paint.bitmapDimensions.first - lsLayout.getWidth()) / 2), (int) ((paint.bitmapDimensions.second - lsLayout.getHeight()) / 2) - paint.offsetTop);
            lsLayout.draw(canvas);
            canvas.save();
            canvas.restore();
        }
        return new BitmapDrawable(paint.res, bm);
    }

    /**
     * get a drawable the size of a marker with a given text (either from cache or freshly built)
     * @param paint - paint data structure for Emojis
     * @param emoji codepoint of the emoji to display
     * @return drawable bitmap with text on it
     */
    @NonNull
    public static BitmapDrawable getEmojiDrawable(final EmojiPaint paint, final int emoji) {
        final int hashcode = new HashCodeBuilder()
            .append(paint.bitmapDimensions.first)
            .append(paint.availableSize)
            .append(paint.fontsize)
            .append(emoji)
            .toHashCode();

        synchronized (emojiCache) {
            CacheMarker marker = emojiCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, getEmojiDrawableHelper(paint, emoji));
                emojiCache.put(hashcode, marker);
            }
            return (BitmapDrawable) marker.getDrawable();
        }
    }

    /**
     * configuration for getEmojiDrawable
     */
    public static class EmojiPaint {
        public final Resources res;
        public final Pair<Integer, Integer> bitmapDimensions;
        public final int availableSize;
        public final int offsetTop;
        public final int fontsize;

        EmojiPaint(final Resources res, final Pair<Integer, Integer> bitmapDimensions, final int availableSize, final int offsetTop, final int fontsize) {
            this.res = res;
            this.bitmapDimensions = bitmapDimensions;
            this.availableSize = availableSize;
            this.offsetTop = offsetTop;
            this.fontsize = fontsize;
        }
    }
}
