package com.example.notacara.data.ml

import android.content.Context
import android.util.Log
import com.google.mlkit.vision.text.Text
import org.json.JSONObject
import java.io.IOException

class TextProcessor(private val context: Context) {
    private var word2idx: Map<String, Int> = emptyMap()
    private val maxSeqLength = 256

    private data class OcrElement(
        val text: String,
        val yCenter: Float,
        val xMin: Float,
        val xMax: Float,
        val height: Float
    )

    init {
        loadVocab()
    }

    private fun loadVocab() {
        try {
            val jsonString = context.assets.open("word2idx.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, Int>()
            jsonObject.keys().forEach { key ->
                map[key.lowercase()] = jsonObject.getInt(key)
            }
            word2idx = map
        } catch (e: IOException) {
            Log.e("TextProcessor", "Gagal load vocab: ${e.message}")
        }
    }

    fun cleanRawText(text: String): String {
        return text
            .replace(Regex("([a-zA-Z])(\\d)"), "$1 $2")
            .replace(Regex("(\\d)([a-zA-Z])"), "$1 $2")
            .replace(Regex("(\\d{2,4})(\\d{1,2}[.:]\\d{2})"), "$1 $2")
            .replace(Regex("(\\d+)(\\d{2}[-/.]\\d{2}[-/.]\\d{2,4})"), "$1 $2")
            .replace(Regex("(\\d+)(\\d{2}\\s*[a-zA-Z]+\\s*\\d{2,4})"), "$1 $2")
            .replace(Regex("(\\d{2}[-/.]\\d{2}[-/.](?:20\\d{2}|(?!20)\\d{2}))(\\d+)"), "$1 $2")
            .replace(Regex("(\\d{2}[-/.]\\s*[a-zA-Z]+\\s*[-/.]\\d{2,4})(\\d+)"), "$1 $2")
            .replace(Regex("(\\d{2}(?:[-/.\\s])[A-Za-z]+(?:[-/.\\s](?:20\\d{2}|(?!20)\\d{2})))(\\d+)"), "$1 $2")
    }

    fun processVisionText(visionText: Text): String {
        val elements = mutableListOf<OcrElement>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val box = element.boundingBox ?: continue
                    elements.add(
                        OcrElement(
                            text = element.text,
                            yCenter = box.centerY().toFloat(),
                            xMin = box.left.toFloat(),
                            xMax = box.right.toFloat(),
                            height = box.height().toFloat()
                        )
                    )
                }
            }
        }

        if (elements.isEmpty()) return ""

        elements.sortBy { it.yCenter }
        val avgHeight = if (elements.isNotEmpty()) elements.map { it.height }.average().toFloat() else 0f
        val eps = avgHeight * 0.35f

        val lines = mutableListOf<MutableList<OcrElement>>()
        if (elements.isNotEmpty()) {
            var currentLine = mutableListOf(elements[0])
            lines.add(currentLine)
            for (i in 1 until elements.size) {
                if (elements[i].yCenter - elements[i - 1].yCenter <= eps) {
                    currentLine.add(elements[i])
                } else {
                    currentLine = mutableListOf(elements[i])
                    lines.add(currentLine)
                }
            }
        }

        val resultText = StringBuilder()
        for (line in lines) {
            line.sortBy { it.xMin }
            val lineText = StringBuilder()
            for (i in line.indices) {
                val item = line[i]
                if (i == 0) {
                    lineText.append(item.text)
                } else {
                    val prevItem = line[i - 1]
                    val gap = item.xMin - prevItem.xMax
                    val charWidth = item.height * 0.5f

                    if (gap > charWidth * 1.5f) {
                        val numSpaces = (gap / charWidth).toInt().coerceIn(2, 10)
                        lineText.append(" ".repeat(numSpaces)).append(item.text)
                    } else {
                        lineText.append(" ").append(item.text)
                    }
                }
            }
            resultText.append(lineText).append("\n")
        }

        return resultText.toString()
    }

    fun preprocess(text: String): IntArray {
        val cleaned = cleanRawText(text)
        val tokens = getTokens(cleaned)
        val indices = IntArray(maxSeqLength) { 0 }

        for (i in 0 until minOf(tokens.size, maxSeqLength)) {
            val rawToken = tokens[i]
            val lowercaseToken = rawToken.lowercase()
            val kerasCleanedToken = lowercaseToken.replace(Regex("[^a-z0-9]"), "")

            var idx = word2idx[lowercaseToken] ?: word2idx[kerasCleanedToken]

            if (idx == null) {
                val isPureDigit = kerasCleanedToken.isNotEmpty() && kerasCleanedToken.all { it.isDigit() }
                val isDateOrTime = rawToken.count { it == '.' || it == '/' || it == '-' || it == ':' } >= 2

                if (isPureDigit && !isDateOrTime) {
                    idx = if (kerasCleanedToken.length <= 2) {
                        word2idx["1"] ?: word2idx["2"]
                    } else {
                        word2idx["1000"] ?: word2idx["5000"]
                    }
                }
            }

            indices[i] = idx ?: word2idx["<oov>"] ?: word2idx["oov"] ?: 1
        }

        return indices
    }

    fun getTokens(text: String): List<String> {
        val regex = Regex("\\w+|[^\\w\\s]+")
        return regex.findAll(text)
            .map { it.value }
            .filter { it.isNotBlank() }
            .take(maxSeqLength)
            .toList()
    }
}
