package com.foursquare.android.sample.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.foursquare.android.sample.R;

public class StaticMethods {
	public static ProgressDialog launchLoadingDialog(Context context, String loadingMessage) {
		final ProgressDialog ringProgressDialog = ProgressDialog.show(context, "Please wait ...", loadingMessage, true);
		ringProgressDialog.setCancelable(true);
		return ringProgressDialog;
	}

	public static void showDialogAlert(final Context currentContext, String alertTitle, String alertMessage, String buttonText, final boolean IsFinishActivity) {
		Builder builder = new AlertDialog.Builder(currentContext);
		builder.setTitle(alertTitle);
		builder.setMessage(alertMessage);
		builder.setIcon(R.drawable.ic_lanucher);
		builder.setCancelable(true);

		builder.setPositiveButton(buttonText, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				if (IsFinishActivity) {
					((Activity) currentContext).finish();
				}
			}
		});
		AlertDialog dialog = builder.create();
		dialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogAnimation;

		dialog.show();

	}

	public static boolean HaveNetworkConnection(Context context) {
		boolean HaveConnectedWifi = false;
		boolean HaveConnectedMobile = false;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					HaveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					HaveConnectedMobile = true;
		}
		return HaveConnectedWifi || HaveConnectedMobile;
	}

}
