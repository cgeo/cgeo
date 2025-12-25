// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.databinding.DialogTitleButtonButtonBinding
import cgeo.geocaching.databinding.EmojiselectorBinding
import cgeo.geocaching.maps.CacheMarker
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.extension.EmojiLRU
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.functions.Action1

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Pair
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import java.util.HashMap
import java.util.Locale
import java.util.Map
import java.util.Objects

import com.google.android.material.button.MaterialButton
import org.apache.commons.lang3.builder.HashCodeBuilder

class EmojiUtils {

    public static val NO_EMOJI: Int = 0

    // internal consts for calculated circles
    private static val COLOR_VALUES: Int = 3;  // supported # of different values per RGB
    private static val COLOR_SPREAD: Int = 127
    private static val COLOR_OFFSET: Int = 0
    private static val OPACITY_SPREAD: Int = 51
    private static val CUSTOM_SET_SIZE: Int = COLOR_VALUES * COLOR_VALUES * COLOR_VALUES

    // internal consts for plain numbers
    public static val NUMBER_START: Int = 0x30
    public static val NUMBER_END: Int = 0x39

    // Unicode custom glyph area needed/supported by this class
    private static val CUSTOM_ICONS_START: Int = 0xe000
    private static val CUSTOM_ICONS_END: Int = CUSTOM_ICONS_START + CUSTOM_SET_SIZE - 1; // max. possible value by Unicode definition: 0xf8ff
    private static val CUSTOM_ICONS_START_CIRCLES: Int = CUSTOM_ICONS_START

    // Select emoji variation for unicode characters, see https://emojipedia.org/variation-selector-16/
    private static val VariationSelectorEmoji: Int = 0xfe0f

    // list of emojis supported by the EmojiPopup
    // should ideally be supported by the Android API level we have set as minimum (currently API 21 = Android 5),
    // but starting with API 23 (Android 6) the app automatically filters out characters not supported by their fonts
    // for a list of supported Unicode standards by API level see https://developer.android.com/guide/topics/resources/internationalization
    // for characters by Unicode version see https://unicode.org/emoji/charts-5.0/full-emoji-list.html (v5.0 - preferred compatibility standard based on our minAPI level)
    // The newest emoji standard can be found here: https://unicode.org/emoji/charts/full-emoji-list.html

    public static val SMILEY_LIKE: Int = 0x1f600
    public static val SPARKLES: Int = 0x2728
    public static val RECENT: Int = 0x27f2

    public static val RED_FLAG: Int = 0x1f6a9
    public static val GREEN_CHECK_BOXED: Int = 0x2705
    public static val GRAY_CHECK_BOXED: Int = 0x2611
    public static val DOUBLE_RED_EXCLAMATION_MARK: Int = 0x203c

    private static final EmojiSet[] symbols = {
            // category symbols
            EmojiSet(R.string.emoji_category_symbols, 0x2764, Int[]{
                    /* hearts */        0x2764, 0x1f9e1, 0x1f49b, 0x1f49a, 0x1f499, 0x1f49c, 0x1f90e, 0x1f5a4, 0x1f90d,
                    /* geometric */     0x1f7e5, 0x1f7e7, 0x1f7e8, 0x1f7e9, 0x1f7e6, 0x1f7ea, 0x1f7eb, 0x2b1b, 0x2b1c,
                    /* geometric */     0x1f536, 0x1f537,
                    /* events */        0x1f383, 0x2620, 0x1f380, 0x1f384, 0x1f389,
                    /* award-medal */   0x1f947, 0x1f948, 0x1f949, 0x1f3c6,
                    /* office */        0x1f4c6, 0x1f4ca, 0x1f4c8,
                    /* money */         0x1fa99, 0x1f4b0,
                    /* warning */       0x26a0, 0x26d4, 0x1f6ab, 0x1f6b3, 0x1f6d1, 0x2622,
                    /* av-symbol */     0x1f505, 0x1f506,
                    /* other-symbol */  0x2b55, GREEN_CHECK_BOXED, GRAY_CHECK_BOXED, 0x2714, 0x2716, 0x2795, 0x2796, 0x274c, 0x274e, 0x2733, 0x2734, 0x2747, DOUBLE_RED_EXCLAMATION_MARK, 0x2049, 0x2753, 0x2757, 0x1f522, 0x1f520,
                    /* flags */         0x1f3c1, RED_FLAG, 0x1f3f4, 0x1f3f3,

            }),
            // category custom symbols - will be filled dynamically below; has to be at position CUSTOM_GLYPHS_ID within EmojiSet[]
            EmojiSet(R.string.emoji_category_dots, 0x1f534, Int[CUSTOM_SET_SIZE]),
            // category places
            EmojiSet(R.string.emoji_category_places, 0x1f30d, Int[]{
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
            EmojiSet(R.string.emoji_category_food, 0x2615, Int[]{
                    /* fruits */        0x1f34a, 0x1f34b, 0x1f34d, 0x1f34e, 0x1f34f, 0x1f95d, 0x1f336, 0x1f344,
                    /* other */         0x1f968, 0x1f354, 0x1f355,
                    /* food-sweet */    0x1f366, 0x1f370, 0x1f36d,
                    /* drink */         0x1f964, 0x2615, 0x1f37a
            }),
            // category activity
            EmojiSet(R.string.emoji_category_activity, 0x1f3c3, Int[]{
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
            EmojiSet(R.string.emoji_category_people, 0x1f600, Int[]{
                    /* smileys */       SMILEY_LIKE, 0x1f60d, 0x1f641, 0x1f621, 0x1f453,
                    /* silhouettes */   0x1f47b, 0x1f464, 0x1f465, 0x1f5e3,
                    /* people */        0x1f466, 0x1f467, 0x1f468, 0x1f469, 0x1f474, 0x1f475, 0x1f46a,
                    /* hands */         0x1f937, 0x1f44d, 0x1f44e, 0x1f4aa, 0x270d,
                    /* companions */    0x1f434, 0x1f408, 0x1f415
            }),
            // numbers
            EmojiSet(R.string.emoji_category_numbers, 0x2460, Int[]{
                    /* numbers */       NUMBER_START, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, NUMBER_END,
                    /* circled n. */    0x24ea, 0x245e, 0x245f, 0x2460, 0x2461, 0x2462, 0x2463, 0x2464, 0x2465, 0x2466, 0x2467, 0x2468, 0x2469, 0x246a, 0x246b, 0x246c, 0x246d, 0x246e, 0x246f, 0x2470, 0x2471, 0x2472, 0x2473,
                    /* parenthesized n. */ 0x2474, 0x2475, 0x2476, 0x2477, 0x2478, 0x2479, 0x247a, 0x247b, 0x247c,
            }),
    }
    private static val CUSTOM_GLYPHS_ID: Int = 1

    private static Boolean fontIsChecked = false
    private static val lockGuard: Boolean = false
    private static val emojiCache: SparseArray<CacheMarker> = SparseArray<>()

    private static val EMOJI_PAINT_CACHE_PER_SIZE: Map<Integer, EmojiPaint> = HashMap<>()

    private static class EmojiSet {
        @StringRes public final Int title
        public final Int tabSymbol
        public Int remaining
        public final Int[] symbols
        var startPosition: Int = 0; // will be filled later

        EmojiSet(@StringRes final Int title, final Int tabSymbol, final Int[] symbols) {
            this.title = title
            this.tabSymbol = tabSymbol
            this.remaining = symbols.length
            this.symbols = symbols
        }
    }

    private EmojiUtils() {
        // utility class
    }

    public static Unit selectEmojiPopup(final Context context, final Int currentValue, final Geocache cache, final Action1<Integer> setNewCacheIcon) {

        // fill dynamic EmojiSet
        prefillCustomCircles()

        // check EmojiSet for characters not supported on this device
        synchronized (lockGuard) {
            if (!fontIsChecked) {
                val checker: Paint = TextPaint()
                checker.setTypeface(Typeface.DEFAULT)
                for (EmojiSet symbol : symbols) {
                    Int iPosNeu = 0
                    for (Int i = 0; i < symbol.symbols.length; i++) {
                        if (i != iPosNeu) {
                            symbol.symbols[iPosNeu] = symbol.symbols[i]
                        }
                        if ((symbol.symbols[i] >= CUSTOM_ICONS_START && symbol.symbols[i] <= CUSTOM_ICONS_END) || checker.hasGlyph(String(Character.toChars(symbol.symbols[i])))) {
                            iPosNeu++
                        }
                    }
                    symbol.remaining = iPosNeu
                }
                fontIsChecked = true
            }
        }

        // create dialog
        val dialogView: EmojiselectorBinding = EmojiselectorBinding.inflate(LayoutInflater.from(context))
        val customTitle: DialogTitleButtonButtonBinding = DialogTitleButtonButtonBinding.inflate(LayoutInflater.from(context))
        val dialog: AlertDialog = Dialogs.newBuilder(context)
                .setView(dialogView.getRoot())
                .setCustomTitle(customTitle.getRoot())
                .create()

        // calculate symbol list size
        final Int[] lru = EmojiLRU.getLRU()
        Int size = 1 + lru.length
        for (EmojiSet symbol : symbols) {
            size += 1 + symbol.symbols.length
        }

        // collect data for tabs and grid
        final EmojiViewAdapter.EmojiDataType[] data = EmojiViewAdapter.EmojiDataType[size]
        final EmojiViewAdapter.EmojiDataType[] dataTabs = EmojiViewAdapter.EmojiDataType[1 + symbols.length]
        data[0] = EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_TITLE.ordinal(), R.string.emoji_category_recent)
        dataTabs[0] = EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_ITEM.ordinal(), RECENT); // symbol for "recent"
        for (Int i = 0; i < lru.length; i++) {
            data[i + 1] = EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_ITEM.ordinal(), lru[i])
        }
        Int pos = 1 + lru.length
        for (Int i = 0; i < symbols.length; i++) {
            symbols[i].startPosition = pos
            data[pos++] = EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_TITLE.ordinal(), symbols[i].title)
            for (Int j = 0; j < symbols[i].symbols.length; j++) {
                data[pos++] = EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_ITEM.ordinal(), symbols[i].symbols[j])
            }
            dataTabs[1 + i] = EmojiViewAdapter.EmojiDataType(EmojiViewAdapter.EmojiViewType.VIEW_ITEM.ordinal(), symbols[i].tabSymbol)
        }

        // calc sizes for markers
        val wantedEmojiSizeInDp: Int = 22
        val columnWidthInDp: Int = 50
        val maxCols: Int = DisplayUtils.calculateNoOfColumns(context, columnWidthInDp)

        // prepare view adapters
        val glm: GridLayoutManager = GridLayoutManager(context, maxCols)
        glm.setSpanSizeLookup(GridLayoutManager.SpanSizeLookup() {
            override             public Int getSpanSize(final Int position) {
                return data[position].viewType == EmojiViewAdapter.EmojiViewType.VIEW_TITLE.ordinal() ? glm.getSpanCount() : 1
            }
        })
        dialogView.emojiGrid.setLayoutManager(glm)
        val gridAdapter: EmojiViewAdapter = EmojiViewAdapter(context, wantedEmojiSizeInDp, data, currentValue, false, newCacheIcon -> onItemSelected(dialog, setNewCacheIcon, newCacheIcon))
        dialogView.emojiGrid.setAdapter(gridAdapter)

        dialogView.emojiGroups.setLayoutManager(GridLayoutManager(context, 1 + symbols.length))
        val groupsAdapter: EmojiViewAdapter = EmojiViewAdapter(context, wantedEmojiSizeInDp, dataTabs, RECENT, true, tabSymbol -> {
            if (tabSymbol == RECENT) {
                glm.scrollToPositionWithOffset(0, 0)
            } else {
                for (EmojiSet symbol : symbols) {
                    if (symbol.tabSymbol == tabSymbol) {
                        glm.scrollToPositionWithOffset(symbol.startPosition, 0)
                    }
                }
            }
        })
        dialogView.emojiGroups.setAdapter(groupsAdapter)

        customTitle.dialogTitleTitle.setText(R.string.select_icon)

        // right button displays current value; clicking simply closes the dialog
        val dialogButtonRight: MaterialButton = (MaterialButton) customTitle.dialogButtonRight
        if (currentValue == -1) {
            dialogButtonRight.setVisibility(View.VISIBLE)
            dialogButtonRight.setIconResource(R.drawable.ic_menu_mark)
        } else if (currentValue != 0) {
            dialogButtonRight.setVisibility(View.VISIBLE)
            dialogButtonRight.setIcon(getEmojiDrawable(ViewUtils.dpToPixel(wantedEmojiSizeInDp), currentValue))
            dialogButtonRight.setIconTint(null)
        } else if (cache != null) {
            dialogButtonRight.setVisibility(View.VISIBLE)
            // This cross-converting solves a tinting issue described in #11616. Sorry, it is ugly but the only possibility we have found so far.
            dialogButtonRight.setIcon(ViewUtils.bitmapToDrawable(ViewUtils.drawableToBitmap(MapMarkerUtils.getTypeMarker(context.getResources(), cache))))
            dialogButtonRight.setIconTint(null)
        }
        dialogButtonRight.setOnClickListener(v -> dialog.dismiss())

        // left button displays default value (if different from current value)
        val dialogButtonLeft: MaterialButton = (MaterialButton) customTitle.dialogButtonLeft
        if (currentValue != 0) {
            if (cache == null) {
                dialogButtonLeft.setIconResource(R.drawable.ic_menu_reset)
            } else {
                // This cross-converting solves a tinting issue described in #11616. Sorry, it is ugly but the only possibility we have found so far.
                dialogButtonLeft.setIcon(ViewUtils.bitmapToDrawable(ViewUtils.drawableToBitmap(MapMarkerUtils.getTypeMarker(context.getResources(), cache))))
                dialogButtonLeft.setIconTint(null)
            }
            dialogButtonLeft.setVisibility(View.VISIBLE)
            dialogButtonLeft.setOnClickListener(v -> {
                setNewCacheIcon.call(0)
                dialog.dismiss()
            })
        }

        dialog.show()

        // update tab highlighting on scrolling
        ((RecyclerView) Objects.requireNonNull(dialog.findViewById(R.id.emoji_grid))).addOnScrollListener(RecyclerView.OnScrollListener() {
            override             public Unit onScrolled(final RecyclerView recyclerView, final Int dx, final Int dy) {
                super.onScrolled(recyclerView, dx, dy)
                groupsAdapter.setHighlighting(glm.findFirstVisibleItemPosition())
            }
        })
    }

    private static Unit onItemSelected(final AlertDialog dialog, final Action1<Integer> callback, final Int selectedValue) {
        dialog.dismiss()
        EmojiLRU.add(selectedValue)
        callback.call(selectedValue)
    }

    private static Unit prefillCustomCircles() {
        for (Int i = 0; i < CUSTOM_SET_SIZE; i++) {
            symbols[CUSTOM_GLYPHS_ID].symbols[i] = CUSTOM_ICONS_START_CIRCLES + i
        }
    }

    static class EmojiViewAdapter : RecyclerView().Adapter<EmojiViewAdapter.ViewHolder> {

        private enum class EmojiViewType { VIEW_TITLE, VIEW_ITEM }

        static class EmojiDataType {
            public Int viewType
            public Int data

            EmojiDataType(final Int viewType, final Int data) {
                this.viewType = viewType
                this.data = data
            }
        }

        private final EmojiDataType[] data
        private final LayoutInflater inflater
        private final Action1<Integer> callback
        private final Int wantedSizeInDp
        private var currentValue: Int = 0
        private final Boolean highlightCurrent

        EmojiViewAdapter(final Context context, final Int wantedSizeInDp, final EmojiDataType[] data, final Int currentValue, final Boolean highlightCurrent, final Action1<Integer> callback) {
            this.inflater = LayoutInflater.from(context)
            this.wantedSizeInDp = wantedSizeInDp
            this.data = data
            this.currentValue = currentValue
            this.highlightCurrent = highlightCurrent
            this.callback = callback
        }

        override         public EmojiViewAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
            val view: View = inflater.inflate(R.layout.emojiselector_item, parent, false)
            return EmojiViewAdapter.ViewHolder(view)
        }

        override         public Unit onBindViewHolder(final EmojiViewAdapter.ViewHolder holder, final Int position) {
            if (data[position].viewType == EmojiViewType.VIEW_TITLE.ordinal()) {
                holder.tt.setText(CgeoApplication.getInstance().getText(data[position].data))
                holder.tt.setVisibility(View.VISIBLE)
                holder.tv.setVisibility(View.GONE)
                holder.iv.setVisibility(View.GONE)
            } else if (data[position].data >= CUSTOM_ICONS_START && data[position].data <= CUSTOM_ICONS_END) {
                holder.tt.setVisibility(View.GONE)
                holder.iv.setImageDrawable(EmojiUtils.getEmojiDrawable(ViewUtils.dpToPixel(wantedSizeInDp), data[position].data))
                holder.iv.setVisibility(View.VISIBLE)
                holder.tv.setVisibility(View.GONE)
            } else {
                holder.tt.setVisibility(View.GONE)
                holder.tv.setText(getEmojiAsString(data[position].data))
                holder.tv.setTextSize(wantedSizeInDp)
                holder.iv.setVisibility(View.GONE)
                holder.tv.setVisibility(View.VISIBLE)
            }
            holder.sep.setVisibility(highlightCurrent && currentValue == data[position].data ? View.VISIBLE : View.INVISIBLE)
            holder.itemView.setOnClickListener(v -> {
                if (data[position].viewType == EmojiViewType.VIEW_ITEM.ordinal()) {
                    currentValue = data[position].data
                    callback.call(currentValue)
                    if (highlightCurrent) {
                        notifyDataSetChanged()
                    }
                }
            })
        }

        public Unit setHighlighting(final Int dataPosition) {
            currentValue = data[0].data
            for (Int i = symbols.length - 1; i >= 0; i--) {
                if (symbols[i].startPosition <= dataPosition) {
                    currentValue = data[i + 1].data
                    break
                }
            }
            notifyDataSetChanged()
        }

        override         public Int getItemCount() {
            return data.length
        }

        override         public Int getItemViewType(final Int position) {
            return data[position].viewType
        }

        public static class ViewHolder : RecyclerView().ViewHolder {
            protected TextView tt
            protected TextView tv
            protected ImageView iv
            protected View sep

            ViewHolder(final View itemView) {
                super(itemView)
                tt = itemView.findViewById(R.id.info_title)
                tv = itemView.findViewById(R.id.info_text)
                iv = itemView.findViewById(R.id.info_drawable)
                sep = itemView.findViewById(R.id.separator)
            }
        }

    }

    /**
     * get emoji string from codepoint
     *
     * @param emoji codepoint of the emoji to display
     * @return string emoji with protection from rendering as black-and-white glyphs
     */
    public static String getEmojiAsString(final Int emoji) {
        return String(Character.toChars(emoji)) + String(Character.toChars(VariationSelectorEmoji))
    }

    /**
     * builds a drawable the size of a marker with a given text
     *
     * @param paint - paint data structure for Emojis
     * @param emoji codepoint of the emoji to display
     * @return drawable bitmap with text on it
     */
    private static BitmapDrawable getEmojiDrawableHelper(final EmojiPaint paint, final Int emoji) {
        val bm: Bitmap = Bitmap.createBitmap(paint.bitmapDimensions.first, paint.bitmapDimensions.second, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bm)
        if (emoji >= CUSTOM_ICONS_START && emoji <= CUSTOM_ICONS_END) {
            val radius: Int = paint.availableSize / 2
            val cPaint: Paint = Paint()

            // calculate circle details
            val v: Int = emoji - CUSTOM_ICONS_START_CIRCLES
            val aIndex: Int = v / (COLOR_VALUES * COLOR_VALUES * COLOR_VALUES)
            val rIndex: Int = (v / (COLOR_VALUES * COLOR_VALUES)) % COLOR_VALUES
            val gIndex: Int = (v / COLOR_VALUES) % COLOR_VALUES
            val bIndex: Int = v % COLOR_VALUES

            val color: Int = Color.argb(
                    255 - (aIndex * OPACITY_SPREAD),
                    COLOR_OFFSET + rIndex * COLOR_SPREAD,
                    COLOR_OFFSET + gIndex * COLOR_SPREAD,
                    COLOR_OFFSET + bIndex * COLOR_SPREAD)
            cPaint.setColor(color)
            cPaint.setStyle(Paint.Style.FILL)
            canvas.drawCircle((Int) (paint.bitmapDimensions.first / 2), (Int) (paint.bitmapDimensions.second / 2) - paint.offsetTop, radius, cPaint)
        } else {
            val text: String = getEmojiAsString(emoji)
            val tPaint: TextPaint = TextPaint()
            if (emoji > NUMBER_END || emoji < NUMBER_START) {
                tPaint.setTextSize(paint.fontsize)
            } else {
                // increase display size of numbers
                tPaint.setTextSize(paint.fontsize * 1.4f)
            }
            val lsLayout: StaticLayout = StaticLayout(text, tPaint, paint.availableSize, Layout.Alignment.ALIGN_CENTER, 1, 0, false)
            canvas.translate((Int) ((paint.bitmapDimensions.first - lsLayout.getWidth()) / 2), (Int) ((paint.bitmapDimensions.second - lsLayout.getHeight()) / 2) - paint.offsetTop)
            lsLayout.draw(canvas)
            canvas.save()
            canvas.restore()
        }
        return BitmapDrawable(paint.res, bm)
    }

    public static String getFlagEmojiFromCountry(final String countryCode) {
        String cCode = countryCode
        if (cCode == null || cCode.length() != 2) {
            cCode = "EO"; //will return "unknown" flag
        }
        val ccTouse: String = cCode.toUpperCase(Locale.ROOT)
        val firstLetter: Int = Character.codePointAt(ccTouse, 0) - 0x41 + 0x1F1E6
        val secondLetter: Int = Character.codePointAt(ccTouse, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }

    /**
     * get a drawable the size of a marker with a given text (either from cache or freshly built)
     *
     * @param paint - paint data structure for Emojis
     * @param emoji codepoint of the emoji to display
     * @return drawable bitmap with text on it
     */
    public static BitmapDrawable getEmojiDrawable(final EmojiPaint paint, final Int emoji) {
        val hashcode: Int = HashCodeBuilder()
                .append(paint.bitmapDimensions.first)
                .append(paint.availableSize)
                .append(paint.fontsize)
                .append(emoji)
                .toHashCode()

        synchronized (emojiCache) {
            CacheMarker marker = emojiCache.get(hashcode)
            if (marker == null) {
                marker = CacheMarker(hashcode, getEmojiDrawableHelper(paint, emoji))
                emojiCache.put(hashcode, marker)
            }
            return (BitmapDrawable) marker.getDrawable()
        }
    }

    /**
     * get a drawable with given "wantedSize" as both width and height (either from cache or freshly built)
     *
     * @param wantedSize wanted size of drawable (width and height) in pixel
     * @param emoji      codepoint of the emoji to display
     * @return drawable bitmap with emoji on it
     */
    public static BitmapDrawable getEmojiDrawable(final Int wantedSize, final Int emoji) {

        EmojiPaint p
        synchronized (EMOJI_PAINT_CACHE_PER_SIZE) {
            p = EMOJI_PAINT_CACHE_PER_SIZE.get(wantedSize)
            if (p == null) {
                val res: Resources = CgeoApplication.getInstance().getApplicationContext().getResources()
                val markerDimensions: Pair<Integer, Integer> = Pair<>(wantedSize, wantedSize)
                p = EmojiUtils.EmojiPaint(res, markerDimensions, wantedSize, 0, DisplayUtils.calculateMaxFontsize(wantedSize, (Int) (wantedSize * 0.8), (Int) (wantedSize * 1.5), wantedSize))
                EMOJI_PAINT_CACHE_PER_SIZE.put(wantedSize, p)
            }
        }

        return getEmojiDrawable(p, emoji)
    }

    /**
     * configuration for getEmojiDrawable
     */
    public static class EmojiPaint {
        public final Resources res
        public final Pair<Integer, Integer> bitmapDimensions
        public final Int availableSize
        public final Int offsetTop
        public final Int fontsize

        EmojiPaint(final Resources res, final Pair<Integer, Integer> bitmapDimensions, final Int availableSize, final Int offsetTop, final Int fontsize) {
            this.res = res
            this.bitmapDimensions = bitmapDimensions
            this.availableSize = availableSize
            this.offsetTop = offsetTop
            this.fontsize = fontsize
        }
    }
}
