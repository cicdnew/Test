package com.enterprise.analytics.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.core.app.NotificationCompat
import com.enterprise.analytics.R

class FloatingReminderService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("REMINDER_TITLE") ?: "Reminder"
        val videoUriStr = intent?.getStringExtra("REMINDER_VIDEO_URI")
        val bgColorHex = intent?.getStringExtra("BG_COLOR") ?: "#FFFFFF"
        val textColorHex = intent?.getStringExtra("TEXT_COLOR") ?: "#000000"

        startForeground(1, createNotification())
        showFloatingWindow(title, videoUriStr, bgColorHex, textColorHex)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "floating_service",
                "Floating Reminder Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "floating_service")
            .setContentTitle("Reminder Active")
            .setContentText("A floating reminder is currently displayed.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun showFloatingWindow(title: String, videoUriStr: String?, bgColorHex: String, textColorHex: String) {
        if (floatingView != null) {
            windowManager.removeView(floatingView)
        }

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_reminder, null)

        val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedX = sharedPrefs.getInt("floating_x", 0)
        val savedY = sharedPrefs.getInt("floating_y", 100)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = savedX
        params.y = savedY

        windowManager.addView(floatingView, params)

        val mainLayout = floatingView?.findViewById<View>(R.id.floating_main_layout)
        val titleTextView = floatingView?.findViewById<TextView>(R.id.floating_title)
        val videoView = floatingView?.findViewById<VideoView>(R.id.floating_video_view)
        val dismissButton = floatingView?.findViewById<Button>(R.id.floating_dismiss_button)

        try {
            mainLayout?.setBackgroundColor(Color.parseColor(bgColorHex))
            titleTextView?.setTextColor(Color.parseColor(textColorHex))
        } catch (e: IllegalArgumentException) {
            // Fallback colors
            mainLayout?.setBackgroundColor(Color.WHITE)
            titleTextView?.setTextColor(Color.BLACK)
        }

        titleTextView?.text = title

        if (videoUriStr != null) {
            videoView?.visibility = View.VISIBLE
            val videoUri = Uri.parse(videoUriStr)
            videoView?.setVideoURI(videoUri)
            videoView?.start()
        } else {
            videoView?.visibility = View.GONE
        }

        dismissButton?.setOnClickListener {
            stopSelf()
        }

        // Make window draggable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
                    sharedPrefs.edit()
                        .putInt("floating_x", params.x)
                        .putInt("floating_y", params.y)
                        .apply()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager.removeView(floatingView)
            floatingView = null
        }
    }
}
