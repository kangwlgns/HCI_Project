package com.example.HCI_Project

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
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
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    // Firestore instance
    private val db = Firebase.firestore
    private lateinit var job: Job

    // 데이터베이스에서 받을 RoomData
    private lateinit var roomData: Map<String, Object>

    // 상하의 정보 뷰
    private lateinit var myCoatTextView: TextView
    private lateinit var myPantsTextView: TextView
    private lateinit var partnerCoatTextView: TextView
    private lateinit var partnerPantsTextView: TextView

    // 위치, 블루투스 활성화상태 뷰
    private lateinit var gpsActivationView: TextView
    private lateinit var bluetoothActivationView: TextView

    //
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_ALL_PERMISSION = 2
    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
    )
    private var bluetoothAdapter: BluetoothAdapter? = null

    // BroadcastReceiver를 정의하여 블루투스와 위치 서비스 상태 변경을 감지
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            //
            when (intent?.action) {
                ACTION_STATE_CHANGED -> {
                    val state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    updateBluetoothViewColor(state)
                }
            }
            //
            if (BluetoothDevice.ACTION_FOUND == action) {
                // 디바이스를 찾았을 때의 처리
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    if (!devicesArr.contains(it) && it.name != null) {
                        devicesArr.add(it)
                        //디바이스 탐지 후 실행되는 함수있었음
                    }
                }
            }
        }
    }
    private var scanning: Boolean = false
    private var devicesArr = ArrayList<BluetoothDevice>()
    private val SCAN_PERIOD = 1000
    private val handler = Handler()
    private lateinit var viewManager: RecyclerView.LayoutManager
    private val mLeScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("scanCallback", "BLE Scan Failed : " + errorCode)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.let {
                // results is not null
                for (result in it) {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                    }
                    if (!devicesArr.contains(result.device) && result.device.name != null) devicesArr.add(
                        result.device
                    )
                }

            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                // result is not null
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                if (!devicesArr.contains(it.device) && it.device.name != null) devicesArr.add(it.device)
                //스캔 성공시 실행되는 함수있어야함
            }
        }
    }

    // Bluetooth 스캔 함수를 블루투스 전통 스캔으로 변경
    private fun scanDevice(state: Boolean) {
        if (state) {
            // 블루투스 장치 검색 시작
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            bluetoothAdapter?.startDiscovery()

            // 일정 시간 후에 검색 중지
            handler.postDelayed({
                scanning = false
                bluetoothAdapter?.cancelDiscovery()
            }, SCAN_PERIOD)

            // 스캔 상태 설정
            scanning = true
            devicesArr.clear()
        } else {
            // 블루투스 장치 검색 중지
            scanning = false
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    //권한 가지고 있는지를 알려주는 함수
    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    // 블루투스 권한 확인
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ALL_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
                } else {
                    requestPermissions(permissions, REQUEST_ALL_PERMISSION)
                    Toast.makeText(this, "Permissions must be granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                updateLocationViewColor()
            }
        }
    }

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

        //
        // BluetoothAdapter 초기화
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Bluetooth 장치 검색 BroadcastReceiver 등록
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        viewManager = LinearLayoutManager(this)
        //scanDevice(true) 함수 실행시 블루투스 스캔 시작
        //

        // 주기적으로 방 정보를 받아오기
        job = startRepeatingJob(5000L) {
            fetchDataFromFirestore()
        }

        // 나, 상대방 상하의 뷰
        myCoatTextView = findViewById(R.id.myCoat)
        myPantsTextView = findViewById(R.id.myPants)
        partnerCoatTextView = findViewById(R.id.partnerCoat)
        partnerPantsTextView = findViewById(R.id.partnerPants)

        // 위치, 블루투스 활성화여부 뷰
        gpsActivationView = findViewById(R.id.gpsActivationStatus)
        bluetoothActivationView = findViewById(R.id.bluetoothActivationStatus)

        // 초기 블루투스 상태 확인
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        updateBluetoothViewColor(bluetoothAdapter?.state ?: BluetoothAdapter.STATE_OFF)

        // 초기 위치 서비스 상태 확인
        updateLocationViewColor()

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
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
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

    override fun onDestroy() {
        super.onDestroy()
        // Activity가 파괴될 때 Coroutine을 취소합니다.

        unregisterReceiver(bluetoothReceiver) // 블루투스
        job.cancel()
    }

    private fun fetchDataFromFirestore() {
        db.collection("rooms").document("QyFo5Z9zejFqEhydbL2c")
            .get()
            .addOnSuccessListener { document ->
                // 데이터 처리
                roomData = document.data as Map<String, Object>
                Log.d("방 정보", "${roomData}")
                roomData.forEach { (name, attributes) ->
                    if (name != "영탁") {
                        // 상대방 정보
                        val other_data = attributes as Map<String, Any>
                        Log.d("상대방 정보", "${other_data}")

                        // 상대방 상하의 정보
                        partnerCoatTextView.text = "" + other_data.get("coats")
                        partnerPantsTextView.text = "" + other_data.get("pants")

//                        // 상대방 위치정보
//                        val other_latLng = LatLng(
//                            other_data.get("lat") as Double,
//                            other_data.get("lng") as Double
//                        )
//
//                        Log.d("상대방 위치", "${other_latLng}")
//                        if (other_currentLocationMarker == null) { // 상대의 마커를 최초롤 세팅하는 경우
//                            // 상대편 -> DB에서 가져온 이름으로 변경
//                            val markerOptions = MarkerOptions().position(other_latLng).title(name)
//                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.main_other_location_icon)) // 이미지 설정
//                            other_currentLocationMarker = mGoogleMap.addMarker(markerOptions)
//                            other_currentLocationMarker?.showInfoWindow()
//                        } else {// 이미 마커가 있으면 위치만 업데이트
//                            other_currentLocationMarker?.position = other_latLng
//                        }
                    } else {
                        // 내 정보
                        val my_data = attributes as Map<String, Any>
                        Log.d("내 정보", "${my_data}")

                        // 내 상하의 정보
                        myCoatTextView.text = "" + my_data.get("coats")
                        myPantsTextView.text = "" + my_data.get("pants")

                    }
                }
            }.addOnFailureListener { exception ->
                // 오류를 처리합니다.
                exception.printStackTrace()
            }
    }

    private fun startRepeatingJob(timeInterval: Long, action: () -> Unit): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                action()
                delay(timeInterval)
            }
        }
    }

    // 위치, 블루투스 리시버 등록 및 해제
    override fun onStart() {
        super.onStart()
        // 브로드캐스트 리시버 등록
        val bluetoothFilter = IntentFilter(ACTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, bluetoothFilter)

        val locationFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        registerReceiver(locationReceiver, locationFilter)
    }

    override fun onStop() {
        super.onStop()
        // 브로드캐스트 리시버 해제
        unregisterReceiver(bluetoothReceiver)
        unregisterReceiver(locationReceiver)
    }

    // 블루투스 활성화 여부에 따라 뷰 배경색을 변경
    private fun updateBluetoothViewColor(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_ON -> {
                bluetoothActivationView.text = "ON"
                bluetoothActivationView.setTextColor(Color.WHITE)
                val svgDrawable = ContextCompat.getDrawable(this, R.drawable.main_bluetooth)
                val newSvgDrawable = svgDrawable?.mutate()
                newSvgDrawable?.setTint(ContextCompat.getColor(this, R.color.white))
                bluetoothActivationView.setCompoundDrawablesWithIntrinsicBounds(
                    newSvgDrawable,
                    null,
                    null,
                    null
                )

                val newDrawable =
                    ContextCompat.getDrawable(this, R.drawable.main_access_status_active)
                // 새로운 배경으로 설정
                bluetoothActivationView.background = newDrawable
            }

            BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_TURNING_ON -> {
                bluetoothActivationView.text = "OFF"
                bluetoothActivationView.setTextColor(Color.parseColor("#5e5e5e"))
                val svgDrawable = ContextCompat.getDrawable(this, R.drawable.main_bluetooth)
                val newSvgDrawable = svgDrawable?.mutate()
                newSvgDrawable?.setTint(ContextCompat.getColor(this, R.color.darkgray))
                bluetoothActivationView.setCompoundDrawablesWithIntrinsicBounds(
                    newSvgDrawable,
                    null,
                    null,
                    null
                )

                val newDrawable =
                    ContextCompat.getDrawable(this, R.drawable.main_access_status_inactive)
                // 새로운 배경으로 설정
                bluetoothActivationView.background = newDrawable
            }
        }
    }

    // 위치 활성화 여부에 따라 뷰 배경색을 변경
    private fun updateLocationViewColor() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (isGpsEnabled || isNetworkEnabled) {
            gpsActivationView.text = "ON"
            gpsActivationView.setTextColor(Color.WHITE)
            val svgDrawable = ContextCompat.getDrawable(this, R.drawable.main_gps)
            val newSvgDrawable = svgDrawable?.mutate()
            newSvgDrawable?.setTint(ContextCompat.getColor(this, R.color.white))
            gpsActivationView.setCompoundDrawablesWithIntrinsicBounds(
                newSvgDrawable,
                null,
                null,
                null
            )

            val newDrawable = ContextCompat.getDrawable(this, R.drawable.main_access_status_active)
            // 새로운 배경으로 설정
            gpsActivationView.background = newDrawable
        } else {
            gpsActivationView.text = "OFF"
            gpsActivationView.setTextColor(Color.parseColor("#5e5e5e"))
            val svgDrawable = ContextCompat.getDrawable(this, R.drawable.main_gps)
            val newSvgDrawable = svgDrawable?.mutate()
            newSvgDrawable?.setTint(ContextCompat.getColor(this, R.color.darkgray))
            gpsActivationView.setCompoundDrawablesWithIntrinsicBounds(
                newSvgDrawable,
                null,
                null,
                null
            )

            val newDrawable =
                ContextCompat.getDrawable(this, R.drawable.main_access_status_inactive)
            // 새로운 배경으로 설정
            gpsActivationView.background = newDrawable
        }
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
                        db.collection("rooms").document("QyFo5Z9zejFqEhydbL2c")
                            .update("영탁.lat", location.latitude)
                        db.collection("rooms").document("QyFo5Z9zejFqEhydbL2c")
                            .update("영탁.lng", location.longitude)
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
        if (setting_initview) {
            return; } // GPS 수신 전 눌렀을 때 앱이 종료되는 현상 방지
        val cameraPosition = CameraPosition.Builder().target(current_latLng)
            .zoom(mGoogleMap.getCameraPosition().zoom).build() // 현재 카메라 줌을 유지
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
                            findViewById<TextView>(R.id.partnerStatusTextView).text =
                                "상대방과 위치를 공유하고 있어요"
                            // 이부분을 DB에서 가져온 값으로 변경
                            val other_latLng = LatLng(
                                other_data.get("lat") as Double,
                                other_data.get("lng") as Double
                            )
                            Log.d("상대방 위치", "${other_latLng}")
                            if (other_currentLocationMarker == null) { // 상대의 마커를 최초로 세팅하는 경우
                                // 상대편 -> DB에서 가져온 이름으로 변경
                                val markerOptions =
                                    MarkerOptions().position(other_latLng).title(name)
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

    fun bluetoothOnOff() {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.d("bluetoothAdapter", "Device doesn't support Bluetooth")
        } else {
            if (bluetoothAdapter?.isEnabled == false) { // 블루투스 꺼져 있으면 블루투스 활성화
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else { // 블루투스 켜져있으면 블루투스 비활성화
                bluetoothAdapter?.disable()
            }
        }
    }


}

private fun Handler.postDelayed(function: () -> Boolean?, scanPeriod: Int) {

}