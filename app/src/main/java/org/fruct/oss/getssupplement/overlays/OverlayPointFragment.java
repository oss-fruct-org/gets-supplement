package org.fruct.oss.getssupplement.overlays;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.fruct.oss.getssupplement.R;
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
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

/**
 * Диалог добавления/изменения точки
 */

public class OverlayPointFragment extends Fragment {

    private IMyLocationProvider locationProvider;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.overlay_point, null);

        // карта
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

        MapView mapView = new MapView(getActivity(),tileProviderArray);
        mapView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18);

        layout.addView(mapView);

        // отслеживание местоположения
        MyLocationNewOverlay locationOverlay = new ScrollableOverlay(locationProvider, mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.overlay_point, menu);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Add point");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_cancel) {
            FragmentTransaction tr = getFragmentManager().beginTransaction();
            // скрываем панель фильтра
            Fragment fr = getFragmentManager().findFragmentByTag("overlay-add-point-fragment");
            tr.hide(fr);
            getFragmentManager().popBackStack();
            tr.commit();
            return true;
        }
        if (item.getItemId() == R.id.action_add) {
            // TODO: сделать отправку точки
        }
        return super.onOptionsItemSelected(item);
    }

    // TODO: issue 1
    public void setLocationProvider(IMyLocationProvider provider) {
        this.locationProvider = provider;
    }
}
