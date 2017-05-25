package org.fruct.oss.getssupplement.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
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

import org.fruct.oss.gets.Disability;
import org.fruct.oss.gets.Point;
import org.fruct.oss.gets.PointsService;
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
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.gms.internal.zzt.TAG;

/**
 * Отображение вкладки карты
 */

public class MapFragment extends Fragment implements LocationReceiver.Listener,
        PointsService.Listener,
        ItemizedIconOverlay.OnItemGestureListener<MapFragment.Obstacle> {

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

    // последнее сохраненное положение
    private Location lastSavedLocation = null;

    // GeTS service
    private PointsService pointsService;
    private ServiceConnection pointConnection = new PointConnection();

    private ItemizedIconOverlay<Obstacle> obstaclesOverlay;

    // отключенные для отображения категории инвалидности
    private List<String> excludedDisabilities = new ArrayList<>();

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

        // Bundle GeTS service
        Intent intent = new Intent(getActivity(), PointsService.class);
        getActivity().bindService(intent, pointConnection, Context.BIND_AUTO_CREATE);

        lastSavedLocation = new Location(LocationReceiver.MOCK_PROVIDER);
        lastSavedLocation.setAccuracy(1);
        if (savedInstanceState != null) {
            isFollowingActive = savedInstanceState.getBoolean(STATE_FOLLOW);

            lastSavedLocation.setLatitude(savedInstanceState.getDouble(STATE_LOCATION_LAT));
            lastSavedLocation.setLongitude(savedInstanceState.getDouble(STATE_LOCATION_LON));
            Log.d(this.getClass().getSimpleName(), "found location: " + lastSavedLocation.toString());
        } else {
            // грузим из настроек приложения
            lastSavedLocation.setLatitude(pref.getFloat(STATE_LOCATION_LAT, 0.0f));
            lastSavedLocation.setLongitude(pref.getFloat(STATE_LOCATION_LON, 0.0f));
            isFollowingActive = pref.getBoolean(STATE_FOLLOW, true);
            Log.d(getClass().getSimpleName(), "Preference load: " + isFollowingActive + " " + lastSavedLocation);
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

        if (lastSavedLocation != null) {
            newLocation(lastSavedLocation);
            mapView.getController().setCenter(new GeoPoint(lastSavedLocation.getLatitude(), lastSavedLocation.getLongitude()));
            Log.d(getClass().getSimpleName(), "Update location to " + lastSavedLocation);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        locationReceiver.stop();
        locationReceiver.removeListener(this);

        if (pointsService != null) {
            pointsService.removeListener(this);
            pointsService = null;
        }

        getActivity().unbindService(pointConnection);
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
        if (item.getItemId() == R.id.action_update) {
            pointsService.refresh();
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

        obstaclesOverlay = new ItemizedIconOverlay<>(new ArrayList<Obstacle>(), this, this.getContext());
        mapView.getOverlayManager().add(obstaclesOverlay);

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

    // обработка получения нового местоположения
    @Override
    public void newLocation(Location location) {
        Intent intent = new Intent(LocationProvider.BC_LOCATION);
        intent.putExtra(LocationProvider.ARG_LOCATION, location);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        if (pointsService != null) {
            pointsService.setLocation(location);
        }
    }

    // когда установлен коннект с pointsService
    private void onPointsServiceReady(PointsService service) {
        this.pointsService = service;
        this.pointsService.setServerUrl("http://gets.cs.petrsu.ru/obstacle");
        this.pointsService.setLocation(lastSavedLocation);
        pointsService.addListener(this);
        getActivity().supportInvalidateOptionsMenu();
    }

    // Пришла свежая порция данных с сервера!!!
    @Override
    public void onDataUpdated(boolean isRemoteUpdate) {
        Log.v(getClass().getSimpleName(), "Points was Updated: " + isRemoteUpdate);

        // установка категорий инвалидности
        List<Disability> disabilities = pointsService.queryList(pointsService.requestDisabilities());
        for (Disability d : disabilities) {
            if (excludedDisabilities.contains(d.getName()) && d.isActive()) {
                pointsService.setDisabilityState(d, false);
            }

            if (!(excludedDisabilities.contains(d.getName()) || d.isActive())) {
                pointsService.setDisabilityState(d, true);
            }
        }

        // обновление точек на карте
        obstaclesOverlay.removeAllItems();

        List <Point>obstaclePoints = pointsService.queryList(pointsService.requestPoints());
        List <Obstacle> obstacles = new ArrayList<>(obstaclePoints.size());
        for (Point pt : obstaclePoints) {
            obstacles.add(new Obstacle(pt.getName(), pt.getDescription(),
                    new GeoPoint(pt.getLat(), pt.getLon()),
                    new BitmapDrawable(getContext().getResources(), pt.getCategory().getIcon())));
        }
        obstaclesOverlay.addItems(obstacles);
        Log.i(getClass().getSimpleName(), "Added " + obstacles.size() + " items to show");
        mapView.invalidate();
    }

    @Override
    public void onDataUpdateFailed(Throwable throwable) {
        Log.e(getClass().getSimpleName(), "Error updating points", throwable);
    }

    // Щелчок на препятствие
    @Override
    public boolean onItemSingleTapUp(int index, Obstacle item) {
        return false;
    }

    // долгое нажатие на препятствие
    @Override
    public boolean onItemLongPress(int index, Obstacle item) {
        return false;
    }

    // соединение с PointsService
    private class PointConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            PointsService service = ((PointsService.Binder) binder).getService();
            onPointsServiceReady(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            pointsService = null;
        }
    }

    // Объект точка на карте
    static class Obstacle extends OverlayItem {
        Obstacle(String aTitle, String aSnippet, GeoPoint aGeoPoint, Drawable drawable) {
            super(aTitle, aSnippet, aGeoPoint);
            setMarker(drawable);
        }
    }
}
