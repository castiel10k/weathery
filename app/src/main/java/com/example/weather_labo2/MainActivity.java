package com.example.weather_labo2;

import android.Manifest;
import android.annotation.SuppressLint;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    /**
     *initialisation des variables globales
     */
    final String OPENWEATHER_API_KEY = "fae2c7a90e8748888dc38897e7f07022";
    final String API_URL = "https://api.openweathermap.org/";
    String OPENWEATHER_ICON_URL = "https://openweathermap.org/img/wn/{XX}@4x.png";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    OkHttpClient client;
    Location currentLocation;
    Handler mainHandler = new Handler();
    WeatherPOJO weatherPOJO;
    FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;


    /**
     *  Creation de l'activite et affectation au locationRequest + fusedLocationClient.
     * Appel de la methode verify permissions.

     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        verifyPermissions();


    }

    /**
     * methode qui met a jour automatiquement l'emplacement du telephone.
     */
    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                //Log.d("onCreate0: ", "" + location.getLatitude() + "  " + location.getLongitude());
                currentLocation = location;
                //Log.d("onCreate1: ", "" + currentLocation.getLatitude() + "  " + currentLocation.getLongitude());
                activerLocalisation();
            }
        }
    };

    /**
     * Interface pour le call a l'api avec retrofit2
     */
    interface openWeatherAPIService {
        @GET("data/2.5/weather")
        Call<WeatherPOJO> getWeatherInformationByLang(@Query("appid") String api_key, @Query("units") String unitType, @Query("lat") double lat, @Query("lon") double lon);
    }

    @Override
    public void onClick(View view) {
        if (view.getId()==R.id.button) {
            verifyPermissions();
        }
    }
    /**
     * Methode pour commencer la localisation une fois
     * que les permissions necessaires sont autoriser
     *
      * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (isPermissionAuth(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    isPermissionAuth(permissions, grantResults, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                checkSettingsStartLocationUpdates();
                activerLocalisation();
            }
        }
    }

    /**
     * Methode pour verifier si les permissions ont ete autoriser
     *
     * @param permissions
     * @param grantResults
     * @param permission
     * @return
     */
    private boolean isPermissionAuth(String[] permissions, int[] grantResults, String permission) {
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].compareToIgnoreCase(permission) == 0) {
                return (grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
        }
        return false;
    }

    /**
     * Methode pour demander les permissions
     */
    private void verifyPermissions() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkSettingsStartLocationUpdates();
            activerLocalisation();
        }
    }


    /**
     * Methode qui localise du telephone et continue par appeler l'api si la location n'est pas null
     */
    @SuppressLint("MissingPermission")
    private void activerLocalisation() {
        if (fusedLocationClient != null) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        currentLocation = location;
                        //Log.d("onCreate: ", "" + currentLocation.getLatitude() + "  " + currentLocation.getLongitude());
                        fetchWeatherData();
                    }
                }
            });
        }
    }

    /**
     * Methode qui fait l'appelle a l'api et place les valeurs json dans un objet WeatherPOJO
     */
    private void fetchWeatherData() {
        client = new OkHttpClient();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL).addConverterFactory(GsonConverterFactory.create())
                .build();

        openWeatherAPIService apiService = retrofit.create(openWeatherAPIService.class);
        Call<WeatherPOJO> weatherPOJOCall = apiService.getWeatherInformationByLang(OPENWEATHER_API_KEY, "metric", currentLocation.getLatitude(), currentLocation.getLongitude());
        //Log.d("TAG", "onResponse: ConfigurationListener::" + weatherPOJOCall.request().url());

        weatherPOJOCall.enqueue(new Callback<WeatherPOJO>() {
            public void onResponse(Call<WeatherPOJO> call, retrofit2.Response<WeatherPOJO> response) {
                if (response.code() == 200) {
                    weatherPOJO = response.body();
                    afficherWeatherInformation();
                }

            }
            @Override
            public void onFailure(Call<WeatherPOJO> call, Throwable t) {

                Log.e("onFailure Call: ", "response fail");
            }

        });
    }

    /**
     * Regarder si on a les options necessaire pour faire la mise a jour automatique
     * de l'emplacement du telephone en temps reel.
     */
    private void checkSettingsStartLocationUpdates() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);

        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {

            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationUpdates();
            }
        });
        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {

            @Override
            public void onFailure(@NonNull Exception e) {
                stopLocationUpdates();
            }
        });

    }

    /**
     * Commencer la mise a jour automatique de l'emplacement
     * du telephone en temps reel lorsque on arrete l'activite.
     */
    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();

    }

    /**
     * Commencer la mise a jour automatique de l'emplacement
     * du telephone en temps reel lorsque on arrete l'activite.
     */
    @Override
    protected void onResume() {
        super.onResume();
        activerLocalisation();
       //afficherWeatherInformation();
    }

    /**
     * Terminer la mise a jour automatique de l'emplacement du telephone en temps reel.
     */
    @SuppressLint("MissingPermission")
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /**
     * Commencer la mise a jour automatique de l'emplacement du telephone en temps reel.
     */
    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * Afficher les donnees et images sur l'ecran du telephone
     */
    private void afficherWeatherInformation() {

        TextView textView = (TextView) findViewById(R.id.tempTextView);
        textView.setText(weatherPOJO.getMain().getTemp() + " °C");
        textView = (TextView) findViewById(R.id.descriptionTextView);
        textView.setText(weatherPOJO.getWeather().get(0).getDescription());
        textView = (TextView) findViewById(R.id.cityProvinceTextView);
        textView.setText(weatherPOJO.getName());

        new FetchImage(OPENWEATHER_ICON_URL.replace("{XX}", weatherPOJO.getWeather().get(0).getIcon())).start();
    }

    /**
     * Methode pour chercher une image a partir d'un lien (URL)
     */
    class FetchImage extends Thread {

        String URL;
        Bitmap bmp;

        FetchImage(String URL) {

            this.URL = URL;


        }

        @Override
        public void run() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                }
            });
               InputStream inputStream;

            try {

                inputStream = new java.net.URL(URL).openStream();
                bmp = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mainHandler.post(new Runnable() {

                @Override
                public void run() {

                    findViewById(R.id.loadingCircle).setVisibility(View.INVISIBLE);
                    ImageView view = (ImageView) findViewById(R.id.weartherIcon);
                    view.setImageBitmap(bmp);
                    findViewById(R.id.weartherIcon).setVisibility(View.VISIBLE);
                }
            });
        }
    }
}



