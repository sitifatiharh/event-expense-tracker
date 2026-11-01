package com.example.notacara.data.ml

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class ViterbiDecoder(private val context: Context) {
    internal var transitions: Array<FloatArray> = arrayOf()
    internal var idx2label: Map<Int, String> = emptyMap()
    internal var numTags: Int = 0

    init {
        loadCrfData()
    }

    internal fun loadCrfData() {
        try {
            val labelsString = context.assets.open("idx2label.json")
                .bufferedReader().use { it.readText() }
            val labelsJson = JSONObject(labelsString)
            val map = mutableMapOf<Int, String>()
            labelsJson.keys().forEach { key ->
                map[key.toInt()] = labelsJson.getString(key)
            }
            idx2label = map
            numTags = map.size

            val transString = context.assets.open("crf_transitions.json").bufferedReader().use { it.readText() }
            val transArray = JSONArray(transString)
            
            transitions = Array(numTags) { FloatArray(numTags) }
            for (i in 0 until numTags) {
                val row = transArray.getJSONArray(i)
                for (j in 0 until numTags) {
                    transitions[i][j] = row.getDouble(j).toFloat()
                }
            }
            
            Log.d("ViterbiDecoder", "Data CRF berhasil dimuat. Total Tag: $numTags")
        } catch (e: Exception) {
            Log.e("ViterbiDecoder", "Gagal memuat data CRF: ${e.message}", e)
        }
    }

    fun decode(emissions: Array<FloatArray>): List<String> {
        val seqLen = emissions.size
        if (seqLen == 0 || idx2label.isEmpty() || numTags == 0) return emptyList()

        val viterbi = Array(seqLen) { FloatArray(numTags) { -1e10f } }
        val backpointer = Array(seqLen) { IntArray(numTags) { 0 } }

        for (i in 0 until numTags) {
            viterbi[0][i] = emissions[0][i]
        }

        for (t in 1 until seqLen) {
            for (currTag in 0 until numTags) {
                var maxScore = -1e10f
                var bestPrevTag = 0
                for (prevTag in 0 until numTags) {
                    val score = viterbi[t - 1][prevTag] + transitions[prevTag][currTag] + emissions[t][currTag]
                    if (score > maxScore) {
                        maxScore = score
                        bestPrevTag = prevTag
                    }
                }
                viterbi[t][currTag] = maxScore
                backpointer[t][currTag] = bestPrevTag
            }
        }

        // Backtracking
        val bestPath = IntArray(seqLen)
        var maxFinalScore = -1e10f
        var lastTag = 0
        for (i in 0 until numTags) {
            if (viterbi[seqLen - 1][i] > maxFinalScore) {
                maxFinalScore = viterbi[seqLen - 1][i]
                lastTag = i
            }
        }
        bestPath[seqLen - 1] = lastTag

        for (t in seqLen - 2 downTo 0) {
            bestPath[t] = backpointer[t + 1][bestPath[t + 1]]
        }

        return bestPath.map { idx2label[it] ?: "O" }
    }
}
