package com.darloonlino.tcc_sig;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
    private ServiceFeatureTable shapefileRegion;

    int PERMISSION_ID = 2;
    String[] REQUEST_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    String URL = "https://services1.arcgis.com/N3Vx53K5yCP624mB/arcgis/rest/services/layersmg/FeatureServer/";

    public enum RiversMG {
        SAO_FRANC_ALTO(0),
        DOCE(1),
        GRANDE(2),
        JEQUITINHONHA(3),
        LESTE(4),
        SAO_FRANC_MEDIO(5),
        PARAIBA_DO_SUL(6),
        PARANAIBA(7),
        PARDO(8),
        PIRACICABAJAGUARI(9);

        private final int index;
        RiversMG(int index){
            this.index = index;
        }
        public int getValue(){
            return this.index;
        }
    }

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
        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_COMMUNITY);
        ArcGISRuntimeEnvironment.setApiKey(BuildConfig.ARCGIS_API_KEY);
        this.mapView.setMap(map);
        this.mapView.setViewpoint((new Viewpoint( -19.26, -45.45, 12000000.0)));

        ServiceFeatureTable shapefileFeatureTable = new ServiceFeatureTable(URL + "10");
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

                    // busca o nome da área de Minas clicado
                    identifyLayerResultListenableFuture.addDoneListener(() -> {
                        try {
                            IdentifyLayerResult identiyfLayerResult = identifyLayerResultListenableFuture.get();
                            Feature feature = (Feature) identiyfLayerResult.getElements().get(0);
                            Map<String, Object> attr = feature.getAttributes();
                            String name = (String) attr.get("Nome");

                            // compara o nome da area com o enum de indices da url
                            for (RiversMG riversEnum : RiversMG.values()) {
                                String nameRiverFormated = name.replaceAll("/ /g", "_");

                                // quando encontrado o nome no enum, navega para a outra tela com a url do shapefile de rios dessa região
                                if(riversEnum.name().equals(nameRiverFormated)) {
                                    Intent regionIntent = new Intent(MapActivity.this, RegionActivity.class);
                                    regionIntent.putExtra("url",URL + riversEnum.getValue());
                                    startActivity(regionIntent);
                                }
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

    /** Não utilizados */

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