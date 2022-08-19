package com.ivanohey.signalmappingapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.SettingInjectorService;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static android.telephony.TelephonyManager.NETWORK_TYPE_1xRTT;
import static android.telephony.TelephonyManager.NETWORK_TYPE_CDMA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EDGE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B;
import static android.telephony.TelephonyManager.NETWORK_TYPE_GPRS;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP;
import static android.telephony.TelephonyManager.NETWORK_TYPE_IDEN;
import static android.telephony.TelephonyManager.NETWORK_TYPE_LTE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_NR;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UMTS;

import static com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    private Button openMapButton;

    private TextView providerNameText;
    private String providerName;

    private TextView signalTypeText;
    private String signalType;

    private TextView coordinatesText;
    private String coordinates;

    private TelephonyManager telephonyManager;

    private int networkType;
    private int REQUEST_PERMISSION_PHONE;

    //Variables used for refresh of location
    private int REQUEST_PERMISSION_LOCATION;
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private int LOCATION_REFRESH_TIME = 5000; // 15 seconds to update
    private int LOCATION_REFRESH_DISTANCE = 500; // 500 meters to update
    private boolean requestingLocationUpdates;

    LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (locationResult == null) {
                Log.i("Location update", "null");
                return;
            }
            for (Location location : locationResult.getLocations()) {
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());
                coordinatesText.setText(processCoordinates(latitude, longitude));
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //We ask for permission for coordinates
        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION,false);
                            Boolean readPhoneStateGranted = result.getOrDefault(Manifest.permission.READ_PHONE_STATE, false);
                            if (fineLocationGranted != null && fineLocationGranted) {
                                // Precise location access granted.
                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                // Only approximate location access granted.
                            } else {
                                // No location access granted.
                            }
                        }
                );
        //We ask for the permissions at the launch of the activity
        locationPermissionRequest.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
        });

        //Set button logic
        openMapButton = findViewById(R.id.openMapButton);
        openMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent goToMapsIntent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(goToMapsIntent);
            }
        });
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(PRIORITY_HIGH_ACCURACY);
    }


    private void checkSettingsAndStartLocationUpdates(){
        LocationSettingsRequest request = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);
        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //Settings satisfied
                startLocationUpdates();
            }
        });
        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Settings aren't satisfied
                if(e instanceof ResolvableApiException){
                    ResolvableApiException apiException = (ResolvableApiException) e;
                    try {
                        apiException.startResolutionForResult(MainActivity.this, 1001);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }

            }
        });
    }
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    //Updates all values once
    @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    protected void onPostResume() {
        super.onPostResume();
        //Set the provider name logic
        providerNameText = findViewById(R.id.providerName);
        providerName = getProviderName();
        providerNameText.setText("Provider: " + providerName);

        //Set the signal type logic
        signalTypeText = findViewById(R.id.signalType);
        signalType = getSignalType();
        signalTypeText.setText("Signal Type: " + signalType);

        //Set the coordinates logic
        coordinatesText = findViewById(R.id.gpsCoordinatesText);
        getCoordinates();
        checkSettingsAndStartLocationUpdates();
        coordinatesText.setText(coordinates);


    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

    //Get the type of network connection and return String with type of Data connection
    @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE})
    public String getSignalType() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PackageManager.PERMISSION_GRANTED);
        //If permission was granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            networkType = telephonyManager.getDataNetworkType();

            switch (telephonyManager.getDataNetworkType()) {
                case NETWORK_TYPE_EDGE:
                case NETWORK_TYPE_GPRS:
                case NETWORK_TYPE_CDMA:
                case NETWORK_TYPE_IDEN:
                case NETWORK_TYPE_1xRTT:
                    return "2G";
                case NETWORK_TYPE_UMTS:
                case NETWORK_TYPE_HSDPA:
                case NETWORK_TYPE_HSPA:
                case NETWORK_TYPE_HSPAP:
                case NETWORK_TYPE_EVDO_0:
                case NETWORK_TYPE_EVDO_A:
                case NETWORK_TYPE_EVDO_B:
                    return "3G";
                case NETWORK_TYPE_LTE:
                    return "4G";
                case NETWORK_TYPE_NR:
                    return "5G";
                default:
                    return "Unknown";
            }
            //Need to call method to save coordinates + quality of connection + network provider
        }
        //If permission has to be granted
        else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_PERMISSION_PHONE);
            return "Unknown";
        }
    }

    //Checks if the results are granted and updates data accordingly
    @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                signalType = getSignalType();
                signalTypeText.setText("Signal Type: " + signalType);
            }
            else {
                signalTypeText.setText("Signal Type: Unknown");
            }
        }
    }

    //Get the providers name and returns it
    public String getProviderName(){
        TelephonyManager telephonyManager = ((TelephonyManager) this.getSystemService(MainActivity.this.TELEPHONY_SERVICE));
        return telephonyManager.getNetworkOperatorName();
    }

    //Gets GPS coordinates of the phone and returns them as a String
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void getCoordinates(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        fusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            String longitude = String.valueOf(location.getLongitude());
                            String latitude = String.valueOf(location.getLatitude());

                            //We call the processCoordinates() to get the data out of the callback method and make it available for the rest of the activity
                            processCoordinates(latitude, longitude);
                        }
                        else{
                            //Overloaded processCoordinates() made for error situation where coordinates are null
                            processCoordinates(-1);
                        }
                    }
                });
    }


    public String processCoordinates(int error){
        this.coordinates = "Error "+error+": Coordinates unknown";
        return "Error "+error+": Coordinates unknown";
    }

    public String processCoordinates(String latitude, String longitude){
        String coordinates;
        coordinates ="Coordinates: \n" + latitude + ", \n" + longitude;
        this.coordinates = coordinates;
        return coordinates;
    }

    //Refreshes all the data (coordinates, provider name, data type)
    public void refreshAllData(){
        //Trouver une logique pour rafraichir les informations sur la 4G + provider (refresh en même temps que les coordonnées)
    }






}