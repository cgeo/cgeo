package cgeo.geocaching.ui;


import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.CalculatedCoordinateType;
import cgeo.geocaching.utils.KeyableCharSet;
import cgeo.geocaching.utils.TextParser;
import cgeo.geocaching.utils.formulas.Formula;
import cgeo.geocaching.utils.formulas.FormulaException;
import cgeo.geocaching.utils.functions.Func1;
import static cgeo.geocaching.models.CalculatedCoordinateType.PLAIN;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.gridlayout.widget.GridLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class CalculatedCoordinateInputGuideView extends LinearLayout {

    private static final Map<CalculatedCoordinateType, String> REFILL_PATTERNS = new HashMap<>();
    private static final Map<CalculatedCoordinateType, Func1<Geopoint, String>> LAT_CONVERTERS = new HashMap<>();
    private static final Map<CalculatedCoordinateType, Func1<Geopoint, String>> LON_CONVERTERS = new HashMap<>();

    private static final Set<Integer> SIGNIFICANT_CHARS = new HashSet<>();

    private static final KeyableCharSet CLOSING_PAREN_SET = KeyableCharSet.createFor(")");

    private GridLayout grid;
    private EditText value;
    private View valueLayout;
    private View valueHiddenHint;

    private CharButton markedButton;

    static {
        final Locale locale = Locale.US;
        final char decSep = '.';
        REFILL_PATTERNS.put(CalculatedCoordinateType.DEGREE, "1__._____°");
        REFILL_PATTERNS.put(CalculatedCoordinateType.DEGREE_MINUTE, "1__°__.___'");
        REFILL_PATTERNS.put(CalculatedCoordinateType.DEGREE_MINUTE_SEC, "1__°__'__.___\"");

        LAT_CONVERTERS.put(CalculatedCoordinateType.DEGREE, gp ->
                String.format(locale, "%c %08.5f°", gp.getLatDir(), Math.abs(gp.getLatitude())));
        LON_CONVERTERS.put(CalculatedCoordinateType.DEGREE, gp ->
                String.format(locale, "%c%09.5f°", gp.getLonDir(), Math.abs(gp.getLongitude())));

        LAT_CONVERTERS.put(CalculatedCoordinateType.DEGREE_MINUTE, gp ->
                String.format(locale, "%c %02d°%02d%c%03d'", gp.getLatDir(), gp.getDecMinuteLatDeg(), gp.getDecMinuteLatMin(), decSep, gp.getDecMinuteLatMinFrac()));
        LON_CONVERTERS.put(CalculatedCoordinateType.DEGREE_MINUTE, gp ->
                String.format(locale, "%c%03d°%02d%c%03d'", gp.getLonDir(), gp.getDecMinuteLonDeg(), gp.getDecMinuteLonMin(), decSep, gp.getDecMinuteLonMinFrac()));

        LAT_CONVERTERS.put(CalculatedCoordinateType.DEGREE_MINUTE_SEC, gp ->
                String.format(Locale.getDefault(), "%c %02d°%02d'%02d%c%03d\"", gp.getLatDir(), gp.getDMSLatDeg(), gp.getDMSLatMin(), gp.getDMSLatSec(), decSep, gp.getDMSLatSecFrac()));
        LON_CONVERTERS.put(CalculatedCoordinateType.DEGREE_MINUTE_SEC, gp ->
                String.format(Locale.getDefault(), "%c%03d°%02d'%02d%c%03d\"", gp.getLonDir(), gp.getDMSLonDeg(), gp.getDMSLonMin(), gp.getDMSLonSec(), decSep, gp.getDMSLonSecFrac()));

        for (int c = 'A'; c <= 'Z'; c++) {
            SIGNIFICANT_CHARS.add(c);
        }
        for (int c = 'a'; c <= 'z'; c++) {
            SIGNIFICANT_CHARS.add(c);
        }
        for (int c = '0'; c <= '9'; c++) {
            SIGNIFICANT_CHARS.add(c);
        }
        SIGNIFICANT_CHARS.add((int) '_');
    }

    private Consumer<Pair<String, String>> changeListener = null;
    private boolean changeListenerActive = true;

    public CalculatedCoordinateInputGuideView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CalculatedCoordinateInputGuideView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CalculatedCoordinateInputGuideView(final Context context) {
        super(context);
        init();
    }

    public void setChangeListener(final Consumer<Pair<String, String>> changeListener) {
        this.changeListener = changeListener;
    }

    /**
     * returns true if latPatternn/lonPattern could be applied, false otherwise
     */
    public boolean setData(final CalculatedCoordinateType type, final String latPattern, final String lonPattern, final Geopoint gp) {
        this.setVisibility(type == PLAIN ? GONE : VISIBLE);
        boolean result = false;
        if (type != PLAIN) {
            result = recreate(type, latPattern, lonPattern, gp);
        }
        markButton(null);
        callChangeListener();
        return result;
    }

    @Nullable
    public static CalculatedCoordinateType guessType(final String latPattern, final String lonPattern) {
        for (CalculatedCoordinateType type : CalculatedCoordinateType.values()) {
            if (PLAIN == type) {
                continue;
            }
            final boolean b1 = checkAndApply(REFILL_PATTERNS.get(type), latPattern, 0, null, -1);
            final boolean b2 = checkAndApply(REFILL_PATTERNS.get(type), lonPattern, 1, null, -1);
            if (b1 && b2) {
                return type;
            }
        }
        return null;
    }

    public Pair<String, String> getPlain() {
        return new Pair<>(scanString(0), scanString(1));
    }

    private String scanString(final int row) {
        if (grid.getChildCount() == 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(((TextView) grid.getChildAt(row * this.grid.getColumnCount())).getText());
        for (int vIdx = row * this.grid.getColumnCount() + 1; vIdx < (row + 1) * this.grid.getColumnCount(); vIdx++) {
            final View v = grid.getChildAt(vIdx);
            if (v instanceof TextView) {
                final String text = ((TextView) v).getText().toString();
                sb.append(text);
                //add a space after degree/minute/second-marker if it is not the last char
                if (!".".equals(text) && vIdx + 1 < (row + 1) * this.grid.getColumnCount()) {
                    sb.append(" ");
                }
            } else if (v instanceof CharButton) {
                sb.append(((CharButton) v).getFormulaText());
            }
        }
        return sb.toString();
    }


    private void init() {
        setOrientation(VERTICAL);
        final Context ctw = ViewUtils.wrap(getContext());
        inflate(ctw, R.layout.calculatedcoordinate_input_guide, this);
        this.grid = findViewById(R.id.cc_guide_grid);
        this.value = findViewById(R.id.cc_guide_value);
        this.valueLayout = findViewById(R.id.cc_guide_value_layout);
        this.valueHiddenHint = findViewById(R.id.cc_guide_value_hiddenhint);

        this.value.addTextChangedListener(ViewUtils.createSimpleWatcher(t -> {
            if (markedButton != null) {
                markedButton.setText(t.toString());
            }
        }));

        this.value.setOnEditorActionListener((vv, a, e) -> {
            if (markedButton == null) {
                return false;
            }
            final View nextButton = ViewUtils.nextView(markedButton, v -> v instanceof GridLayout, v -> v instanceof CharButton);
            if (nextButton == null) {
                return false;
            }
            markButton((CharButton) nextButton);
            return true;
        });

        setData(PLAIN, null, null, null);
        markButton(null);
    }

    private void callChangeListener() {
        if (changeListenerActive && this.changeListener != null) {
            this.changeListener.accept(getPlain());
        }
    }

    // splitting up that method would not help improve readability
    @SuppressWarnings("PMD.NPathComplexity")
    private static boolean checkAndApply(final String fillPattern, final String coordPattern, final int row, final GridLayout grid, final int viewPos) {
        if (coordPattern == null) {
            return false;
        }
        final TextParser coordParser = new TextParser(coordPattern.trim());
        int vp = viewPos;
        if (!checkAndApplyHemisphere(row, coordParser, grid, vp)) {
            return false;
        }
        if (vp >= 0) {
            vp++;
        }

        for (char fp : fillPattern.toCharArray()) {
            switch (fp) {
                case '_':
                case '1':
                    if (fp == '1' && row == 0) {
                        if (vp > 0) {
                            vp++;
                        }
                        break;
                    }
                    coordParser.skipWhitespaces();
                    if (coordParser.eof()) {
                        return false;
                    }
                    final String digitText;
                    if (coordParser.ch() == '(') {
                        try {
                            final Formula f = Formula.compile(coordPattern, coordParser.pos() + 1, CLOSING_PAREN_SET);
                            digitText = f.getExpression();
                            coordParser.setPos(coordParser.pos() + digitText.length() + 2);
                        } catch (FormulaException fe) {
                            return false;
                        }
                    } else if (coordParser.chIsIn(SIGNIFICANT_CHARS)) {
                        digitText = coordParser.ch() + "";
                        coordParser.next();
                    } else {
                        return false;
                    }
                    if (vp >= 0) {
                        ((CharButton) grid.getChildAt(vp)).setText(digitText);
                        vp++;
                    }
                    break;
                default:
                    final boolean isWs = Character.isWhitespace(coordParser.ch());
                    coordParser.skipWhitespaces();
                    if (!isWs && coordParser.ch() != fp) {
                        return false;
                    }
                    if (coordParser.ch() == fp) {
                        coordParser.next();
                    }
                    if (vp >= 0) {
                        vp++;
                    }
            }
        }
        return coordParser.eof();
    }

    @SuppressLint("SetTextI18n")
    private static boolean checkAndApplyHemisphere(final int row, final TextParser coordParser, final GridLayout grid, final int vp) {
        final char hem = Character.toUpperCase(coordParser.ch());
        switch (row) {
            case 0:
                if (hem != 'N' && hem != 'S') {
                    return false;
                }
                break;
            case 1:
            default:
                if (hem != 'E' && hem != 'W' && hem != 'O') {
                    return false;
                }
                break;
        }
        coordParser.next();
        if (vp >= 0) {
            ((TextView) grid.getChildAt(vp)).setText(hem + "");
        }
        return true;
    }

    private boolean recreate(final CalculatedCoordinateType type, final String latPattern, final String lonPattern, final Geopoint gp) {
        changeListenerActive = false;
        this.setVisibility(INVISIBLE);
        recreateViews(REFILL_PATTERNS.get(type));


        //set default digits from given geopoint (if any)
        final boolean gpSuccess = applyPatternsFor(type, gp);
        //set pattern (if a valid pattern is given)
        final boolean patternSuccess = applyPatterns(type, latPattern, lonPattern);

        if (!patternSuccess) {
            //in case of non-success, reapply previous patterns for a consistent view
            if (gpSuccess) {
                applyPatternsFor(type, gp);
            } else {
                applyPatternsFor(type, Geopoint.ZERO);
            }
        }

        this.setVisibility(VISIBLE);
        changeListenerActive = true;
        callChangeListener();
        return patternSuccess;
    }

    private void recreateViews(final String viewPattern) {
        this.grid.removeAllViews();
        this.grid.setColumnCount(viewPattern.length() + 1);
        this.grid.setRowCount(2);
        for (int row = 0; row < 2; row++) {
            final Button hem = ViewUtils.createButton(getContext(), this, TextParam.text("N"));
            hem.setOnClickListener(v -> changeHemisphereButtonValue(hem));
            final GridLayout.LayoutParams hlp = createGridLayoutParams(0);
            hlp.setMargins(0, 0, ViewUtils.dpToPixel(3), 0);
            this.grid.addView(hem, hlp);

            for (char c : viewPattern.toCharArray()) {
                if (c == '_' || c == '0' || c == '1') {
                    final View v;
                    if ((c == '0' && row == 1) || (c == '1' && row == 0)) {
                        //add a space
                        v = new Space(getContext());
                    } else {
                        v = new CharButton(getContext());
                        ((CharButton) v).setText("x");
                    }
                    final GridLayout.LayoutParams lp = createGridLayoutParams(1);
                    lp.setMargins(ViewUtils.dpToPixel(1), ViewUtils.dpToPixel(2), ViewUtils.dpToPixel(1), ViewUtils.dpToPixel(2));
                    this.grid.addView(v, lp);
                } else {
                    final TextView tv = ViewUtils.createTextItem(getContext(), R.style.text_label, TextParam.text("" + c));
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);

                    final GridLayout.LayoutParams lp = createGridLayoutParams(0);
                    lp.setMargins(ViewUtils.dpToPixel(1), ViewUtils.dpToPixel(2), ViewUtils.dpToPixel(1), ViewUtils.dpToPixel(2));
                    tv.setGravity(c == '.' ? Gravity.CENTER : Gravity.TOP);
                    this.grid.addView(tv, lp);
                }
            }
        }
    }

    private boolean applyPatternsFor(final CalculatedCoordinateType type, final Geopoint gp) {
        if (gp == null) {
            return false;
        }
        final String latPattern = LAT_CONVERTERS.get(type).call(gp);
        final String lonPattern = LON_CONVERTERS.get(type).call(gp);
        return applyPatterns(type, latPattern, lonPattern);

    }

    private boolean applyPatterns(final CalculatedCoordinateType type, final String latPattern, final String lonPattern) {
        if (latPattern == null || lonPattern == null) {
            return false;
        }

        final boolean b1 = checkAndApply(REFILL_PATTERNS.get(type), latPattern, 0, grid, 0);
        final boolean b2 = checkAndApply(REFILL_PATTERNS.get(type), lonPattern, 1, grid, REFILL_PATTERNS.get(type).length() + 1);

        return b1 && b2;
    }

    @SuppressLint("SetTextI18n")
    private void changeHemisphereButtonValue(final Button hem) {
        final char newChar;
        switch (Character.toUpperCase(hem.getText().charAt(0))) {
            case 'N':
                newChar = 'S';
                break;
            case 'S':
                newChar = 'N';
                break;
            case 'E':
            case 'O':
                newChar = 'W';
                break;
            case 'W':
                newChar = 'E';
                break;
            default:
                newChar = ' ';
                break;
        }
        hem.setText("" + newChar);
        callChangeListener();
    }

    private GridLayout.LayoutParams createGridLayoutParams(final int colWeight) {
        final GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL),
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, colWeight));
        lp.width = colWeight == 0 ? WRAP_CONTENT : 20;
        lp.height = WRAP_CONTENT;
        return lp;
    }

    public void unmarkButtons() {
        if (this.markedButton != null) {
            markButton(null);
        }
    }

    private void markButton(final CharButton newButton) {
        final CharButton prevButton = this.markedButton;
        this.markedButton = null;

        if (prevButton != null) {
            prevButton.unmark();
        }

        if (newButton != null) {
            newButton.mark();
            this.valueLayout.setVisibility(VISIBLE);
            this.valueHiddenHint.setVisibility(INVISIBLE);
            this.value.setText(newButton.getText());
            this.value.requestFocus();
            this.value.setSelection(0, newButton.getText().length());
            Keyboard.show(getContext(), this.value);
        } else {
            this.value.setText("");
            this.valueLayout.setVisibility(INVISIBLE);
            this.valueHiddenHint.setVisibility(VISIBLE);
        }

        this.markedButton = newButton;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        markButton(null);
    }


    private class CharButton extends RelativeLayout {

        /**
         * The actual button used for the most part
         */
        private Button butt;
        private ImageView image;

        private String text;


        public String getText() {
            return text;
        }

        public String getFormulaText() {
            if (StringUtils.isBlank(text)) {
                return "_";
            }
            if (text.length() == 1) {
                return text;
            }
            return "(" + text + ")";
        }

        public void mark() {
            this.setBackgroundResource(R.drawable.calculatecoordinateguide_button_active);
        }

        public void unmark() {
            this.setBackgroundResource(0);
        }

        @SuppressLint("SetTextI18n")
        public void setText(final String newText) {
            this.text = newText == null ? null : ("_".equals(newText) ? " " : newText.trim());
            if (text != null && text.length() > 1) {
                this.butt.setText(" ");
                this.image.setVisibility(VISIBLE);
            } else {
                this.butt.setText(text == null || text.length() == 0 ? " " : "" + text.charAt(0));
                this.image.setVisibility(GONE);
            }
            callChangeListener();
        }

        /**
         * These variables are accessed from the derived class 'CalculateButton'
         */
        CharButton(final Context context) {
            super(context);
            init();
        }

        @SuppressLint("SetTextI18n")
        private void init() {

            initView();

            setLongClickable(true);
            butt.setClickable(false);
            butt.setLongClickable(false);
            butt.setText("0");

            super.setOnClickListener(v -> markButton(this));
        }

        private void initView() {
            butt = new Button(ViewUtils.wrap(getContext()));
            butt.setTextSize(22f);
            butt.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            butt.setGravity(Gravity.CENTER);
            butt.setPadding(0, 0, 0, 0);

            image = new ImageView(ViewUtils.wrap(getContext()));
            image.setImageResource(R.drawable.ic_menu_variable);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setPadding(ViewUtils.dpToPixel(3), ViewUtils.dpToPixel(3), ViewUtils.dpToPixel(3), ViewUtils.dpToPixel(3));
            image.setVisibility(GONE);
            image.setElevation(1000f);

            final LayoutParams lp = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_VERTICAL);
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            addView(butt, lp);
            addView(image, lp);

            unmark();
        }

    }


}
