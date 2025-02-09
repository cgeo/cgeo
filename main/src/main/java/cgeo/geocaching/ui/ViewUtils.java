package cgeo.geocaching.ui;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.CheckboxItemBinding;
import cgeo.geocaching.databinding.DialogEdittextBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ScalableDrawable;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.annotation.StyleableRes;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.text.util.LinkifyCompat;
import androidx.core.util.Consumer;
import androidx.core.util.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class ViewUtils {

    //if this flag is true, then layouts will be generated with background colors, for layout checking use cases
    private static final boolean DEBUG_LAYOUT = false;

    private static final Resources APP_RESOURCES = CgeoApplication.getInstance() == null || CgeoApplication.getInstance().getApplicationContext() == null ? null :
            CgeoApplication.getInstance().getApplicationContext().getResources();

    private ViewUtils() {
        //no instance
    }

    public static boolean isDebugLayout() {
        return DEBUG_LAYOUT;
    }

    public static int dpToPixel(final float dp) {
        return Math.round(dpToPixelFloat(dp));
    }

    public static float dpToPixelFloat(final float dp) {
        return dp * (APP_RESOURCES == null ? 20f : APP_RESOURCES.getDisplayMetrics().density);
    }

    public static int spToPixel(final float sp) {
        return Math.round(spToPixelFloat(sp));
    }

    public static float spToPixelFloat(final float sp) {
        return sp * (APP_RESOURCES == null ? 20f : APP_RESOURCES.getDisplayMetrics().scaledDensity);
    }

    public static int pixelToDp(final float px) {
        return (int) (px / (APP_RESOURCES == null ? 20f : APP_RESOURCES.getDisplayMetrics().density));
    }

    public static void setTooltip(final View view, final TextParam text) {
        TooltipCompat.setTooltipText(view, text.getText(view.getContext()));
    }

    /**
     * Sets enabled/disabled flag for given view (without crashing on null view)
     */
    public static void setEnabled(final View view, final boolean enabled) {
        if (view == null) {
            return;
        }
        view.setEnabled(enabled);
    }

    /**
     * Sets visibility for given view (without crashing on null view)
     */
    public static void setVisibility(final View view, final int visibility) {
        if (view == null) {
            return;
        }
        view.setVisibility(visibility);
    }

    /**
     * creates a standard column layout and adds it to a given parent view. A standard layout consists of a vertically orientated LinearLayout per column.
     *
     * @param ctx           context to use for creating views
     * @param root          optional. If given, then the column layout will be created IN the given view instead of returning a new one.
     * @param parent        the parent layout to add the new column view to. May be null (if root is not null)
     * @param columnCount   number of columns the layout shall have
     * @param withSeparator if true, a vertical separator line will be drawn between columns
     * @return created linearlayouts, one per column
     */
    public static List<LinearLayout> createAndAddStandardColumnView(final Context ctx, final LinearLayout root, final ViewGroup parent, final int columnCount, final boolean withSeparator) {
        final List<LinearLayout> columns = new ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            final LinearLayout colLl = new LinearLayout(ctx);
            columns.add(colLl);
            colLl.setOrientation(LinearLayout.VERTICAL);
        }

        final ViewGroup colGroup = createColumnView(ctx, root, columnCount, withSeparator, columns::get);
        if (parent != null) {
            parent.addView(colGroup);
        }

        return columns;

    }

    /**
     * creates a column layout and returns it. Provides the option to specify individual layouts per column.
     *
     * @param ctx               context to use for creating views
     * @param root              optional. If given, then the column layout will be created IN the given view instead of returning a new one.
     * @param columnCount       number of columns the layout shall have
     * @param withSeparator     if true, a vertical separator line will be drawn between columns
     * @param columnViewCreator will be called for each column index (0 - "columnCount-1") and shall return the columns content view.
     *                          May return null for some columns, in which case those are empty
     * @return new ViewGroup holding the column layout. If "root" was not null, then "root" is returned.
     */
    public static ViewGroup createColumnView(final Context ctx, final LinearLayout root, final int columnCount, final boolean withSeparator, final Func1<Integer, View> columnViewCreator) {

        final List<Float> columnWidths = new ArrayList<>();
        for (int c = 0; c < columnCount * 2 - 1; c++) {
            columnWidths.add(c % 2 == 0 ? 1f : 0.1f);
        }

        return ViewUtils.createHorizontallyDistributedViews(ctx, root, columnWidths, (i, f) -> {
            if (i % 2 == 1) {
                //column separator
                return ViewUtils.createVerticalSeparator(ctx, !withSeparator);
            }

            return columnViewCreator.call(i / 2);
        }, (i, f) -> f);
    }

    public static <T> ViewGroup createHorizontallyDistributedText(final Context ctx, final LinearLayout root, final List<T> items, final Func2<Integer, T, String> itemTextMapper) {
        return createHorizontallyDistributedViews(ctx, root, items, (idx, item) -> {
            final String itemText = item == null ? null : itemTextMapper.call(idx, item);
            if (itemText != null) {
                final TextView tv = new TextView(ctx);
                tv.setText(itemText);
                tv.setMaxLines(1);
                tv.setTextColor(ctx.getResources().getColor(R.color.colorText));
                if (DEBUG_LAYOUT) {
                    tv.setBackgroundResource(R.drawable.mark_orange);
                }
                return tv;
            }
            return null;
        });
    }

    public static <T> ViewGroup createHorizontallyDistributedViews(final Context ctx, final LinearLayout root, final List<T> items, final Func2<Integer, T, View> viewCreator) {
        return createHorizontallyDistributedViews(ctx, root, items, viewCreator, null);
    }

    public static <T> ViewGroup createHorizontallyDistributedViews(final Context ctx, final LinearLayout root, final List<T> items, final Func2<Integer, T, View> viewCreator, final Func2<Integer, T, Float> weightCreator) {

        final LinearLayout viewGroup = root == null ? new LinearLayout(ctx) : root;
        viewGroup.setOrientation(LinearLayout.HORIZONTAL);

        int idx = 0;
        for (T item : items) {
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.weight = weightCreator == null ? 1 : weightCreator.call(idx, item);

            View itemView = viewCreator.call(idx, item);
            if (itemView == null) {
                itemView = new View(ctx);
            }
            if (itemView instanceof TextView) {
                ((TextView) itemView).setGravity(Gravity.CENTER_HORIZONTAL);
            }
            viewGroup.addView(itemView, lp);

            idx++;
        }
        return viewGroup;
    }

    public static CheckBox addCheckboxItem(final Activity activity, final ViewGroup viewGroup, final TextParam text, final int iconId) {
        return addCheckboxItem(activity, viewGroup, text, iconId, null);
    }

    public static CheckBox addCheckboxItem(final Activity activity, @NonNull final ViewGroup viewGroup, final TextParam text, final int iconId, final TextParam infoText) {

        final ImmutablePair<View, CheckBox> ip = createCheckboxItem(activity, viewGroup, text, ImageParam.id(iconId), infoText);
        viewGroup.addView(ip.left);
        return ip.right;
    }

    public static CheckBox addCheckboxItem(final Activity activity, @NonNull final ViewGroup viewGroup, final TextParam text, final ImageParam imageParam) {

        final ImmutablePair<View, CheckBox> ip = createCheckboxItem(activity, viewGroup, text, imageParam, null);
        viewGroup.addView(ip.left);
        return ip.right;
    }

    public static TextView createTextItem(final Context ctx, @StyleRes final int styleId, final TextParam text) {
        final TextView tv = new TextView(wrap(ctx), null, 0, styleId);
        text.applyTo(tv);
        return tv;
    }

    public static ImageView createIconView(final Context ctx, final int iconId) {
        final ImageView iv = new ImageView(wrap(ctx));
        iv.setImageResource(iconId);
        return iv;
    }

    public static Button createButton(final Context context, @Nullable final ViewGroup root, final TextParam text) {
        return createButton(context, root, text, R.layout.button_view);
    }

    public static Button createButton(final Context context, @Nullable final ViewGroup root, final TextParam text, @LayoutRes final int buttonLayout) {
        final Button button = (Button) LayoutInflater.from(wrap(root == null ? context : root.getContext())).inflate(buttonLayout, root, false);
        text.applyTo(button);
        return button;
    }

    public static TextView createTextSpinnerView(final Context context, @Nullable final ViewGroup root) {
        return (TextView) LayoutInflater.from(wrap(root == null ? context : root.getContext())).inflate(R.layout.textspinner_view, root, false);
    }

    public static Pair<View, EditText> createTextField(final Context context, final String currentValue, final TextParam label, @Nullable final TextParam suffix, final int inputType, final int minLines, final int maxLines) {
        final DialogEdittextBinding binding = DialogEdittextBinding.inflate(LayoutInflater.from(context));
        if (StringUtils.isNotBlank(currentValue)) {
            binding.input.setText(currentValue);
        }
        if (label != null) {
            binding.inputFrame.setHint(label.getText(context));
        }
        if (suffix != null) {
            binding.inputFrame.setSuffixText(suffix.getText(context));
        }
        if (maxLines > 1) {
            binding.input.setSingleLine(false);
            binding.input.setLines(minLines);
            binding.input.setMaxLines(maxLines);
            binding.input.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
            binding.input.setVerticalScrollBarEnabled(true);
            binding.input.setMovementMethod(ScrollingMovementMethod.getInstance());
            binding.input.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
            binding.input.invalidate();
            Dialogs.moveCursorToEnd(binding.input);
        } else {
            binding.input.setSingleLine();
        }
        if (inputType >= 0) {
            binding.input.setInputType(inputType);
        }
        return new Pair<>(binding.getRoot(), binding.input);
    }

    public static ImmutablePair<View, CheckBox> createCheckboxItem(final Activity activity, @Nullable final ViewGroup context, final TextParam text, final ImageParam icon, final TextParam infoText) {

        final View itemView = LayoutInflater.from(context == null ? activity : context.getContext()).inflate(R.layout.checkbox_item, context, false);
        final CheckboxItemBinding itemBinding = CheckboxItemBinding.bind(itemView);
        text.applyTo(itemBinding.itemText);
        if (icon != null) {
            icon.applyTo(itemBinding.itemIcon);
        }
        if (infoText != null) {
            itemBinding.itemInfo.setVisibility(View.VISIBLE);
            itemBinding.itemInfo.setOnClickListener(v -> SimpleDialog.of(activity).setMessage(infoText).show());
        }
        itemView.setOnClickListener(v -> itemBinding.itemCheckbox.toggle());
        itemBinding.itemCheckbox.setContentDescription(itemBinding.itemText.getText());

        return new ImmutablePair<>(itemView, itemBinding.itemCheckbox);
    }

    public static View createVerticalSeparator(final Context context) {
        return createVerticalSeparator(context, false);
    }

    private static View createVerticalSeparator(final Context context, final boolean makeTransparent) {
        final RelativeLayout llSep = new RelativeLayout(context);
        final View separatorView = new View(context, null, 0, R.style.separator_vertical);
        if (makeTransparent) {
            separatorView.setBackgroundResource(R.color.colorBackgroundTransparent);
        }
        final RelativeLayout.LayoutParams rp = new RelativeLayout.LayoutParams(ViewUtils.dpToPixel(1), ViewGroup.LayoutParams.MATCH_PARENT);
        rp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        rp.addRule(RelativeLayout.CENTER_VERTICAL);
        rp.setMargins(0, ViewUtils.dpToPixel(10), 0, ViewUtils.dpToPixel(10));
        llSep.addView(separatorView, rp);
        return llSep;
    }

    public static View createExpandableSeparatorPixelView(final Context context) {
        final TextView view = new TextView(context, null, 0, R.style.separator_pixel);
        view.setText("");
        view.setTextSize(1);
        return view;
    }

    public static void addDetachListener(final View view, final Consumer<View> action) {
        view.getViewTreeObserver().addOnWindowAttachListener(new ViewTreeObserver.OnWindowAttachListener() {
            @Override
            public void onWindowAttached() {
                //do nothing
            }

            @Override
            public void onWindowDetached() {
                action.accept(view);
            }
        });
    }

    /** requests a layout change on given view and runs the given consumer once the view has been measured */
    public static void runOnViewMeasured(final View view, final Function<View, Boolean> action) {
        final ViewTreeObserver.OnGlobalLayoutListener observer = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (view.getMeasuredWidth() > 0 && view.getMeasuredHeight() > 0) {
                    final Boolean continueAction = action.apply(view);
                    if (continueAction == null || !continueAction) {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        };

        //post the run in case "runOnViewMeasured" is used in a layouting context
        //(This is typically the case when it is used in Activity.onCreate)
        view.post(() -> {
            view.getViewTreeObserver().addOnGlobalLayoutListener(observer);
            view.requestLayout();
        });
    }

    /**
     * Sets ListView height dynamically based on the height of the items.
     *
     * @param listView to be resized
     * @return true if the listView is successfully resized, false otherwise
     */
    public static boolean setListViewHeightBasedOnItems(final ListView listView) {
        final ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter != null) {

            final int numberOfItems = listAdapter.getCount();

            // Get total height of all items.
            int totalItemsHeight = 0;
            for (int itemPos = 0; itemPos < numberOfItems; itemPos++) {
                final View item = listAdapter.getView(itemPos, null, listView);
                item.measure(0, 0);
                totalItemsHeight += item.getMeasuredHeight();
            }

            // Get total height of all item dividers.
            final int totalDividersHeight = listView.getDividerHeight() *
                    (numberOfItems - 1);

            // Set list height.
            final ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalItemsHeight + totalDividersHeight;
            listView.setLayoutParams(params);
            listView.requestLayout();

            return true;
        }

        return false;
    }

    public static int getMinimalWidth(final Context ctx, final String text, final int styleId) {
        final TextView tv = new TextView(ctx, null, 0, styleId);
        tv.setText(text);
        tv.measure(0, 0);
        return tv.getMeasuredWidth();

    }

    @SuppressLint("AlwaysShowAction")
    public static void extendMenuActionBarDisplayItemCount(final Context ctx, final Menu menu) {

        final Configuration config = ctx.getResources().getConfiguration();
        final int width = config.screenWidthDp;

        int extendTo = 0;

        //RULES to extend AS ALWAYS are decoded here. See rules of Android in {@link androidx.appcompat.view.ActionBarPolicy#getMaxActionButtons}
        if (width >= 410) {
            extendTo = 4;
        } else if (width >= 360) {
            extendTo = 3;
        }

        for (int pos = 0; pos < menu.size() && pos < extendTo; pos++) {
            final MenuItem item = menu.getItem(pos);
            item.setShowAsAction(SHOW_AS_ACTION_ALWAYS);
        }

    }

    /**
     * Tries its best to return the size of a view. Returns null otherwise.
     * Sometimes it is only possible to return width OR height. In this case the other parameter is set to 0.
     *
     * @return Pair where first is width in pixel and second is height in pixel
     */
    @Nullable
    public static Pair<Integer, Integer> getViewSize(final View view) {
        view.measure(0, 0);
        if (view.getMeasuredHeight() > 0 || view.getMeasuredWidth() > 0) {
            return new Pair<>(view.getMeasuredWidth(), view.getMeasuredHeight());
        }
        //try to get size from layout parameters
        if (view.getLayoutParams() != null) {
            final int w = Math.max(view.getLayoutParams().width, 0);
            final int h = Math.max(view.getLayoutParams().height, 0);
            if (w > 0 || h > 0) {
                return new Pair<>(w, h);
            }
        }

        return null;

    }

    public static boolean currentThreadIsUiThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    public static void runOnUiThread(final boolean forcePost, final Runnable action) {
        if (action == null) {
            return;
        }
        if (!forcePost && currentThreadIsUiThread()) {
            action.run();
        } else {
            AndroidRxUtils.runOnUi(action);
        }
    }

    /** convenience method to call {@link #showToast(Context, TextParam, boolean)} for a short toast with a simple text */
    public static void showShortToast(@Nullable final Context context, final String message) {
        showToast(context, TextParam.text(message), true);
    }

    /** convenience method to call {@link #showToast(Context, TextParam, boolean)} for a short toast with a resource id */
    public static void showShortToast(@Nullable final Context context, final int resId, final Object ... params) {
        showToast(context, TextParam.id(resId, params), true);
    }

    /** convenience method to call {@link #showToast(Context, TextParam, boolean)} for a normal-length toast with a simple text */
    public static void showToast(@Nullable final Context context, final String message) {
        showToast(context, TextParam.text(message), false);
    }

    /** convenience method to call {@link #showToast(Context, TextParam, boolean)} for a normal-length toast with a resource id */
    public static void showToast(@Nullable final Context context, final int resId, final Object ... params) {
        showToast(context, TextParam.id(resId, params), false);
    }

    /**
     * Shows a toast message to the user. This can be called from any thread.
     *
     * @param context any context, usually an activity. If context is null, then the application context will be used.
     * @param text    the message
     * @param shortToast set to true if this should be a short toast
     */
    public static void showToast(@Nullable final Context context, final TextParam text, final boolean shortToast) {
        runOnUiThread(false, () -> {
            final Context toastContext = wrap(context == null || (context instanceof Activity && ((Activity) context).isFinishing()) ?
                    CgeoApplication.getInstance() : context);
            final int toastDuration = shortToast ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
            final CharSequence toastText = text == null ? "---" : text.getText(toastContext);

            Log.iForce("[" + (context == null ? "APP" : context.getClass().getName()) + "].showToast(" + toastText + "){" + (shortToast ? "SHORT" : "LONG") + "}");
            try {
                final Toast toast = Toast.makeText(toastContext, toastText, toastDuration);
                if (Build.VERSION.SDK_INT < 30) {
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
                }
                toast.show();
            } catch (RuntimeException re) {
                //this can happen e.g. in Unit tests when thread has no called Looper.prepare()
                Log.w("Could not show toast '" + toastText + "' to user", re);
            }
        });
    }

    /**
     * If given context is or wraps an Activity, this activity is returned,
     * Otherwise null is returned
     */
    @Nullable
    public static Activity toActivity(final Context ctx) {
        Context iCtx = ctx;
        while (iCtx instanceof ContextWrapper) {
            if (iCtx instanceof Activity) {
                return (Activity) iCtx;
            }
            iCtx = ((ContextWrapper) iCtx).getBaseContext();
        }
        return null;
    }

    public static Context wrap(final Context ctx) {
        return wrap(ctx, R.style.cgeo);
    }

    public static Context wrap(final Context ctx, @StyleRes final int themeResId) {
        //Avoid wrapping already wrapped context's
        if (ctx instanceof ContextThemeWrapperWrapper) {
            final ContextThemeWrapperWrapper wrapperCtx = (ContextThemeWrapperWrapper) ctx;
            if (wrapperCtx.getThemeResId() == themeResId) {
                //ctx is already wrapped with correct themeResId -> just return
                return ctx;
            }
            //re-wrap with new themeResId
            return new ContextThemeWrapperWrapper(wrapperCtx.base, themeResId);
        }
        return new ContextThemeWrapperWrapper(ctx, themeResId);
    }

    /**
     * Wrapper for the ContextThemeWrapper, so we can remember the style/theme id. Should SOLELY be used by method @{link {@link #wrap(Context)}}!
     */
    private static class ContextThemeWrapperWrapper extends ContextThemeWrapper {

        private final int themeResId;
        private final Context base;

        ContextThemeWrapperWrapper(final Context base, @StyleRes final int themeResId) {
            super(base, themeResId);
            this.themeResId = themeResId;
            this.base = base;
        }

        public int getThemeResId() {
            return themeResId;
        }

        public Context getBaseContext() {
            return base;
        }
    }

    public static int indexInParentGroup(final View v) {
        if (v == null || !(v.getParent() instanceof ViewGroup)) {
            return -1;
        }
        return ((ViewGroup) v.getParent()).indexOfChild(v);
    }

    public static void setForParentAndChildren(final View view, final View.OnClickListener clickListener, final View.OnLongClickListener onLongClickListener) {
        if (view == null) {
            return;
        }

        walkViewTree(view, viewItem -> {
            viewItem.setOnClickListener(clickListener);
            viewItem.setOnLongClickListener(onLongClickListener);
            return true;
        }, null);
    }

    public static Bitmap drawableToBitmap(final Drawable drawable) {
        final Bitmap bitmap;

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static BitmapDrawable bitmapToDrawable(final Bitmap bitmap) {
        return new BitmapDrawable(APP_RESOURCES, bitmap);
    }

    public static TextWatcher createSimpleWatcher(@NonNull final Consumer<Editable> callback) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                // Intentionally left empty
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                // Intentionally left empty
            }

            @Override
            public void afterTextChanged(final Editable s) {
                callback.accept(s);
            }
        };

    }

    public static void setSelection(final TextView tv, final int start, final int end) {
        final CharSequence cs = tv.getText();
        if (cs instanceof Spannable) {
            Selection.setSelection((Spannable) cs, start, end);
        }
    }

    /**
     * returns the first (direct or indirect) parent of child satisfying the given condition
     * if child itself satisfies condition, it is returned
     * if no parent of child does satisfy condition, then root is returned
     */
    public static View getParent(final View child, final Predicate<View> condition) {
        View result = child;
        while ((condition == null || !condition.test(result)) && (result.getParent() instanceof View)) {
            result = (View) result.getParent();
        }
        return result;
    }

    /**
     * traverses a viewtree starting from "root". For each view satisfying "condition", the "callback" is called. If "callback" returns false, then treewalk is aborted
     */
    public static boolean walkViewTree(final View root, final Predicate<View> callback, @Nullable final Predicate<View> condition) {
        if ((condition == null || condition.test(root)) && !callback.test(root)) {
            return false;
        }
        if (root instanceof ViewGroup) {
            final ViewGroup vg = (ViewGroup) root;
            final int childCnt = vg.getChildCount();
            for (int i = 0; i < childCnt; i++) {
                if (!walkViewTree(vg.getChildAt(i), callback, condition)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static View nextView(final View start, final Predicate<View> rootCondition, final Predicate<View> condition) {
        final View root = getParent(start, rootCondition);
        final boolean[] foundStart = new boolean[]{false};
        final View[] nextView = new View[]{null};
        ViewUtils.walkViewTree(root, v -> {
            if (foundStart[0]) {
                // we found the start view just before, so this is the next view
                nextView[0] = v;
                return false;
            } else if (v == start) {
                //we found the start view, mark this
                foundStart[0] = true;
            }
            return true;
        }, condition);

        return nextView[0];

    }

    /** null-safe call to view */
    public static void applyToView(@Nullable final View view, final Action1<View> applyMethod) {
        if (view != null) {
            applyMethod.call(view);
        }
    }

    /** safe Linkify.addLinks version, prevents crash on older Android versions, see #14202 */
    public static boolean safeAddLinks(@NonNull final TextView textView, @LinkifyCompat.LinkifyMask final int mask) {
        try {
            return Linkify.addLinks(textView, mask);
        } catch (RuntimeException re) {
            Log.d("Caught Error on Linkify.addLinks", re);
            return false;
        }
    }

    /** safe Linkify.addLinks version, prevents crash on older Android versions, see #14202 */
    public static boolean safeAddLinks(@NonNull final Spannable sp, @LinkifyCompat.LinkifyMask final int mask) {
        try {
            return Linkify.addLinks(sp, mask);
        } catch (RuntimeException re) {
            Log.d("Caught Error on Linkify.addLinks", re);
            return false;
        }
    }

    /** Sets padding (in DP) to apply to each list item.
     *  List of int[] are added to each other to get the final padding. For each int[] following rule applies:
     *  If 4 numbers are given, they are applied to left, top, right, bottom in that order
     *  If 2 numbers are given, they are applied to horizontal (left+right), vertical (top+bottom)
     *  If 1 number is given it is applied to all 4 sides
     */
    public static void applyPadding(final View view, final int[] ... paddingsInDp) {
        if (view == null || paddingsInDp == null) {
            return;
        }
        final int[] result = new int[4];
        for (int[] paddingInDp : paddingsInDp) {
            final int len = paddingInDp == null ? 0 : paddingInDp.length;
            if (len != 4 && len != 2 && len != 1) {
                continue;
            }
            //left
            result[0] += paddingInDp[0];
            //top
            result[1] += len == 4 || len == 2 ? paddingInDp[1] : paddingInDp[0];
            //right
            result[2] += len == 4 ? paddingInDp[2] : paddingInDp[0];
            //bottom
            result[3] += len == 4 ? paddingInDp[3] : len == 2 ? paddingInDp[1] : paddingInDp[0];
        }

        view.setPadding(ViewUtils.dpToPixel(result[0]), ViewUtils.dpToPixel(result[1]), ViewUtils.dpToPixel(result[2]), ViewUtils.dpToPixel(result[3]));
    }

    public static void setCoordinates(@Nullable final Geopoint gp, final TextView textView) {
        if (gp != null) {
            textView.setText(String.format("%s%n%s", gp.format(GeopointFormatter.Format.LAT_DECMINUTE), gp.format(GeopointFormatter.Format.LON_DECMINUTE)));
        } else {
            textView.setText(String.format("%s%n%s", LocalizationUtils.getString(R.string.waypoint_latitude_null), LocalizationUtils.getString(R.string.waypoint_longitude_null)));
        }
    }

    public static Geopoint getCoordinates(final TextView textView) {
        final String[] latlonText = textView.getText().toString().split("\n");
        if (latlonText.length < 2) {
            return null;
        }
        final String latText = StringUtils.trim(latlonText[0]);
        final String lonText = StringUtils.trim(latlonText[1]);

        if (!latText.equals(LocalizationUtils.getString(R.string.waypoint_latitude_null)) && !lonText.equals(LocalizationUtils.getString(R.string.waypoint_longitude_null))) {
            try {
                return new Geopoint(latText, lonText);
            } catch (final Geopoint.ParseException e) {
                // ignore
            }
        }
        return null;
    }

    public static void consumeAttributes(final Context context, final AttributeSet attrs, @StyleableRes final int[] styleable,
                                          @AttrRes final int defStyleAttr, final @StyleRes int defStyleRes, final Consumer<TypedArray> consumer) {
        if (attrs != null) {
            final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, styleable, defStyleAttr, defStyleRes);
            try {
                consumer.accept(a);
            } finally {
                a.recycle();
            }
        }
    }

    public static Drawable getDrawable(@DrawableRes final int drwId, final boolean mutate) {
        return getDrawable(drwId, 1.0f, mutate);
    }

    public static Drawable getDrawable(@DrawableRes final int drwId, final float scalingFactor, final boolean mutate) {
        Drawable result = DrawableCompat.wrap(Objects.requireNonNull(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), drwId, null)));
        if (mutate) {
            result.mutate();
        }
        if (scalingFactor != 1.0f) {
            result = new ScalableDrawable(result, scalingFactor);
            if (mutate) {
                result.mutate();
            }
        }
        return result;
    }

    public static String getEditableText(@Nullable final Editable editable) {
        return editable == null ? "" : editable.toString();
    }
}
