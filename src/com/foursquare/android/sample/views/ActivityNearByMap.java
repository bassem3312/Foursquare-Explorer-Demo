package com.foursquare.android.sample.views;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.foursquare.android.nativeoauth.FoursquareCancelException;
import com.foursquare.android.nativeoauth.FoursquareDenyException;
import com.foursquare.android.nativeoauth.FoursquareInvalidRequestException;
import com.foursquare.android.nativeoauth.FoursquareOAuth;
import com.foursquare.android.nativeoauth.FoursquareOAuthException;
import com.foursquare.android.nativeoauth.FoursquareUnsupportedVersionException;
import com.foursquare.android.nativeoauth.model.AccessTokenResponse;
import com.foursquare.android.nativeoauth.model.AuthCodeResponse;
import com.foursquare.android.sample.R;
import com.foursquare.android.sample.R.layout;
import com.foursquare.android.sample.beans.ExampleTokenStore;
import com.foursquare.android.sample.beans.FsqVenue;
import com.foursquare.android.sample.helpers.CustomWindowAdapter;
import com.foursquare.android.sample.helpers.GPSTracker;
import com.foursquare.android.sample.helpers.RetainMapFragment;
import com.foursquare.android.sample.helpers.StaticMethods;
import com.foursquare.android.sample.parsers.CheckInVenueParser;
import com.foursquare.android.sample.parsers.NearbyParser;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class ActivityNearByMap extends Activity {

	private static final int REQUEST_CODE_FSQ_CONNECT = 200;
	private static final int REQUEST_CODE_FSQ_TOKEN_EXCHANGE = 201;
	private static final String TOKEN_SHARED_KEY = "token_key";
	private static final String LAT_SHARED_KEY = "lat_key";
	private static final String LONG_SHARED_KEY = "long_key";
	/**
	 * Obtain your client id and secret from:
	 * https://foursquare.com/developers/apps
	 */
	private static final String CLIENT_ID = "RAP3KEBDRZOFPTPMB0TM0ZW042ILRE2P3EGTXASM1JB4BGX0";
	private static final String CLIENT_SECRET = "K0OENS1UKPPEL1GQ3HQUERRFV1TELOI23H2VBKT1DCLMD2KX";
	public static final String API_URL = "https://api.foursquare.com/v2";

	private GoogleMap googleMap;
	private ArrayList<FsqVenue> allNearbyVenues;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			String savedString = savedInstanceState.getString("MyString") != null ? savedInstanceState.getString("MyString") : "";
			Log.e("======", savedString);
		}
		setContentView(layout.activity_nearby_map);
		googleMap = ((RetainMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
		if (!getSavedActivityStatus()[0].equals("-1")) {
			ExampleTokenStore.get().setToken(getSavedActivityStatus()[0]);
			displayNearByVenues(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_activity_nearby_map, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_refresh:
			if (StaticMethods.HaveNetworkConnection(ActivityNearByMap.this)) {
				loadNearByVenues(false);
			} else {
				StaticMethods.showDialogAlert(ActivityNearByMap.this, "Error", getString(R.string.open_mobile_network_error_message), getString(R.string.ok_button), false);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_FSQ_CONNECT:
			Log.e("=======", "REQUEST_CODE_FSQ_TOKEN_EXCHANGE");
			onCompleteConnect(resultCode, data);
			break;

		case REQUEST_CODE_FSQ_TOKEN_EXCHANGE:
			Log.e("=======", "REQUEST_CODE_FSQ_TOKEN_EXCHANGE");
			onCompleteTokenExchange(resultCode, data);
			break;

		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void onCompleteConnect(int resultCode, Intent data) {

		AuthCodeResponse codeResponse = FoursquareOAuth.getAuthCodeFromResult(resultCode, data);
		Exception exception = codeResponse.getException();

		if (exception == null) {
			// Success.
			String code = codeResponse.getCode();
			performTokenExchange(code);

		} else {
			if (exception instanceof FoursquareCancelException) {
				// Cancel.
				toastMessage(this, "Canceled");

			} else if (exception instanceof FoursquareDenyException) {
				// Deny.
				toastMessage(this, "Denied");

			} else if (exception instanceof FoursquareOAuthException) {
				// OAuth error.
				String errorMessage = exception.getMessage();
				String errorCode = ((FoursquareOAuthException) exception).getErrorCode();
				toastMessage(this, errorMessage + " [" + errorCode + "]");

			} else if (exception instanceof FoursquareUnsupportedVersionException) {
				// Unsupported Fourquare app version on the device.
				toastError(this, exception);

			} else if (exception instanceof FoursquareInvalidRequestException) {
				// Invalid request.
				toastError(this, exception);

			} else {
				// Error.
				toastError(this, exception);
			}
		}
	}

	private void onCompleteTokenExchange(int resultCode, Intent data) {
		AccessTokenResponse tokenResponse = FoursquareOAuth.getTokenFromResult(resultCode, data);
		Exception exception = tokenResponse.getException();

		if (exception == null) {
			String accessToken = tokenResponse.getAccessToken();
			// Success.
			toastMessage(this, "Access token: " + accessToken);
			Log.e("=======Access Token", accessToken);
			// Persist the token for later use. In this example, we save
			// it to shared prefs.
			ExampleTokenStore.get().setToken(accessToken);

			// Refresh UI.
			loadNearByVenues(false);

		} else {
			if (exception instanceof FoursquareOAuthException) {
				// OAuth error.
				String errorMessage = ((FoursquareOAuthException) exception).getMessage();
				String errorCode = ((FoursquareOAuthException) exception).getErrorCode();
				toastMessage(this, errorMessage + " [" + errorCode + "]");

			} else {
				// Other exception type.
				toastError(this, exception);
			}
		}
	}

	/**
	 * Exchange a code for an OAuth Token. Note that we do not recommend you do
	 * this in your app, rather do the exchange on your server. Added here for
	 * demo purposes.
	 * 
	 * @param code
	 *            The auth code returned from the native auth flow.
	 */
	private void performTokenExchange(String code) {
		Intent intent = FoursquareOAuth.getTokenExchangeIntent(this, CLIENT_ID, CLIENT_SECRET, code);
		startActivityForResult(intent, REQUEST_CODE_FSQ_TOKEN_EXCHANGE);
	}

	public static void toastMessage(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	public static void toastError(Context context, Throwable t) {
		Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
	}

	// ///////////

	private void loadNearByVenues(boolean isSaved) {
		boolean isAuthorized = !TextUtils.isEmpty(ExampleTokenStore.get().getToken());
		if (!isAuthorized) {

			Intent intent = FoursquareOAuth.getConnectIntent(ActivityNearByMap.this, CLIENT_ID);

			// If the device does not have the Foursquare app installed,
			// we'd
			// get an intent back that would open the Play Store for
			// download.
			// Otherwise we start the auth flow.
			if (FoursquareOAuth.isPlayStoreIntent(intent)) {
				// StaticMethods.showDialogAlert(ActivityNearByMap.this,
				// getString(R.string.app_name),getString(R.string.app_not_installed_message),
				// "OK",false);
				toastMessage(ActivityNearByMap.this, getString(R.string.app_not_installed_message));
				startActivity(intent);
			} else {
				startActivityForResult(intent, REQUEST_CODE_FSQ_CONNECT);
			}
		} else {
			displayNearByVenues(isSaved);
		}
	}

	private void displayNearByVenues(boolean isSaved) {
		try {
			googleMap.clear();
			GPSTracker gps = new GPSTracker(ActivityNearByMap.this);
			String latitude = ""+gps.getLatitude();
			String longitude = ""+gps.getLongitude();
			Log.e("lat", "" + latitude);
			Log.e("long", "" + longitude);
			String token;
			if(isSaved){
				token=getSavedActivityStatus()[0];
							latitude=getSavedActivityStatus()[1];
			}else{
				token=ExampleTokenStore.get().getToken();
			}
			GetNearByVenuesAsyncTask getNearByVenuesAsyncTask = new GetNearByVenuesAsyncTask();
			// if (!isSaved) {
			getNearByVenuesAsyncTask.execute(token,latitude, longitude);
			// } else {
			//
			// getNearByVenuesAsyncTask.execute(Double.parseDouble(getSavedActivityStatus()[1]),
			// Double.parseDouble(getSavedActivityStatus()[2]));
			// }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class GetNearByVenuesAsyncTask extends AsyncTask<String, Integer, ArrayList<FsqVenue>> {
		String latitude, longitude;
		ProgressDialog loadingProgressDialog;

		@Override
		protected void onPreExecute() {
			loadingProgressDialog = StaticMethods.launchLoadingDialog(ActivityNearByMap.this, getString(R.string.loading_venues_message));
			super.onPreExecute();
		}

		@Override
		protected ArrayList<FsqVenue> doInBackground(String... params) {
			try {
				String token=params[0];
				latitude = params[1];
				longitude = params[2];
				
				ArrayList<FsqVenue> allVenue = new NearbyParser().getNearby( ExampleTokenStore.get().getToken(),latitude, longitude);
				return allVenue;
			} catch (Exception e) {
				Log.e("Exception", e.toString());
			}
			return null;
		}

		@Override
		protected void onPostExecute(ArrayList<FsqVenue> allVenueS) {
			loadingProgressDialog.dismiss();
			if (allVenueS != null) {
				Log.e("All Venue Size", allVenueS.size() + "");
				addNearByVenuesToMaps(Double.parseDouble(latitude), Double.parseDouble(longitude), allVenueS);
				saveActivityStateInSharedPreference(ExampleTokenStore.get().getToken(), latitude, longitude);
			} else {
				toastMessage(ActivityNearByMap.this, getString(R.string.connection_error_message));
			}
			super.onPostExecute(allVenueS);
		}

	}

	private void addCurrentLocationMarker(double latitude, double longitude) {
		MarkerOptions centerMarker = new MarkerOptions().position(new LatLng(latitude, longitude)).title("Current Location").snippet("-1");
		centerMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
		googleMap.addMarker(centerMarker);

		// adding marker
		CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(latitude, longitude)).zoom(12).build();

		googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//		googleMap.setMyLocationEnabled(true);
		googleMap.getUiSettings().setRotateGesturesEnabled(false);
		googleMap.setInfoWindowAdapter(new CustomWindowAdapter(getLayoutInflater(), allNearbyVenues));
		googleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

			@Override
			public void onInfoWindowClick(Marker marker) {
				if (!marker.getSnippet().equals("-1")) {
					marker.hideInfoWindow();
					showAlertDialogForPoint(marker.getPosition(), Integer.parseInt(marker.getSnippet()));
				}
			}
		});

	}

	private void addNearByVenuesToMaps(double latitude, double longitude, ArrayList<FsqVenue> allNearbyVenues) {
		this.allNearbyVenues = allNearbyVenues;
		addCurrentLocationMarker(latitude, longitude);
		for (int i = 0; i < allNearbyVenues.size(); i++) {
			FsqVenue currentVenue = allNearbyVenues.get(i);
			MarkerOptions centerMarker = new MarkerOptions().position(new LatLng(currentVenue.location.getLatitude(), currentVenue.location.getLongitude())).title(currentVenue.name).icon(BitmapDescriptorFactory.fromBitmap(currentVenue.bmp));
			centerMarker.snippet("" + i);
			googleMap.addMarker(centerMarker);

		}
	}

	// Display the alert that adds the marker
	private void showAlertDialogForPoint(final LatLng point, final int possotion) {
		// inflate message_item.xml view
		View messageView = LayoutInflater.from(ActivityNearByMap.this).inflate(R.layout.message_item, null);
		// Create alert dialog builder
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		// set message_item.xml to AlertDialog builder
		alertDialogBuilder.setView(messageView);

		// Create alert dialog
		final AlertDialog alertDialog = alertDialogBuilder.create();

		// Configure dialog button (OK)
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FsqVenue selectedVenue = allNearbyVenues.get(possotion);
				new CheckInAsyncTask().execute(selectedVenue);

			}
		});

		// Configure dialog button (Cancel)
		alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		// Display the dialog
		alertDialog.show();
	}

	class CheckInAsyncTask extends AsyncTask<FsqVenue, Void, String> {
		ProgressDialog loadingProgressDialog;

		@Override
		protected void onPreExecute() {
			loadingProgressDialog = StaticMethods.launchLoadingDialog(ActivityNearByMap.this, getString(R.string.check_in_venue_message));
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(FsqVenue... params) {
			FsqVenue selectedVenue = params[0];
			return new CheckInVenueParser().addCheckIN(selectedVenue.location.getLatitude(), selectedVenue.location.getLongitude(), selectedVenue.id, ExampleTokenStore.get().getToken());

		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			loadingProgressDialog.dismiss();
			super.onPostExecute(result);
			toastMessage(ActivityNearByMap.this, result);
		}

	}

	private void saveActivityStateInSharedPreference(String token, String lat, String lon) {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.clear();
		editor.putString(TOKEN_SHARED_KEY, token);
		editor.putString(LAT_SHARED_KEY, "" + lat);
		editor.putString(LONG_SHARED_KEY, "" + lon);
		editor.commit();
	}

	private String[] getSavedActivityStatus() {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		String results[] = new String[3];
		results[0] = preferences.getString(TOKEN_SHARED_KEY, "-1");
		results[1] = preferences.getString(LAT_SHARED_KEY, "-1");
		results[2] = preferences.getString(LONG_SHARED_KEY, "-1");
		return results;
	}

	@Override
	public void finish() {
		this.moveTaskToBack(true);

//		super.finish();
	}

}
