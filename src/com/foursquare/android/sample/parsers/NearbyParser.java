package com.foursquare.android.sample.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.foursquare.android.sample.beans.FsqVenue;
import com.foursquare.android.sample.views.ActivityNearByMap;

public class NearbyParser {
	public ArrayList<FsqVenue> getNearby( String mAccessToken,String latitude, String longitude) throws Exception {
		ArrayList<FsqVenue> venueList = new ArrayList<FsqVenue>();

		try {
			String v = timeMilisToString(System.currentTimeMillis());
			String ll = String.valueOf(latitude) + "," + String.valueOf(longitude);
			URL url = new URL(ActivityNearByMap.API_URL + "/venues/explore?ll=" + ll + "&oauth_token=" + mAccessToken + "&v=" + v);

			Log.d("=======", "Opening URL " + url.toString());

			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);
			// urlConnection.setDoOutput(true);

			urlConnection.connect();

			String response = streamToString(urlConnection.getInputStream());
			Log.e("Result", response);
			JSONObject jsonObj = (JSONObject) new JSONTokener(response).nextValue();

			JSONArray groups = (JSONArray) jsonObj.getJSONObject("response").getJSONArray("groups");

			int length = groups.length();

			if (length > 0) {
				for (int i = 0; i < length; i++) {
					JSONObject group = (JSONObject) groups.get(i);
					JSONArray items = (JSONArray) group.getJSONArray("items");

					int ilength = items.length();

					for (int j = 0; j < ilength; j++) {
						JSONObject item = (JSONObject) items.get(j);

						FsqVenue venue = new FsqVenue();
						JSONObject venueJsonObj = item.getJSONObject("venue");
						venue.id = venueJsonObj.getString("id");
						venue.name = venueJsonObj.getString("name");

						JSONObject location = (JSONObject) venueJsonObj.getJSONObject("location");

						Location loc = new Location(LocationManager.GPS_PROVIDER);

						loc.setLatitude(Double.valueOf(location.getString("lat")));
						loc.setLongitude(Double.valueOf(location.getString("lng")));
					
						venue.location = loc;
						try {
							venue.address = location.getString("address");
						} catch (Exception ex) {
						}
						venue.distance = location.getInt("distance");
						venue.herenow = venueJsonObj.getJSONObject("hereNow").getInt("count");
						try {
							venue.type = item.getString("type");
						} catch (Exception ex) {
						}
						JSONObject icon = ((JSONObject) venueJsonObj.getJSONArray("categories").get(0)).getJSONObject("icon");
						venue.iconURL = icon.getString("prefix") + "bg_100" + icon.getString("suffix");
						try {
					        url = new URL(venue.iconURL);
					        venue.bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
					    } catch (Exception e) {
					        e.printStackTrace();
					    }
						Log.e("IconURL", venue.iconURL);
						venueList.add(venue);
					}
				}
			}
		} catch (Exception ex) {
			throw ex;
		}

		return venueList;
	}

	private String streamToString(InputStream is) throws IOException {
		String str = "";

		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));

				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}

				reader.close();
			} finally {
				is.close();
			}

			str = sb.toString();
		}

		return str;
	}

	private String timeMilisToString(long milis) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd");
		Calendar calendar = Calendar.getInstance();

		calendar.setTimeInMillis(milis);

		return sd.format(calendar.getTime());
	}

}
