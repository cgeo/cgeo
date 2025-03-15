package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.DialogTitleButtonButtonBinding;
import cgeo.geocaching.databinding.EmojiselectorBinding;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.extension.EmojiLRU;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.functions.Action1;

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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.android.material.button.MaterialButton;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class EmojiUtils {

    public static final int NO_EMOJI = 0;

    // internal consts for calculated circles
    private static final int COLOR_VALUES = 3;  // supported # of different values per RGB
    private static final int COLOR_SPREAD = 127;
    private static final int COLOR_OFFSET = 0;
    private static final int OPACITY_SPREAD = 51;
    private static final int CUSTOM_SET_SIZE = COLOR_VALUES * COLOR_VALUES * COLOR_VALUES;

    // internal consts for plain numbers
    public static final int NUMBER_START = 0x30;
    public static final int NUMBER_END = 0x39;

    // Unicode custom glyph area needed/supported by this class
    private static final int CUSTOM_ICONS_START = 0xe000;
    private static final int CUSTOM_ICONS_END = CUSTOM_ICONS_START + CUSTOM_SET_SIZE - 1; // max. possible value by Unicode definition: 0xf8ff;
    private static final int CUSTOM_ICONS_START_CIRCLES = CUSTOM_ICONS_START;

    // Select emoji variation for unicode characters, see https://emojipedia.org/variation-selector-16/
    private static final int VariationSelectorEmoji = 0xfe0f;

    // list of emojis supported by the EmojiPopup
    // should ideally be supported by the Android API level we have set as minimum (currently API 21 = Android 5),
    // but starting with API 23 (Android 6) the app automatically filters out characters not supported by their fonts
    // for a list of supported Unicode standards by API level see https://developer.android.com/guide/topics/resources/internationalization
    // for characters by Unicode version see https://unicode.org/emoji/charts-5.0/full-emoji-list.html (v5.0 - preferred compatibility standard based on our minAPI level)
    // The newest emoji standard can be found here: https://unicode.org/emoji/charts/full-emoji-list.html

    public static final int SMILEY_LIKE = 0x1f600;
    public static final int SPARKLES = 0x2728;
    public static final int RECENT = 0x27f2;

    private static final EmojiSet[] symbols = {
            // category symbols
            new EmojiSet(R.string.emoji_category_symbols, 0x2764, new int[]{
                    /* hearts */        0x2764, 0x1f9e1, 0x1f49b, 0x1f49a, 0x1f499, 0x1f49c, 0x1f90e, 0x1f5a4, 0x1f90d,
                    /* geometric */     0x1f7e5, 0x1f7e7, 0x1f7e8, 0x1f7e9, 0x1f7e6, 0x1f7ea, 0x1f7eb, 0x2b1b, 0x2b1c,
                    /* geometric */     0x1f536, 0x1f537,
                    /* events */        0x1f383, 0x2620, 0x1f380, 0x1f384, 0x1f389,
                    /* award-medal */   0x1f947, 0x1f948, 0x1f949, 0x1f3c6,
                    /* office */        0x1f4c6, 0x1f4ca, 0x1f4c8,
                    /* money */         0x1fa99, 0x1f4b0,
                    /* warning */       0x26a0, 0x26d4, 0x1f6ab, 0x1f6b3, 0x1f6d1, 0x2622,
                    /* av-symbol */     0x1f505, 0x1f506,
                    /* other-symbol */  0x2b55, 0x2705, 0x2611, 0x2714, 0x2716, 0x2795, 0x2796, 0x274c, 0x274e, 0x2733, 0x2734, 0x2747, 0x203c, 0x2049, 0x2753, 0x2757, 0x1f522, 0x1f520,
                    /* flags */         0x1f3c1, 0x1f6a9, 0x1f3f4, 0x1f3f3,

            }),
            // category custom symbols - will be filled dynamically below; has to be at position CUSTOM_GLYPHS_ID within EmojiSet[]
            new EmojiSet(R.string.emoji_category_dots, 0x1f534, new int[CUSTOM_SET_SIZE]),
            // category places
            new EmojiSet(R.string.emoji_category_places, 0x1f30d, new int[]{
                    /* globe */         0x1f30d, 0x1f30e, 0x1f30f, 0x1f5fa,
                    /* geographic */    0x26f0, 0x1f3d4, 0x1f3d6, 0x1f3dc, 0x1f3dd, 0x1f3de,
                    /* buildings */     0x1f3e0, 0x1f3e1, 0x1f3e2, 0x1f3d9, 0x1f3eb, 0x1f3ea, 0x1F3ed, 0x1f3e5, 0x1f3da, 0x1f3f0, 0x1f3e8,
                    /* other */         0x1f5fd, 0x26f2, 0x2668, 0x1f6d2, 0x1f3ad, 0x1f3a8,
                    /* plants */        0x1f332, 0x1f333, 0x1f334, 0x1f335, 0x1f340,
                    /* transport */     0x1f682, 0x1f683, 0x1f686, 0x1f687, 0x1f68d, 0x1f695, 0x1f6b2, 0x1f697, 0x1f699, 0x26fd, 0x1f6a7, 0x2693, 0x26f5, 0x2708, 0x1f680,
                    /* arrows/pins */   0x2b07, 0x2195, 0x1f500, 0x1f4cd, 0x1f4cc,
                    /* transp.-sign */  0x267f, 0x1f6bb,
                    /* time */          0x23f3, 0x231a, 0x23f1, 0x1f324, 0x2600, 0x1f319, 0x1f318, SPARKLES
            }),
            // category food
            new EmojiSet(R.string.emoji_category_food, 0x2615, new int[]{
                    /* fruits */        0x1f34a, 0x1f34b, 0x1f34d, 0x1f34e, 0x1f34f, 0x1f95d, 0x1f336, 0x1f344,
                    /* other */         0x1f968, 0x1f354, 0x1f355,
                    /* food-sweet */    0x1f366, 0x1f370, 0x1f36d,
                    /* drink */         0x1f964, 0x2615, 0x1f37a
            }),
            // category activity
            new EmojiSet(R.string.emoji_category_activity, 0x1f3c3, new int[]{
                    /* person-sport */  0x26f7, 0x1f3c4, 0x1f6a3, 0x1f3ca, 0x1f6b4,
                    /* p.-activity */   0x1f6b5, 0x1f9d7,
                    /* person-role */   0x1f575,
                    /* sport */         0x26bd, 0x1f94e, 0x1f3c0, 0x1f3c8, 0x1f93f, 0x1f3bf, 0x1f6f6, 0x2693, 0x1f3af, 0x1f9ff, 0x1f6f6,
                    /* tool */          0x1fa9c, 0x1f3a3, 0x1f6e0, 0x1fa9b, 0x1f512, 0x1f511, 0x1f50b, 0x1f9f2,
                    /* science */       0x2697, 0x1f9ea, 0x1f52c,
                    /* games */         0x1f9e9,
                    /* travel */        0x2708, 0x1f9f3, 0x1f680, 0x1f6f8, 0x1fa82, 0x1f3d5, 0x26fa,
                    /* other things */  0x1f50e, 0x1f526, 0x1f4a1, 0x1f4d4, 0x1f4dc, 0x1f4ec, 0x1f3f7, 0x1f4e5
            }),
            // category people
            new EmojiSet(R.string.emoji_category_people, 0x1f600, new int[]{
                    /* smileys */       SMILEY_LIKE, 0x1f60d, 0x1f641, 0x1f621, 0x1f453,
                    /* silhouettes */   0x1f47b, 0x1f464, 0x1f465, 0x1f5e3,
                    /* people */        0x1f466, 0x1f467, 0x1f468, 0x1f469, 0x1f474, 0x1f475, 0x1f46a,
                    /* hands */         0x1f937, 0x1f44d, 0x1f44e, 0x1f4aa, 0x270d,
                    /* companions */    0x1f434, 0x1f408, 0x1f415
            }),
            // numbers
            new EmojiSet(R.string.emoji_category_numbers, 0x2460, new int[]{
                    /* numbers */       NUMBER_START, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, NUMBER_END,
                    /* circled n. */    0x24ea, 0x245e, 0x245f, 0x2460, 0x2461, 0x2462, 0x2463, 0x2464, 0x2465, 0x2466, 0x2467, 0x2468, 0x2469, 0x246a, 0x246b, 0x246c, 0x246d, 0x246e, 0x246f, 0x2470, 0x2471, 0x2472, 0x2473,
                    /* parenthesized n. */ 0x2474, 0x2475, 0x2476, 0x2477, 0x2478, 0x2479, 0x247a, 0x247b, 0x247c,
            }),
    };
    private static final int CUSTOM_GLYPHS_ID = 1;

    private static boolean fontIsChecked = false;
    private static final Boolean lockGuard = false;
    private static final SparseArray<CacheMarker> emojiCache = new SparseArray<>();

    private static final Map<Integer, EmojiPaint> EMOJI_PAINT_CACHE_PER_SIZE = new HashMap<>();

    private static class EmojiSet {
        @StringRes public final int title;
        public final int tabSymbol;
        public int remaining;
        public final int[] symbols;
        public int startPosition = 0; // will be filled later

        EmojiSet(@StringRes final int title, final int tabSymbol, final int[] symbols) {
            this.title = title;
            this.tabSymbol = tabSymbol;
            this.remaining = symbols.length;
            this.symbols = symbols;
        }
    }

    private EmojiUtils() {
        // utility class
    }

    public static void selectEmojiPopup(final Context context, final int currentValue, @Nullable final Geocache cache, final Action1<Integer> setNewCacheIcon) {

        // fill dynamic EmojiSet
        prefillCustomCircles();

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
        final EmojiselectorBinding dialogView = EmojiselectorBinding.inflate(LayoutInflater.from(context));
        final DialogTitleButtonButtonBinding customTitle = DialogTitleButtonButtonBinding.inflate(LayoutInflater.from(context));
        final AlertDialog dialog = Dialogs.newBuilder(context)
                .setView(dialogView.getRoot())
                .setCustomTitle(customTitle.getRoot())
                .create();

        // calculate symbol list size
        final int[] lru = EmojiLRU.getLRU();
        int size = 1 + lru.length;
        for (EmojiSet symbol : symbols) {
            size += 1 + symbol.symbols.length;
        }

        // collect data for tabs and grid
        final EmojiViewAdapter.EmojiDataType[] data = new EmojiViewAdapter.EmojiDataType[size];
        final EmojiViewAdapter.EmojiDataType[] dataTabs = new EmojiViewAdapter.EmojiDataType[1 + symbols.length];
        data[0] = new EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_TITLE.ordinal(), R.string.emoji_category_recent);
        dataTabs[0] = new EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_ITEM.ordinal(), RECENT); // symbol for "recent"
        for (int i = 0; i < lru.length; i++) {
            data[i + 1] = new EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_ITEM.ordinal(), lru[i]);
        }
        int pos = 1 + lru.length;
        for (int i = 0; i < symbols.length; i++) {
            symbols[i].startPosition = pos;
            data[pos++] = new EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_TITLE.ordinal(), symbols[i].title);
            for (int j = 0; j < symbols[i].symbols.length; j++) {
                data[pos++] = new EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_ITEM.ordinal(), symbols[i].symbols[j]);
            }
            dataTabs[1 + i] = new EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_ITEM.ordinal(), symbols[i].tabSymbol);
        }

        // calc sizes for markers
        final int wantedEmojiSizeInDp = 22;
        final int columnWidthInDp = 50;
        final int maxCols = DisplayUtils.calculateNoOfColumns(context, columnWidthInDp);

        // prepare view adapters
        final GridLayoutManager glm = new GridLayoutManager(context, maxCols);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(final int position) {
                return data[position].viewType == EmojiViewAdapter.EmojiViewType.VIEW_TITLE.ordinal() ? glm.getSpanCount() : 1;
            }
        });
        dialogView.emojiGrid.setLayoutManager(glm);
        final EmojiViewAdapter gridAdapter = new EmojiViewAdapter(context, wantedEmojiSizeInDp, data, currentValue, false, newCacheIcon -> onItemSelected(dialog, setNewCacheIcon, newCacheIcon));
        dialogView.emojiGrid.setAdapter(gridAdapter);

        dialogView.emojiGroups.setLayoutManager(new GridLayoutManager(context, 1 + symbols.length));
        final EmojiViewAdapter groupsAdapter = new EmojiViewAdapter(context, wantedEmojiSizeInDp, dataTabs, RECENT, true, tabSymbol -> {
            if (tabSymbol == RECENT) {
                glm.scrollToPositionWithOffset(0, 0);
            } else {
                for (EmojiSet symbol : symbols) {
                    if (symbol.tabSymbol == tabSymbol) {
                        glm.scrollToPositionWithOffset(symbol.startPosition, 0);
                    }
                }
            }
        });
        dialogView.emojiGroups.setAdapter(groupsAdapter);

        customTitle.dialogTitleTitle.setText(R.string.select_icon);

        // right button displays current value; clicking simply closes the dialog
        final MaterialButton dialogButtonRight = (MaterialButton) customTitle.dialogButtonRight;
        if (currentValue == -1) {
            dialogButtonRight.setVisibility(View.VISIBLE);
            dialogButtonRight.setIconResource(R.drawable.ic_menu_mark);
        } else if (currentValue != 0) {
            dialogButtonRight.setVisibility(View.VISIBLE);
            dialogButtonRight.setIcon(getEmojiDrawable(ViewUtils.dpToPixel(wantedEmojiSizeInDp), currentValue));
            dialogButtonRight.setIconTint(null);
        } else if (cache != null) {
            dialogButtonRight.setVisibility(View.VISIBLE);
            // This cross-converting solves a tinting issue described in #11616. Sorry, it is ugly but the only possibility we have found so far.
            dialogButtonRight.setIcon(ViewUtils.bitmapToDrawable(ViewUtils.drawableToBitmap(MapMarkerUtils.getTypeMarker(context.getResources(), cache))));
            dialogButtonRight.setIconTint(null);
        }
        dialogButtonRight.setOnClickListener(v -> dialog.dismiss());

        // left button displays default value (if different from current value)
        final MaterialButton dialogButtonLeft = (MaterialButton) customTitle.dialogButtonLeft;
        if (currentValue != 0) {
            if (cache == null) {
                dialogButtonLeft.setIconResource(R.drawable.ic_menu_reset);
            } else {
                // This cross-converting solves a tinting issue described in #11616. Sorry, it is ugly but the only possibility we have found so far.
                dialogButtonLeft.setIcon(ViewUtils.bitmapToDrawable(ViewUtils.drawableToBitmap(MapMarkerUtils.getTypeMarker(context.getResources(), cache))));
                dialogButtonLeft.setIconTint(null);
            }
            dialogButtonLeft.setVisibility(View.VISIBLE);
            dialogButtonLeft.setOnClickListener(v -> {
                setNewCacheIcon.call(0);
                dialog.dismiss();
            });
        }

        dialog.show();

        // update tab highlighting on scrolling
        ((RecyclerView) Objects.requireNonNull(dialog.findViewById(R.id.emoji_grid))).addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
                super.onScrolled(recyclerView, dx, dy);
                groupsAdapter.setHighlighting(glm.findFirstVisibleItemPosition());
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Dialogs.basicOneTimeMessage(context, OneTimeDialogs.DialogType.MISSING_UNICODE_CHARACTERS);
        }
    }

    private static void onItemSelected(final AlertDialog dialog, final Action1<Integer> callback, final int selectedValue) {
        dialog.dismiss();
        EmojiLRU.add(selectedValue);
        callback.call(selectedValue);
    }

    private static void prefillCustomCircles() {
        for (int i = 0; i < CUSTOM_SET_SIZE; i++) {
            symbols[CUSTOM_GLYPHS_ID].symbols[i] = CUSTOM_ICONS_START_CIRCLES + i;
        }
    }

    static class EmojiViewAdapter extends RecyclerView.Adapter<EmojiViewAdapter.ViewHolder> {

        private enum EmojiViewType { VIEW_TITLE, VIEW_ITEM };

        static class EmojiDataType {
            public int viewType;
            public int data;

            EmojiDataType(final int viewType, final int data) {
                this.viewType = viewType;
                this.data = data;
            }
        }

        private final EmojiDataType[] data;
        private final LayoutInflater inflater;
        private final Action1<Integer> callback;
        private final int wantedSizeInDp;
        private int currentValue = 0;
        private final boolean highlightCurrent;

        EmojiViewAdapter(final Context context, final int wantedSizeInDp, final EmojiDataType[] data, final int currentValue, final boolean highlightCurrent, final Action1<Integer> callback) {
            this.inflater = LayoutInflater.from(context);
            this.wantedSizeInDp = wantedSizeInDp;
            this.data = data;
            this.currentValue = currentValue;
            this.highlightCurrent = highlightCurrent;
            this.callback = callback;
        }

        @Override
        @NonNull
        public EmojiViewAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = inflater.inflate(R.layout.emojiselector_item, parent, false);
            return new EmojiViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final EmojiViewAdapter.ViewHolder holder, final int position) {
            if (data[position].viewType == EmojiViewType.VIEW_TITLE.ordinal()) {
                holder.tt.setText(CgeoApplication.getInstance().getText(data[position].data));
                holder.tt.setVisibility(View.VISIBLE);
                holder.tv.setVisibility(View.GONE);
                holder.iv.setVisibility(View.GONE);
            } else if (data[position].data >= CUSTOM_ICONS_START && data[position].data <= CUSTOM_ICONS_END) {
                holder.tt.setVisibility(View.GONE);
                holder.iv.setImageDrawable(EmojiUtils.getEmojiDrawable(ViewUtils.dpToPixel(wantedSizeInDp), data[position].data));
                holder.iv.setVisibility(View.VISIBLE);
                holder.tv.setVisibility(View.GONE);
            } else {
                holder.tt.setVisibility(View.GONE);
                holder.tv.setText(getEmojiAsString(data[position].data));
                holder.tv.setTextSize(wantedSizeInDp);
                holder.iv.setVisibility(View.GONE);
                holder.tv.setVisibility(View.VISIBLE);
            }
            holder.sep.setVisibility(highlightCurrent && currentValue == data[position].data ? View.VISIBLE : View.INVISIBLE);
            holder.itemView.setOnClickListener(v -> {
                if (data[position].viewType == EmojiViewType.VIEW_ITEM.ordinal()) {
                    currentValue = data[position].data;
                    callback.call(currentValue);
                    if (highlightCurrent) {
                        notifyDataSetChanged();
                    }
                }
            });
        }

        public void setHighlighting(final int dataPosition) {
            currentValue = data[0].data;
            for (int i = symbols.length - 1; i >= 0; i--) {
                if (symbols[i].startPosition <= dataPosition) {
                    currentValue = data[i + 1].data;
                    break;
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return data.length;
        }

        @Override
        public int getItemViewType(final int position) {
            return data[position].viewType;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            protected TextView tt;
            protected TextView tv;
            protected ImageView iv;
            protected View sep;

            ViewHolder(final View itemView) {
                super(itemView);
                tt = itemView.findViewById(R.id.info_title);
                tv = itemView.findViewById(R.id.info_text);
                iv = itemView.findViewById(R.id.info_drawable);
                sep = itemView.findViewById(R.id.separator);
            }
        }

    }

    /**
     * get emoji string from codepoint
     *
     * @param emoji codepoint of the emoji to display
     * @return string emoji with protection from rendering as black-and-white glyphs
     */
    private static String getEmojiAsString(final int emoji) {
        return new String(Character.toChars(emoji)) + new String(Character.toChars(VariationSelectorEmoji));
    }

    /**
     * builds a drawable the size of a marker with a given text
     *
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
            final String text = getEmojiAsString(emoji);
            final TextPaint tPaint = new TextPaint();
            if (emoji > NUMBER_END || emoji < NUMBER_START) {
                tPaint.setTextSize(paint.fontsize);
            } else {
                // increase display size of numbers
                tPaint.setTextSize(paint.fontsize * 1.4f);
            }
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
     *
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
     * get a drawable with given "wantedSize" as both width and height (either from cache or freshly built)
     *
     * @param wantedSize wanted size of drawable (width and height) in pixel
     * @param emoji      codepoint of the emoji to display
     * @return drawable bitmap with emoji on it
     */
    @NonNull
    public static BitmapDrawable getEmojiDrawable(final int wantedSize, final int emoji) {

        EmojiPaint p;
        synchronized (EMOJI_PAINT_CACHE_PER_SIZE) {
            p = EMOJI_PAINT_CACHE_PER_SIZE.get(wantedSize);
            if (p == null) {
                final Resources res = CgeoApplication.getInstance().getApplicationContext().getResources();
                final Pair<Integer, Integer> markerDimensions = new Pair<>((int) (wantedSize * 1.2), (int) (wantedSize * 1.2));
                p = new EmojiUtils.EmojiPaint(res, markerDimensions, wantedSize, 0, DisplayUtils.calculateMaxFontsize(wantedSize, (int) (wantedSize * 0.8), (int) (wantedSize * 1.5), wantedSize));
                EMOJI_PAINT_CACHE_PER_SIZE.put(wantedSize, p);
            }
        }

        return getEmojiDrawable(p, emoji);
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
