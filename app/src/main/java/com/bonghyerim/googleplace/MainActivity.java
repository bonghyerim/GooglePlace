package com.bonghyerim.googleplace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.bonghyerim.googleplace.api.NetworkClient;
import com.bonghyerim.googleplace.api.PlaceApi;
import com.bonghyerim.googleplace.config.Config;
import com.bonghyerim.googleplace.model.Place;
import com.bonghyerim.googleplace.model.PlaceList;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {

    EditText editKeyword;
    ImageView btnSearch;

    ArrayList<Place> placeArrayList = new ArrayList<>();

    // 내 위치 가져오기 위한 멤버변수
    LocationManager locationManager;
    LocationListener locationListener;

    double lat;
    double lng;

    int radius = 2000;  // 미터 단위
    String keyword;

    boolean isLocationReady;

    String pagetoken;
    SupportMapFragment mapFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editKeyword = findViewById(R.id.editKeyword);
        btnSearch = findViewById(R.id.btnSearch);

        // 폰의 위치를 가져오기 위해서는, 시스템서비스로부터 로케이션 매니져를
        // 받아온다.
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // 로케이션 리스터를 만든다.
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                lat = location.getLatitude();
                lng = location.getLongitude();

                if (!isLocationReady) { // 위치가 처음 설정될 때만 초기화
                    isLocationReady = true;

                    mapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            // 초기 위치로 이동
                            LatLng initialLatLng = new LatLng(lat, lng);
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, 15));

                            // 내 위치에 마커 추가
                            googleMap.addMarker(new MarkerOptions().position(initialLatLng).title("내 위치"));

                            // 처음 앱 실행시에는 빈 ArrayList이므로 마커는 추가하지 않습니다.
                            // 검색 결과 마커는 검색 버튼 클릭시에 추가됩니다.
                        }
                    });
                }
            }
        };


        mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                // 위치 권한이 부여되지 않았다면 초기 위치를 지정할 수 없음
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    LatLng defaultLatLng = new LatLng(0, 0);
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 1)); // 맵을 움직이지 않도록 초기 위치 설정
                    return;
                }

                // 초기 위치로 이동
                LatLng initialLatLng = new LatLng(lat, lng);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, 17));

                // 내 위치에 마커 추가
                googleMap.addMarker(new MarkerOptions().position(initialLatLng).title("내 위치"));

                // 처음 앱 실행시에는 빈 ArrayList이므로 마커는 추가하지 않습니다.
                // 검색 결과 마커는 검색 버튼 클릭시에 추가됩니다.
            }
        });



        if( ActivityCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ){

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION} ,
                    100);
            return;
        }

        // 위치기반 허용하였으므로,
        // 로케이션 매니저에, 리스너를 연결한다. 그러면 동작한다.
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                3000,
                -1,
                locationListener);

        // 맵 초기화 및 설정





        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(isLocationReady == false){
                    Snackbar.make(btnSearch,
                            "아직 위치를 잡지 못했습니다. 잠시후 다시 검색하세요.",
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                keyword = editKeyword.getText().toString().trim();

                Log.i("AAA", keyword);

                if(keyword.isEmpty()){
                    Log.i("AAA", "isEmpty");
                    return;
                }

                getNetworkData();

            }
        });

    }

    private void addNetworkData() {


        Retrofit retrofit = NetworkClient.getRetrofitClient(MainActivity.this);
        PlaceApi api = retrofit.create(PlaceApi.class);

        Call<PlaceList> call = api.getPlaceList("ko",
                lat+","+lng,
                radius,
                Config.GOOGLE_API_KEY,
                keyword);

        call.enqueue(new Callback<PlaceList>() {
            @Override
            public void onResponse(Call<PlaceList> call, Response<PlaceList> response) {


                if(response.isSuccessful()){

                    PlaceList placeList = response.body();

                    pagetoken = placeList.next_page_token;

                    placeArrayList.addAll( placeList.results );



                    
                    mapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            googleMap.clear(); // 기존 마커들 제거
                            LatLng initialLatLng = new LatLng(lat, lng);
                            googleMap.addMarker(new MarkerOptions().position(initialLatLng).title("내 위치"));

                            for (Place place : placeList.results) {
                                LatLng placeLatLng = new LatLng(
                                        place.geometry.location.lat,
                                        place.geometry.location.lng);

                                String title = place.name;
                                if (place.opening_hours != null) {
                                    if (place.opening_hours.open_now) {
                                        title += " - 영업 중";
                                    } else {
                                        title += " - 영업 종료";
                                    }
                                } else {
                                    title += " - 영업 정보 없음";
                                }

                                googleMap.addMarker(new MarkerOptions()
                                        .position(placeLatLng)
                                        .title(title)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            }
                        }
                    });


                } else {

                }
            }

            @Override
            public void onFailure(Call<PlaceList> call, Throwable t) {

            }
        });
    }


    private void getNetworkData() {
        Log.i("AAA", "getNetworkData");



        placeArrayList.clear();

        Retrofit retrofit = NetworkClient.getRetrofitClient(MainActivity.this);
        PlaceApi api = retrofit.create(PlaceApi.class);

        Call<PlaceList> call = api.getPlaceList("ko",
                lat+","+lng,
                radius,
                Config.GOOGLE_API_KEY,
                keyword);

        call.enqueue(new Callback<PlaceList>() {
            @Override
            public void onResponse(Call<PlaceList> call, Response<PlaceList> response) {



                if(response.isSuccessful()){

                    PlaceList placeList = response.body();

                    pagetoken = placeList.next_page_token;

                    placeArrayList.addAll(placeList.results);



                    // 기존 마커들 제거
                    // 기존 마커들 제거
                    mapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            googleMap.clear(); // 기존 마커들 제거
                            LatLng initialLatLng = new LatLng(lat, lng);
                            googleMap.addMarker(new MarkerOptions().position(initialLatLng).title("내 위치"));

                            for (Place place : placeList.results) {
                                LatLng placeLatLng = new LatLng(
                                        place.geometry.location.lat,
                                        place.geometry.location.lng);

                                String title = place.name;
                                if (place.opening_hours != null) {
                                    if (place.opening_hours.open_now) {
                                        title += " - 영업 중";
                                    } else {
                                        title += " - 영업 종료";
                                    }
                                } else {
                                    title += " - 영업 정보 없음";
                                }

                                googleMap.addMarker(new MarkerOptions()
                                        .position(placeLatLng)
                                        .title(title)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            }
                        }
                    });


                } else {

                }
            }

            @Override
            public void onFailure(Call<PlaceList> call, Throwable t) {

            }
        });

    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 100){

            if( ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED ){

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION} ,
                        100);
                return;
            }

            // 위치기반 허용하였으므로,
            // 로케이션 매니저에, 리스너를 연결한다. 그러면 동작한다.
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    3000,
                    -1,
                    locationListener);

        }

    }







}