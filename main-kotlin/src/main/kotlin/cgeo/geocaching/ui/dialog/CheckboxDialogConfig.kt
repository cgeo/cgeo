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

package cgeo.geocaching.ui.dialog

import cgeo.geocaching.utils.functions.Func1

import androidx.annotation.StringRes

class CheckboxDialogConfig {
    private final @StringRes Int textRes

    private var isVisible: Boolean = true
    private var isCheckedOnInit: Boolean = false
    private var positiveCondition: CheckCondition = CheckCondition.NONE
    private var negativeCondition: CheckCondition = CheckCondition.NONE
    private var actionButtonLabel: ActionButtonLabel = ActionButtonLabel.OK_CANCEL

    enum class class CheckCondition {
        CHECKED(isChecked -> isChecked),
        UNCHECKED(isChecked -> !isChecked),
        NONE(isChecked -> true)

        private final Func1<Boolean, Boolean> condition

        CheckCondition(final Func1<Boolean, Boolean> condition) {
            this.condition = condition
        }

        public Boolean eval(final Boolean isChecked) {
            return condition.call(isChecked)
        }
    }

    enum class class ActionButtonLabel {
        OK_CANCEL,
        YES_NO
    }

    private CheckboxDialogConfig(final @StringRes Int textRes) {
        this.textRes = textRes
    }

    public static CheckboxDialogConfig newCheckbox(final @StringRes Int textRes) {
        return CheckboxDialogConfig(textRes)
    }

    public CheckboxDialogConfig setVisible(final Boolean visible) {
        this.isVisible = visible
        return this
    }

    public CheckboxDialogConfig setCheckedOnInit(final Boolean checked) {
        this.isCheckedOnInit = checked
        return this
    }

    public CheckboxDialogConfig setPositiveButtonCheckCondition(final CheckCondition checkCondition) {
        this.positiveCondition = checkCondition
        return this
    }

    public CheckboxDialogConfig setNegativeButtonCheckCondition(final CheckCondition checkCondition) {
        this.negativeCondition = checkCondition
        return this
    }

    public CheckboxDialogConfig setActionButtonLabel(final ActionButtonLabel labelType) {
        this.actionButtonLabel = labelType
        return this
    }

    public Int getTextRes() {
        return textRes
    }

    public Boolean isVisible() {
        return isVisible
    }

    public Boolean isCheckedOnInit() {
        return isCheckedOnInit
    }

    public CheckCondition getPositiveCheckCondition() {
        return positiveCondition
    }

    public CheckCondition getNegativeCheckCondition() {
        return negativeCondition
    }

    public ActionButtonLabel getActionButtonLabel() {
        return actionButtonLabel
    }
}
