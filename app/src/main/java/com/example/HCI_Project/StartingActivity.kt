package com.example.HCI_Project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton


class StartingActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private val images = listOf(
        R.drawable.starting_guideimg1,
        R.drawable.starting_guideimg2,
        R.drawable.starting_guideimg3,
        R.drawable.starting_guideimg4
    )
    private var code: String = ""

    //starting_guidpage1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.starting_main)

        viewPager = findViewById(R.id.viewPager)
        val adapter = ImageSliderAdapter(images)
        viewPager.adapter = adapter

        val myButton: Button = findViewById(R.id.enterButton)

        // TODO 링크를 통해서 code 받아오기

        if (code == "") {
            code = generateRandomString(10)
        }

        myButton.setOnClickListener {
            // 두번째 페이지로 변경 필요
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("CODE", code)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.guideX).setOnClickListener {
            findViewById<FrameLayout>(R.id.guideGroup).visibility = View.INVISIBLE
        }

    }

    fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
}

class ImageSliderAdapter(private val images: List<Int>) :
    RecyclerView.Adapter<ImageSliderAdapter.ImageSliderViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageSliderViewHolder {
//        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.starting_main, parent, false)
//        return ImageSliderViewHolder(itemView)
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.starting_guide, parent, false)
        return ImageSliderViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ImageSliderViewHolder, position: Int) {
        val imageResId = images[position]
        holder.bind(imageResId)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class ImageSliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(imageResId: Int) {
            imageView.setImageResource(imageResId)
        }
    }
}
