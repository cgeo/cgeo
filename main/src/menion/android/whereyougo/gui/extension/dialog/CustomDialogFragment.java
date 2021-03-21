package menion.android.whereyougo.gui.extension.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

public abstract class CustomDialogFragment extends DialogFragment {

    public CustomDialogFragment() {
        super();
    }

    public abstract Dialog createDialog(Bundle savedInstanceState);

    /**
     * My own implementation of visibility check
     */
    public boolean isDialogVisible() {
        return isAdded() && !isHidden();
    }

    /**
     * This is called when the Fragment's Activity is ready to go, after its content view has been
     * installed; it is called both after the initial fragment creation and after the fragment is
     * re-attached to a new activity.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        try {
            super.onActivityCreated(savedInstanceState);
        } catch (Exception e) {
            dismissAllowingStateLoss();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // tell the framework to try to keep this fragment around
        // during a configuration change (true), or recreate (false)
        setRetainInstance(shouldRetainInstance());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = createDialog(savedInstanceState);
        if (dialog != null) {
            dialog.setCancelable(isCancelable());
        }
        return dialog;
    }

    /**
     * This is called when the fragment is going away. It is NOT called when the fragment is being
     * propagated between activity instances.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // hack on this issue http://code.google.com/p/android/issues/detail?id=17423
    // This is to work around what is apparently a bug. If you don't have it
    // here the dialog will be dismissed on rotation, so tell it not to dismiss.
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    /**
     * This is called right before the fragment is detached from its current activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
    }

    public boolean shouldRetainInstance() {
        return true;
    }
}
