package com.ivanohey.signalmappingapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SaveCoordinatesService extends Service {
    String url = "https://cda0-129-194-90-8.eu.ngrok.io";
    JSONObject requestObject = new JSONObject();

    //We create LocalBinder instance
    public LocalBinder localBinder = new LocalBinder();

    //Constructor
    public SaveCoordinatesService(){}

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    public void sendRecord(){
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(createRequest());
    }

    public void createJSONObjectRequest(String latitude, String longitude, String provider, String networkType) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("latitude", latitude);
            jsonBody.put("longitude", longitude);
            jsonBody.put("provider", provider);
            jsonBody.put("network", networkType);
            this.requestObject = jsonBody;
            Log.d("JSON object created: ", jsonBody.toString());

        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public JsonObjectRequest createRequest(){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url + "/records", requestObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("REQUEST SENT", response.toString());
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
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String>  params = new HashMap<String, String>();
            params.put("Content-Type", "application/json");
            return params;
        }};
        Log.d("DEBUG INFO", "JSON object request created");
        return jsonObjectRequest;
    }

    public String showServiceStatus(){
        return "saveCoordinates service working !";
    }

    public void createSignalRecord(String latitude, String longitude, String signalType, String Provider){
        //Implement function to POST data on backend


    }

    public class LocalBinder extends Binder {
        //We define a getBoundService method returning a SaveCoordinatesService instance. This method will be called from the activity when binding the service
        SaveCoordinatesService getBoundService(){
            return SaveCoordinatesService.this;
        }
    }


}