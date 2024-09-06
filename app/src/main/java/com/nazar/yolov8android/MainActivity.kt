package com.nazar.yolov8android

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nazar.yolov8android.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), ObjectDetectorHelper.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false

    private lateinit var detector: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val cameraExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            setUpDetector()
            binding.viewFinder.post {
                setUpCamera()
            }
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }

        initBottomSheetControls()
    }

    private fun setUpDetector() {
        detector = ObjectDetectorHelper(
            threshold = 0.2f,
            numThreads = 4,
            maxResults = 5,
            currentDelegate = ObjectDetectorHelper.DELEGATE_CPU,
            context = this,
            detectorListener = this
        )
    }
    private fun initBottomSheetControls() {
        // When clicked, lower detection score threshold floor
        binding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (detector.threshold >= 0.1) {
                detector.threshold -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        binding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (detector.threshold <= 0.8) {
                detector.threshold += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, reduce the number of objects that can be detected at a time
//        binding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
//            if (detector.maxResults > 1) {
//                detector.maxResults--
//                updateControlsUi()
//            }
//        }

        // When clicked, increase the number of objects that can be detected at a time
//        binding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
//            if (detector.maxResults < 10) {
//                detector.maxResults++
//                updateControlsUi()
//            }
//        }

        // When clicked, decrease the number of threads used for detection
//        binding.bottomSheetLayout.threadsMinus.setOnClickListener {
//            if (detector.numThreads > 1) {
//                detector.numThreads--
//                updateControlsUi()
//            }
//        }

        // When clicked, increase the number of threads used for detection
//        binding.bottomSheetLayout.threadsPlus.setOnClickListener {
//            if (detector.numThreads < 4) {
//                detector.numThreads++
//                updateControlsUi()
//            }
//        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
//        binding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
//        binding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
//                    detector.currentDelegate = p2
//                    updateControlsUi()
//                }
//
//                override fun onNothingSelected(p0: AdapterView<*>?) {}
//            }

        // When clicked, change the underlying model used for object detection
//        binding.bottomSheetLayout.spinnerModel.setSelection(0, false)
//        binding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
//                    detector.currentModel = p2
//                    updateControlsUi()
//                }
//
//                override fun onNothingSelected(p0: AdapterView<*>?) {
//                    /* no op */
//                }
//            }
    }

    // Update the values displayed in the bottom sheet. Reset detector.
    private fun updateControlsUi() {
//        binding.bottomSheetLayout.maxResultsValue.text = detector.maxResults.toString()
        binding.bottomSheetLayout.thresholdValue.text = String.format("%.2f", detector.threshold)
//        binding.bottomSheetLayout.threadsValue.text = detector.numThreads.toString()

        detector.clearObjectDetector()
        binding.overlay.clear()
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { image ->
                    if (!::bitmapBuffer.isInitialized) {
                        bitmapBuffer = Bitmap.createBitmap(
                            image.width,
                            image.height,
                            Bitmap.Config.ARGB_8888
                        )
                    }

                    detectObjects(image)
                }
            }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, image.width.toFloat(), image.height.toFloat())
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        detector.detect(rotatedBitmap)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it[Manifest.permission.CAMERA] == true) {
            setUpDetector()
            binding.viewFinder.post {
                setUpCamera()
            }
        } else {
            Log.e(TAG, "Camera permission denied.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector?.close()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            setUpDetector()
            binding.viewFinder.post {
                setUpCamera()
            }
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            binding.overlay.clear()
            Log.d(TAG, "No detection results")
        }
    }

    override fun onResults(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.bottomSheetLayout.inferenceTimeVal.text = String.format("%d ms", inferenceTime)
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(baseContext, error, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "Camera"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
