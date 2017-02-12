package cgeo.geocaching.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import cgeo.geocaching.R;

import static cgeo.geocaching.models.CalcState.ERROR_CHAR;

/**
 * This class derives from EditButton and handles all the attribures that are unique to this particular application of button.
 **
 * In particular this button stores three values:
 * 1. The value obtained from cache coordinates that were at the time the calculater war crreated
 * 2. An 'AutoChar' which is the next alphabetic character used when the user clicks on the button
 * 3. A custom character which is a specific value assigned by user by perforning a long-click.
 *
 * The button displayed one of these values as appropriate as the text on the button.
 **/

public class CalculateButton extends EditButton {

    // Flag values used to desognate that no AutoChar has been set
    static final char EMPTY_CHAR = '-';

    // The three stated the button can be put into
    private enum ValueType {
        INPUT_VAL, AUTO_CHAR, CUSTOM
    }

    // Every button (but the last) has a reference to the next button in the calculator.
    // This reference is used to determine the alphabetic progression of the 'AutoChars'.
    private CalculateButton nextButton = null;

    // The buttons own state is stored in a 'state' object to facilitate easy saving and restoring.
    private ButtonData buttonData;

    // Data used to capture the state of this particular button such that it can be restored again later.
    public static class ButtonData implements Serializable {
        ValueType type = ValueType.INPUT_VAL;
        char inputVal;                  // Value obtained from CoordinateInputDialog.
        char autoChar = EMPTY_CHAR;     // Character obtained by automatically 'counting up' variable names.
        char customChar;                // User defined character.

        ButtonData() { }

        public ButtonData(final JSONObject jason) {
            type = ValueType.values()[jason.optInt("type", 0)];
            inputVal   = (char) jason.optInt("inputVal",   ERROR_CHAR);
            autoChar   = (char) jason.optInt("autoChar",   ERROR_CHAR);
            customChar = (char) jason.optInt("customChar", ERROR_CHAR);
        }

        public JSONObject toJASON() throws JSONException {
            final JSONObject rv = new JSONObject();

            rv.put("type", type.ordinal());
            rv.put("inputVal", inputVal);
            rv.put("autoChar", autoChar);
            rv.put("customChar", customChar);

            return rv;
        }
    }

    private class CoordDigitClickListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            toggleType();
        }
    }

    public CalculateButton(final Context context) {
        super(context);
        setup();
    }

    public CalculateButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    private void setup() {
        setData(new ButtonData());
        butt.setOnClickListener(new CoordDigitClickListener());
    }

    // The 'Label' is the 'name' that is to be displayed on the button
    public char getLabel() {
        final char rv;
        switch (getType()) {
            case INPUT_VAL: rv = buttonData.inputVal;   break;
            case AUTO_CHAR: rv = buttonData.autoChar;   break;
            case CUSTOM:    rv = buttonData.customChar; break;
            default:        rv = '*'; // Should never happen.
        }

        return rv;
    }

    public void setLabel(final char lable) {
        butt.setText(String.valueOf(lable));
    }

    public ValueType getType() {
        return buttonData.type;
    }

    public void setType(final ValueType type) {
        buttonData.type = type;
        setBackgroundColour();
    }

    // Buttons displaying a custom value are given a 'steel-like' appearance so as to distinguish them from regular buttons.
    private  void setBackgroundColour() {
        if (buttonData.type == ValueType.CUSTOM) {
            butt.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.button_default));
            butt.setTextColor(ContextCompat.getColor(getContext(), R.color.steel));
            butt.setTypeface(null, Typeface.BOLD);
        } else {
            butt.setBackgroundResource(R.drawable.button_background_kitkat);
            butt.setTextColor(Color.WHITE);
            butt.setTypeface(null, Typeface.NORMAL);
        }
    }

    // Data used to preserve this buttons state between sessions.
    public ButtonData getData() {
        return buttonData;
    }

    public void setData(final ButtonData buttonData) {
        this.buttonData = buttonData;
        setBackgroundColour();
        updateButtonText();
    }

    public void setInputVal(final String inputVal) {
        setInputVal(inputVal.charAt(0));
    }

    public void setInputVal(final char inputVal) {
        buttonData.inputVal = inputVal;
        updateButtonText();
    }

    public CalculateButton getNextButton() {
        return nextButton;
    }

    public void setNextButton(final CalculateButton nextButton) {
        this.nextButton = nextButton;
    }

    /**
     * This method is used to assign buttons alphabetically increasing names.
     * @param currentChar:  values indicated that the autoChar will be and is updated appropriately for the next button based on the current button's state.
     */
    public void setAutoChar(final char currentChar) {
        char nextChar = currentChar;

        if (nextChar < 'A') {
            nextChar = 'A';
        } else if (nextChar > 'Z') {
            nextChar = 'Z';
        }

        switch (getType()) {
            case INPUT_VAL:
                buttonData.autoChar = nextChar;
                break;

            case AUTO_CHAR:
                buttonData.autoChar = nextChar++;
                break;

            case CUSTOM:
                buttonData.autoChar = nextChar;
                if ('A' <= buttonData.customChar && buttonData.customChar <= 'Z') {
                    nextChar = buttonData.customChar;
                    nextChar++;
                }
                break;

            default:
        }

        updateButtonText();

        // Propogate auto-char
        if (nextButton != null) {
            nextButton.setAutoChar(nextChar);
        }
    }

    public void setCustomChar(final Editable customChar) {
        setCustomChar(customChar.charAt(0));
    }

    /**
     * Sets a custom character for this button and updates the 'AutoChar' of subsequent buttons appropriately
     * @param customChar: The 'name' to be assigned to this button
     */
    @Override
    public void setCustomChar(final char customChar) {
        buttonData.customChar = customChar;
        setType(ValueType.CUSTOM);
        updateButtonText();

        final char nextChar;
        if ('A' <= buttonData.customChar && buttonData.customChar <= 'Z') {
            nextChar = (char) (customChar + 1);
        } else {
            nextChar = buttonData.autoChar;
        }

        if (nextButton != null) {
            nextButton.setAutoChar(nextChar);
        }
    }

    /**
     * Restore this button to its original state (as in then the calculator was first created)
     * This method is called whenever the coordinate-format cache is manually changed.
     * This is prevent the values of unseen buttons (such as say 'Seconds' buttons) from confusing the user when they change formats.
     * It is also a nice way to clear the calculator and restart afresh.
     */
    public void resetButton() {
        buttonData.autoChar = EMPTY_CHAR;
        setType(ValueType.INPUT_VAL);
        updateButtonText();

        if (nextButton != null) {
            nextButton.resetButton();
        }
    }

    /**
     * When the user clicks on a button it's type is switched between 'AutoChar' and 'InputVal'.
     * Note: 'CustomValues' buttons will also be assigned to 'InputVal' as well.
     */
    public void toggleType() {
        if (getType() == ValueType.INPUT_VAL) {
            setType(ValueType.AUTO_CHAR);
        } else {
            setType(ValueType.INPUT_VAL);
        }

        setAutoChar(buttonData.autoChar);
    }

    private void updateButtonText() {
        setLabel(getLabel());
    }
}
