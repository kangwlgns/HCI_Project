package com.example.HCI_Project

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
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
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    // Firestore instance
    private val db = Firebase.firestore
    private lateinit var job: Job

    // 데이터베이스에서 받을 RoomData
    private lateinit var roomData: Map<String, Object>

    // 상하의 정보 뷰
    private lateinit var myNameView: TextView
    private lateinit var partnerNameView: TextView
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
    private val permissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private val serviceUuid = ParcelUuid.fromString("0000BBBB-0000-1000-8000-00805F9B34FB") // 자신의 서비스 UUID로 변경

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
    private val VIBR_PERIOD = 5000
    private var canVibrate = true
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
        @SuppressLint("ServiceCast")
        private fun vibratePhone() { //휴대폰 진동일으키는 함수
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            // Android 버전에 따라 진동 효과를 다르게 설정
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Android O 이전 버전에서는 deprecated된 메서드를 사용
                @Suppress("DEPRECATION")
                vibrator.vibrate(1000)
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
                //스캔 성공시 실행되는 코드나 함수가 있어야함
                // Advertising 데이터에 포함된 서비스 UUID 확인
                val uuids = result.scanRecord?.serviceUuids
                if (uuids != null && uuids.contains(serviceUuid)) {
                    // Advertising 데이터 추출
                    val serviceData = result.scanRecord?.getServiceData(serviceUuid)
                    if (serviceData != null) {
                        // 문자열과 UUID 추출
                        val receivedString = String(serviceData, Charsets.UTF_8) // 블루투스 advertising 한 데이터 중 룸 ID 수신한 것을 UTF-8로 변환
                        val receivedUuid = result.device?.address ?: ""

                        // 수신한 데이터 처리

                        if (receivedString == roomId) { //수정이 필요함
                            if (canVibrate) {
                                vibratePhone() //진동 일으킴
                                Toast.makeText(this@MainActivity, "근처에 사용자가 존재합니다.", Toast.LENGTH_SHORT).show()
                                // 5초간 진동 및 알림 중지
                                canVibrate = false
                                Handler(Looper.getMainLooper()).postDelayed({
                                    canVibrate = true
                                }, 5000)
                            }
                        }
                        Log.d("BluetoothAdvertising", "Received RoomID: $receivedString, Received UUID: $receivedUuid")
                    }
                }
            }
        }
    }

    // Bluetooth 스캔 함수를 블루투스 전통 스캔으로 변경
    private fun scanDevice(state: Boolean) {
        if (state) {
            handler.postDelayed({
                scanning = false
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                bluetoothLeScanner?.stopScan(mLeScanCallback)
            }, SCAN_PERIOD)

            scanning = true
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
            }
            bluetoothLeScanner?.startScan(mLeScanCallback)
        } else {
            scanning = false
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {

            }
            bluetoothLeScanner?.stopScan(mLeScanCallback)
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

    // 나가는 경우의 함수
    private fun quitActivity() {
        val nickname = intent.getStringExtra("NICKNAME").toString()
        val updates = hashMapOf<String, Any>(
            nickname to FieldValue.delete()
        )

        db.collection("rooms").document(roomId).update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "삭제 성공")
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "삭제 실패")
            }

        finishAffinity()
    }

    // 나가는 팝업창 화면 재사용
    private fun quitPopUp() {
        // Create an AlertDialog to confirm exit
        AlertDialog.Builder(this)
            .setTitle("나가기")
            .setMessage("정말 나가실 건가요? \n" +
                    "링크를 통해서만 재입장할 수 있어요.")
            .setPositiveButton("나가기") { dialog, which ->
                // Handle the OK button action here
                quitActivity()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 시간 초과 팝업창 화면 재사용
    private fun timeOverPopUp() {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("시간 종료")
            .setMessage("공유 시간이 종료되었어요. \n" +
                    "링크를 통해서만 재입장할 수 있어요.")
            .setPositiveButton("나가기") { dialog, which ->
                // Handle the OK button action here
                quitActivity()
            }
            .setNegativeButton("5분 연장하기") { dialog, which ->
                add5Minutes()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                updateLocationViewColor()
            }
        }
    }
    // 스마트폰 뒤로가기 버튼 처리
    override fun onBackPressed() {
        if(false) { super.onBackPressed() } // 에러 처리
        quitPopUp()
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
    lateinit var current_latLng_other: LatLng

    // "내 위치 고정" 버튼 표시
    private lateinit var toggleButton: FloatingActionButton

    // 내 닉네임
    lateinit var myNickname: String

    // 방 ID
    var roomId: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // activity_main을 현재 액티비티 뷰 설정
        setContentView(R.layout.activity_main)

        myNameView = findViewById(R.id.myName)
        partnerNameView = findViewById(R.id.partnerName)

        // BluetoothAdapter 초기화
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // BluetoothAdapter 초기화
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        // Bluetooth 장치 검색 BroadcastReceiver 등록
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)

        // 권한 확인 및 요청
        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_ALL_PERMISSION)
        } else {
            startAdvertising()
            scanDevice(true)
        }
        // 방 ID
        roomId = intent.getStringExtra("ROOM_ID").toString()

        // 내 닉네임
        myNickname = intent.getStringExtra("NICKNAME").toString()
        myNameView.text = myNickname

        // 주기적으로 방 정보를 받아오기
        job = startRepeatingJob(5000L) {
            if(roomId !== null) {
                fetchDataFromFirestore(roomId)
            }
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
            // 방 ID를 포함한 딥링크를 공유하는 창 띄우기
            val deepLinkUri = Uri.parse("https://www.locashare.com/room/$roomId")
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, deepLinkUri.toString())
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "링크 공유하기"))
        }
        // 나가기 버튼 처리
        val outButton: Button = findViewById(R.id.materialButton)
        outButton.setOnClickListener {
            quitPopUp()
        }

        // 타이머 시간 설정 후 시작
        // TODO: 추후 이전 페이지에서 설정된 시간(분)을 가져와서 설정
        startTime = intent.getStringExtra("TIME")?.toInt() ?: 5;
        timerTextView = findViewById(R.id.remainingTimeView)
        startTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Activity가 파괴될 때 Coroutine을 취소합니다.
        unregisterReceiver(bluetoothReceiver) // 블루투스
        job.cancel()
    }

    private fun fetchDataFromFirestore(roomId: String) {
        db.collection("rooms").document(roomId)
            .get()
            .addOnSuccessListener { document ->
                // 데이터 처리
                roomData = document.data as Map<String, Object>
                Log.d("방 정보", "${roomData}")
                roomData.forEach { (name, attributes) ->
                    if (name != myNickname) {
                        // 상대방 정보
                        val other_data = attributes as Map<String, Any>
                        Log.d("상대방 정보", "${other_data}")
                        enter_partner(roomData.keys.size) // 인원수를 계산하여 처리
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
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        // 브로드캐스트 리시버 등록
        val bluetoothFilter = IntentFilter(ACTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, bluetoothFilter)

        val locationFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        registerReceiver(locationReceiver, locationFilter)
    }

    @RequiresApi(Build.VERSION_CODES.M)
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
                timeOverPopUp()
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
    fun add5Minutes() {
        // 현재 남은 시간에 5분 추가 후 카운트다운 재시작
        timeRemainingInMillis += 5 * 60000 // 5분을 밀리초로 변환하여 추가
        if (timeRemainingInMillis >= 60*60000) {  timeRemainingInMillis = 60*60000 }
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
            interval = 5000
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
                        db.collection("rooms").document(roomId)
                            .update("${myNickname}.lat", location.latitude)
                        db.collection("rooms").document(roomId)
                            .update("${myNickname}.lng", location.longitude)
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
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { document ->
                if (document.data != null) {
                    roomData = document.data!!
                    Log.d("방 정보", "${roomData}")
                    enter_partner(roomData.keys.size) // 인원수를 계산하여 에러 처리
                    roomData.forEach { (name, attributes) ->
                        Log.d("roomData", "${name}")
                        if (name != myNickname) {
                            other_data = attributes as Map<String, Any>
                            Log.d("상대방 정보", "${other_data}")

                            // 이부분을 DB에서 가져온 값으로 변경
                            val other_latLng = LatLng(
                                other_data.get("lat") as Double,
                                other_data.get("lng") as Double
                            )
                            current_latLng_other = other_latLng
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
                            var between_distance = calculateDistance(current_latLng , current_latLng_other)
                            if(between_distance >= 1) {
                                findViewById<TextView>(R.id.nearingStatus).text = String.format("%.2fkm", between_distance)
                            }else {
                                if ( between_distance*1000 > 10) {
                                    findViewById<TextView>(R.id.nearingStatus).text =  String.format("%.0fm", between_distance*1000)

                                } else {
                                    findViewById<TextView>(R.id.nearingStatus).text =  "10m 이내"
                                }
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
    fun enter_partner(num_person : Int) {
        if( num_person == 2) {
            findViewById<TextView>(R.id.partnerStatusTextView).text = "상대방과 위치를 공유하고 있어요"
            findViewById<ConstraintLayout>(R.id.partnerInfoLayout).visibility= View.VISIBLE
        }else {
            findViewById<TextView>(R.id.partnerStatusTextView).text = "상대방의 입장을 기다리고 있어요"
            findViewById<ConstraintLayout>(R.id.partnerInfoLayout).visibility= View.INVISIBLE
        }
    }
    private fun startAdvertising() { //블루투스 광고 함수
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(serviceUuid)
            .addServiceData(serviceUuid, roomId.toByteArray(Charsets.UTF_8)) // 문자열을 포함하는 데이터 추가 수정이 필요함
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //권한 없을시 하는 코드임 일단 비워둠
        }
        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertisingCallback)
    }

    private val advertisingCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "Advertisement started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Advertisement failed to start: $errorCode")
        }
    }
    companion object {
        private const val TAG = "BluetoothAdvertisement"
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


// 위도 경도 거리 계산 함수
fun calculateDistance(my_latLng : LatLng, your_latLng_other : LatLng): Double {
    var latitude1 = my_latLng.latitude
    var latitude2 = your_latLng_other.latitude
    var longitude1 = my_latLng.longitude
    var longitude2 = your_latLng_other.longitude
    val earthRadius = 6371 // Earth's radius in kilometers

    val deltaLatitude = Math.toRadians(90 - latitude2) - Math.toRadians(90 - latitude1)
    val deltaLongitude = Math.toRadians(longitude1) - Math.toRadians(longitude2)

    val distance = earthRadius * acos(
        cos(Math.toRadians(90 - latitude1)) * cos(Math.toRadians(90 - latitude2)) +
                sin(Math.toRadians(90 - latitude1)) * sin(Math.toRadians(90 - latitude2)) * cos(deltaLongitude)
    )

    return distance
}


private fun Handler.postDelayed(function: () -> Unit?, scanPeriod: Int) {
}