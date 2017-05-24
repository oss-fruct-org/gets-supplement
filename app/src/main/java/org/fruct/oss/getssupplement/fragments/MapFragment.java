package org.fruct.oss.getssupplement.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import org.fruct.oss.getssupplement.R;
import org.fruct.oss.getssupplement.map.LocationProvider;
import org.fruct.oss.getssupplement.map.LocationReceiver;
import org.fruct.oss.getssupplement.map.ScrollableOverlay;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;
import org.osmdroid.tileprovider.modules.TileWriter;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import static com.google.android.gms.internal.zzt.TAG;

/**
 * Отображение вкладки карты
 */

public class MapFragment extends Fragment implements LocationReceiver.Listener {

    private static final String STATE_FOLLOW = "is-following-state";
    private static final String STATE_LOCATION_LAT = "last-location-lat-state";
    private static final String STATE_LOCATION_LON = "last-location-lon-state";

    // объект карты
    private MapView mapView;

    private MyLocationNewOverlay locationOverlay;

    // флаг включения отслеживания
    private boolean isFollowingActive;

    // сканер текущего местоположения
    private LocationReceiver locationReceiver;

    private Location lastLocation = null;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    public static String getTitle(Context context) {
        return context.getString(R.string.title_map);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        lastLocation = new Location(LocationReceiver.MOCK_PROVIDER);
        lastLocation.setAccuracy(1);
        if (savedInstanceState != null) {
            isFollowingActive = savedInstanceState.getBoolean(STATE_FOLLOW);

            lastLocation.setLatitude(savedInstanceState.getDouble(STATE_LOCATION_LAT));
            lastLocation.setLongitude(savedInstanceState.getDouble(STATE_LOCATION_LON));
            Log.d(this.getClass().getSimpleName(), "found location: " + lastLocation.toString());
        } else {
            // грузим из настроек приложения
            lastLocation.setLatitude(pref.getFloat(STATE_LOCATION_LAT, 0.0f));
            lastLocation.setLongitude(pref.getFloat(STATE_LOCATION_LON, 0.0f));
            isFollowingActive = pref.getBoolean(STATE_FOLLOW, true);
            Log.d(getClass().getSimpleName(), "Preference load: " + isFollowingActive + " " + lastLocation);
        }

        locationReceiver = new LocationReceiver(getContext());
        locationReceiver.addListener(this);
        locationReceiver.start();

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_map, container, false);

        createMapView(view);

        createLocationOverlay();

        if (lastLocation != null) {
            newLocation(lastLocation);
            mapView.getController().setCenter(new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude()));
            Log.d(getClass().getSimpleName(), "Update location to " + lastLocation);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        locationReceiver.stop();
        locationReceiver.removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onStop() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        pref.edit().putBoolean(STATE_FOLLOW, isFollowingActive)
                .putFloat(STATE_LOCATION_LON, (float) mapView.getMapCenter().getLongitude())
                .putFloat(STATE_LOCATION_LAT, (float) mapView.getMapCenter().getLatitude()).apply();

        Log.d(this.getClass().getSimpleName(), "Preference stored: " + isFollowingActive + " " + mapView.getMapCenter());
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_FOLLOW, isFollowingActive);
        org.osmdroid.api.IGeoPoint pt = mapView.getMapCenter();
        outState.putDouble(STATE_LOCATION_LAT, pt.getLatitude());
        outState.putDouble(STATE_LOCATION_LON, pt.getLongitude());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.fragment_map_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_position) {
            if (isFollowingActive) {
                deactivateFollowingMode();
            } else {
                activateFollowingMode();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * создание слоя с картой
     */
    private void createMapView(View view) {
        ViewGroup layout = (ViewGroup) view.findViewById(R.id.map_view);

        IRegisterReceiver registerReceiver = new SimpleRegisterReceiver(getActivity().getApplicationContext());
//        ITileSource tileSource = new XYTileSource(
//                "OSMWithoutSidewalks", ResourceProxy.string.mapnik, 0, 17, 256, ".png",
//                new String[] { "http://etourism.cs.petrsu.ru:20209/osm_tiles/" });
        ITileSource tileSource = new OnlineTileSourceBase("OSMWithoutSidewalks", 0, 18, 256, ".png",
                new String[] { "http://etourism.cs.petrsu.ru:20209/osm_tiles/" }) {
            @Override
            public String getTileURLString(MapTile aTile) {
                return getBaseUrl() + aTile.getZoomLevel() + "/" + aTile.getX() + "/" + aTile.getY()
                        + mImageFilenameEnding;
            }
        };

        TileWriter tileWriter = new TileWriter();
        NetworkAvailabliltyCheck networkAvailabliltyCheck = new NetworkAvailabliltyCheck(getActivity());

        MapTileFilesystemProvider fileSystemProvider = new MapTileFilesystemProvider(
                registerReceiver, tileSource);
        MapTileDownloader downloaderProvider = new MapTileDownloader(tileSource, tileWriter, networkAvailabliltyCheck);

        MapTileProviderArray tileProviderArray = new MapTileProviderArray(
                tileSource, registerReceiver, new MapTileModuleProviderBase[]{
                fileSystemProvider, downloaderProvider});

        //mapView = new MapView(getActivity(), 256, new DefaultResourceProxyImpl(getActivity()), tileProviderArray);
          mapView = new MapView(getActivity(),tileProviderArray);
        mapView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18);


        layout.addView(mapView);

    }

    private void createLocationOverlay() {
        IMyLocationProvider provider = new LocationProvider(getActivity());
        locationOverlay = new ScrollableOverlay(provider, mapView);
        locationOverlay.enableMyLocation();

        if (isFollowingActive)
            activateFollowingMode();

        mapView.getOverlayManager().add(locationOverlay);
    }

    private void deactivateFollowingMode() {
        isFollowingActive = false;
        locationOverlay.disableFollowLocation();
        Log.d(TAG,"deactivateFollowingMode");
    }

    private void activateFollowingMode() {
        isFollowingActive = true;
        locationOverlay.enableFollowLocation();
        Log.d(TAG, "activateFollowingMode");
    }

    @Override
    public void newLocation(Location location) {
        Intent intent = new Intent(LocationProvider.BC_LOCATION);
        intent.putExtra(LocationProvider.ARG_LOCATION, location);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }
}
