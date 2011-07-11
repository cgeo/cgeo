package cgeo.geocaching;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;
import android.view.Gravity;

public class cgWarning {
	public Context context = null;

	public cgWarning(Context contextIn) {
		context = contextIn;
	}

	public void showToast(String text) {
		if (text.length() > 0) {
			Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);

			toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM, 0, 100);
			toast.show();
		}
	}

	public void showShortToast(String text) {
		if (text.length() > 0) {
			Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);

			toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM, 0, 100);
			toast.show();
		}
	}

	public void helpDialog(String title, String message) {
		if (message == null || message.length() == 0) {
			return;
		}

		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setCancelable(true);
		dialog.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
           }
       });
	   
	   AlertDialog alert = dialog.create();
	   alert.show();
	}
}
