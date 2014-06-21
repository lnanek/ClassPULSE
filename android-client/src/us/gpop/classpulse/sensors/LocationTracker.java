package us.gpop.classpulse.sensors;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Tracks the most accurate location yet found.
 */
public class LocationTracker {
	public static final String TAG = LocationTracker.class.getSimpleName();
	final LocationManager locationManager;
	private Float locationAccuracy;
	private Double longitude;
	private Double latitude;
	private boolean listening;

	private LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			if (null == location) {
				return;
			}
			// Take any location if we have none, otherwise take most accurate
			// vs. past
			if (null == locationAccuracy || locationAccuracy > location.getAccuracy()) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				locationAccuracy = location.getAccuracy();
			}
			
			// Just keep first one for now.
			stopListeningForLocations();
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider,
				int status, Bundle extras) {
		}
	};
	
	public LocationTracker(final Context context) {
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	public void startAccquiringLocationData() {
		Log.d(TAG, "accquireLocationData");
		
		listening = true;

		final List<String> providers = locationManager.getAllProviders();
		for (final String provider : providers) {
			final Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
			locationListener.onLocationChanged(lastKnownLocation);
		}
		for (final String provider : providers) {
			Log.d(TAG, "requestLocationUpdates: " + provider);
			locationManager.requestLocationUpdates(provider, 0, 0, locationListener);
		}
	}
	
	public void stopListeningForLocations() {
		Log.d(TAG, "stopListeningForLocations");		
		
		if ( listening ) {
			locationManager.removeUpdates(locationListener);
			listening = false;
		}		

	}

	public Double getLongitude() {
		return longitude;
	}

	public Double getLatitude() {
		return latitude;
	}
	
	public boolean hasLocation() {
		return null != getLatitude() && null != getLongitude();
	}
}
