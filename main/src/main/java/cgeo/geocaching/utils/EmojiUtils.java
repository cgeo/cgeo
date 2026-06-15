package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.EmojiPickerDialogBinding;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.extension.EmojiLRU;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.functions.Action1;
import static cgeo.geocaching.ui.dialog.Dialogs.newContextThemeWrapper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.text.Editable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.emoji2.emojipicker.EmojiPickerView;
import androidx.emoji2.emojipicker.RecentEmojiAsyncProvider;
import androidx.emoji2.emojipicker.RecentEmojiProviderAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class EmojiUtils {
    public static final String NO_EMOJI = null;

    // internal consts for plain numbers
    public static final int NUMBER_START = 0x30;
    public static final int NUMBER_END = 0x39;

    // extra (non-emoji) unicode characters offered in addition to the bundled emoji set; they are surfaced through the
    // picker's search field (matched by their Unicode name). Non-renderable codepoints are skipped automatically.
    private static final int[] EXTRA_SEARCHABLE_CODEPOINTS = {
        /* numbers */          NUMBER_START, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, NUMBER_END,
        /* circled n. */       0x24ea, 0x2460, 0x2461, 0x2462, 0x2463, 0x2464, 0x2465, 0x2466, 0x2467, 0x2468, 0x2469, 0x246a, 0x246b, 0x246c, 0x246d, 0x246e, 0x246f, 0x2470, 0x2471, 0x2472, 0x2473,
        /* parenthesized n. */ 0x2474, 0x2475, 0x2476, 0x2477, 0x2478, 0x2479, 0x247a, 0x247b, 0x247c,
    };

    // Select emoji variation for unicode characters, see https://emojipedia.org/variation-selector-16/
    private static final int VariationSelectorEmoji = 0xfe0f;
    private static final String VARIATION_SELECTOR_EMOJI_STR = new String(Character.toChars(VariationSelectorEmoji));

    // commonly used built-in emojis (single codepoints) referenced from various places in the app
    public static final String SMILEY_LIKE = "\uD83D\uDE00";
    public static final String SPARKLES = "✨";

    public static final String RED_FLAG = "\uD83D\uDEA9";
    public static final String GREEN_CHECK_BOXED = "✅";
    public static final String DOUBLE_RED_EXCLAMATION_MARK = "‼";

    private static final SparseArray<CacheMarker> emojiCache = new SparseArray<>();

    private static final Map<Integer, EmojiPaint> EMOJI_PAINT_CACHE_PER_SIZE = new HashMap<>();

    private EmojiUtils() {
        // utility class
    }

    /**
     * Opens a dialog hosting the androidx EmojiPicker so the user can select an emoji for a cache icon / list marker /
     * filter marker.
     *
     * @param currentValue the currently assigned emoji or empty/null for "none assigned"; pass {@code null} together
     *                     with {@code showResetAllAction=true} for the "assign to multiple caches" case where there is no single
     *                     current value
     * @param showResetAllAction {@code true} forces display of "reset" and "close" buttons.
     *                    generic marker icon as preview and always offers the reset-to-default action
     * @param cache        optional cache used to render the type marker as default/preview icon. If null then a generic marker is used
     * @param onSelection callback receiving the selected emoji String, or the empty string when reset to default
     */
    public static void selectEmojiPopup(final Context context, @Nullable final String currentValue, final boolean showResetAllAction, @Nullable final Geocache cache, final Action1<String> onSelection) {

        final Context themedContext = newContextThemeWrapper(context);
        final @NonNull EmojiPickerDialogBinding emojiPickerDialogBinding = EmojiPickerDialogBinding.inflate(LayoutInflater.from(themedContext));
        final ViewGroup pickerLayout = emojiPickerDialogBinding.getRoot();

        final BottomSheetDialog dialog = new BottomSheetDialog(themedContext);
        dialog.setContentView(pickerLayout);
        dialog.setOnShowListener(d -> {
            dialog.getBehavior().setSkipCollapsed(true);
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            dialog.getBehavior().setDraggable(false);
        });

        // fix scroll scroll area height to have same height in search/emoji views
        final View scrollArea = pickerLayout.findViewById(R.id.emoji_picker_scroll);
        final ViewGroup.LayoutParams scrollParams = scrollArea.getLayoutParams();
        scrollParams.height = (int) (DisplayUtils.getDisplaySize().y * 0.6);
        scrollArea.setLayoutParams(scrollParams);

        final int columns = DisplayUtils.calculateNoOfColumns(context, R.dimen.emoji_picker_emoji_size);
        final EmojiPickerView picker = pickerLayout.findViewById(R.id.emoji_picker);
        picker.setEmojiGridColumns(columns);
        picker.setRecentEmojiProvider(new RecentEmojiProviderAdapter(new EmojiLruRecentProvider(columns * 2)));

        // move the picker's internal category bar up into the search row, leaving only the emoji grid in the picker
        final FrameLayout categoryHolder = pickerLayout.findViewById(R.id.emoji_category_holder);
        relocatePickerHeader(picker, categoryHolder);

        // search field: hide picker, show self-built grid of emojis whose Character.getName matches the query
        final RecyclerView searchResults = pickerLayout.findViewById(R.id.emoji_search_results);
        searchResults.setLayoutManager(new GridLayoutManager(themedContext, columns));
        final EmojiSearchAdapter searchAdapter = new EmojiSearchAdapter(columns, emoji -> {
            EmojiLRU.add(emoji, columns * 2);
            dialog.dismiss();
            onSelection.call(emoji);
        });
        searchResults.setAdapter(searchAdapter);

        final EditText searchField = pickerLayout.findViewById(R.id.emoji_search);
        // "clear"-button for search field
        final View searchClear = pickerLayout.findViewById(R.id.emoji_search_clear);
        searchClear.setOnClickListener(v -> searchField.setText(""));
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                // no-op
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                // no-op
            }

            @Override
            public void afterTextChanged(final Editable s) {
                final String query = s.toString().trim();
                final boolean searching = !query.isEmpty();
                picker.setVisibility(searching ? View.GONE : View.VISIBLE);
                categoryHolder.setVisibility(searching ? View.GONE : View.VISIBLE);
                searchResults.setVisibility(searching ? View.VISIBLE : View.GONE);
                searchClear.setVisibility(searching ? View.VISIBLE : View.GONE);
                if (!searching) {
                    searchAdapter.setEmojis(Collections.emptyList());
                    return;
                }
                // the index is built lazily off the main thread; re-read the field when it is ready so we never apply
                // results for a query the user has already changed
                withSearchIndex(context, index -> {
                    final String current = searchField.getText().toString().trim();
                    if (!current.isEmpty()) {
                        searchAdapter.setEmojis(filterEmojis(index, current));
                        searchResults.scrollToPosition(0);
                    }
                });
            }
        });

        picker.setOnEmojiPickedListener(item -> {
            dialog.dismiss();
            onSelection.call(item.getEmoji());
        });

        // closing chevron matching the other bottom sheets
        pickerLayout.findViewById(R.id.emoji_picker_close).setOnClickListener(v -> dialog.dismiss());

        final int wantedEmojiSizeInDp = 22;

        // right button: current value, click = close
        final MaterialButton dialogButtonRight = pickerLayout.findViewById(R.id.dialog_buttonRight);
        if (StringUtils.isNotBlank(currentValue) || cache != null) {
            dialogButtonRight.setVisibility(View.VISIBLE);
            dialogButtonRight.setIcon(StringUtils.isNotBlank(currentValue) ? ImageParam.emoji(currentValue).getAsDrawable() : getTintedCacheIcon(context, cache));
            dialogButtonRight.setIconTint(null);
        }
        dialogButtonRight.setOnClickListener(v -> dialog.dismiss());

        // left button: Reset to default
        final MaterialButton dialogButtonLeft = pickerLayout.findViewById(R.id.dialog_buttonLeft);
        if (showResetAllAction || StringUtils.isNotBlank(currentValue)) {
            if (cache == null) {
                dialogButtonLeft.setIconResource(R.drawable.ic_menu_reset);
            } else {
                dialogButtonLeft.setIcon(getTintedCacheIcon(context, cache));
                dialogButtonLeft.setIconTint(null);
            }
            dialogButtonLeft.setVisibility(View.VISIBLE);
            dialogButtonLeft.setOnClickListener(v -> {
                onSelection.call("");
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    // This cross-converting solves a tinting issue described in #11616. Sorry, it is ugly but the only possibility we have found so far.
    private static BitmapDrawable getTintedCacheIcon(final Context context, final Geocache cache) {
        return ViewUtils.bitmapToDrawable(ViewUtils.drawableToBitmap(MapMarkerUtils.getTypeMarker(context.getResources(), cache)));
    }

    @SuppressWarnings("DiscouragedApi")
    // Moves the EmojiPickerView's internal category bar into the header row
    private static void relocatePickerHeader(final EmojiPickerView picker, final FrameLayout target) {
        final int headerId = picker.getResources().getIdentifier("emoji_picker_header", "id", picker.getContext().getPackageName());
        if (headerId == 0) {
            return;
        }
        picker.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final View header = picker.findViewById(headerId);
                if (header == null) {
                    // picker has not built its children yet - wait for a later layout pass
                    return;
                }
                if (header.getParent() != target) {
                    final ViewParent parent = header.getParent();
                    if (parent instanceof ViewGroup) {
                        ((ViewGroup) parent).removeView(header);
                    }
                    header.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    target.addView(header);
                }
                picker.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    // adapter feeding cgeo's {@link EmojiLRU} as recent-emoji source to the androidx picker
    private static final class EmojiLruRecentProvider implements RecentEmojiAsyncProvider {
        final int maxLruLength;

        EmojiLruRecentProvider(final int maxLruLength) {
            this.maxLruLength = maxLruLength;
        }

        @NonNull
        @Override
        public ListenableFuture<List<String>> getRecentEmojiListAsync() {
            return Futures.immediateFuture(EmojiLRU.getLRU());
        }

        @Override
        public void recordSelection(@NonNull final String emoji) {
            EmojiLRU.add(emoji, maxLruLength);
        }
    }

    // lazily built, app-wide cache of (emoji -> lowercased Unicode name) used to back the picker's search field
    @Nullable
    private static volatile List<EmojiSearchEntry> searchIndex;

    private static final class EmojiSearchEntry {
        final String emoji;
        final String searchName;

        EmojiSearchEntry(final String emoji, final String searchName) {
            this.emoji = emoji;
            this.searchName = searchName;
        }
    }

    // delivers the (cached or freshly built) emoji search index to {@code onReady} on the UI thread
    private static void withSearchIndex(final Context context, final androidx.core.util.Consumer<List<EmojiSearchEntry>> onReady) {
        final List<EmojiSearchEntry> cached = searchIndex;
        if (cached != null) {
            onReady.accept(cached);
            return;
        }
        final Context appContext = context.getApplicationContext();
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
            List<EmojiSearchEntry> index = searchIndex;
            if (index == null) {
                index = buildSearchIndex(appContext);
                searchIndex = index;
            }
            return index;
        }, onReady::accept);
    }

    @SuppressWarnings("DiscouragedApi")
    // Builds the search index by reading the same bundled emoji data the androidx picker uses
    private static List<EmojiSearchEntry> buildSearchIndex(final Context context) {
        final Resources res = context.getResources();
        final String pkg = context.getPackageName();
        int arrayId = res.getIdentifier("emoji_by_category_raw_resources_gender_inclusive", "array", pkg);
        if (arrayId == 0) {
            arrayId = res.getIdentifier("emoji_by_category_raw_resources", "array", pkg);
        }
        if (arrayId == 0) {
            return Collections.emptyList();
        }
        final List<EmojiSearchEntry> index = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        final Paint glyphCheck = new Paint();
        final TypedArray categories = res.obtainTypedArray(arrayId);
        try {
            for (int i = 0; i < categories.length(); i++) {
                final int rawId = categories.getResourceId(i, 0);
                if (rawId == 0) {
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.openRawResource(rawId), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // each line is "baseEmoji,variant1,variant2,..."; we only index the base emoji
                        final int comma = line.indexOf(',');
                        addSearchEntry(index, seen, glyphCheck, (comma >= 0 ? line.substring(0, comma) : line).trim());
                    }
                } catch (final Exception ignore) {
                    // skip a category we cannot read rather than failing the whole index
                }
            }
        } finally {
            categories.recycle();
        }
        // additionally offer the extra (non-emoji) unicode characters; non-renderable ones are skipped
        for (final int cp : EXTRA_SEARCHABLE_CODEPOINTS) {
            addSearchEntry(index, seen, glyphCheck, new String(Character.toChars(cp)));
        }
        return index;
    }

    // adds an emoji/char to the search index, skipping blanks, duplicates and codepoints with no renderable glyph
    private static void addSearchEntry(final List<EmojiSearchEntry> index, final Set<String> seen, final Paint glyphCheck, final String emoji) {
        if (emoji.isEmpty() || !seen.add(emoji) || !glyphCheck.hasGlyph(emoji)) {
            return;
        }
        index.add(new EmojiSearchEntry(emoji, unicodeNameOf(emoji)));
    }

    // @return the concatenated, lowercased Unicode names of all codepoints in the given emoji (e.g. "grinning face")
    private static String unicodeNameOf(final String emoji) {
        final StringBuilder sb = new StringBuilder();
        int offset = 0;
        final int length = emoji.length();
        while (offset < length) {
            final int cp = emoji.codePointAt(offset);
            offset += Character.charCount(cp);
            final String name = Character.getName(cp);
            if (name != null) {
                sb.append(name).append(' ');
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    // @return emojis whose Unicode name contains every whitespace-separated token of the (already non-empty) query
    private static List<String> filterEmojis(final List<EmojiSearchEntry> index, final String query) {
        final String[] tokens = query.toLowerCase(Locale.ROOT).split("\\s+");
        final List<String> result = new ArrayList<>();
        for (final EmojiSearchEntry entry : index) {
            boolean matchesAll = true;
            for (final String token : tokens) {
                if (!entry.searchName.contains(token)) {
                    matchesAll = false;
                    break;
                }
            }
            if (matchesAll) {
                result.add(entry.emoji);
            }
        }
        return result;
    }

    // simple grid adapter rendering search-result emojis as clickable text cells
    private static final class EmojiSearchAdapter extends RecyclerView.Adapter<EmojiSearchAdapter.EmojiViewHolder> {

        // scale search result emojis to match the androidx picker display size
        private static final float EMOJI_CELL_FILL_RATIO = 0.725f;

        private final Action1<String> onEmojiPicked;
        private final int columns;
        private List<String> emojis = Collections.emptyList();

        EmojiSearchAdapter(final int columns, final Action1<String> onEmojiPicked) {
            this.onEmojiPicked = onEmojiPicked;
            this.columns = columns;
        }

        @SuppressLint("NotifyDataSetChanged")
        void setEmojis(final List<String> emojis) {
            this.emojis = emojis;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EmojiViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final Context ctx = parent.getContext();
            final TextView textView = new TextView(ctx);
            final int cellSize = (parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight()) / columns;
            textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellSize));
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, cellSize * EMOJI_CELL_FILL_RATIO);
            textView.setTextColor(ContextCompat.getColor(ctx, R.color.colorText));
            return new EmojiViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull final EmojiViewHolder holder, final int position) {
            final String emoji = emojis.get(position);
            ((TextView) holder.itemView).setText(ensureEmojiPresentation(emoji));
            holder.itemView.setOnClickListener(v -> onEmojiPicked.call(emoji));
        }

        @Override
        public int getItemCount() {
            return emojis.size();
        }

        static final class EmojiViewHolder extends RecyclerView.ViewHolder {
            EmojiViewHolder(@NonNull final View itemView) {
                super(itemView);
            }
        }
    }

    // @return the single Unicode codepoint of the given String, or -1 if it is null/empty or contains more than one codepoint
    private static int singleCodePoint(@Nullable final String emoji) {
        if (StringUtils.isBlank(emoji) || emoji.codePointCount(0, emoji.length()) != 1) {
            return -1;
        }
        return emoji.codePointAt(0);
    }

    // ensures a single-codepoint emoji is rendered as a color emoji glyph by appending the variation selector
    private static String ensureEmojiPresentation(@Nullable final String emoji) {
        if (StringUtils.isBlank(emoji)) {
            return "";
        }
        if (emoji.codePointCount(0, emoji.length()) == 1 && !emoji.endsWith(VARIATION_SELECTOR_EMOJI_STR)) {
            return emoji + VARIATION_SELECTOR_EMOJI_STR;
        }
        return emoji;
    }

    /**
     * builds a drawable the size of a marker with a given emoji string
     *
     * @param paint - paint data structure for Emojis
     * @param emoji emoji String to display
     * @return drawable bitmap with the emoji on it
     */
    @NonNull
    private static BitmapDrawable getEmojiDrawableHelper(final EmojiPaint paint, final String emoji) {
        final Bitmap bm = Bitmap.createBitmap(paint.bitmapDimensions.first, paint.bitmapDimensions.second, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bm);
        final int singleCp = singleCodePoint(emoji);
        final TextPaint tPaint = new TextPaint();
        if (singleCp >= NUMBER_START && singleCp <= NUMBER_END) {
            // increase display size of numbers
            tPaint.setTextSize(paint.fontsize * 1.4f);
        } else {
            tPaint.setTextSize(paint.fontsize);
        }
        final StaticLayout lsLayout = new StaticLayout(ensureEmojiPresentation(emoji), tPaint, paint.availableSize, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        canvas.translate((paint.bitmapDimensions.first - lsLayout.getWidth()) / 2f, (paint.bitmapDimensions.second - lsLayout.getHeight()) / 2f - paint.offsetTop);
        lsLayout.draw(canvas);
        canvas.save();
        canvas.restore();
        return new BitmapDrawable(paint.res, bm);
    }

    public static String getFlagEmojiFromCountry(final String countryCode) {
        String cCode = countryCode;
        if (cCode == null || cCode.length() != 2) {
            cCode = "EO"; //will return "unknown" flag
        }
        final String ccTouse = cCode.toUpperCase(Locale.ROOT);
        final int firstLetter = Character.codePointAt(ccTouse, 0) - 0x41 + 0x1F1E6;
        final int secondLetter = Character.codePointAt(ccTouse, 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
    }

    /**
     * get a drawable the size of a marker with a given emoji string (either from cache or freshly built)
     *
     * @param paint - paint data structure for Emojis
     * @param emoji emoji String to display
     * @return drawable bitmap with the emoji on it
     */
    @NonNull
    public static BitmapDrawable getEmojiDrawable(final EmojiPaint paint, final String emoji) {
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
     * get a drawable with given "wantedSize" as both width and height for a given emoji string
     *
     * @param wantedSize wanted size of drawable (width and height) in pixel
     * @param emoji      emoji String to display
     * @return drawable bitmap with emoji on it
     */
    @NonNull
    public static BitmapDrawable getEmojiDrawable(final int wantedSize, final String emoji) {
        return getEmojiDrawable(paintForSize(wantedSize), emoji);
    }

    /**
     * int-based bridge kept for built-in emoji constants (e.g. {@link #RED_FLAG}) used via {@code ImageParam.emoji(int)}
     */
    @NonNull
    public static BitmapDrawable getEmojiDrawable(final int wantedSize, final int emoji) {
        return getEmojiDrawable(wantedSize, new String(Character.toChars(emoji)));
    }

    private static EmojiPaint paintForSize(final int wantedSize) {
        synchronized (EMOJI_PAINT_CACHE_PER_SIZE) {
            EmojiPaint p = EMOJI_PAINT_CACHE_PER_SIZE.get(wantedSize);
            if (p == null) {
                final Resources res = CgeoApplication.getInstance().getApplicationContext().getResources();
                final Pair<Integer, Integer> markerDimensions = new Pair<>(wantedSize, wantedSize);
                p = new EmojiUtils.EmojiPaint(res, markerDimensions, wantedSize, 0, DisplayUtils.calculateMaxFontsize(wantedSize, (int) (wantedSize * 0.8), (int) (wantedSize * 1.5), wantedSize));
                EMOJI_PAINT_CACHE_PER_SIZE.put(wantedSize, p);
            }
            return p;
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
