package com.foursquare.android.sample.parsers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class CheckInVenueParser {
	public String addCheckIN(double lat, double lon, String venueID, String accessToKen) {
		try {
			// Construct data
			String v = timeMilisToString(System.currentTimeMillis());

			String data = URLEncoder.encode("ll", "UTF-8") + "=" + URLEncoder.encode(lat + "," + lon, "UTF-8");
			data += "&" + URLEncoder.encode("venueId", "UTF-8") + "=" + URLEncoder.encode(venueID, "UTF-8");
			data += "&" + URLEncoder.encode("oauth_token", "UTF-8") + "=" + URLEncoder.encode(accessToKen, "UTF-8") + "&v=" + v;
			Log.e("Check In Parm", data);
			// Send data
			URL url = new URL("https://api.foursquare.com/v2/checkins/add?" + data);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			// wr.write(data);
			wr.flush();

			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String line;
			String response = null;
			while ((line = rd.readLine()) != null) {
				response = line;
			}
			wr.close();
			rd.close();
			return parse(response);

		} catch (Exception e) {
			Log.e("Exception ", e.toString());
			return e.toString();
		}
	}

	private String parse(String response) throws Exception {
		Log.e("Result", response);
		JSONObject jsonObj = (JSONObject) new JSONTokener(response).nextValue();

		String resultCode = jsonObj.getJSONObject("meta").getString("code");
		if (resultCode.equals("200")) {
			return "Check in proccess sucssefully";
		} else {
			try {
				return jsonObj.getJSONObject("meta").getString("errorDetail");
			} catch (Exception ex) {
				return "Sorry, there is an error in checkin";
			}
		}

	}

	private String timeMilisToString(long milis) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd");
		Calendar calendar = Calendar.getInstance();

		calendar.setTimeInMillis(milis);

		return sd.format(calendar.getTime());
	}

}
