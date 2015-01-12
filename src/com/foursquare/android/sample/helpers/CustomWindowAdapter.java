package com.foursquare.android.sample.helpers;

import java.util.ArrayList;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.foursquare.android.sample.R;
import com.foursquare.android.sample.beans.FsqVenue;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

public class CustomWindowAdapter implements InfoWindowAdapter {
	LayoutInflater mInflater;
	private ArrayList<FsqVenue> allFSQVenues;

	public CustomWindowAdapter(LayoutInflater i, ArrayList<FsqVenue> allFsqVenues) {
		mInflater = i;
		this.allFSQVenues = allFsqVenues;
	}

	// This defines the contents within the info window based on the marker
	@Override
	public View getInfoContents(Marker marker) {
		// Getting view from the layout file
		View v = mInflater.inflate(R.layout.custom_info_window, null);
		// Populate fields
		TextView tvTitle = (TextView) v.findViewById(R.id.tv_info_window_title);
		tvTitle.setText(marker.getTitle());

		if (!marker.getSnippet().equals("-1")) {
			FsqVenue selectedVenue = allFSQVenues.get(Integer.parseInt(marker.getSnippet()));

			TextView tvAddress = (TextView) v.findViewById(R.id.tv_info_window_address);
			tvAddress.setVisibility(View.VISIBLE);
			tvAddress.setText(selectedVenue.address);
			
			TextView tvClickHere = (TextView) v.findViewById(R.id.tv_info_click_here);
			tvClickHere.setVisibility(View.VISIBLE);
		}
		// Return info window contents
		return v;
	}

	// This changes the frame of the info window; returning null uses the
	// default frame.
	@Override
	public View getInfoWindow(Marker marker) {
		return null;
	}
}