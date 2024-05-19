package com.hci.hci_project

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.hci.hci_project.MakingUtility.Companion.manClick
import com.hci.hci_project.MakingUtility.Companion.womanClick

class MakingActivity : AppCompatActivity() {
    val TAG = "HCI"
    var isMale: Int = 0;
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
    }
}