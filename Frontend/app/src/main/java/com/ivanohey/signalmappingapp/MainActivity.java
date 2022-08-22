package com.ivanohey.signalmappingapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
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
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.ivanohey.signalmappingapp.SaveCoordinatesService.LocalBinder;

public class MainActivity extends AppCompatActivity {

    //We create an instance of SaveCoordinatesService that we use in saveCoordinatesServiceConnection class
    SaveCoordinatesService saveCoordinatesService = new SaveCoordinatesService();
    boolean serviceIsConnected = false;

    //Interface variables
    private Button openMapButton;

    private TextView providerNameText;
    private String providerName;

    private TextView signalTypeText;
    private String signalType;

    private TextView coordinatesText;
    private String coordinatesString;
    private String coordinates;

    //Required variables for permissions
    private TelephonyManager telephonyManager;

    private int networkType;
    private int REQUEST_PERMISSION_PHONE;

    //Variables used for refresh of location
    private int REQUEST_PERMISSION_LOCATION;
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private int LOCATION_REFRESH_TIME = 3000; // 15 seconds to update
    private int LOCATION_REFRESH_DISTANCE = 500; // 500 meters to update
    private boolean requestingLocationUpdates;
    private String latitude;
    private String longitude;

    //Interface elements of saveCoordinatesService
    private TextView saveCoordText;


    //Callback method extracting location updates
    LocationCallback locationCallback = new LocationCallback(){
        @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (locationResult == null) {
                Log.i("Location update", "null");
                return;
            }
            for (Location location : locationResult.getLocations()) {
                latitude = String.valueOf(location.getLatitude());
                longitude = String.valueOf(location.getLongitude());
                coordinatesText.setText(processCoordinates(latitude, longitude));

                //We update the other data
                signalType = getSignalType();
                signalTypeText.setText("Signal Type: " + signalType);
                providerName = getProviderName();
                providerNameText.setText(("Provider: "+providerName));

                //We use this method to call the SaveCoordinatesService and send data to backend using HTTP POST method
                sendRecord();

            }
        }
    };

    public void sendRecord(){
        //Here we call method to push to backend
        saveCoordinatesService.createJSONObjectRequest(latitude, longitude, providerName, signalType);
        saveCoordinatesService.sendRecord();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection saveCoordinatesServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            //We get instance of the connection
            LocalBinder binder = (LocalBinder) service;
            saveCoordinatesService = binder.getBoundService();  //We create the instance of the service
            serviceIsConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceIsConnected = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //We create the locationRequest used to set up intervals and accuray of the location updates
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(PRIORITY_HIGH_ACCURACY);

        //We ask for permission for coordinates
        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION,false);
                            Boolean readPhoneStateGranted = result.getOrDefault(Manifest.permission.READ_PHONE_STATE, false);
                            Boolean accessInternet = result.getOrDefault(Manifest.permission.INTERNET, false);
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
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.INTERNET
        });

        //Set button logic
        openMapButton = findViewById(R.id.openMapButton);
        openMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent goToMapsIntent = new Intent(MainActivity.this, MapsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("coordinates", MainActivity.this.coordinates);
                goToMapsIntent.putExtras(bundle);
                startActivity(goToMapsIntent);
            }
        });
        saveCoordText = findViewById(R.id.saveCoordStatusTextView);
        saveCoordText.setText(saveCoordinatesService.showServiceStatus());
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Bind to SaveCoordinatesService
        Intent intent = new Intent(this, SaveCoordinatesService.class);
        bindService(intent, saveCoordinatesServiceConnection, Context.BIND_AUTO_CREATE);
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
        coordinatesText.setText(coordinatesString);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
        unbindService(saveCoordinatesServiceConnection);
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

    //Get the type of network connection and return String with type of Data connection
    @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE})
    public String getSignalType() {
        //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PackageManager.PERMISSION_GRANTED);
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
                    return "None";
            }
        }
        //If permission has to be granted
        else{
            return "No permission";
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
                signalTypeText.setText("Signal Type: None");
            }
        }
    }

    //Get the providers name and returns it
    public String getProviderName(){
        TelephonyManager telephonyManager = ((TelephonyManager) this.getSystemService(MainActivity.this.TELEPHONY_SERVICE));
        if(telephonyManager.getNetworkOperatorName() == ""){
            return "None";
        }
        else{
            return telephonyManager.getNetworkOperatorName();
        }
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
                            Log.d("Coordinates result","Permission not granted");
                            processCoordinates(-1);
                        }
                    }
                });
    }

    public String processCoordinates(int error){
        this.coordinatesString = "Error "+error+": Coordinates unknown";
        return "Error "+error+": Coordinates unknown";
    }

    public String processCoordinates(String latitude, String longitude){
        String coordinates;
        coordinates ="Coordinates: \n" + latitude + ", \n" + longitude;
        this.coordinatesString = coordinates;
        this.coordinates = latitude +", "+ longitude;
        return coordinates;
    }





}