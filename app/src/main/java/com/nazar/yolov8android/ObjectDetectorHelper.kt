package com.nazar.yolov8android

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ObjectDetectorHelper(
    var threshold: Float = 0.2f,
    var numThreads: Int = 4,
    var maxResults: Int = 5,
    var currentDelegate: Int = 0,
    private val context: Context,
    private val detectorListener: DetectorListener
) {

    private var interpreter: Interpreter? = null
    private val labels = mutableListOf<String>()

    private var tensorWidth: Int = 0
    private var tensorHeight: Int = 0
    private var numChannel: Int = 0
    private var numElements: Int = 0

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    private var objectDetector: ObjectDetector? = null

    init {
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        objectDetector = null
    }

    fun setupObjectDetector() {
        interpreter?.close()

        // Configure interpreter options
        val options = Interpreter.Options()

        if (CompatibilityList().isDelegateSupportedOnThisDevice) {
            options.addDelegate(GpuDelegate(GpuDelegate.Options()))
        } else {
            options.setNumThreads(4)
//            options.addDelegate(NnApiDelegate())
        }

        try {
            val model = loadModelFile("model.tflite")
            interpreter = Interpreter(model, options)
            initializeTensorShapes()
            loadLabels("labels.txt")
        } catch (e: IOException) {
            e.printStackTrace()
            detectorListener.onError("Model or labels could not be loaded: ${e.message}")
        }
    }

    private fun initializeTensorShapes() {
        interpreter?.let {
            val inputShape = it.getInputTensor(0).shape()
            val outputShape = it.getOutputTensor(0).shape()

            tensorWidth = inputShape[1]
            tensorHeight = inputShape[2]

            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2]
                tensorHeight = inputShape[3]
            }

            numChannel = outputShape[1]
            numElements = outputShape[2]
        } ?: run {
            detectorListener.onError("Interpreter is not initialized.")
        }
    }

    private fun loadLabels(fileName: String) {
        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        if (line.isNotBlank()) labels.add(line)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            detectorListener.onError("Labels could not be loaded: ${e.message}")
        }
    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(fileName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    fun detect(frame: Bitmap) {
        if (interpreter == null || tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) {
            detectorListener.onError("Interpreter or tensor shapes are not properly initialized.")
            return
        }

        val startTime = SystemClock.uptimeMillis()

        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)
        val tensorImage = TensorImage(INPUT_IMAGE_TYPE).apply { load(resizedBitmap) }
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter?.run(imageBuffer, outputBuffer.buffer)

        val bestBoxes = getBestBoxes(outputBuffer.floatArray)
        val inferenceTime = SystemClock.uptimeMillis() - startTime

        if (bestBoxes == null) {
            detectorListener.onEmptyDetect()
        } else {
            detectorListener.onResults(bestBoxes, inferenceTime)
        }
    }

    private fun getBestBoxes(array: FloatArray): List<BoundingBox>? {
        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            var maxConf = threshold
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > threshold && maxIdx in 0 until labels.size) {
                val clsName = labels[maxIdx]
                val cx = array[c]
                val cy = array[c + numElements]
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w / 2F)
                val y1 = cy - (h / 2F)
                val x2 = cx + (w / 2F)
                val y2 = cy + (h / 2F)
                if (x1 in 0F..1F && y1 in 0F..1F && x2 in 0F..1F && y2 in 0F..1F) {
                    boundingBoxes.add(
                        BoundingBox(x1, y1, x2, y2, cx, cy, w, h, maxConf, maxIdx, clsName)
                    )
                }
            }
        }

        return if (boundingBoxes.isNotEmpty()) applyNMS(boundingBoxes) else null
    }

    private fun applyNMS(boxes: List<BoundingBox>): List<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }
        val selectedBoxes = mutableListOf<BoundingBox>()

        for (box in sortedBoxes) {
//            if (selectedBoxes.size >= maxResults) break
            if (selectedBoxes.none { calculateIoU(it, box) >= IOU_THRESHOLD }) {
                selectedBoxes.add(box)
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    interface DetectorListener {
        fun onEmptyDetect()
        fun onError(error: String)
        fun onResults(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val IOU_THRESHOLD = 0.5F
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
    }
}
data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val cnf: Float,
    val classIdx: Int,
    val clsName: String
)
