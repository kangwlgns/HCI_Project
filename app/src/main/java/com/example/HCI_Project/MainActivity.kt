package com.example.HCI_Project

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import android.content.Intent

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // 위치 공유시간 타이머 관련 변수들
    private var startTime: Int = 0  // 설정시간(분)
    private var totalTimeInMillis: Long = 0 // 설정시간(밀리초)
    private var timeRemainingInMillis: Long = 0 // 남은시간(밀리초)
    private lateinit var timerTextView: TextView    // 남은시간 표시부
    private var countDownTimer: CountDownTimer? = null  // 내장 타이머 객체

    // 구글맵 인스턴스
    private lateinit var mGoogleMap: GoogleMap

    //위치 서비스가 gps를 사용해서 위치를 확인
    lateinit var fusedLocationClient: FusedLocationProviderClient

    //위치 값 요청에 대한 갱신 정보를 받는 변수
    lateinit var locationCallback: LocationCallback
    lateinit var locationPermission: ActivityResultLauncher<Array<String>>

    // 내위치 정보
    lateinit var current_latLng: LatLng
    // "내 위치 고정" 버튼 표시
    private lateinit var toggleButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        // activity_main을 현재 액티비티 뷰 설정
        setContentView(R.layout.activity_main)

        // 위치 권한 요청 결과를 처리
        locationPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            if (results.all { it.value }) {
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
        // 내 위치 버튼 처리
        toggleButton = findViewById(R.id.toggleButton)
        toggleButton.setOnClickListener { refreshCarmera() } // 카메라 재이동

        // 링크 공유 버튼 처리
        val shareButton: Button = findViewById(R.id.shareButton)
        shareButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"

            val url = "exampleURL"
            val content = "링크를 클릭하여 위치 공유에 참여하세요"
            intent.putExtra(Intent.EXTRA_TEXT, "$content\n\n$url")

            val chooserTitle = "링크 공유하기"
            startActivity(Intent.createChooser(intent, chooserTitle))
        }

        // 타이머 시간 설정 후 시작
        // TODO: 추후 이전 페이지에서 설정된 시간(분)을 가져와서 설정
        startTime = 5;
        timerTextView = findViewById(R.id.remainingTimeView)
        startTimer()
    }

    // 타이머 시작 함수
    fun startTimer() {
        val startTimeInMinutes = startTime.toLong()
        totalTimeInMillis = startTimeInMinutes * 60000 // 분을 밀리초로 변환
        timeRemainingInMillis = totalTimeInMillis

        startCountDown()
    }

    // 카운트다운 로직 함수(타이머 시작 함수에서 실행)
    private fun startCountDown() {
        countDownTimer?.cancel()    // 기존 타이머가 진행중이라면 취소

        // 1초마다 시간을 갱신
        countDownTimer = object : CountDownTimer(timeRemainingInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingInMillis = millisUntilFinished
                updateCountDownText()
            }

            // TODO: 공유시간이 만료된 후의 로직
            override fun onFinish() {
                timerTextView.text = "00:00"
            }
        }

        countDownTimer?.start()
    }

    // 남은시간을 뷰에 표시
    private fun updateCountDownText() {
        val minutes = (timeRemainingInMillis / 60000) % 60
        val seconds = (timeRemainingInMillis % 60000) / 1000
        val timeString = String.format("%02d:%02d", minutes, seconds)
        timerTextView.text = timeString
    }

    // 시간 5분 연장 기능
    fun add5Minutes(view: View) {
        // 현재 남은 시간에 5분 추가 후 카운트다운 재시작
        timeRemainingInMillis += 5 * 60000 // 5분을 밀리초로 변환하여 추가
        startCountDown()
        updateCountDownText()
    }

    // 지도 객체를 이용할 수 있는 상황이 될 때
    override fun onMapReady(p0: GoogleMap) {
        
        val chungnam_univ = LatLng(36.36652, 127.3444)
        mGoogleMap = p0
        mGoogleMap.mapType = GoogleMap.MAP_TYPE_NORMAL // default 노말 생략 가능
        // 위치 정보를 얻기 위한 초기화 작업
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // 시작 위치 세팅 : 공대 5호관 - GPS가 수신되지 않았을 때 보여 주는 위치
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chungnam_univ, 15f))
        updateLocation()
    }
    // 자신의 위치 받아와 업데이트 하는 함수
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
                    for (location in it.locations) {
                        Log.d("내 위치정보", "위도: ${location.latitude} 경도: ${location.longitude}")
                        // DB 업데이트
                        val db = Firebase.firestore
                        db.collection("rooms").document("QyFo5Z9zejFqEhydbL2c").update("영탁.lat", location.latitude)
                        db.collection("rooms").document("QyFo5Z9zejFqEhydbL2c").update("영탁.lng", location.longitude)
                        setLastLocation(location)
                        // 추가로 상대방의 위치 정보도 세팅 (DB에서 가져옴)
                        setOtherlocation()
                    }
                }
            }
        }
        // GPS 권한 처리
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { // 권한이 없을때 함수 종료
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback,
            Looper.myLooper()!!
        )
    }

    // 처음 GPS가 잡혔을 때 본인 위치로 이동하기 위한 변수
    var setting_initview = true

    // 내 위치 버튼을 눌렀을 때 카메라 이동하는 함수
    fun refreshCarmera() {
        if( setting_initview ) { return; } // GPS 수신 전 눌렀을 때 앱이 종료되는 현상 방지
        val cameraPosition = CameraPosition.Builder().target(current_latLng).zoom(mGoogleMap.getCameraPosition().zoom).build() // 현재 카메라 줌을 유지 
        mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }
    // 마커 객체를 저장할 변수를 선언
    var currentLocationMarker: Marker? = null       // 나
    var other_currentLocationMarker: Marker? = null // 상대
    // 나의 위치 마커를 set하는 함수
    fun setLastLocation(lastLocation: Location) {
        val latLng = LatLng(lastLocation.latitude, lastLocation.longitude)
        current_latLng = latLng // 현재 나의 위-경도
        if (currentLocationMarker == null) { // 처음 마커를 추가하는 경우
            val markerOptions = MarkerOptions().position(latLng).title("나")
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.main_my_location_icon))   // 이미지 설정
            currentLocationMarker = mGoogleMap.addMarker(markerOptions)
            currentLocationMarker?.showInfoWindow() // 항상 title이 보이도록 설정
        } else { // 이미 마커가 있으면 위치만 업데이트
            currentLocationMarker?.position = latLng
        }
        if (setting_initview) { // 첫 GPS 수신때만 카메라가 이동되도록 구현
            val cameraPosition = CameraPosition.Builder().target(latLng).zoom(15.0f).build()
            mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            setting_initview = false
        }
    }

    // 상대의 위치 마커를 Set 하는 함수
    fun setOtherlocation() {
        // 나와 상대 정보
        var my_data: Any
        var partner_data: Any

        // Firebase 데이터 가져오기
        val db = Firebase.firestore
        var roomData: Map<String, Any>
        var other_data: Map<String, Any> =
            mapOf("coat" to "미확인", "pants" to "미확인", "lat" to 0.0, "lng" to 0.0)
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
                            val other_latLng = LatLng(
                                other_data.get("lat") as Double,
                                other_data.get("lng") as Double
                            )
                            Log.d("상대방 위치", "${other_latLng}")
                            if (other_currentLocationMarker == null) { // 상대의 마커를 최초롤 세팅하는 경우
                                // 상대편 -> DB에서 가져온 이름으로 변경
                                val markerOptions = MarkerOptions().position(other_latLng).title(name)
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.main_other_location_icon)) // 이미지 설정
                                other_currentLocationMarker = mGoogleMap.addMarker(markerOptions)
                                other_currentLocationMarker?.showInfoWindow()
                            } else {// 이미 마커가 있으면 위치만 업데이트
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