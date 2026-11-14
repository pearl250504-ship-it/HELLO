package com.example.littlecompanion

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var toggle: Button
    private val prefs by lazy { getSharedPreferences("companion", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 34, 28, 24)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFFFFFBF4.toInt())
        }
        val title = TextView(this).apply {
            text = "Little Companion"
            textSize = 28f
            setTextColor(0xFF332B26.toInt())
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "화면 위에서 함께 움직이는 작은 친구"
            textSize = 14f
            setTextColor(0xFF756A62.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 26)
        }
        status = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF51463E.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 18)
        }
        toggle = Button(this).apply {
            textSize = 16f
            setOnClickListener { toggleCompanion() }
        }
        val permission = Button(this).apply {
            text = "다른 앱 위에 표시 권한 설정"
            setOnClickListener {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }
        val sizeLabel = TextView(this).apply {
            text = "캐릭터 크기"
            textSize = 15f
            setTextColor(0xFF51463E.toInt())
            setPadding(0, 24, 0, 4)
        }
        val size = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("size", 55)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val value = p.coerceAtLeast(15)
                    prefs.edit().putInt("size", value).apply()
                    val i = Intent(this@MainActivity, OverlayService::class.java)
                        .setAction(OverlayService.ACTION_RESIZE).putExtra("size", value)
                    startService(i)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        val reset = Button(this).apply {
            text = "캐릭터 위치 초기화"
            setOnClickListener {
                val i = Intent(this@MainActivity, OverlayService::class.java)
                    .setAction(OverlayService.ACTION_RESET)
                startService(i)
                Toast.makeText(this@MainActivity, "위치를 초기화했어요.", Toast.LENGTH_SHORT).show()
            }
        }
        val note = TextView(this).apply {
            text = "캐릭터를 길게 누른 채 움직이면 원하는 위치로 옮길 수 있어요.\n자동 이동 중에도 드래그할 수 있습니다."
            textSize = 13f
            setTextColor(0xFF756A62.toInt())
            gravity = Gravity.CENTER
            setPadding(8, 24, 8, 0)
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(toggle, LinearLayout.LayoutParams(-1, -2))
        root.addView(permission, LinearLayout.LayoutParams(-1, -2))
        root.addView(sizeLabel)
        root.addView(size, LinearLayout.LayoutParams(-1, -2))
        root.addView(reset, LinearLayout.LayoutParams(-1, -2))
        root.addView(note)
        setContentView(root)
    }

    private fun toggleCompanion() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            Toast.makeText(this, "권한을 허용한 뒤 다시 눌러 주세요.", Toast.LENGTH_LONG).show()
            return
        }
        val running = prefs.getBoolean("enabled", false)
        if (running) {
            stopService(Intent(this, OverlayService::class.java))
            prefs.edit().putBoolean("enabled", false).apply()
        } else {
            val i = Intent(this, OverlayService::class.java)
            ContextCompat.startForegroundService(this, i)
            prefs.edit().putBoolean("enabled", true).apply()
        }
        refresh()
    }

    private fun refresh() {
        val enabled = prefs.getBoolean("enabled", false)
        status.text = if (enabled) "캐릭터 표시가 켜져 있습니다." else "캐릭터 표시가 꺼져 있습니다."
        toggle.text = if (enabled) "캐릭터 표시 끄기" else "캐릭터 표시 켜기"
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }
}
