package com.darloonlino.tcc_sig;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Map;
import java.util.Set;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private FusedLocationProviderClient currentLocationService;
    private Location currentLocation;
    private LocationDisplay locationDisplay;
    private FeatureLayer shapeFeatureLayer;
    private Callout mapCallout;

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
        //getLastLocation();
        //setupMap();
        setupMapCallout();
        //setupLocationDisplay();
        //setupGPS();
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

    // cria um mapa do tipo TOPOGRAPHIC e adiciona por cima o shapeFile
    private void setupMapHydrology() {
        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);
        ArcGISRuntimeEnvironment.setApiKey(BuildConfig.ARCGIS_API_KEY);
        this.mapView.setMap(map);
        this.mapView.setViewpoint((new Viewpoint(-20.0, -41.0, 2500000.0)));

        ServiceFeatureTable shapefileFeatureTable = new ServiceFeatureTable("https://services3.arcgis.com/U26uBjSD32d7xvm2/arcgis/rest/services/rio_doce/FeatureServer/0");
        this.shapeFeatureLayer = new FeatureLayer(shapefileFeatureTable);

        this.mapView.getMap().getOperationalLayers().add(this.shapeFeatureLayer);

    }

    // tabela com atributos dos rios
    private void setupMapCallout() {
        this.setupMapHydrology();
        this.mapCallout = this.mapView.getCallout();

        // captura o toque do usuário no mapa, com um certa tolerancia (area de erro)
        mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                try {
                    if (mapCallout.isShowing()) mapCallout.dismiss();

                    final Point screenPoint = new Point(Math.round(e.getX()), Math.round(e.getY()));
                    int tolerance = 10;
                    final ListenableFuture<IdentifyLayerResult> identifyLayerResultListenableFuture;
                    identifyLayerResultListenableFuture = mapView.identifyLayerAsync(shapeFeatureLayer, screenPoint, tolerance, false, 1);

                    // busca e manipula os dados da tabela correspondentes ao rio clicado
                    identifyLayerResultListenableFuture.addDoneListener(() -> {
                        try {
                            IdentifyLayerResult identiyfLayerResult = identifyLayerResultListenableFuture.get();

                            // balão com os valores
                            TextView calloutContent = new TextView(getApplicationContext());
                            calloutContent.setTextColor(Color.BLACK);
                            calloutContent.setSingleLine(false);
                            calloutContent.setVerticalScrollBarEnabled(true);
                            calloutContent.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                            calloutContent.setMovementMethod(new ScrollingMovementMethod());

                            // para cada element (coluna da tabela)
                            for (GeoElement element : identiyfLayerResult.getElements()) {
                                Feature feature = (Feature) element;
                                Map<String, Object> attr = feature.getAttributes();
                                Set<String> keys = attr.keySet();

                                for (String key : keys) {
                                    Object value = attr.get(key);
                                    calloutContent.append(key + " | " + value + "\n");
                                }

                                Envelope envelope = feature.getGeometry().getExtent();
                                mapView.setViewpointGeometryAsync(envelope, 200);

                                mapCallout.setLocation(envelope.getCenter());
                                mapCallout.setContent(calloutContent);
                                mapCallout.show();
                            }
                        } catch (Exception el) {
                            Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + el.getMessage());
                        }
                    });
                } catch (Exception er) {
                    Log.d("error layer", er.getMessage());
                }
                    return super.onSingleTapConfirmed(e);
            }
        });
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