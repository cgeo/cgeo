package cgeo.geocaching.ui;

import cgeo.geocaching.calculator.ButtonData;
import cgeo.geocaching.calculator.CoordinatesCalculateUtils;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;

/**
 * This class derives from EditButton and handles all the attributes that are unique to this particular application of button.
 *
 * In particular this button stores three values:
 * 1. The value obtained from the waypoint coordinates that was used at the time the calculator was created
 * 2. An 'AutoChar' which is the next alphabetic character used when the user clicks on the button
 * 3. A custom character which is a specific value assigned by the user when performing a long-click.
 *
 * The button displays one of these values as appropriate as the text of the button.
 */
public class CalculateButton extends EditButton {

    /**
     * Every button (but the last) has a reference to the next button in the calculator.
     * This reference is used to determine the alphabetic progression of the 'AutoChars'.
     */
    private CalculateButton nextButton = null;

    /**
     * The buttons own state is stored in a 'state' object to facilitate easy saving and restoring
     */
    private ButtonData buttonData;


    private class CoordDigitClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View view) {
            handleClick();
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

    @Override
    public void handleClick() {
        toggleType();
        super.handleClick();
    }

    private void setup() {
        setData(new ButtonData());
        butt.setOnClickListener(new CoordDigitClickListener());
    }

    /**
     * The 'Label' is the 'name' that is to be displayed on the button
     */
    @Override
    public char getLabel() {
        return getType().getLabel(getData());
    }

    private void setLabel(final char label) {
        butt.setText(String.valueOf(label));
    }

    private ButtonData.ValueType getType() {
        return buttonData.type;
    }

    private void setType(final ButtonData.ValueType type) {
        buttonData.type = type;
        setBackgroundColour();
    }

    /**
     * Buttons displaying a custom value are given a 'steel-like' appearance so as to distinguish them from regular buttons
     */
    private void setBackgroundColour() {
        /* @todo adapt to new Material theme
        switch (buttonData.type) {

            case CUSTOM:
            case BLANK:
                butt.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.button_default));
                butt.setTextColor(ContextCompat.getColor(getContext(), R.color.steel));
                butt.setTypeface(null, Typeface.BOLD);
                break;

            default:
                final boolean lightSkin = Settings.isLightSkin();

                butt.setBackgroundResource(R.drawable.button_background_kitkat);
                butt.setTypeface(null, Typeface.NORMAL);
                break;
        }
        */
    }

    /**
     * Data used to preserve this buttons state between sessions
     */
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

    private void setInputVal(final char inputVal) {
        buttonData.inputVal = inputVal;
        updateButtonText();
    }

    public CalculateButton getNextButton() {
        return nextButton;
    }

    public CalculateButton setNextButton(final CalculateButton nextButton) {
        this.nextButton = nextButton;

        return nextButton;  // This is done so we can chain 'next' assignments.
    }

    /**
     * This method is used to assign buttons alphabetically increasing names
     *
     * @param currentChar values indicated that the autoChar will be and is updated appropriately for the next button based on the current button's state
     */
    private void setAutoChar(final char currentChar) {
        char nextChar = currentChar;

        if (nextChar < 'A') {
            nextChar = 'A';
        } else if (nextChar > 'Z') {
            nextChar = 'Z';
        }

        nextChar = getType().setAutoChar(getData(), nextChar);
        updateButtonText();

        // Propagate auto-char
        if (nextButton != null) {
            nextButton.setAutoChar(nextChar);
        }
    }

    public void setCustomChar(final Editable customChar) {
        setCustomChar(customChar.charAt(0));
    }

    /**
     * Sets a custom character for this button and updates the 'AutoChar' of subsequent buttons appropriately
     *
     * @param customChar name to be assigned to this button
     */
    @Override
    public void setCustomChar(final char customChar) {
        buttonData.customChar = customChar;
        setType(ButtonData.ValueType.CUSTOM);
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
     *
     * This method is called whenever the coordinate-format cache is manually changed.
     * This is prevent the values of unseen buttons (such as say 'Seconds' buttons) from confusing the user when they change formats.
     * It is also a nice way to clear the calculator and restart afresh.
     */
    public void resetButton() {
        buttonData.autoChar = CoordinatesCalculateUtils.EMPTY_CHAR;
        setType(ButtonData.ValueType.INPUT_VAL);
        updateButtonText();

        if (nextButton != null) {
            nextButton.resetButton();
        }
    }

    /**
     * When the user clicks on a button it's type is switched between 'AutoChar' and 'InputVal'
     *
     * Note: 'CustomValues' buttons will also be assigned to 'InputVal' as well.
     */
    private void toggleType() {
        switch (getType()) {
            case INPUT_VAL:
                setType(ButtonData.ValueType.AUTO_CHAR);
                break;

            case AUTO_CHAR:
                setType(ButtonData.ValueType.BLANK);
                break;

            default:
                setType(ButtonData.ValueType.INPUT_VAL);
        }

        setAutoChar(buttonData.autoChar);
    }

    private void updateButtonText() {
        setLabel(getLabel());
    }

}
