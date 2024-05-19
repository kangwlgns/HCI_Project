package com.hci.hci_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.hci.hci_project.MakingUtility.Companion.manClick
import com.hci.hci_project.MakingUtility.Companion.womanClick

class MakingActivity : AppCompatActivity() {
    // TODO 사용자 이름 랜덤으로
    val TAG = "HCHI"
    var isMale: Int = 0;
    var curTime: String = "10";
    var code: String = "";
    val buttonStates: Array<Int> = arrayOf(
        R.drawable.making_male_on,
        R.drawable.making_male_off,
        R.drawable.making_female_on,
        R.drawable.making_female_off
    )
    lateinit var buttons: Array<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.making_main)

        // TODO room_id를 받아오자.

        buttons = arrayOf(
            findViewById<android.widget.Button>(com.hci.hci_project.R.id.buttonMan),
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
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("IS_MALE", isMale)
            intent.putExtra("TIME", curTime)
            intent.putExtra("CLOTHES", findViewById<EditText>(R.id.clothesInfo).text.toString())
            intent.putExtra("PANTS", findViewById<EditText>(R.id.pantsInfo).text.toString())
            intent.putExtra("NICKNAME", findViewById<EditText>(R.id.nickName).text.toString())
            intent.putExtra("ROOM_ID", code)

            startActivity(intent)
        }

    }
}