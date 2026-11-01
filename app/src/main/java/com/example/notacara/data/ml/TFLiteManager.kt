package com.example.notacara.data.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteManager(private val context: Context) {
    internal var interpreter: Interpreter? = null
    private val modelPath = "model_bilstm.tflite"
    internal var maxSeqLength = 256 
    internal var numTags = 17 

    init {
        loadModel()
    }

    internal fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options().apply {
                addDelegate(FlexDelegate())
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            
            interpreter?.let { interp ->
                val inputTensor = interp.getInputTensor(0)
                if (inputTensor.shape().size >= 2) {
                    maxSeqLength = inputTensor.shape()[1]
                }
                val outputTensor = interp.getOutputTensor(0)
                if (outputTensor.shape().size >= 3) {
                    numTags = outputTensor.shape()[2]
                }
            }
            Log.d("TFLITE_DEBUG", "Model berhasil dimuat.")
        } catch (e: Exception) {
            Log.e("TFLITE_DEBUG", "Gagal memuat model: ${e.message}")
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset, fileDescriptor.length)
        fileDescriptor.close()
        return buffer
    }

    fun predict(input: IntArray): Array<FloatArray> {
        val tflite = interpreter ?: return arrayOf()
        val inputBuffer = Array(1) { FloatArray(maxSeqLength) }
        for (i in 0 until minOf(input.size, maxSeqLength)) {
            inputBuffer[0][i] = input[i].toFloat()
        }
        val outputBuffer = Array(1) { Array(maxSeqLength) { FloatArray(numTags) } }
        
        return try {
            tflite.run(inputBuffer, outputBuffer)
            outputBuffer[0]
        } catch (e: Exception) {
            Log.e("TFLITE_DEBUG", "Inference Error: ${e.message}")
            arrayOf()
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
