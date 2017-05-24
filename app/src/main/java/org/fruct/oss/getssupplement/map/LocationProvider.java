package org.fruct.oss.getssupplement.map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;

import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

/**
 * Класс для отслеживания местоположения
 */

public class LocationProvider implements IMyLocationProvider {

    public static final String ARG_LOCATION = "org.fruct.oss.getssupplement.location.ARG_LOCATION";
    public static final String BC_LOCATION = "org.fruct.oss.getssupplement.location.BC_LOCATION";

    private final Context context;

    private IMyLocationConsumer consumer;

    private BroadcastReceiver receiver;

    // текущее / последнее местоположение
    private Location location;

    public LocationProvider(Context context) {
        this.context = context;
    }

    @Override
    public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
        this.consumer = myLocationConsumer;

        LocalBroadcastManager.getInstance(context).registerReceiver(receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                location = intent.getParcelableExtra(ARG_LOCATION);
                consumer.onLocationChanged(location, LocationProvider.this);
            }
        }, new IntentFilter(BC_LOCATION));

        return true;
    }

    @Override
    public void stopLocationProvider() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
        receiver = null;
    }

    @Override
    public Location getLastKnownLocation() {
        return location;
    }

    @Override
    public void destroy() {
        stopLocationProvider();
    }
}
