package com.example.littlecompanion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.random.Random

class OverlayService : Service() {
    companion object {
        const val ACTION_RESIZE = "resize"
        const val ACTION_RESET = "reset"
        private const val CHANNEL = "little_companion"
        private const val NOTIFICATION_ID = 41
    }

    private lateinit var wm: WindowManager
    private var pet: ImageView? = null
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var dragging = false
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var targetX = 0
    private var targetY = 0
    private var lastMoveAt = 0L
    private var screenW = 1080
    private var screenH = 1920
    private var sizeDp = 150

    private val wander = object : Runnable {
        override fun run() {
            val v = pet ?: return
            if (!dragging) {
                val maxX = (screenW - v.width).coerceAtLeast(0)
                val maxY = (screenH - v.height).coerceAtLeast(0)
                targetX = Random.nextInt(0, maxX + 1)
                targetY = Random.nextInt((screenH * 0.12f).toInt().coerceAtMost(maxY), (screenH * 0.78f).toInt().coerceAtLeast(1).coerceAtMost(maxY + 1))
            }
            handler.postDelayed(this, Random.nextLong(2600, 5200))
        }
    }

    private val animate = object : Runnable {
        override fun run() {
            val v = pet
            if (v != null && !dragging) {
                val dx = targetX - params.x
                val dy = targetY - params.y
                if (abs(dx) + abs(dy) > 5) {
                    params.x += (dx * 0.018f).toInt().let { if (it == 0) dx.compareTo(0) else it }
                    params.y += (dy * 0.018f).toInt().let { if (it == 0) dy.compareTo(0) else it }
                    try { wm.updateViewLayout(v, params) } catch (_: Exception) {}
                    v.rotation = if (dx >= 0) 2f else -2f
                } else {
                    v.rotation = 0f
                }
                val t = System.currentTimeMillis()
                if (t - lastMoveAt > 5000) {
                    v.animate().rotation(0f).setDuration(500).start()
                }
            }
            handler.postDelayed(this, 30)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        showPet()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "stop" -> stopSelf()
            ACTION_RESIZE -> resize(intent.getIntExtra("size", 55))
            ACTION_RESET -> resetPosition()
            else -> if (pet == null) showPet()
        }
        return START_STICKY
    }

    private fun showPet() {
        if (pet != null) return
        val metrics = resources.displayMetrics
        screenW = metrics.widthPixels
        screenH = metrics.heightPixels
        sizeDp = (getSharedPreferences("companion", MODE_PRIVATE).getInt("size", 55) * 2.2f).toInt().coerceAtLeast(70)
        val px = (sizeDp * metrics.density).toInt()
        val image = ImageView(this).apply {
            setImageResource(R.drawable.companion)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "움직이는 캐릭터"
            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dragging = true
                        lastMoveAt = System.currentTimeMillis()
                        downX = event.rawX
                        downY = event.rawY
                        startX = params.x
                        startY = params.y
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = startX + (event.rawX - downX).toInt()
                        params.y = startY + (event.rawY - downY).toInt()
                        try { wm.updateViewLayout(view, params) } catch (_: Exception) {}
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        dragging = false
                        targetX = params.x
                        targetY = params.y
                        true
                    }
                    else -> false
                }
            }
        }
        params = WindowManager.LayoutParams(
            px, px, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenW - px) / 2
            y = (screenH * 0.55f).toInt()
        }
        pet = image
        try {
            wm.addView(image, params)
            targetX = params.x
            targetY = params.y
            handler.postDelayed(wander, 1800)
            handler.post(animate)
        } catch (_: Exception) {
            pet = null
            stopSelf()
        }
    }

    private fun resize(progress: Int) {
        val v = pet ?: return
        val px = ((progress.coerceAtLeast(15) * 2.2f) * resources.displayMetrics.density).toInt()
        params.width = px
        params.height = px
        try { wm.updateViewLayout(v, params) } catch (_: Exception) {}
    }

    private fun resetPosition() {
        val v = pet ?: return
        params.x = (screenW - v.width) / 2
        params.y = (screenH * 0.55f).toInt()
        targetX = params.x
        targetY = params.y
        try { wm.updateViewLayout(v, params) } catch (_: Exception) {}
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "캐릭터 표시", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, OverlayService::class.java).setAction("stop"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Little Companion 실행 중")
            .setContentText("캐릭터가 화면 위에서 움직이고 있어요.")
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_media_pause, "끄기", stop)
            .setOngoing(true).build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) { super.onTaskRemoved(rootIntent) }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        pet?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        pet = null
        getSharedPreferences("companion", MODE_PRIVATE).edit().putBoolean("enabled", false).apply()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
