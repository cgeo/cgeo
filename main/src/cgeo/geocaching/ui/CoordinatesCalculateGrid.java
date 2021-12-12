package cgeo.geocaching.ui;


import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.CalculatedCoordinateType;
import cgeo.geocaching.utils.functions.Func1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;
import static android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
import static android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import androidx.core.util.Consumer;
import androidx.gridlayout.widget.GridLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CoordinatesCalculateGrid extends GridLayout {

    private static final Map<CalculatedCoordinateType, String> REFILL_PATTERNS = new HashMap<>();
    private static final Map<CalculatedCoordinateType, Func1<Geopoint, String>> LAT_CONVERTERS = new HashMap<>();
    private static final Map<CalculatedCoordinateType, Func1<Geopoint, String>> LON_CONVERTERS = new HashMap<>();

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
    }


    private CalculatedCoordinateType type = null;
    private Consumer<Pair<String, String>> changeListener = null;
    private boolean changeListenerActive = true;

    public CoordinatesCalculateGrid(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CoordinatesCalculateGrid(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CoordinatesCalculateGrid(final Context context) {
        super(context);
        init();
    }

    public void setChangeListener(final Consumer<Pair<String, String>> changeListener) {
        this.changeListener = changeListener;
    }

    public void setData(final CalculatedCoordinateType type, final String latPattern, final String lonPattern, final Geopoint gp) {
        if (this.type == type) {
            return;
        }
        this.type = type;
        this.setVisibility(this.type == CalculatedCoordinateType.PLAIN ? GONE : VISIBLE);
        if (this.type != CalculatedCoordinateType.PLAIN) {
            recreate(type, latPattern, lonPattern, gp);
        }
    }

    public Pair<String, String> getPlain() {
        return new Pair<>(scanString(0), scanString(1));
    }

    private String scanString(final int row) {
        if (getChildCount() == 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(((TextView) getChildAt(row * getColumnCount())).getText());
        for (int vIdx = row * getColumnCount() + 1; vIdx < (row + 1) * getColumnCount(); vIdx++) {
            final View v = getChildAt(vIdx);
            if (v instanceof TextView) {
                final String text = ((TextView) v).getText().toString();
                sb.append(text);
                if (!".".equals(text) && vIdx + 1 < (row + 1) * getColumnCount()) {
                    sb.append(" ");
                }
            } else if (v instanceof SingleCharButton) {
                sb.append(((SingleCharButton) v).getChar());
            }
        }
        return sb.toString();
    }


    private void init() {
        setData(CalculatedCoordinateType.PLAIN, null, null, null);
    }

    private void callChangeListener() {
        if (changeListenerActive && this.changeListener != null) {
            this.changeListener.accept(getPlain());
        }
    }

    private char getNextFreeVar() {
        final Set<Character> usedVars = new HashSet<>();
        for (int i = 0; i < getChildCount(); i++) {
            final View v = getChildAt(i);
            if (v instanceof SingleCharButton) {
                final char c = ((SingleCharButton) v).getChar();
                if (c >= 'A' && c <= 'Z') {
                    usedVars.add(c);
                }
            }
        }
        char test = 'A';
        while (test < 'Z' && usedVars.contains(test)) {
            test = (char) (((int) test) + 1);
        }
        return test;
    }

    private void recreate(final CalculatedCoordinateType type, final String latPattern, final String lonPattern, final Geopoint gp) {
        changeListenerActive = false;
        this.setVisibility(INVISIBLE);
        recreateViews(REFILL_PATTERNS.get(type));


        //set default digits from given geopoint (if any)
        applyPatternsFor(type, gp);
        //set pattern (if a valid pattern is given)
        applyPatterns(latPattern, lonPattern);

        this.setVisibility(VISIBLE);
        changeListenerActive = true;
        callChangeListener();
    }

    private void recreateViews(final String viewPattern) {
        this.removeAllViews();
        setColumnCount(viewPattern.length() + 1);
        setRowCount(2);
        for (int row = 0; row < 2; row++) {
            final Button hem = ViewUtils.createButton(getContext(), this, TextParam.text("N"));
            hem.setOnClickListener(v -> changeHemisphereButtonValue(hem));
            final GridLayout.LayoutParams hlp = createLayoutParams(0);
            hlp.setMargins(0, 0, ViewUtils.dpToPixel(3), 0);
            this.addView(hem, hlp);

            for (char c : viewPattern.toCharArray()) {
                if (c == '_' || c == '0' || c == '1') {
                    final View v;
                    if ((c == '0' && row == 1) || (c == '1' && row == 0)) {
                        //add a space
                        v = new Space(getContext());
                    } else {
                        v = new SingleCharButton(getContext());
                    }
                    final GridLayout.LayoutParams lp = createLayoutParams(1);
                    lp.setMargins(ViewUtils.dpToPixel(1), ViewUtils.dpToPixel(2), ViewUtils.dpToPixel(1), ViewUtils.dpToPixel(2));
                    this.addView(v, lp);
                } else {
                    final TextView tv = ViewUtils.createTextItem(getContext(), R.style.text_label, TextParam.text("" + c));
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
                    final GridLayout.LayoutParams lp = createLayoutParams(0);
                    lp.setMargins(ViewUtils.dpToPixel(1), ViewUtils.dpToPixel(2), ViewUtils.dpToPixel(1), ViewUtils.dpToPixel(2));
                    tv.setGravity(c == '.' ? Gravity.CENTER : Gravity.TOP);
                    this.addView(tv, lp);
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
        return applyPatterns(latPattern, lonPattern);

    }

    private boolean applyPatterns(final String latPattern, final String lonPattern) {
        if (latPattern == null || lonPattern == null) {
            return false;
        }
        for (int row = 0; row < 2; row++) {
            final String rowPattern = row == 0 ? latPattern : lonPattern;
            final int[] pos = new int[] { -1 };
            if (applyHemisphere(row, rowPattern, pos)) {
                return false;
            }
            for (int col = 1; col < this.getColumnCount(); col++) {
                final View v = this.getChildAt(row * getColumnCount() + col);
                if (v instanceof SingleCharButton) {
                    final char c = nextSignificantChar(rowPattern, pos);
                    if (c == '#') {
                        return false;
                    }
                    ((SingleCharButton) v).setChar(c);
                }
            }
            if (nextSignificantChar(rowPattern, pos) != '#') {
                return false;
            }
        }
        return true;
    }

    @SuppressLint("SetTextI18n")
    private boolean applyHemisphere(final int row, final String rowPattern, final int[] pos) {
        final char hem = nextSignificantChar(rowPattern, pos);
        switch (row) {
            case 0:
                if (hem != 'N' && hem != 'S') {
                    return true;
                }
                break;
            case 1:
            default:
                if (hem != 'E' && hem != 'W' && hem != 'O') {
                    return true;
                }
                break;
        }
        ((TextView) this.getChildAt(row * getColumnCount())).setText(hem + "");
        return false;
    }

    private char nextSignificantChar(final String pattern, final int[] pos) {
        if (pattern == null) {
            return '#';
        }
        while (true) {
            final char c = nextChar(pattern, pos);
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_') || (c == '#')) {
                return c;
            }
        }
    }

    private char nextChar(final String pattern, final int[] pos) {
        return (++pos[0] < pattern.length()) ? pattern.charAt(pos[0]) : '#';
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

    private GridLayout.LayoutParams createLayoutParams(final int colWeight) {
        final GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL),
            GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, colWeight));
        lp.width = colWeight == 0 ? WRAP_CONTENT : 0;
        return lp;
    }


    private class SingleCharButton extends RelativeLayout {

        /**
         * EditText used to facilitate keyboard entry
         */
        private EditText edit;
        /**
         * The actual button used for the most part
         */
        private Button butt;

        private char lastSetDigit = '0';
        private char lastSetVariable = (char) 0;

        public char changeCharacter(final char currentChar) {
            if (currentChar == '_') {
                //next char is a digit
                return lastSetDigit;
            } else if (currentChar >= '0' && currentChar <= '9') {
                //next char is a variable
                if (lastSetVariable == (char) 0) {
                    lastSetVariable = getNextFreeVar();
                }
                return lastSetVariable;
            } else {
                //next is the overflow char
                return '_';
            }

        }

        @SuppressLint("SetTextI18n")
        public void setChar(final char c) {
            final char cToSet = c == '_' ? ' ' : c;
            this.butt.setText("" + Character.toUpperCase(cToSet));
            if (c >= '0' && c <= '9') {
                this.lastSetDigit = c;
            }
            if (c >= 'A' && c <= 'Z') {
                this.lastSetVariable = c;
            }
            callChangeListener();
        }

        public char getChar() {
            final char c = this.butt.getText().charAt(0);
            return c == ' ' ? '_' : c;
        }

        /**
         * These variables are accessed from the derived class 'CalculateButton'
         */
        SingleCharButton(final Context context) {
            super(context);
            init();
        }

        @SuppressLint("SetTextI18n")
        private void init() {

            initView();

            setLongClickable(true);

            edit.setClickable(false);
            edit.setLongClickable(false);

            butt.setClickable(false);
            butt.setLongClickable(false);
            butt.setText("0");

            edit.setVisibility(INVISIBLE);

            super.setOnClickListener(v -> setChar(changeCharacter(getChar())));
            super.setOnLongClickListener(v -> handleLongClick());
        }

        private void initView() {
            //copied from EditButton class
            edit = new EditText(getContext());
            edit.setMaxLines(1);
            edit.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            edit.setInputType(InputType.TYPE_CLASS_TEXT);
            edit.setTextSize(22f);
            edit.setInputType(TYPE_TEXT_FLAG_CAP_CHARACTERS | TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            edit.setPadding(0, 0, 0, 0);

            butt = new Button(getContext());
            butt.setTextSize(22f);
            butt.setInputType(TYPE_TEXT_FLAG_CAP_CHARACTERS | TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            butt.setPadding(0, 0, 0, 0);

            final RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            edit.setLayoutParams(lp);
            butt.setLayoutParams(lp);

            addView(butt);
            addView(edit);

            edit.setVisibility(INVISIBLE);
        }

        private boolean handleLongClick() {
            final InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            butt.setVisibility(View.INVISIBLE);
            edit.setVisibility(View.VISIBLE);

            if (edit.requestFocus()) {
                imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
            }

            edit.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
                final Editable text = edit.getText();

                if (text.length() > 0) {
                    final char customChar = text.charAt(0);

                    if (Character.isLetterOrDigit(customChar)) {
                        setChar(Character.toUpperCase(customChar));
                    } else {
                        final Context context = getContext();
                        Toast.makeText(context, context.getString(R.string.warn_invalid_character), Toast.LENGTH_SHORT).show();
                    }

                    edit.setText("");
                } else {
                    edit.clearFocus();
                }
            }));

            edit.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    edit.setVisibility(View.INVISIBLE);
                    butt.setVisibility(View.VISIBLE);
                    imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
                }
            });

            return true;
        }
    }
}
