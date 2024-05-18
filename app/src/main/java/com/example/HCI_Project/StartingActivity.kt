package com.example.HCI_Project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.HCI_Project.R


class StartingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.starting_main)
//
//        binding.enterButton.setOnClickListener {
//            val intent = Intent(this, MakingActivity::class.java)
//            startActivity(intent);
//        }
//
//        binding.guideX.setOnClickListener {
//            binding.guideGroup.visibility = View.INVISIBLE
//        }
    }
}