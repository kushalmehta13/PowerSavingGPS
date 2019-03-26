package com.example.powersavinggps;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, View.OnClickListener, SensorEventListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private double lattitude;
    private double longitude;
    private LatLng loc;
    private Marker m;
    private Button getCurrentLoc;
    private SensorManager sensorManager;
    private static float direction;
    private static Sensor accelerometer;
    private static Sensor magnetometer;
    private static float[] mAccelerometer = new float[3];
    private static float[] mGeomagnetic = new float[3];
    private static float[] velocity = new float[3];
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp = 0f;


    private TextView X, Y;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        X = findViewById(R.id.X);
        Y = findViewById(R.id.Y);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
            return;
        }


        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, Criteria.ACCURACY_FINE, (LocationListener) this);
        getCurrentLoc = findViewById(R.id.getLocation);
        getCurrentLoc.setOnClickListener(this);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMinZoomPreference(1);
    }


    @Override
    public void onLocationChanged(Location location) {
        longitude = location.getLongitude();
        lattitude = location.getLatitude();
        loc = new LatLng(lattitude, longitude);
        if (m != null) {
            animateMarkerToGB(m, loc, new LatLngInterpolator.Spherical());
        } else {
            MarkerOptions a = new MarkerOptions().position(loc);
            m = mMap.addMarker(a);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
        }
        CameraUpdate l = CameraUpdateFactory.newLatLngZoom(loc, 1);
        mMap.animateCamera(l);
    }

    static void animateMarkerToGB(final Marker marker, final LatLng finalPosition, final LatLngInterpolator latLngInterpolator) {
        final LatLng startPosition = marker.getPosition();
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final float durationInMs = 500;

        handler.post(new Runnable() {
            long elapsed;
            float t;
            float v;

            @Override
            public void run() {
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / durationInMs;
                v = interpolator.getInterpolation(t);

                marker.setPosition(latLngInterpolator.interpolate(v, startPosition, finalPosition));

                // Repeat till progress is complete.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, Criteria.ACCURACY_FINE, this);
    }

    @Override
    public void onClick(View v) {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, Criteria.ACCURACY_FINE, this);
        }
        else{
            CameraUpdate l = CameraUpdateFactory.newLatLngZoom(loc, 10);
            mMap.animateCamera(l);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.8f;
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            mAccelerometer[0] = formatNumber(event.values[0]) - mAccelerometer[0];
            mAccelerometer[1] = formatNumber(event.values[1]) - mAccelerometer[1];
            mAccelerometer[2] = formatNumber(event.values[2]) - mAccelerometer[2];
            velocity = getVelocity(mAccelerometer, event);

            X.setText(new Float(velocity[0]).toString());
            Y.setText(new Float(velocity[1]).toString());
            System.out.println(mAccelerometer[0]);
        }
        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            mGeomagnetic = event.values;
        }
        if(mAccelerometer != null && mGeomagnetic != null){
            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, mAccelerometer, mGeomagnetic);

            if(success){
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                direction = (float) Math.toDegrees(orientation[0]);
//                System.out.println(direction);
            }
        }
    }

    public float formatNumber(float x){
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        String z= df.format(x);
        float v = Float.parseFloat(z);
        return v;
    }

    private float[] getVelocity(float[] mAccelerometer, SensorEvent event) {
        if(timestamp!=0){
            final float dT = (event.timestamp - timestamp) * NS2S;
            velocity[0] += mAccelerometer[0] * dT;
            velocity[1] += mAccelerometer[1] * dT;
            velocity[2] += mAccelerometer[2] * dT;
        }
        else {
            timestamp = event.timestamp;
        }
        return velocity;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
