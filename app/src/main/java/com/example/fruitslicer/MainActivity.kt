package com.example.fruitslicer

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnAccessibility: Button
    private lateinit var btnStartCapture: Button
    private lateinit var btnToggleBot: Button

    private var projectionManager: MediaProjectionManager? = null
    private var botRunning = false

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnStartCapture = findViewById(R.id.btnStartCapture)
        btnToggleBot = findViewById(R.id.btnToggleBot)

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        btnAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Find 'Fruit Auto Slicer' and enable it", Toast.LENGTH_LONG).show()
        }

        btnStartCapture.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Please enable the Accessibility Service first (Step 1)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val captureIntent = projectionManager!!.createScreenCaptureIntent()
            startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION)
        }

        btnToggleBot.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Enable Accessibility Service first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ScreenCaptureService.mediaProjection == null) {
                Toast.makeText(this, "Grant Screen Capture first (Step 2)!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            toggleBot()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Start the foreground service with the projection token
                val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
                }
                startForegroundService(serviceIntent)
                Toast.makeText(this, "Screen capture ready! Now press START BOT.", Toast.LENGTH_SHORT).show()
                updateStatus()
            } else {
                Toast.makeText(this, "Screen capture permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
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
        val captureOk = ScreenCaptureService.mediaProjection != null
        tvStatus.text = when {
            !accessOk -> "Status: ⚠ Accessibility not enabled"
            !captureOk -> "Status: ⚠ Screen capture not granted"
            botRunning -> "Status: 🟢 Running"
            else -> "Status: ✅ Ready — press START BOT"
        }
    }
}
