package com.example.map_google

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mGoogleMap: GoogleMap

    //위치 서비스가 gps를 사용해서 위치를 확인
    lateinit var fusedLocationClient: FusedLocationProviderClient

    //위치 값 요청에 대한 갱신 정보를 받는 변수
    lateinit var locationCallback: LocationCallback
    lateinit var locationPermission: ActivityResultLauncher<Array<String>>

    // 내위치 정보
    lateinit var current_latLng: LatLng
    // 내 위치 고정 버튼 표시
    private lateinit var toggleButton: FloatingActionButton
    private var b_isOn = true // 초기 상태 설정

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // activity_main을 현재 액티비티 뷰 설정
        setContentView(R.layout.activity_main)
        // 위치 권한 요청 결과를 처리하는 코드
        locationPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            // 모든 권한이 승인되었는지 확인
            if (results.all { it.value }) {
                // 승인 - 구글맵을 로드
                (supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment)!!.getMapAsync(
                    this
                )
            } else { // 거부 - 권한 승인이 필요하다는 메시지 표시
                Toast.makeText(this, "권한 승인이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }
        //권한 요청
        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
        // 버튼 처리 코드
        toggleButton = findViewById(R.id.toggleButton)
        toggleButton.setOnClickListener {
            // 버튼 클릭 시 상태 전환
            // b_isOn = !b_isOn
            // 카메라 재이동
            refreshCarmera()
        }
        
    }

    // 지도 객체를 이용할 수 있는 상황이 될 때
    override fun onMapReady(p0: GoogleMap) {

        //val seoul = LatLng(37.566610, 126.978403)
        val chungnam_univ = LatLng(36.36652, 127.3444)
        mGoogleMap = p0
        mGoogleMap.mapType = GoogleMap.MAP_TYPE_NORMAL // default 노말 생략 가능
//        mGoogleMap.apply {
//            val markerOptions = MarkerOptions()
//            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
//            markerOptions.position(seoul)
//            markerOptions.title("서울시청")
//            markerOptions.snippet("Tel:01-120")
//            addMarker(markerOptions)
//        }
        // 위치 정보를 얻기 위한 초기화 작업
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // 시작 위치 세팅 : 공대 5호관 - GPS가 수신되지 않았을 때 보여주는 위치
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chungnam_univ, 15f))
        updateLocation()
    }

    fun updateLocation() {

        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            //1초에 한번씩 변경된 위치 정보가 onLocationResult 으로 전달된다.
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult?.let {
                    // 여기서 위치를 받아옴
                    for (location in it.locations) {
                        Log.d("내 위치정보", "위도: ${location.latitude} 경도: ${location.longitude}")
                        val db = Firebase.firestore
                        db.collection("rooms").document("QyFo5Z9zejFqEhydbL2c").update("영탁.lat", location.latitude)
                        db.collection("rooms").document("QyFo5Z9zejFqEhydbL2c").update("영탁.lng", location.longitude)
                        setLastLocation(location) //계속 실시간으로 위치를 받아오고 있기 때문에 맵을 확대해도 다시 줄어든다.
                        // 상대방의 위치 정보도 세팅
                        setOtherlocation()
                    }
                }
            }
        }
        //권한 처리
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { // 권한이 없을때 메시지 출력 + 함수 종료
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback,
            Looper.myLooper()!!
        )
    }

    //
    var setting_initview = true

    // 마커 객체를 저장할 변수를 선언
    var currentLocationMarker: Marker? = null
    var other_currentLocationMarker: Marker? = null

    fun setLastLocation(lastLocation: Location) {
        val latLng = LatLng(lastLocation.latitude, lastLocation.longitude)
        current_latLng = latLng
        // 처음 마커를 추가하는 경우
        if (currentLocationMarker == null) {
            val markerOptions = MarkerOptions().position(latLng).title("나")
            currentLocationMarker = mGoogleMap.addMarker(markerOptions)
            currentLocationMarker?.showInfoWindow() // 항상 title이 보이도록 설정
        } else {
            // 이미 마커가 있으면 위치만 업데이트
            currentLocationMarker?.position = latLng
        }
        // 처음 시작에만 카메라가 이동되도록
        if (setting_initview) {
            val cameraPosition = CameraPosition.Builder().target(latLng).zoom(15.0f).build()
            mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            setting_initview = false
        }
    }
    // "내 위치" 버튼을 눌렀을때 실행
    fun refreshCarmera() {
        val cameraPosition = CameraPosition.Builder().target(current_latLng).zoom(15.0f).build()
        mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }
    fun setOtherlocation() {
        // 나와 상대 정보
        var my_data: Any
        var partner_data: Any

        // Firebase 데이터 가져오기
        val db = Firebase.firestore
        var roomData: Map<String, Any>
        var other_data: Map<String, Any> = mapOf("coat" to "미확인", "pants" to "미확인", "lat" to 0.0, "lng" to 0.0)
        db.collection("rooms").document("QyFo5Z9zejFqEhydbL2c").get()
            .addOnSuccessListener { document ->
                if (document.data != null) {
                    roomData = document.data!!
                    Log.d("방 정보", "${roomData}")
                    roomData.forEach { (name, attributes) ->
                        Log.d("roomData", "${name}")
                        if (name != "영탁") {
                            other_data = attributes as Map<String, Any>

                            Log.d("상대방 정보", "${other_data}")
                            // 이부분을 DB에서 가져온 값으로 변경
                            val other_latLng = LatLng(other_data.get("lat") as Double, other_data.get("lng") as Double)
                            Log.d("상대방 위치", "${other_latLng}")
                            if (other_currentLocationMarker == null) {
                                // 상대편 -> DB에서 가져온 이름으로 변경
                                val markerOptions = MarkerOptions().position(other_latLng).title(name)
                                other_currentLocationMarker = mGoogleMap.addMarker(markerOptions)
                                other_currentLocationMarker?.showInfoWindow() // 항상 title이 보이도록 설정
                            } else {
                                // 이미 마커가 있으면 위치만 업데이트
                                other_currentLocationMarker?.position = other_latLng
                            }
                        }
                    }
                } else {
                    println("No such document")
                }
            }
            .addOnFailureListener { exception ->
                println("get failed with ${exception}")
            }

    }

}