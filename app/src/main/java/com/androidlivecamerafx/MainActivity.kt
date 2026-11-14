package com.androidlivecamerafx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var fpsText: TextView

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val frameCount = AtomicInteger(0)
    private val fpsHandler = Handler(Looper.getMainLooper())
    private var fpsTask: Runnable? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else statusText.text = "مجوز دوربین داده نشد"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        statusText = findViewById(R.id.statusText)
        fpsText = findViewById(R.id.fpsText)

        // Prefer the lower-cost rendering path on supported devices.
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        statusText.text = "در حال راه‌اندازی دوربین..."
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            val provider = providerFuture.get()
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(640, 480),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                )
                .build()

            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(cameraExecutor) { image ->
                frameCount.incrementAndGet()
                image.close()
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis
                )
                statusText.text = "دوربین فعال است • حالت بهینه‌سازی"
                startFpsMonitor()
            } catch (error: Exception) {
                statusText.text = "خطای راه‌اندازی: ${error.localizedMessage ?: "نامشخص"}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startFpsMonitor() {
        fpsTask?.let(fpsHandler::removeCallbacks)
        val task = object : Runnable {
            override fun run() {
                val fps = frameCount.getAndSet(0)
                fpsText.text = "FPS پردازش: $fps  •  وضوح هدف: 640×480"
                fpsHandler.postDelayed(this, 1000L)
            }
        }
        fpsTask = task
        fpsHandler.post(task)
    }

    override fun onDestroy() {
        fpsTask?.let(fpsHandler::removeCallbacks)
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}
