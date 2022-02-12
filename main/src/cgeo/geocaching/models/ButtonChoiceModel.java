package cgeo.geocaching.models;

import cgeo.geocaching.utils.functions.Action1;

import android.view.View;

import java.util.ArrayList;
import java.util.Objects;

import com.google.android.material.button.MaterialButtonToggleGroup;

public class ButtonChoiceModel<T> {
    public final int resButton;
    public final T assignedValue;
    public final String info;
    public View button = null;

    public ButtonChoiceModel(final int resButton, final T assignedValue, final String info) {
        this.resButton = resButton;
        this.assignedValue = assignedValue;
        this.info = info;
    }

    public static class ToggleButtonWrapper<T> {
        private final MaterialButtonToggleGroup toggleGroup;
        private final ArrayList<ButtonChoiceModel<T>> list;
        private final Action1<T> setValue;
        private final T originalValue;

        public ToggleButtonWrapper(final T originalValue, final Action1<T> setValue, final MaterialButtonToggleGroup toggleGroup) {
            this.originalValue = originalValue;
            this.toggleGroup = toggleGroup;
            this.setValue = setValue;
            this.list = new ArrayList<>();
        }

        public void add(final ButtonChoiceModel<T> item) {
            list.add(item);
        }

        public ButtonChoiceModel<T> getByResId(final int id) {
            for (ButtonChoiceModel<T> item : list) {
                if (item.resButton == id) {
                    return item;
                }
            }
            return null;
        }

        public ButtonChoiceModel<T> getByAssignedValue(final T value) {
            for (ButtonChoiceModel<T> item : list) {
                if (Objects.equals(item.assignedValue, value)) {
                    return item;
                }
            }
            return null;
        }

        public void init() {
            for (final ButtonChoiceModel<T> button : list) {
                button.button = toggleGroup.findViewById(button.resButton);
            }
            toggleGroup.check(getByAssignedValue(originalValue).resButton);
        }

        public void setValue() {
            final T currentValue = getByResId(toggleGroup.getCheckedButtonId()).assignedValue;
            if (setValue != null && !originalValue.equals(currentValue)) {
                this.setValue.call(currentValue);
            }
        }

        public ArrayList<ButtonChoiceModel<T>> getList() {
            return list;
        }
    }
}
