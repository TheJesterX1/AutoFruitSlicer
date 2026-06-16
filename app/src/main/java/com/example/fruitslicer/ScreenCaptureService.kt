package com.example.fruitslicer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager

class ScreenCaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val CHANNEL_ID = "FruitSlicerChannel"
        private const val TAG = "ScreenCaptureService"

        // Use a simple flag instead of holding the projection object
        @Volatile var isReady = false
        @Volatile var latestBitmap: Bitmap? = null
        // Keep projection here so BotController can check
        @Volatile var mediaProjection: MediaProjection? = null
    }

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Fruit Auto Slicer")
            .setContentText("Screen capture active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
        startForeground(1, notification)

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

        if (resultCode == -1 || resultData == null) {
            Log.e(TAG, "Bad intent data")
            stopSelf()
            return START_NOT_STICKY
        }

        handler.postDelayed({
            setupCapture(resultCode, resultData)
        }, 500) // small delay to let foreground service fully start

        return START_STICKY
    }

    private fun setupCapture(resultCode: Int, resultData: Intent) {
        try {
            val metrics = DisplayMetrics()
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getMetrics(metrics)
            val w = metrics.widthPixels
            val h = metrics.heightPixels
            val dpi = metrics.densityDpi

            val pm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = pm.getMediaProjection(resultCode, resultData)
            mediaProjection = projection

            imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
            imageReader!!.setOnImageAvailableListener({ reader ->
                val image = try { reader.acquireLatestImage() } catch (e: Exception) { return@setOnImageAvailableListener } ?: return@setOnImageAvailableListener
                try {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * w
                    val bmp = Bitmap.createBitmap(w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888)
                    bmp.copyPixelsFromBuffer(buffer)
                    val cropped = Bitmap.createBitmap(bmp, 0, 0, w, h)
                    bmp.recycle()
                    latestBitmap?.recycle()
                    latestBitmap = cropped
                    isReady = true
                } catch (e: Exception) {
                    Log.e(TAG, "Frame error: ${e.message}")
                } finally {
                    image.close()
                }
            }, handler)

            virtualDisplay = projection.createVirtualDisplay(
                "FruitCapture", w, h, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface, null, handler
            )

            isReady = true
            Log.d(TAG, "Capture setup complete")

        } catch (e: Exception) {
            Log.e(TAG, "Setup failed: ${e.message}")
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isReady = false
        mediaProjection = null
        virtualDisplay?.release()
        imageReader?.close()
        latestBitmap?.recycle()
        latestBitmap = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Fruit Slicer", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
