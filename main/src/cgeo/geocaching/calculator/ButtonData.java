package cgeo.geocaching.calculator;

import static cgeo.geocaching.models.CalcState.ERROR_CHAR;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Data used to capture the state of this particular button such that it can be restored again later
 */
public final class ButtonData implements JSONAble, Serializable {

    /**
     * The three states the button can be put into
     */
    public enum ValueType {
        INPUT_VAL {
            @Override
            public char getLabel(final ButtonData buttonData) {
                return buttonData.inputVal;
            }

            @Override
            public char setAutoChar(final ButtonData buttonData, final char nextChar) {
                buttonData.autoChar = nextChar;
                return nextChar;
            }

        },
        AUTO_CHAR {
            @Override
            public char getLabel(final ButtonData buttonData) {
                return buttonData.autoChar;
            }

            @Override
            public char setAutoChar(final ButtonData buttonData, final char autoChar) {
                char nextChar = autoChar;
                buttonData.autoChar = nextChar++;
                return nextChar;
            }
        },
        BLANK {
            @Override
            public char getLabel(final ButtonData buttonData) {
                return ButtonData.BLANK;
            }

            @Override
            public char setAutoChar(final ButtonData buttonData, final char autoChar) {
                buttonData.autoChar = autoChar;
                return autoChar;
            }
        },
        CUSTOM {
            @Override
            public char getLabel(final ButtonData buttonData) {
                return buttonData.customChar;
            }

            @Override
            public char setAutoChar(final ButtonData buttonData, final char autoChar) {
                char nextChar = autoChar;
                buttonData.autoChar = nextChar;
                if ('A' <= buttonData.customChar && buttonData.customChar <= 'Z') {
                    nextChar = buttonData.customChar;
                    nextChar++;
                }

                return nextChar;
            }
        };

        public abstract char getLabel(ButtonData buttonData);

        public abstract char setAutoChar(ButtonData buttonData, char nextChar);
    }

    private static final long serialVersionUID = -9043775643928797403L;

    /**
     * Character used to 'hide' button
     **/
    public static final char BLANK = ' ';

    public ValueType type = ValueType.INPUT_VAL;
    /**
     * Value obtained from CoordinateInputDialog
     */
    public char inputVal;
    /**
     * Character obtained by automatically 'counting up' variable names
     */
    public char autoChar = CoordinatesCalculateUtils.EMPTY_CHAR;
    /**
     * User defined character
     */
    public char customChar;

    public ButtonData() {
    }

    protected ButtonData(final JSONObject json) {
        type = ValueType.values()[json.optInt("type", 0)];
        inputVal = (char) json.optInt("inputVal", ERROR_CHAR);
        autoChar = (char) json.optInt("autoChar", ERROR_CHAR);
        customChar = (char) json.optInt("customChar", ERROR_CHAR);
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        final JSONObject returnValue = new JSONObject();

        returnValue.put("type", type.ordinal());
        returnValue.put("inputVal", inputVal);
        returnValue.put("autoChar", autoChar);
        returnValue.put("customChar", customChar);

        return returnValue;
    }
}
