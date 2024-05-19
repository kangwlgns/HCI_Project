package com.hci.hci_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout

class StartingActivity : AppCompatActivity() {
    val TAG ="HCI"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.starting_main)

        findViewById<Button>(R.id.enterButton).setOnClickListener {
            val intent = Intent(this, MakingActivity::class.java)
            startActivity(intent);
        }

        findViewById<Button>(R.id.guideX).setOnClickListener {
            findViewById<FrameLayout>(R.id.guideGroup).visibility = View.INVISIBLE
        }
    }
}