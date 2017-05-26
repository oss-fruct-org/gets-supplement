package org.fruct.oss.getssupplement;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.fruct.oss.getssupplement.fragments.MapFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int MY_PERMISSIONS_REQUEST_CODE = 201;
    private static final String[] MY_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    public static final List<String> AvailablePerms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        checkPerms();

        displayView(R.id.nav_map);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    public void displayView(int id) {
        Fragment fragment = null;
        String title = getResources().getString(R.string.app_name);

        if (id == R.id.nav_map) {
            fragment = MapFragment.newInstance();
            title = MapFragment.getTitle(this);

        } else if (id == R.id.nav_queue) {

        } else if (id == R.id.nav_policy) {

        } else if (id == R.id.nav_about) {

        }

        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.container, fragment);
            ft.commit();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        Log.d(this.getLocalClassName(), "Update view to " + id + "(" + title + ")");
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        displayView(id);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // проверка наличия прав (Андрюша 6+)
    private void checkPerms() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            List<String> requestedPerms = new ArrayList<>();
            for (String perm : MY_PERMISSIONS) {
                if (this.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                    requestedPerms.add(perm);
                } else {
                    AvailablePerms.add(perm);
                }
            }
            if (requestedPerms.size() > 0)
                this.requestPermissions(requestedPerms.toArray(new String[requestedPerms.size()]),
                            MY_PERMISSIONS_REQUEST_CODE);
        } else {
            AvailablePerms.addAll(Arrays.asList(MY_PERMISSIONS));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                AvailablePerms.add(permissions[i]);
            }
            Log.d(getClass().getSimpleName(), "Perm: " + permissions[i] + " granted: " + grantResults[i]);
        }
    }
}
