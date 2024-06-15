package com.example.HCI_Project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.HCI_Project.MakingUtility.Companion.manClick
import com.example.HCI_Project.MakingUtility.Companion.womanClick
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class MakingActivity : AppCompatActivity() {
    // TODO 사용자 이름 랜덤으로
    val TAG = "HCHI"
    var isMale: Int = 0;
    var curTime: String = "10";
    val buttonStates: Array<Int> = arrayOf(
        R.drawable.making_male_on,
        R.drawable.making_male_off,
        R.drawable.making_female_on,
        R.drawable.making_female_off
    )
    lateinit var buttons: Array<Button>

    // 랜덤스트링 생성함수
    fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.making_main)

        // db를 확인해서 room_id의 데이터 개수 확인.
        val db = Firebase.firestore
        var fieldsCount = 0

        // roomId 받아오기
        var roomId = intent.getStringExtra("ROOM_ID")
        if (roomId == null) {
            // 없으면 랜덤 코드 생성
            roomId = getRandomString(10)
        }

        // db에 요소가 2개 이상인 경우, 방 입장 불가능하도록.
        runBlocking {
            try {
                val doc = db.collection("rooms").document(roomId).get().await()
                if (doc.exists()) {
                    fieldsCount = doc.data?.size ?: 0
                } else {
                    fieldsCount = 0
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error", e)
            }

            if (fieldsCount > 1) {
                AlertDialog.Builder(this@MakingActivity)
                    .setMessage("방에 입장할 수 없습니다.\n링크를 다시 확인해 주세요.")
                    .setPositiveButton("확인") { dialog, which ->
                        finishAffinity()
                    }
                    .show()
            }
        }

        buttons = arrayOf(
            findViewById<android.widget.Button>(com.example.HCI_Project.R.id.buttonMan),
            findViewById<Button>(R.id.buttonWoman)
        )

        findViewById<Button>(R.id.buttonMan).setOnClickListener {
            isMale = manClick(isMale, this, buttonStates, buttons)
        }

        findViewById<Button>(R.id.buttonWoman).setOnClickListener {
            isMale = womanClick(isMale, this, buttonStates, buttons)
        }

        findViewById<Button>(R.id.minusButton1).setOnClickListener {
            curTime = MakingUtility.changeClock(curTime, "-10")
            findViewById<TextView>(R.id.clockInput).setText(curTime)
        }

        findViewById<Button>(R.id.minusButton2).setOnClickListener {
            curTime = MakingUtility.changeClock(curTime, "-5")
            findViewById<TextView>(R.id.clockInput).setText(curTime)
        }

        findViewById<Button>(R.id.plusButton1).setOnClickListener {
            curTime = MakingUtility.changeClock(curTime, "10")
            findViewById<TextView>(R.id.clockInput).setText(curTime)
        }

        findViewById<Button>(R.id.plusButton2).setOnClickListener {
            curTime = MakingUtility.changeClock(curTime, "5")
            findViewById<TextView>(R.id.clockInput).setText(curTime)
        }

        findViewById<Button>(R.id.makingButton).setOnClickListener {
            val tmp = mutableMapOf(
                "coats" to findViewById<EditText>(R.id.clothesInfo).text.toString(),
                "pants" to findViewById<EditText>(R.id.pantsInfo).text.toString(),
                "isMale" to isMale,
                "lat" to 0,
                "lng" to 0,
            )

            val myInfoMap = mutableMapOf<String, Any>()
            val nickname = findViewById<EditText>(R.id.nickName).text.toString()
            myInfoMap[nickname] = tmp

            // deeplink로 roomId를 받아오는 구문이 필요할 듯
            db.collection("rooms").document(roomId).set(myInfoMap, SetOptions.merge())

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("IS_MALE", isMale)
            intent.putExtra("TIME", curTime)
            intent.putExtra("CLOTHES", findViewById<EditText>(R.id.clothesInfo).text.toString())
            intent.putExtra("PANTS", findViewById<EditText>(R.id.pantsInfo).text.toString())
            intent.putExtra("NICKNAME", findViewById<EditText>(R.id.nickName).text.toString())
            intent.putExtra("ROOM_ID", roomId)

            startActivity(intent)
        }
    }
}