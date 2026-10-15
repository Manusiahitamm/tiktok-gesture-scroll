package com.example.gesturescroll

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var status: TextView
    private lateinit var toggle: Button
    private var landmarker: HandLandmarker? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var running = false
    private var previousY: Float? = null
    private var lastMotionAt = 0L

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else status.text = "Izin kamera diperlukan untuk membaca tangan."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        status = TextView(this).apply {
            text = "1. Aktifkan layanan aksesibilitas.\\n2. Letakkan tangan di depan kamera."
            textSize = 16f
        }
        previewView = PreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        val accessibility = Button(this).apply {
            text = "Buka Pengaturan Aksesibilitas"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        toggle = Button(this).apply {
            text = "Mulai Auto Scroll"
            setOnClickListener {
                val service = ScrollAccessibilityService.instance
                if (service == null) {
                    status.text = "Aktifkan Gesture Scroll di pengaturan Aksesibilitas, lalu kembali."
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } else {
                    running = !running
                    service.enabled = running
                    text = if (running) "Hentikan Auto Scroll" else "Mulai Auto Scroll"
                    status.text = if (running) "Aktif. Gerakkan tangan naik/turun perlahan." else "Auto scroll berhenti."
                }
            }
        }
        root.addView(status)
        root.addView(accessibility)
        root.addView(previewView)
        root.addView(toggle)
        setContentView(root)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) startCamera()
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun createLandmarker() {
        try {
            val base = BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build()
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setResultListener { result: HandLandmarkerResult, _ ->
                    if (running && result.landmarks().isNotEmpty()) {
                        val y = result.landmarks()[0][0].y()
                        val old = previousY
                        val now = System.currentTimeMillis()
                        if (old != null && now - lastMotionAt > 700) {
                            val delta = y - old
                            val service = ScrollAccessibilityService.instance
                            if (delta < -0.10f) {
                                service?.scrollDown()
                                lastMotionAt = now
                                runOnUiThread { status.text = "Tangan naik: scroll ke video berikutnya." }
                            } else if (delta > 0.10f) {
                                service?.scrollUp()
                                lastMotionAt = now
                                runOnUiThread { status.text = "Tangan turun: scroll ke atas." }
                            }
                        }
                        previousY = y
                    } else if (result.landmarks().isEmpty()) {
                        previousY = null
                    }
                }
                .setErrorListener { error -> runOnUiThread { status.text = "Deteksi tangan: ${error.message}" } }
                .build()
            landmarker = HandLandmarker.createFromOptions(this, options)
        } catch (e: Exception) {
            status.text = "Model tangan belum tersedia. Lihat README di proyek."
        }
    }

    private fun startCamera() {
        createLandmarker()
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val bitmap = previewView.bitmap
                    if (bitmap != null && landmarker != null) {
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        landmarker?.detectAsync(mpImage, System.currentTimeMillis())
                    }
                } catch (_: Exception) {
                } finally {
                    imageProxy.close()
                }
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
            } catch (e: Exception) {
                status.text = "Kamera tidak dapat dimulai: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        running = false
        ScrollAccessibilityService.instance?.enabled = false
        landmarker?.close()
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}
