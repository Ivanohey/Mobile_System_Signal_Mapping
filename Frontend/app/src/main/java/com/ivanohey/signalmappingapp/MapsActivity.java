package com.ivanohey.signalmappingapp;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ivanohey.signalmappingapp.databinding.ActivityMapsBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    protected String BACKEND_URL = "https://cda0-129-194-90-8.eu.ngrok.io";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
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
        //Get the bundle
        Bundle bundle = getIntent().getExtras();
        String currentCoordinates = bundle.getString("coordinates");
        //double currentLatitude = Double.parseDouble(bundle.getString("coordinates"));
        String latLong[] = currentCoordinates.split(", ");
        double currentLatitude = Double.parseDouble(latLong[0]);
        double currentLongitude = Double.parseDouble(latLong[1]);
        mMap = googleMap;
        LatLng currentLocationMarker = new LatLng(currentLatitude, currentLongitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocationMarker,18));
        //Get the bundle

        getAllRecords();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getAllRecords();
    }

    public void getAllRecords(){
        //HTTP GET METHOD
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(recordsJsonObjectRequest);
    }

    public void loadAllRecords(JSONObject response) {
        String latitude;
        String longitude;
        String provider;
        String network;
        try{
            JSONArray records = response.getJSONArray("records");
            Log.i("JSON ARRAY", records.toString());
            for (int i = 0; i < records.length(); i++ ) {
                latitude = records.getJSONObject(i).getString("latitude");
                longitude = records.getJSONObject(i).getString("longitude");
                provider = records.getJSONObject(i).getString("provider");
                network = records.getJSONObject(i).getString("network");

                int icon;

                //From here, create markers on Gmap for all records
                switch (network){
                    case "3G":
                        icon = R.drawable.ic_red_gmap;
                        break;
                    case "4G":
                        icon = R.drawable.ic_yellow_gmap;
                        break;
                    case "5G":
                        icon = R.drawable.ic_green_gmap;
                        break;
                    default:
                        icon = R.drawable.ic_blue_gmap;
                        break;
                }
                //We create marker for every record
                mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)))
                                .title(provider + " - " +network)
                                .icon(BitmapFromVector(getApplicationContext(), icon)))
                        .setAlpha(0.5f);
            }

        } catch (Exception e){
            e.printStackTrace();
        }

    }

    //Request to get all records
    public JsonObjectRequest recordsJsonObjectRequest = new JsonObjectRequest(Request.Method.GET, BACKEND_URL + "/records", null,new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.d("RESPONSE OF SERVER", response.toString());
            try{
                loadAllRecords(response);
            }
            catch (Exception e){
                Log.e("ERROR RESPONSE", e.getMessage());
            }

        }
    }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            try {
                if (error instanceof TimeoutError) {
                    Log.e("Request error", error.getMessage());
                } else if(error instanceof NoConnectionError){
                    Log.e("Request error", error.getMessage());
                } else if (error instanceof AuthFailureError) {
                    Log.e("Request error", error.getMessage());
                } else if (error instanceof ServerError) {
                    Log.e("Request error", error.getMessage());
                } else if (error instanceof NetworkError) {
                    Log.e("Request error", error.getMessage());
                } else if (error instanceof ParseError) {
                    Log.e("Request error", error.getMessage());
                }else{
                    Log.e("Request error", "Unknown error");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });


    //Converting vector image to Bitmap
    private BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}