package com.hci.hci_project

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.hci.hci_project.databinding.MakingMainBinding

class MakingActivity : AppCompatActivity() {
    val binding by lazy { MakingMainBinding.inflate(layoutInflater) }
    val TAG ="HCI"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}