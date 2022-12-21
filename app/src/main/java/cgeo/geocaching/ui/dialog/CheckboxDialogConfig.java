package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.utils.functions.Func1;

import androidx.annotation.StringRes;

public class CheckboxDialogConfig {
    private final @StringRes int textRes;

    private boolean isVisible = true;
    private boolean isCheckedOnInit = false;
    private CheckCondition positiveCondition = CheckCondition.NONE;
    private CheckCondition negativeCondition = CheckCondition.NONE;
    private ActionButtonLabel actionButtonLabel = ActionButtonLabel.OK_CANCEL;

    public enum CheckCondition {
        CHECKED(isChecked -> isChecked),
        UNCHECKED(isChecked -> !isChecked),
        NONE(isChecked -> true);

        private final Func1<Boolean, Boolean> condition;

        CheckCondition(final Func1<Boolean, Boolean> condition) {
            this.condition = condition;
        }

        public boolean eval(final boolean isChecked) {
            return condition.call(isChecked);
        }
    }

    public enum ActionButtonLabel {
        OK_CANCEL,
        YES_NO
    }

    private CheckboxDialogConfig(final @StringRes int textRes) {
        this.textRes = textRes;
    }

    public static CheckboxDialogConfig newCheckbox(final @StringRes int textRes) {
        return new CheckboxDialogConfig(textRes);
    }

    public CheckboxDialogConfig setVisible(final boolean visible) {
        this.isVisible = visible;
        return this;
    }

    public CheckboxDialogConfig setCheckedOnInit(final boolean checked) {
        this.isCheckedOnInit = checked;
        return this;
    }

    public CheckboxDialogConfig setPositiveButtonCheckCondition(final CheckCondition checkCondition) {
        this.positiveCondition = checkCondition;
        return this;
    }

    public CheckboxDialogConfig setNegativeButtonCheckCondition(final CheckCondition checkCondition) {
        this.negativeCondition = checkCondition;
        return this;
    }

    public CheckboxDialogConfig setActionButtonLabel(final ActionButtonLabel labelType) {
        this.actionButtonLabel = labelType;
        return this;
    }

    public int getTextRes() {
        return textRes;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public boolean isCheckedOnInit() {
        return isCheckedOnInit;
    }

    public CheckCondition getPositiveCheckCondition() {
        return positiveCondition;
    }

    public CheckCondition getNegativeCheckCondition() {
        return negativeCondition;
    }

    public ActionButtonLabel getActionButtonLabel() {
        return actionButtonLabel;
    }
}
