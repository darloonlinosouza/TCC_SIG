package com.darloonlino.tcc_sig;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.google.android.gms.location.FusedLocationProviderClient;

import java.util.Map;
import java.util.Set;

public class RegionActivity extends AppCompatActivity {
    private MapView mapView;
    private String url;
    private FeatureLayer shapeFeatureLayer;
    private Callout mapCallout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_region);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mapView = findViewById(R.id.mapView);

        Intent regionIntent = getIntent(); // gets the previously created intent
        String url = regionIntent.getStringExtra("url"); // will return "FirstKeyValue"
        this.url = url;

        setupMapCallout();
    }

    // opção d retorno à página anterior
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    // cria um mapa do tipo TOPOGRAPHIC e adiciona por cima o shapeFile
    private void setupMapHydrology() {
        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);
        this.mapView.setMap(map);
        this.mapView.setViewpoint((new Viewpoint( -19.26, -45.45, 12000000.0)));

        ServiceFeatureTable shapefileFeatureTable = new ServiceFeatureTable(this.url);
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
}