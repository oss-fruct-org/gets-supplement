package org.fruct.oss.getssupplement.map;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class LocationReceiver implements LocationListener {
	public static final String TAG = "LocationReceiver";

	public static final String MOCK_PROVIDER = "org.fruct.oss.socialnavigator.MockProvider";

	private String vehicle = "WALK";
	private LocationManager locationManager;
	private Location oldLocation;
	private List<Listener> listeners = new ArrayList<Listener>();

	private boolean isDisableRealLocation = false;
	private boolean isStarted = false;

	private String lastReason = "";

	public LocationReceiver(Context context) {
		this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	public void addListener(Listener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		this.listeners.remove(listener);
	}

	public void mockLocation(Location location) {
		newLocation(location);
	}

	public void start() {
		isStarted = true;

		setupUpdates();
	}

	private void setupUpdates() {
		try {
			if (!isDisableRealLocation) {
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_FINE);

                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, getMillsFreq(vehicle), getMeterFreq(vehicle), this, Looper.getMainLooper());
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, getMillsFreq(vehicle), getMeterFreq(vehicle), this, Looper.getMainLooper());
                } catch (SecurityException ex) {
                    Log.w(TAG, "Security problem: " + ex.getMessage());
                }
			}
		} catch (Exception ex) {
			Log.e(TAG, "Can't setup location providers: " + ex.getMessage());
		}
		Log.d(TAG, "Update location started");
	}

	public void stop() {
		locationManager.removeUpdates(this);

		isStarted = false;
	}

	private int getMeterFreq(String v) {
		if ("car".equalsIgnoreCase(v)) {
			return 10;
		} else {
			return 2;
		}
	}

	private int getMillsFreq(String v) {
		if ("car".equalsIgnoreCase(v)) {
			return 1000;
		} else {
			return 1000;
		}
	}

	public Location getLastKnownLocation() {
		Location networkLocation = null;
		Location gpsLocation = null;
        try {
            networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException ex) {
            Log.w(TAG, "Can't locate: " + ex.getMessage());
        }

		Location locationToSend = null;

		if (gpsLocation != null && networkLocation != null) {
			if (isBetterLocation(networkLocation, gpsLocation))
				locationToSend = networkLocation;
			else
				locationToSend = gpsLocation;
		} else if (gpsLocation != null)
			locationToSend = gpsLocation;
		else if (networkLocation != null)
			locationToSend = networkLocation;

		return locationToSend;
	}

	public Location getOldLocation() {
		if (oldLocation == null)
			return getLastKnownLocation();
		else
			return oldLocation;
	}

	/**
	 * Retrieves last location from LocationManager and sends it to listener
	 */
	public void sendLastLocation() {
		if (listeners.size() == 0)
			return;

		Location locationToSend = getLastKnownLocation();

		if (locationToSend != null) {
			oldLocation = locationToSend;
			for(Listener ls: listeners){
				ls.newLocation(locationToSend);
			}
			Log.d(TAG, "LocationReceiver send last location");
		}
	}

	public boolean isStarted() {
		return isStarted;
	}

	public void disableRealLocation() {
		isDisableRealLocation = true;
	}

	private void newLocation(Location location) {
		String dbg = "LocationReceiver new location accuracy = " + location.getAccuracy()
				+ " provider = " + location.getProvider();

		if (isBetterLocation(location, oldLocation)) {
			oldLocation = location;

			if (listeners.size() > 0 && isStarted()) {
				for (Listener ls: listeners) {
					ls.newLocation(location);
				}
			}

			Log.i(TAG, dbg + " accepted. Reason = " + lastReason);
		} else {
			Log.i(TAG, dbg + " dropped. Reason = " + lastReason);
		}
	}

	private boolean isBetterLocation(Location location, Location oldLocation) {
		final long TIME_LIMIT = 2 * 60 * 1000;
		final float ACC_LIMIT = 200;

		if (oldLocation == null) {
			lastReason = "No old location";
			return true;
		}

		long timeDiff = location.getTime() - oldLocation.getTime();
		if (timeDiff > TIME_LIMIT) {
			lastReason = "Significantly newer";
			return true;
		} else if (timeDiff < -TIME_LIMIT) {
			lastReason = "Significantly older";
			return false;
		}

		int accDiff = (int) (location.getAccuracy() - oldLocation.getAccuracy());

		if (accDiff < 0) {
			lastReason = "Accuracy better";
			return true;
		} else if (timeDiff > 0 && accDiff <= 0) {
			lastReason = "Newer and not worse";
			return true;
		} else if (timeDiff > 0
				&& accDiff < ACC_LIMIT
				&& (location.getProvider() != null && location.getProvider()
				.equals(oldLocation.getProvider()))) {
			lastReason = "Same provider";
			return true;
		} else {
			lastReason = "Accuracy worse";
			return false;
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		newLocation(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void setVehicle(String vehicle) {
		if (this.vehicle.equals(vehicle))
			return;

		this.vehicle = vehicle;
		if (isStarted) {
			locationManager.removeUpdates(this);
			setupUpdates();
		}
	}

	public interface Listener {
		void newLocation(Location location);
	}
}