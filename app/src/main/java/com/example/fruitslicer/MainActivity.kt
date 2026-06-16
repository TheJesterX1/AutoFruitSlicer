package com.example.fruitslicer

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnAccessibility: Button
    private lateinit var btnStartCapture: Button
    private lateinit var btnToggleBot: Button

    private var projectionManager: MediaProjectionManager? = null
    private var botRunning = false
    private val handler = Handler(Looper.getMainLooper())

    // Step 1: ask for POST_NOTIFICATIONS (Android 13+)
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // whether granted or not, proceed to capture
        launchCaptureIntent()
    }

    // Step 2: the actual screen capture dialog
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            startForegroundService(serviceIntent)
            tvStatus.text = "Status: ⏳ Starting capture..."
            pollForReady()
        } else {
            Toast.makeText(this, "Screen capture was denied — please try again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun pollForReady() {
        if (ScreenCaptureService.isReady) {
            tvStatus.text = "Status: ✅ Ready — press START BOT"
            btnToggleBot.isEnabled = true
            Toast.makeText(this, "✅ Screen capture ready!", Toast.LENGTH_SHORT).show()
        } else {
            handler.postDelayed({ pollForReady() }, 300)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus       = findViewById(R.id.tvStatus)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnStartCapture  = findViewById(R.id.btnStartCapture)
        btnToggleBot     = findViewById(R.id.btnToggleBot)

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        btnToggleBot.isEnabled = false

        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Find 'Fruit Auto Slicer' and enable it", Toast.LENGTH_LONG).show()
        }

        btnStartCapture.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Enable Accessibility Service first! (Step 1)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            // On Android 13+ request POST_NOTIFICATIONS first so the foreground
            // service notification is allowed — otherwise startForegroundService crashes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                if (granted != PackageManager.PERMISSION_GRANTED) {
                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@setOnClickListener
                }
            }
            launchCaptureIntent()
        }

        btnToggleBot.setOnClickListener { toggleBot() }

        updateStatus()
    }

    private fun launchCaptureIntent() {
        try {
            val intent = projectionManager!!.createScreenCaptureIntent()
            screenCaptureLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start capture: ${e.message}", Toast.LENGTH_LONG).show()
        }
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
            !ScreenCaptureService.isReady -> "Status: ⚠ Press 'Request Screen Capture'"
            botRunning -> "Status: 🟢 Running"
            else -> "Status: ✅ Ready — press START BOT"
        }
        btnToggleBot.isEnabled = ScreenCaptureService.isReady
    }
}
