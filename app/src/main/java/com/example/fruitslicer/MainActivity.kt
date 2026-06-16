package com.example.fruitslicer

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnAccessibility: Button
    private lateinit var btnStartCapture: Button
    private lateinit var btnToggleBot: Button

    private var projectionManager: MediaProjectionManager? = null
    private var botRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            startForegroundService(serviceIntent)

            // Poll until the service confirms it's ready
            pollForReady()
        } else {
            Toast.makeText(this, "Screen capture denied.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pollForReady() {
        if (ScreenCaptureService.isReady) {
            tvStatus.text = "Status: ✅ Ready — press START BOT"
            btnToggleBot.isEnabled = true
            Toast.makeText(this, "Screen capture ready!", Toast.LENGTH_SHORT).show()
        } else {
            // Check again in 300ms
            handler.postDelayed({ pollForReady() }, 300)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnStartCapture = findViewById(R.id.btnStartCapture)
        btnToggleBot = findViewById(R.id.btnToggleBot)

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        btnToggleBot.isEnabled = false

        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Find 'Fruit Auto Slicer' and enable it", Toast.LENGTH_LONG).show()
        }

        btnStartCapture.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Enable Accessibility Service first!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            screenCaptureLauncher.launch(projectionManager!!.createScreenCaptureIntent())
        }

        btnToggleBot.setOnClickListener {
            toggleBot()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        if (ScreenCaptureService.isReady) btnToggleBot.isEnabled = true
    }

    private fun toggleBot() {
        botRunning = !botRunning
        if (botRunning) {
            BotController.start()
            btnToggleBot.text = "⏹ STOP BOT"
            btnToggleBot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#E53935")
            )
            tvStatus.text = "Status: 🟢 Running — switch to Fruit Ninja!"
        } else {
            BotController.stop()
            btnToggleBot.text = "▶ START BOT"
            btnToggleBot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#43A047")
            )
            tvStatus.text = "Status: ⏹ Stopped"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${packageName}/${SlicerAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(service)
    }

    private fun updateStatus() {
        val accessOk = isAccessibilityServiceEnabled()
        tvStatus.text = when {
            !accessOk -> "Status: ⚠ Accessibility not enabled"
            !ScreenCaptureService.isReady -> "Status: ⚠ Screen capture not granted yet"
            botRunning -> "Status: 🟢 Running"
            else -> "Status: ✅ Ready — press START BOT"
        }
        btnToggleBot.isEnabled = ScreenCaptureService.isReady
    }
}
