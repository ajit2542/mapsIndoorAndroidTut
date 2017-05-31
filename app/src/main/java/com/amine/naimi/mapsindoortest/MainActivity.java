package com.amine.naimi.mapsindoortest;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.mapspeople.data.OnDataReadyListener;
import com.mapspeople.debug.dbglog;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.models.AppConfig;
import com.mapspeople.models.CategoryCollection;
import com.mapspeople.models.LocationDisplayRule;
import com.mapspeople.models.LocationDisplayRules;
import com.mapspeople.models.POIType;
import com.mapspeople.models.Point;
import com.mapspeople.models.PushMessageCollection;
import com.mapspeople.models.Solution;
import com.mapspeople.models.VenueCollection;
import com.mapspeople.routing.DirectionsRenderer;
import com.mapspeople.routing.MPDirectionsRenderer;
import com.mapspeople.routing.OnLegSelectedListener;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements   OnDataReadyListener {
    private VenueCollection venues;
    private Solution solution;

    private boolean solutionReady = false;
    private boolean venueReady = false;
    private boolean settingsReady = false;
    private boolean dataReady = false;
    private AppConfig settings;
    private Point START_POSITION = new Point();

    String TAG = "DEXTER LOG";

    private static Map<String, Object> locationTypeImages = new HashMap<>();


    MapControl myMapControl;
    private GoogleMap mMap;
    LocationDisplayRules displayRules;
    SupportMapFragment mapFragment;
    GPSPositionProvider gpsProvider;

    PermissionListener mLocationPermissionListener;
    PermissionRequestErrorListener mLocationPermissionRequestErrorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbglog.useDebug(true);


        createPermissionListeners();

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(mLocationPermissionListener)
                .withErrorListener(mLocationPermissionRequestErrorListener)
                .check();
    }



    void setupMapIfNeeded() {

        // init a mapControl
        myMapControl = new MapControl(this, mapFragment, mMap);
        myMapControl.initMap("550c26a864617400a40f0000", "rtx");

        //add a display rule
        displayRules = new LocationDisplayRules();
        displayRules.add(new LocationDisplayRule("parking", R.drawable.elevator, 17f, false));
        myMapControl.addDisplayRules(displayRules);

        // setting the current user position(blue dot)
        double latitude = 57.08585;
        double longitude = 9.95751;
        myMapControl.setCurrentPosition(new Point(latitude,longitude ), 0);

        //Add a position provider in able to track the user's position.
        gpsProvider = new GPSPositionProvider(this.getApplicationContext());
        //Telling map control about our provider
        myMapControl.addPositionProvider(gpsProvider);
        myMapControl.startPositioning();
        myMapControl.showUserPosition(true);

        myMapControl.setOnDataReadyListener(this);

        MPDirectionsRenderer drRenderer = new MPDirectionsRenderer(this, new OnLegSelectedListener() {
            @Override
            public void onLegSelected(int i) {

            }
        });

        drRenderer.setMap(mMap);

    }




    @Override
    public void onVenueDataReady(final VenueCollection venueCollection)
    {
        venues = venueCollection;
        venueReady = true;
        new Handler(getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                START_POSITION = venues.getDefaultVenue().getPosition();
                myMapControl.selectFloor(venues.getDefaultVenue().getDefaultFloor());
                myMapControl.setMapPosition(START_POSITION, 17, false);
            }
        });
        onDataReady();
    }

    @Override
    public void onSolutionDataReady(final Solution solution)
    {
        // when the solution data finish loading we can load the icons and store them locally
        this.solution = solution;
        List<POIType> types = solution.getTypes();
        for (POIType t : types)
        {
            loadIcon(t.name, t.icon);
        }
        solutionReady = true;
        onDataReady();
    }

    @Override
    public void onAppConfigDataReady(AppConfig settings)
    {
        this.settings = settings;
        settingsReady = true;
        onDataReady();
    }

    private void onDataReady()
    {
        if (solutionReady && venueReady && settingsReady && !dataReady)
        {
            dataReady = true;

        }
    }

    @Override
    public void onLocationDataReady() {

    }

    @Override
    public void onAppDataReady() {

    }



    @Override
    public void onCategoryDataReady(CategoryCollection categoryCollection) {

    }

    @Override
    public void onPushMessageDataReady(PushMessageCollection pushMessageCollection) {

    }


    //Loads an image from a URL and adds them to the locationTypeImages dictionary (if found)
    public static void loadIcon(final String typeName, final String iconURL)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    Bitmap bitmap = BitmapFactory.decodeStream((InputStream)new URL(iconURL).getContent());
                    if (bitmap != null)
                    {
                        locationTypeImages.put(typeName, bitmap);
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    private void createPermissionListeners()
    {
        mLocationPermissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted( PermissionGrantedResponse permissionGrantedResponse ) {

                mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment));

                mapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        mMap = googleMap;
                        setupMapIfNeeded();
                    }
                });
            }

            @Override
            public void onPermissionDenied( PermissionDeniedResponse permissionDeniedResponse ) {
                finish();
                System.exit(0);
                Log.d( TAG, "Dexter.onPermissionDenied: User has denied permissions and selected 'Never ask again'" );


            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken ) {
                permissionToken.continuePermissionRequest();
                Log.d( TAG, "Should be shown again" );

            }
        };

        mLocationPermissionRequestErrorListener = new PermissionRequestErrorListener() {
            @Override
            public void onError( DexterError dexterError ) {
                Log.d( TAG, String.format( Locale.US, "Dexter.onError: %s", dexterError ) );
            }
        };
    }

}
