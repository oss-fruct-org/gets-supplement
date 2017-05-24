package org.fruct.oss.getssupplement.map;

import android.view.MotionEvent;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

/**
 * Created by kulakov on 23.05.17.
 */

public class ScrollableOverlay extends MyLocationNewOverlay {

    public ScrollableOverlay(IMyLocationProvider myLocationProvider, MapView mapView) {
        super(myLocationProvider, mapView);
    }

    @Override
    public boolean onScroll(MotionEvent pEvent1, MotionEvent pEvent2, float pDistanceX, float pDistanceY, MapView pMapView) {
        //deactivateFollowMode();
        return super.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY, pMapView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event, MapView mapView) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return false;
        } else {
            return super.onTouchEvent(event, mapView);
        }
    }
}