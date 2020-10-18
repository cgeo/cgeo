package cgeo.geocaching.ui

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import cgeo.geocaching.R
import cgeo.geocaching.models.CalcState
import cgeo.geocaching.settings.Settings
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
import kotlin.jvm.Throws

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
class CalculateButton : EditButton {
    /** The three states the button can be put into  */
    enum class ValueType {
        INPUT_VAL {
            override fun getLabel(buttonData: ButtonData): Char {
                return buttonData.inputVal
            }

            override fun setAutoChar(buttonData: ButtonData, nextChar: Char): Char {
                buttonData.autoChar = nextChar
                return nextChar
            }
        },
        AUTO_CHAR {
            override fun getLabel(buttonData: ButtonData): Char {
                return buttonData.autoChar
            }

            override fun setAutoChar(buttonData: ButtonData, autoChar: Char): Char {
                var nextChar = autoChar
                buttonData.autoChar = nextChar++
                return nextChar
            }
        },
        BLANK {
            override fun getLabel(buttonData: ButtonData): Char {
                return ButtonData.BLANK
            }

            override fun setAutoChar(buttonData: ButtonData, autoChar: Char): Char {
                buttonData.autoChar = autoChar
                return autoChar
            }
        },
        CUSTOM {
            override fun getLabel(buttonData: ButtonData): Char {
                return buttonData.customChar
            }

            override fun setAutoChar(buttonData: ButtonData, autoChar: Char): Char {
                var nextChar = autoChar
                buttonData.autoChar = nextChar
                if ('A' <= buttonData.customChar && buttonData.customChar <= 'Z') {
                    nextChar = buttonData.customChar
                    nextChar++
                }
                return nextChar
            }
        };

        abstract fun getLabel(buttonData: ButtonData): Char
        abstract fun setAutoChar(buttonData: ButtonData, nextChar: Char): Char
    }

    /**
     * Every button (but the last) has a reference to the next button in the calculator.
     * This reference is used to determine the alphabetic progression of the 'AutoChars'.
     */
    var nextButton: CalculateButton? = null
        private set

    /**
     * The buttons own state is stored in a 'state' object to facilitate easy saving and restoring
     */
    private var buttonData: ButtonData? = null

    /**
     * Data used to capture the state of this particular button such that it can be restored again later
     */
    class ButtonData : Serializable, JSONAble {
        var type = ValueType.INPUT_VAL

        /** Value obtained from CoordinateInputDialog  */
        var inputVal = 0.toChar()

        /** Character obtained by automatically 'counting up' variable names  */
        var autoChar = EMPTY_CHAR

        /** User defined character  */
        var customChar = 0.toChar()

        constructor() {}
        constructor(json: JSONObject) {
            type = ValueType.values()[json.optInt("type", 0)]
            inputVal = json.optInt("inputVal", CalcState.ERROR_CHAR.toInt()).toChar()
            autoChar = json.optInt("autoChar", CalcState.ERROR_CHAR.toInt()).toChar()
            customChar = json.optInt("customChar", CalcState.ERROR_CHAR.toInt()).toChar()
        }

        @Throws(JSONException::class)
        override fun toJSON(): JSONObject {
            val returnValue = JSONObject()
            returnValue.put("type", type.ordinal)
            returnValue.put("inputVal", inputVal.toInt())
            returnValue.put("autoChar", autoChar.toInt())
            returnValue.put("customChar", customChar.toInt())
            return returnValue
        }

        companion object {
            private const val serialVersionUID = -9043775643928797403L

            /** Character used to 'hide' button  */
            const val BLANK = ' '
        }
    }

    class ButtonDataFactory : JSONAbleFactory<ButtonData?> {
        override fun fromJSON(json: JSONObject): ButtonData {
            return ButtonData(json)
        }
    }

    private inner class CoordDigitClickListener : OnClickListener {
        override fun onClick(view: View) {
            handleClick()
        }
    }

    constructor(context: Context?) : super(context) {
        setup()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setup()
    }

    override fun handleClick() {
        toggleType()
        super.handleClick()
    }

    private fun setup() {
        data = ButtonData()
        butt.setOnClickListener(CoordDigitClickListener())
    }

    /**
     * The 'Label' is the 'name' that is to be displayed on the button
     */
    override fun getLabel(): Char {
        return if (data == null) ' ' else type.getLabel(data!!)
    }

    private fun setLabel(label: Char) {
        butt.text = label.toString()
    }

    private var type: ValueType
        private get() = buttonData!!.type
        private set(type) {
            buttonData!!.type = type
            setBackgroundColour()
        }

    /**
     * Buttons displaying a custom value are given a 'steel-like' appearance so as to distinguish them from regular buttons
     */
    private fun setBackgroundColour() {
        when (buttonData!!.type) {
            ValueType.CUSTOM, ValueType.BLANK -> {
                butt.setBackgroundColor(ContextCompat.getColor(context, R.color.button_default))
                butt.setTextColor(ContextCompat.getColor(context, R.color.steel))
                butt.setTypeface(null, Typeface.BOLD)
            }
            else -> {
                val lightSkin = Settings.isLightSkin()
                val normalText = ContextCompat.getColor(context, if (lightSkin) R.color.text_light else R.color.text_dark)
                butt.setBackgroundResource(R.drawable.button_background_kitkat)
                butt.setTextColor(normalText)
                butt.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    /**
     * Data used to preserve this buttons state between sessions
     */
    var data: ButtonData?
        get() = buttonData
        set(buttonData) {
            this.buttonData = buttonData
            setBackgroundColour()
            updateButtonText()
        }

    fun setInputVal(inputVal: String) {
        setInputVal(inputVal[0])
    }

    private fun setInputVal(inputVal: Char) {
        buttonData!!.inputVal = inputVal
        updateButtonText()
    }

    fun setNextButton(nextButton: CalculateButton?): CalculateButton? {
        this.nextButton = nextButton
        return nextButton // This is done so we can chain 'next' assignments.
    }

    /**
     * This method is used to assign buttons alphabetically increasing names
     *
     * @param currentChar values indicated that the autoChar will be and is updated appropriately for the next button based on the current button's state
     */
    private fun setAutoChar(currentChar: Char) {
        var nextChar = currentChar
        if (nextChar < 'A') {
            nextChar = 'A'
        } else if (nextChar > 'Z') {
            nextChar = 'Z'
        }
        nextChar = if (data == null) ' ' else type.setAutoChar(data!!, nextChar)
        updateButtonText()

        // Propagate auto-char
        if (nextButton != null) {
            nextButton!!.setAutoChar(nextChar)
        }
    }

    fun setCustomChar(customChar: Editable) {
        setCustomChar(customChar[0])
    }

    /**
     * Sets a custom character for this button and updates the 'AutoChar' of subsequent buttons appropriately
     *
     * @param customChar name to be assigned to this button
     */
    override fun setCustomChar(customChar: Char) {
        buttonData!!.customChar = customChar
        type = ValueType.CUSTOM
        updateButtonText()
        val nextChar: Char
        nextChar = if ('A' <= buttonData!!.customChar && buttonData!!.customChar <= 'Z') {
            (customChar.toInt() + 1).toChar()
        } else {
            buttonData!!.autoChar
        }
        if (nextButton != null) {
            nextButton!!.setAutoChar(nextChar)
        }
    }

    /**
     * Restore this button to its original state (as in then the calculator was first created)
     *
     * This method is called whenever the coordinate-format cache is manually changed.
     * This is prevent the values of unseen buttons (such as say 'Seconds' buttons) from confusing the user when they change formats.
     * It is also a nice way to clear the calculator and restart afresh.
     */
    fun resetButton() {
        buttonData!!.autoChar = EMPTY_CHAR
        type = ValueType.INPUT_VAL
        updateButtonText()
        if (nextButton != null) {
            nextButton!!.resetButton()
        }
    }

    /**
     * When the user clicks on a button it's type is switched between 'AutoChar' and 'InputVal'
     *
     * Note: 'CustomValues' buttons will also be assigned to 'InputVal' as well.
     */
    private fun toggleType() {
        type = when (type) {
            ValueType.INPUT_VAL -> ValueType.AUTO_CHAR
            ValueType.AUTO_CHAR -> ValueType.BLANK
            else -> ValueType.INPUT_VAL
        }
        setAutoChar(buttonData!!.autoChar)
    }

    private fun updateButtonText() {
        setLabel(label)
    }

    companion object {
        /** Flag values used to designate that no AutoChar has been set  */
        private const val EMPTY_CHAR = '-'
    }
}
