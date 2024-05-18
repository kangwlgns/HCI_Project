package com.hci.hci_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.hci.hci_project.databinding.StartingMainBinding

class StartingActivity : AppCompatActivity() {
    val binding by lazy { StartingMainBinding.inflate(layoutInflater) }
    val TAG ="HCI"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.enterButton.setOnClickListener {
            val intent = Intent(this, MakingActivity::class.java)
            startActivity(intent);
        }

        binding.guideX.setOnClickListener {
            binding.guideGroup.visibility = View.INVISIBLE
        }
    }
}