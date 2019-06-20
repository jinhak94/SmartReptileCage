package com.example.jinha.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapFragment extends Fragment implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{
    int count = 0;
    private static final LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2002;
    private static final int UPDATE_INTERVAL_MS = 15000;
    private static final int FASTEST_UPDATE_INTERVAL_MS = 15000;
    private GoogleMap googleMap = null;
    private MapView mapView = null;
    private GoogleApiClient googleApiClient = null;
    private Marker currentMarker = null;
    List<Marker> previous_marker = null;
    LatLng startingPoint = null;

    private String selectedPhone = "";

    private GoogleMap mGoogleMap = null;
    private final static int MAXENTRIES = 5;
    private String[] LikelyPlaceNames = null;
    private String[] LikelyAddresses = null;
    private String[] LikelyAttributions = null;
    private LatLng[] LikelyLatLngs = null;
    private Button searchHospital = null;
    public MapFragment() {
    }




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_map, container, false);
        searchHospital = layout.findViewById(R.id.search_location);
        previous_marker = new ArrayList<Marker>();

        mapView = (MapView) layout.findViewById(R.id.mapView);
        mapView.getMapAsync(this);

        // 자동완성 기능
        /*PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getActivity().getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Location location = new Location("");
                location.setLatitude(place.getLatLng().latitude);
                location.setLongitude(place.getLatLng().longitude);

                setCurrentLocation(location, place.getName().toString(), place.getAddress().toString());
            }

            @Override
            public void onPlaceSelected(com.google.android.gms.location.places.Place place) {

            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });*/


        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }


    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onLowMemory();

        if (googleApiClient != null) {
            googleApiClient.unregisterConnectionCallbacks(this);
            googleApiClient.unregisterConnectionFailedListener(this);

            if (googleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
                googleApiClient.disconnect();
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //액티비티가 처음 생성될 때 실행되는 함수
        MapsInitializer.initialize(Objects.requireNonNull(getActivity()).getApplicationContext());

        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
        }
    }


    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {


        if (currentMarker != null) currentMarker.remove();

        if (location != null) {
            //현재위치의 위도 경도 가져옴
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(currentLocation);
            markerOptions.title(markerTitle);
            markerOptions.snippet(markerSnippet);
            markerOptions.draggable(true);
            //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            currentMarker = this.googleMap.addMarker(markerOptions);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        }

        /*MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        currentMarker = this.googleMap.addMarker(markerOptions);

        this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(DEFAULT_LOCATION));
*/
    }

    // 구글맵 API

    public double distance(double P1_latitude, double P1_longitude,
                           double P2_latitude, double P2_longitude) {
        if ((P1_latitude == P2_latitude) && (P1_longitude == P2_longitude)) {
            return 0;
        }

        double e10 = P1_latitude * Math.PI / 180;
        double e11 = P1_longitude * Math.PI / 180;
        double e12 = P2_latitude * Math.PI / 180;
        double e13 = P2_longitude * Math.PI / 180;

        /* 타원체 GRS80 */
        double c16 = 6356752.314140910;
        double c15 = 6378137.000000000;
        double c17 = 0.0033528107;

        double f15 = c17 + c17 * c17;
        double f16 = f15 / 2;
        double f17 = c17 * c17 / 2;
        double f18 = c17 * c17 / 8;
        double f19 = c17 * c17 / 16;

        double c18 = e13 - e11;
        double c20 = (1 - c17) * Math.tan(e10);
        double c21 = Math.atan(c20);
        double c22 = Math.sin(c21);
        double c23 = Math.cos(c21);
        double c24 = (1 - c17) * Math.tan(e12);
        double c25 = Math.atan(c24);
        double c26 = Math.sin(c25);
        double c27 = Math.cos(c25);

        double c29 = c18;
        double c31 = (c27 * Math.sin(c29) * c27 * Math.sin(c29))
                + (c23 * c26 - c22 * c27 * Math.cos(c29))
                * (c23 * c26 - c22 * c27 * Math.cos(c29));
        double c33 = (c22 * c26) + (c23 * c27 * Math.cos(c29));
        double c35 = Math.sqrt(c31) / c33;
        double c36 = Math.atan(c35);
        double c38 = 0;
        if (c31 == 0) {
            c38 = 0;
        } else {
            c38 = c23 * c27 * Math.sin(c29) / Math.sqrt(c31);
        }

        double c40 = 0;
        if ((Math.cos(Math.asin(c38)) * Math.cos(Math.asin(c38))) == 0) {
            c40 = 0;
        } else {
            c40 = c33 - 2 * c22 * c26
                    / (Math.cos(Math.asin(c38)) * Math.cos(Math.asin(c38)));
        }

        double c41 = Math.cos(Math.asin(c38)) * Math.cos(Math.asin(c38))
                * (c15 * c15 - c16 * c16) / (c16 * c16);
        double c43 = 1 + c41 / 16384
                * (4096 + c41 * (-768 + c41 * (320 - 175 * c41)));
        double c45 = c41 / 1024 * (256 + c41 * (-128 + c41 * (74 - 47 * c41)));
        double c47 = c45
                * Math.sqrt(c31)
                * (c40 + c45
                / 4
                * (c33 * (-1 + 2 * c40 * c40) - c45 / 6 * c40
                * (-3 + 4 * c31) * (-3 + 4 * c40 * c40)));
        double c50 = c17
                / 16
                * Math.cos(Math.asin(c38))
                * Math.cos(Math.asin(c38))
                * (4 + c17
                * (4 - 3 * Math.cos(Math.asin(c38))
                * Math.cos(Math.asin(c38))));
        double c52 = c18
                + (1 - c50)
                * c17
                * c38
                * (Math.acos(c33) + c50 * Math.sin(Math.acos(c33))
                * (c40 + c50 * c33 * (-1 + 2 * c40 * c40)));

        double c54 = c16 * c43 * (Math.atan(c35) - c47);

        // return distance in meter
        return c54;
    }

    public short bearingP1toP2(double P1_latitude, double P1_longitude, double P2_latitude, double P2_longitude)
    {
        // 현재 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        double Cur_Lat_radian = P1_latitude * (3.141592 / 180);
        double Cur_Lon_radian = P1_longitude * (3.141592 / 180);


        // 목표 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        double Dest_Lat_radian = P2_latitude * (3.141592 / 180);
        double Dest_Lon_radian = P2_longitude * (3.141592 / 180);

        // radian distance
        double radian_distance = 0;
        radian_distance = Math.acos(Math.sin(Cur_Lat_radian) * Math.sin(Dest_Lat_radian)
                + Math.cos(Cur_Lat_radian) * Math.cos(Dest_Lat_radian) * Math.cos(Cur_Lon_radian - Dest_Lon_radian));

        // 목적지 이동 방향을 구한다.(현재 좌표에서 다음 좌표로 이동하기 위해서는 방향을 설정해야 한다. 라디안값이다.
        double radian_bearing = Math.acos((Math.sin(Dest_Lat_radian) - Math.sin(Cur_Lat_radian)
                * Math.cos(radian_distance)) / (Math.cos(Cur_Lat_radian) * Math.sin(radian_distance)));

        // acos의 인수로 주어지는 x는 360분법의 각도가 아닌 radian(호도)값이다.

        double true_bearing = 0;
        if (Math.sin(Dest_Lon_radian - Cur_Lon_radian) < 0)
        {
            true_bearing = radian_bearing * (180 / 3.141592);
            true_bearing = 360 - true_bearing;
        }
        else
        {
            true_bearing = radian_bearing * (180 / 3.141592);
        }

        return (short)true_bearing;
    }
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        // OnMapReadyCallback implements 해야 mapView.getMapAsync(this); 사용가능. this 가 OnMapReadyCallback
        final Location location = new Location("");
        //LatLng는 위도와 경도를 저장하는 자료형
        //location.getLatitude()와 location.getLongitude()는 현재 위도와 경도를 받아오는 메소드
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

        this.googleMap = googleMap;

        Location firstLoc= new Location("초기값");
        firstLoc.setLatitude(37.492706);
        firstLoc.setLongitude(127.128098);


        final LatLng[] lat = new LatLng[10];
        lat[0] = new LatLng(37.328938, 127.076683);
        lat[1] = new LatLng(35.135524, 129.090830);
        lat[2] = new LatLng(35.201373, 129.098787);
        lat[3] = new LatLng(37.276156, 126.978561);
        lat[4] = new LatLng(37.495380, 127.052714);
        lat[5] = new LatLng(37.513755, 127.061942);
        lat[6] = new LatLng(37.654688, 126.777736);
        lat[7] = new LatLng(37.478240, 126.931822);
        lat[8] = new LatLng(37.503729, 127.020793);
        lat[9] = new LatLng(37.544272, 127.015376);

        String[] hospital = new String[10];
        hospital[0] = "어울림동물병원";
        hospital[1] = "UN 동물 의료센터";
        hospital[2] = "레알동물병원";
        hospital[3] = "이지훈 동물병원";
        hospital[4] = "고려종합동물병원";
        hospital[5] = "아크리스동물병원";
        hospital[6] = "가람동물병원";
        hospital[7] = "한성동물병원";
        hospital[8] = "대인종합동물병원";
        hospital[9] = "오석헌 동물병원";



        String[] snippet = new String[10];
        snippet[0] = "파충류 수술 전문";
        snippet[1] = "부산 특수 동물병원";
        snippet[2] = "특수동물수술전문";
        snippet[3] = "애견미용 및 특수진료";
        snippet[4] = "조류 파충류 진료";
        snippet[5] = "특수동물을 진료한지 제일 오래된 동물병원";
        snippet[6] = "특수동물 예방접종 및 치료";
        snippet[7] = "파충류 전문 진료";
        snippet[8] = "거북 및 파충류 진료, 치료";
        snippet[9] = "작은 동물부터 큰 동물까지";

        final String[] address = new String[10];
        address[0] = "수지구 신봉동 955-1번지 1층 전체 용인시 경기도 KR";
        address[1] = "부산광역시 남구 수영로 221";
        address[2] = "부산광역시 동래구 안락동";
        address[3] = "경기도 수원시 권선구 구운동 507-5";
        address[4] = "서울특별시 강남구 도곡동 527";
        address[5] = "서울특별시 강남구 삼성동 160-21";
        address[6] = "경기도 고양시 일산동구 마두동 796";
        address[7] = "서울특별시 관악구 신림동 409-165";
        address[8] = "서울특별시 서초구 반포1동 745-6";
        address[9] = "서울특별시 성동구 독서당로 223 래미안옥수리버젠 상가동 1층";

        final String[] phoneNum = new String[10];
        phoneNum[0] = "031-548-2911";
        phoneNum[1] = "051-624-2475";
        phoneNum[2] = "051-524-8275";
        phoneNum[3] = "031-295-5975";
        phoneNum[4] = "02-575-7902";
        phoneNum[5] = "02-583-7582";
        phoneNum[6] = "031-906-0976";
        phoneNum[7] = "02-872-7609";
        phoneNum[8] = "02-543-9538";
        phoneNum[9] = "02-6402-0301";

        //런타임 퍼미션 요청 대화상자나 GPS 활성 요청 대화상자 보이기전에 지도의 초기위치를 서울로 이동
        setCurrentLocation(firstLoc, "에코특수동물병원", "도마뱀 식욕부진 치료");

        // 상점 좌표를 통해 마커 지정
        MarkerOptions[] marker = new MarkerOptions[10];
        for(int i=0; i<10; i++){
            marker[i] = new MarkerOptions();
            marker[i].position(lat[i])
                    .title(hospital[i])
                    .snippet(snippet[i]);
            googleMap.addMarker(marker[i]).showInfoWindow();
        }

        // 최단거리 계산

        double min = distance(currentLocation.latitude,currentLocation.longitude,
                lat[0].latitude, lat[0].longitude);
        for(int i=1;i<10;i++) {
            if (min > distance(currentLocation.latitude,currentLocation.longitude,
                    lat[i].latitude, lat[i].longitude)) {
                min = distance(currentLocation.latitude,currentLocation.longitude,
                        lat[i].latitude, lat[i].longitude);
                count = i;
            }
        }

        searchHospital.setOnClickListener(new View.OnClickListener() {
            private FragmentManager supportFragmentManager;

            public FragmentManager getSupportFragmentManager() {
                return supportFragmentManager;
            }

            @Override
            public void onClick(View v) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(lat[count]));
            }
        });

       /*MarkerOptions marker = new MarkerOptions();
        marker.position(lat[0])
                .title(hospital[0])
                .snippet(snippet[0]);
        googleMap.addMarker(marker).showInfoWindow();

        MarkerOptions marker1 = new MarkerOptions();
        marker1.position(lat[1])
                .title(hospital[1])
                .snippet(snippet[1]);
        googleMap.addMarker(marker1).showInfoWindow();


        MarkerOptions marker2 = new MarkerOptions();
        marker2.position(lat[2])
                .title(hospital[2])
                .snippet(snippet[2]);
        googleMap.addMarker(marker2).showInfoWindow();

        MarkerOptions marker3 = new MarkerOptions();
        marker3.position(lat[3])
                .title(hospital[3])
                .snippet(snippet[3]);
        googleMap.addMarker(marker3).showInfoWindow();*/

        //나침반이 나타나도록 설정
        googleMap.getUiSettings().setCompassEnabled(true);
        // 매끄럽게 이동함
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        /*for (int idx = 0; idx < 10; idx++) {
            // 1. 마커 옵션 설정 (만드는 과정)
            MarkerOptions makerOptions = new MarkerOptions();
            makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
                    .position(new LatLng(37.52487 + idx, 126.92723))
                    .title("마커" + idx); // 타이틀.

            // 2. 마커 생성 (마커를 나타냄)
            googleMap.addMarker(makerOptions);
        }*/
        //  API 23 이상이면 런타임 퍼미션 처리 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 사용권한체크
            int hasFineLocationPermission = ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.ACCESS_FINE_LOCATION);

            if (hasFineLocationPermission == PackageManager.PERMISSION_DENIED) {
                //사용권한이 없을경우
                //권한 재요청
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            } else {
                //사용권한이 있는경우
                if (googleApiClient == null) {
                    buildGoogleApiClient();
                }

                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(true);
                }
            }
        } else {

            if (googleApiClient == null) {
                buildGoogleApiClient();
            }

            googleMap.setMyLocationEnabled(true);
        }

        /*// 마커 말풍선 클릭 리스너
        GoogleMap.OnInfoWindowClickListener infoWindowClickListener = new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                String markerId = marker.getId();
                Toast.makeText(getActivity(), "정보창 클릭 Marker ID : "+markerId, Toast.LENGTH_SHORT).show();
            }
        };*/



        // 마커 클릭 리스너
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            public boolean onMarkerClick(Marker marker) {

                /*String text = "[마커 클릭 이벤트] latitude ="
                        + marker.getPosition();
                        //.latitude + , longitude ="
                       // + marker.getPosition().longitude;
                Toast.makeText(getActivity(), text, Toast.LENGTH_LONG)
                        .show();*/

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setPositiveButton("전화 걸기",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:"+selectedPhone));
                                startActivity(intent);
                            }
                        });
                builder.setNegativeButton("닫기",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                // 추후 수정 필요...
                switch(marker.getSnippet()){
                    case "도마뱀 식욕부진 치료":
                        builder.setTitle("에코특수동물병원");
                        builder.setMessage("서울특별시 송파구 가락동 137-3 청공 빌딩 101 호" + "\n" + "02-443-2222");
                        selectedPhone = "02-443-2222";
                        break;
                    case "파충류 수술 전문":
                        builder.setTitle("어울림동물병원");
                        builder.setMessage(address[0] + "\n" + phoneNum[0]);
                        selectedPhone = phoneNum[0];
                        break;
                    case "부산 특수 동물병원":
                        builder.setTitle("UN 동물 의료센터");
                        builder.setMessage(address[1] + "\n" + phoneNum[1]);
                        selectedPhone = phoneNum[1];
                        break;
                    case "특수동물수술전문":
                        builder.setTitle("레알동물병원");
                        builder.setMessage(address[2] + "\n" + phoneNum[2]);
                        selectedPhone = phoneNum[2];
                        break;
                    case "애견미용 및 특수진료":
                        builder.setTitle("이지훈 동물병원");
                        builder.setMessage(address[3] + "\n" + phoneNum[3]);
                        selectedPhone = phoneNum[3];
                        break;
                    case "조류 파충류 진료":
                        builder.setTitle("고려종합동물병원");
                        builder.setMessage(address[4] + "\n" + phoneNum[4]);
                        selectedPhone = phoneNum[4];
                        break;
                    case "특수동물을 진료한지 제일 오래된 동물병원":
                        builder.setTitle("아크리스동물병원");
                        builder.setMessage(address[5] + "\n" + phoneNum[5]);
                        selectedPhone = phoneNum[5];
                        break;
                    case "특수동물 예방접종 및 치료":
                        builder.setTitle("가람동물병원");
                        builder.setMessage(address[6] + "\n" + phoneNum[6]);
                        selectedPhone = phoneNum[6];
                        break;
                    case "파충류 전문 진료":
                        builder.setTitle("한성동물병원");
                        builder.setMessage(address[7] + "\n" + phoneNum[7]);
                        selectedPhone = phoneNum[7];

                        break;
                    case "거북 및 파충류 진료, 치료":
                        builder.setTitle("대인종합동물병원");
                        builder.setMessage(address[8] + "\n" + phoneNum[8]);
                        selectedPhone = phoneNum[8];
                        break;
                    case "작은 동물부터 큰 동물까지":
                        builder.setTitle("오석헌 동물병원");
                        builder.setMessage(address[9] + "\n" + phoneNum[9]);
                        selectedPhone = phoneNum[9];
                        break;
                    default:
                        break;
                }

                builder.show();
                return false;
            }
        });
    }

    private double distanceBetween(LatLng currentLocation, LatLng latLng) {
        return 0;
    }


    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(Objects.requireNonNull(getActivity()))
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(getActivity(), this)
                .build();
        googleApiClient.connect();
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) Objects.requireNonNull(getActivity()).getSystemService(Context.LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    // 사용 권한
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (!checkLocationServicesStatus()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
            builder.setTitle("위치 서비스 비활성화");
            builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n" +
                    "위치 설정을 수정하십시오.");
            builder.setCancelable(true);
            builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent callGPSSettingIntent =
                            new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
                }
            });
            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.create().show();
        }

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL_MS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                LocationServices.FusedLocationApi
                        .requestLocationUpdates(googleApiClient, locationRequest, this);
            }
        } else {
            LocationServices.FusedLocationApi
                    .requestLocationUpdates(googleApiClient, locationRequest, this);

            this.googleMap.getUiSettings().setCompassEnabled(true);
            this.googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }

    }

    @Override
    public void onConnectionSuspended(int cause) {
        if (cause == CAUSE_NETWORK_LOST)
            Log.e(TAG, "onConnectionSuspended(): Google Play services " +
                    "connection lost.  Cause: network lost.");
        else if (cause == CAUSE_SERVICE_DISCONNECTED)
            Log.e(TAG, "onConnectionSuspended():  Google Play services " +
                    "connection lost.  Cause: service disconnected");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Location location = new Location("");
        location.setLatitude(DEFAULT_LOCATION.latitude);
        location.setLongitude((DEFAULT_LOCATION.longitude));

        setCurrentLocation(location, "위치정보 가져올 수 없음",
                "위치 퍼미션과 GPS활성 여부 확인");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "onLocationChanged call..");
        //searchCurrentPlaces();
    }

    private void searchCurrentPlaces() {
        @SuppressWarnings("MissingPermission")
        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                .getCurrentPlace(googleApiClient, null);
        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {

            @Override
            public void onResult(@NonNull PlaceLikelihoodBuffer placeLikelihoods) {
                int i = 0;
                LikelyPlaceNames = new String[MAXENTRIES];
                LikelyAddresses = new String[MAXENTRIES];
                LikelyAttributions = new String[MAXENTRIES];
                LikelyLatLngs = new LatLng[MAXENTRIES];

                for (PlaceLikelihood placeLikelihood : placeLikelihoods) {
                    LikelyPlaceNames[i] = (String) placeLikelihood.getPlace().getName();
                    LikelyAddresses[i] = (String) placeLikelihood.getPlace().getAddress();
                    LikelyAttributions[i] = (String) placeLikelihood.getPlace().getAttributions();
                    LikelyLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                    i++;
                    if (i > MAXENTRIES - 1) {
                        break;
                    }
                }

                placeLikelihoods.release();

                Location location = new Location("");
                location.setLatitude(LikelyLatLngs[0].latitude);
                location.setLongitude(LikelyLatLngs[0].longitude);

                setCurrentLocation(location, LikelyPlaceNames[0], LikelyAddresses[0]);
            }
        });
    }
}