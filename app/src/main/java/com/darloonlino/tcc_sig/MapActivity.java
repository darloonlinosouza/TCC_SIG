package com.darloonlino.tcc_sig;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private FusedLocationProviderClient currentLocationService;
    private Location currentLocation;
    private LocationDisplay locationDisplay;

    int PERMISSION_ID = 2;
    String[] REQUEST_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        currentLocationService = LocationServices.getFusedLocationProviderClient(this);
        mapView = findViewById(R.id.mapView);
        getLastLocation();
        setupMap();
        setupLocationDisplay();
        setupGPS();
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (!checkLocationPermissions()) requestLocationPermissions();

        if(isLocationEnabled()){
            currentLocationService.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if(task.isSuccessful()) {
                        if (task.getResult() != null)
                            currentLocation = task.getResult();
                    }
                }
            });
        }
    }

    private void setupMap() {
        if (mapView != null) {
            Basemap.Type baseMapType = Basemap.Type.STREETS_VECTOR;

            // coords do vídeo
            double latitude = -21.2526;
            double longitude = -43.1511;

            if (currentLocation != null) {
                // localização atual, se disponível
                latitude = currentLocation.getLatitude();
                longitude = currentLocation.getLongitude();
            }

            int levelOfDetail = 20;
            ArcGISMap map = new ArcGISMap(baseMapType, latitude, longitude, levelOfDetail);
            mapView.setMap(map);
        }
    }

    private  void setupLocationDisplay() {
        locationDisplay = mapView.getLocationDisplay();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        locationDisplay.startAsync();
    }

    private  void setupGPS() {
        locationDisplay.addDataSourceStatusChangedListener(dataSourceStatusChangedEvent -> {
            if(dataSourceStatusChangedEvent.isStarted() || dataSourceStatusChangedEvent.getError() == null)
                return;

            if(!checkLocationPermissions()) requestLocationPermissions();
            else Toast.makeText(this, "Erro", Toast.LENGTH_LONG).show();
        });
    }

    // solicita a permissão de localização
    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, REQUEST_PERMISSIONS, PERMISSION_ID);
    }

    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // verifica se o acesso a localização foi dado
    private boolean checkLocationPermissions() {
        return (ContextCompat.checkSelfPermission(this, REQUEST_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, REQUEST_PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            locationDisplay.startAsync();
        else {
           Toast.makeText(this, "Localização recusada! Mapa com localização padrão", Toast.LENGTH_LONG).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onPause() {
        if(mapView != null) mapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mapView != null) mapView.resume();
    }

    @Override
    protected void onDestroy() {
        if(mapView != null) mapView.dispose();
        super.onDestroy();
    }
}