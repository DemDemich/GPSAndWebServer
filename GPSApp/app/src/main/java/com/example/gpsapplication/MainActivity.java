package com.example.gpsapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.INTERNET;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList();
    private final String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET};

    private final static int ALL_PERMISSIONS_RESULT = 101;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private Location loc;
    TextView lat;
    TextView lon;
    TextView responseText_tv;
    double longitude;
    double latitude;

    String responseText;
    private Handler mHandler;

    private static final String URL = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=:lat&longitude=:lon&localityLanguage=ru";
    OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        client = new OkHttpClient();

        mHandler = new Handler(Looper.getMainLooper());


        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.
        lat = findViewById(R.id.Lat_tv);
        lon = findViewById(R.id.Lon_tv);

        responseText_tv = findViewById(R.id.responseText_tv);

        callPermissions();



    }

    public void callPermissions() {
        String rationale = "Please provide location permission so that you can ...";
        Permissions.Options options = new Permissions.Options()
                .setRationaleDialogTitle("Info")
                .setSettingsDialogTitle("Warning");

        Permissions.check(this/*context*/, permissions, rationale, options, new PermissionHandler() {
            @Override
            public void onGranted() {
                requestLocationUpdate();
                Button btn = findViewById(R.id.btn);

                //лисенер на кнопку
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Toast.makeText(getApplicationContext(), "TEST", Toast.LENGTH_LONG).show();
                        if(latitude != 0 && longitude != 0) {
                            String requestStr = URL.replace(":lat", Double.toString(latitude)).replace(":lon", Double.toString(longitude));
                            run(requestStr, new Callback() {
                                @Override
                                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                    Log.d("TEST", e.getMessage());
                                }

                                @Override
                                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                    responseText = response.body().string();
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            responseText_tv.setText(responseText);
                                        }
                                    });
                                    Log.d("TEST", responseText);
                                }
                            });

                        } else {
                            Toast.makeText(getApplicationContext(), "Подождите пока получим данные о вашем местоположении", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }

            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                callPermissions();
            }
        });
    }

    Call run(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    public void requestLocationUpdate() {
        fusedLocationClient = new FusedLocationProviderClient(this);
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(2000).setInterval(4000);
        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                loc = locationResult.getLastLocation();
                latitude = loc.getLatitude();
                longitude = loc.getLongitude();
                lat.setText("Lat: " + latitude);
                lon.setText("Lat: " + longitude);
                Log.d("TEST", String.valueOf(latitude) + " " + String.valueOf(longitude));
            }
        }, getMainLooper());
    }

    private ArrayList findUnAskedPermissions(List<String> wanted) {
        ArrayList result = new ArrayList();

        for (Object perm : wanted) {
            if (!hasPermission(perm.toString())) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }
                }
                break;
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();


    }
}
