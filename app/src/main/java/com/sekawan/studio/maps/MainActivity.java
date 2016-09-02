package com.sekawan.studio.maps;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.sekawan.studio.R;
import com.sekawan.studio.maps.utils.PermissionUtils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This demo shows how GMS Location can be used to check for changes to the users location.  The
 * "My Location" button uses GMS Location to set the blue dot representing the users location.
 * Permission for {@link android.Manifest.permission#ACCESS_FINE_LOCATION} is requested at run
 * time. If the permission has not been granted, the Activity is finished with an error message.
 */
public class MainActivity extends AppCompatActivity
        implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        LocationListener,
        View.OnClickListener {

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    private GoogleMap mMap;
    LatLng latLng;
    double lat;
    double log;
    private LocationManager locationManager;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;

    private View layoutBubble;
    private LinearLayout layoutMarker;
    private int selectedPostion = -1;
    //private AutoCompleteTextView etSource;
    private EditText etSource;
    private Address address;
    private String strAddress = null;
    public static boolean isMapTouched = false;
    private LatLng curretLatLng;
    private boolean isSource;
    private Marker markerDestination, markerSource;
    private ArrayList<LatLng> points;
    private PolylineOptions lineOptions;
    private Location myLocation;
    MainActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps);

        //view
        layoutMarker = (LinearLayout) findViewById(R.id.layoutMarker);
        layoutBubble = findViewById(R.id.layoutBubble);
        etSource = (EditText) findViewById(R.id.etEnterSouce);

        //onclick
        findViewById(R.id.markerBubblePickMeUp).setOnClickListener(this);
        findViewById(R.id.imgClearSource).setOnClickListener(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        etSource.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ) {
                    // Do whatever you want here
                    hideSoftKeyboard();
                    Log.i("Done",etSource.getText().toString());
                    SearchLokasi(etSource.getText().toString());
                    return true;
                }
                return false;
            }
        });

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.markerBubblePickMeUp:
                if (isValidate()) {
                    Toast.makeText(this, "Test", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.imgClearSource:
                etSource.setText("");
                break;
        }
    }
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        //mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();

		/*latLng = new LatLng(gps.getLatitude(), gps.getLongitude());
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 14);
		mMap.animateCamera(cameraUpdate);
		lat = gps.getLatitude();
		log = gps.getLongitude();
		String gps = Double.toString(lat)+Double.toString(log);
		Toast.makeText(this, gps, Toast.LENGTH_SHORT).show();*/
        LatLng center = mMap.getCameraPosition().target;
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {

                Log.i("centerLat",Double.toString(cameraPosition.target.latitude));

                Log.i("centerLong",Double.toString(cameraPosition.target.longitude));
                lat = cameraPosition.target.latitude;
                log = cameraPosition.target.longitude;
                getAddressFromLocation(new LatLng(cameraPosition.target.latitude,cameraPosition.target.longitude), etSource);
            }
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    public void onLocationChanged(Location location) {
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        mMap.animateCamera(cameraUpdate);
        locationManager.removeUpdates(this);


        // etDestination.setAdapter(adapterDestination);
		/*
		*/

        lat = location.getLatitude();
        log = location.getLongitude();
        String st = Double.toString(lat)+" Dan "+Double.toString(log);
        Toast.makeText(this,st,Toast.LENGTH_SHORT).show();
        getAddressFromLocation(new LatLng(lat,log), etSource);
        String response = "28";
        points = new ArrayList<LatLng>();
        lineOptions = new PolylineOptions();

        if (points != null && points.size() > 0) {
            setMarker(new LatLng(points.get(0).latitude,
                    points.get(0).longitude), isSource);
            if (isSource) {
                getAddressFromLocation(
                        new LatLng(points.get(0).latitude,
                                points.get(0).longitude), etSource);
            }
            // else {
            // getAddressFromLocation(
            // new LatLng(points.get(0).latitude,
            // points.get(0).longitude), etDestination);
            // }
				/*if (markerSource != null && markerDestination != null) {
					showDirection(markerSource.getPosition(),
							markerDestination.getPosition());
				}*/
        }

        LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
        if(mMap != null){
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(loc).zoom(15).build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            lat = location.getLatitude();
            log = location.getLongitude();
            Toast.makeText(this, "Location is: " + String.valueOf(lat) + ", "
                    + String.valueOf(log), Toast.LENGTH_LONG).show();
        }

    }

    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    protected boolean isValidate() {
        String msg = null;
        if (latLng == null) {
            msg = getString(R.string.text_location_not_found);
        } else if (selectedPostion == -1) {
            msg = getString(R.string.text_select_type);
        } else if (TextUtils.isEmpty(etSource.getText().toString())
                || etSource.getText().toString()
                .equalsIgnoreCase("Waiting for Address")) {
            msg = getString(R.string.text_waiting_for_address);
        }
        if (msg == null)
            return true;
        Toast.makeText(this, "Location is: " + String.valueOf(lat) + ", "
                + String.valueOf(log)+" Nama Lokasi ="+strAddress, Toast.LENGTH_LONG).show();

        //startActivity(new Intent(this, MainActivity.class));
        return false;
    }

    private void getAddressFromLocation(final LatLng latlng, final EditText et) {
        et.setText("Waiting for Address");
        et.setTextColor(Color.GRAY);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Geocoder gCoder = new Geocoder(MainActivity.this);
                try {
                    final List<Address> list = gCoder.getFromLocation(
                            latlng.latitude, latlng.longitude, 1);
                    if (list != null && list.size() > 0) {
                        address = list.get(0);
                        StringBuilder sb = new StringBuilder();
                        if (address.getAddressLine(0) != null) {
                            if (address.getMaxAddressLineIndex() > 0) {
                                for (int i = 0; i < address
                                        .getMaxAddressLineIndex(); i++) {
                                    sb.append(address.getAddressLine(i))
                                            .append("\n");
                                }
                                sb.append(",");
                                sb.append(address.getCountryName());
                            } else {
                                sb.append(address.getAddressLine(0));
                            }
                        }

                        strAddress = sb.toString();
                        strAddress = strAddress.replace(",null", "");
                        strAddress = strAddress.replace("null", "");
                        strAddress = strAddress.replace("Unnamed", "");
                    }
                    if (MainActivity.this == null)
                        return;

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!TextUtils.isEmpty(strAddress)) {
                                et.setFocusable(false);
                                et.setFocusableInTouchMode(false);
                                et.setText(strAddress);
                                et.setTextColor(getResources().getColor(android.R.color.black));
                                et.setFocusable(true);
                                et.setFocusableInTouchMode(true);
                            } else {
                                et.setText("");
                                et.setTextColor(getResources().getColor(android.R.color.black));
                            }
                            etSource.setEnabled(true);
                        }
                    });
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }).start();
    }

    private LatLng getLocationFromAddress(final String place) {
        LatLng loc = null;
        Geocoder gCoder = new Geocoder(MainActivity.this);
        try {
            final List<Address> list = gCoder.getFromLocationName(place, 1);
            if (list != null && list.size() > 0) {
                loc = new LatLng(list.get(0).getLatitude(), list.get(0)
                        .getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loc;
    }

    private void setMarker(LatLng latLng, boolean isSource) {
        //if (!Maps.this.isVisible())
        //	return;
        if (MainActivity.this != null && MainActivity.this.getCurrentFocus() != null) {
            // inputMethodManager.hideSoftInputFromWindow(getActivity()
            // .getCurrentFocus().getWindowToken(), 0);
            //activity.hideKeyboard();
        }

        if (latLng != null && mMap != null) {
            if (isSource) {
                if (markerSource == null) {
                    markerSource = mMap.addMarker(new MarkerOptions()
                            .position(
                                    new LatLng(latLng.latitude,
                                            latLng.longitude))
                            .title(getResources().getString(
                                    R.string.text_source_pin_title))
                            .icon(BitmapDescriptorFactory
                                    .fromResource(R.drawable.pin_client_org)));
                    // markerSource.setDraggable(true);
                } else {
                    markerSource.setPosition(latLng);
                }
                CameraUpdateFactory.newLatLng(latLng);
            }
        } else {
            Toast.makeText(this, "Unable to get location..!",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void SearchLokasi(String lokasi){
        LatLng cari = getLocationFromAddress(lokasi);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cari, 15);
        mMap.animateCamera(cameraUpdate);
        locationManager.removeUpdates(this);
    }

    public void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE
        );
        imm.hideSoftInputFromWindow(etSource.getWindowToken(), 0);
    }
}

