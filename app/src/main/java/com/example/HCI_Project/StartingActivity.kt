package com.example.HCI_Project

import android.content.Intent
import android.net.Uri
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
import com.example.HCI_Project.MakingActivity


class StartingActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private val images = listOf(
        R.drawable.starting_guideimg1,
        R.drawable.starting_guideimg2,
        R.drawable.starting_guideimg3,
        R.drawable.starting_guideimg4
    )

    //starting_guidpage1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.starting_main)

        viewPager = findViewById(R.id.viewPager)
        val adapter = ImageSliderAdapter(images)
        viewPager.adapter = adapter

        val myButton: Button = findViewById(R.id.enterButton)

<<<<<<< HEAD
        // TODO 링크를 통해서 code 받아오기
=======
        handleDeepLink(intent)  // 딥링크 로직
>>>>>>> 959ff1205c71c19edefbc252e570e83a19d97cf9

        myButton.setOnClickListener {
            // 두번째 페이지로 변경 필요

            val intent = Intent(this, MakingActivity::class.java)
            intent.putExtra("CODE", "")
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.guideX).setOnClickListener {
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.guideGroup).visibility = View.INVISIBLE
        }
    }

    fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    // 딥링크 관련 로직
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    // 딥링크로 실행된 경우 방 참여 프로필 편집화면으로 이동
    // 그렇지 않은 경우 현재 페이지에 머무르기
    private fun handleDeepLink(intent: Intent) {
        val action: String? = intent.action
        val data: Uri? = intent.data

        if (Intent.ACTION_VIEW == action && data != null) {
            val pathSegments = data.pathSegments
            if (pathSegments.size > 1 && pathSegments[0] == "room") {
                val roomId = pathSegments[1]
                // 방ID가 존재하면 MakingActivity로 전환
                val makingIntent = Intent(this, MakingActivity::class.java).apply {
                    putExtra("ROOM_ID", roomId)
                }
                startActivity(makingIntent)
                finish() // StartingActivity 종료
            } else {
                // do nothing
            }
        }
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
