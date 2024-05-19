package com.hci.hci_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView

class StartingActivity : AppCompatActivity() {
    val TAG ="HCI"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.starting_main)

        // TODO 링크를 통해서 들어왔다면 room_id를 넘겨주자.

        findViewById<Button>(R.id.enterButton).setOnClickListener {
            val intent = Intent(this, MakingActivity::class.java)
            startActivity(intent);
        }

        findViewById<ImageView>(R.id.guideX).setOnClickListener {
            findViewById<FrameLayout>(R.id.guideGroup).visibility = View.INVISIBLE
        }
    }
}