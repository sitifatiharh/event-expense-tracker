package com.example.notacara

import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notacara.data.ml.SemanticAnalyzer
import com.example.notacara.data.model.AnalysisResult
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccuracyTest {

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val analyzer = SemanticAnalyzer(targetContext)

    private class Stats {
        var totalCER = 0.0
        var totalWER = 0.0
        var matchToko = 0
        var matchTanggal = 0
        var matchTotal = 0
        var matchItemSize = 0
        var matchSempurna = 0
        var count = 0
        val detailedLogs = StringBuilder()
    }

    @Test
    fun runAccuracyEvaluation() {
        val assetManager = testContext.assets
        val imageFiles = assetManager.list("groundtruth/img") ?: return

        val cleanStats = Stats()
        val dirtyStats = Stats()
        val overallStats = Stats()

        imageFiles.sorted().forEach { fileName ->
            val baseName = fileName.substringBeforeLast(".")
            val isClean = fileName.startsWith("a_")
            val currentStats = if (isClean) cleanStats else dirtyStats

            try {
                val bitmap = BitmapFactory.decodeStream(assetManager.open("groundtruth/img/$fileName"))
                val visionText = Tasks.await(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(InputImage.fromBitmap(bitmap, 0)))

                val gtText = assetManager.open("groundtruth/txt/$baseName.txt").bufferedReader().use { it.readText() }
                val gtJson = JSONObject(assetManager.open("groundtruth/json/$baseName.json").bufferedReader().use { it.readText() })

                val result = analyzer.analyze(visionText)

                val cer = calculateCER(gtText, visionText.text)
                val wer = calculateWER(gtText, visionText.text)

                val mToko = checkToko(result, gtJson)
                val mTgl = checkTanggal(result, gtJson)
                val mTot = checkTotal(result, gtJson)
                val mItems = checkItems(result, gtJson)
                val isSempurna = mToko && mTgl && mTot

                updateStats(currentStats, fileName, cer, wer, mToko, mTgl, mTot, mItems, isSempurna)
                updateStats(overallStats, fileName, cer, wer, mToko, mTgl, mTot, mItems, isSempurna)

            } catch (e: Exception) {
                Log.e("AccuracyTest", "Error processing $fileName: ${e.message}")
            }
        }

        printSummary("KONDISI BERSIH (A - a_test_xx)", cleanStats)
        printSummary("KONDISI KOTOR (B - b_test_xx)", dirtyStats)
        printSummary("HASIL KESELURUHAN SISTEM", overallStats)
    }

    private fun mark(b: Boolean) = if (b) "✅" else "❌"

    private fun updateStats(s: Stats, fileName: String, cer: Double, wer: Double, toko: Boolean, tgl: Boolean, tot: Boolean, items: Boolean, sempurna: Boolean) {
        s.count++
        s.totalCER += cer
        s.totalWER += wer
        if (toko) s.matchToko++
        if (tgl) s.matchTanggal++
        if (tot) s.matchTotal++
        if (items) s.matchItemSize++
        if (sempurna) s.matchSempurna++

        val line = String.format("\n   %-12s | Toko:%s | Tgl:%s | Tot:%s | Itm:%s | %s",
            fileName, mark(toko), mark(tgl), mark(tot), mark(items), if(sempurna) "✅ LULUS" else "❌ FAIL")
        s.detailedLogs.append(line)
    }

    private fun printSummary(title: String, s: Stats) {
        if (s.count == 0) return
        Log.i("FINAL_RESULT", """
            
            ================================================================================
            --- $title ---
            ================================================================================
            DETAIL PER FOTO:
            ${s.detailedLogs}
            
            --------------------------------------------------------------------------------
            RINGKASAN STATISTIK:
            Jumlah Sampel     : ${s.count}
            Rerata CER        : ${String.format("%.2f", s.totalCER / s.count)}%
            Rerata WER        : ${String.format("%.2f", s.totalWER / s.count)}%
            --------------------------------------------------------------------------------
            1. Akurasi Toko      : ${perc(s.matchToko, s.count)}% (Match: ${s.matchToko}, Fail: ${s.count - s.matchToko})
            2. Akurasi Tanggal   : ${perc(s.matchTanggal, s.count)}% (Match: ${s.matchTanggal}, Fail: ${s.count - s.matchTanggal})
            3. Akurasi Total     : ${perc(s.matchTotal, s.count)}% (Match: ${s.matchTotal}, Fail: ${s.count - s.matchTotal})
            4. Akurasi Jml Item  : ${perc(s.matchItemSize, s.count)}% (Match: ${s.matchItemSize}, Fail: ${s.count - s.matchItemSize})
            --------------------------------------------------------------------------------
            LULUS SEMPURNA (E2E) : ${perc(s.matchSempurna, s.count)}% (Match: ${s.matchSempurna})
            ================================================================================
        """.trimIndent())
    }

    private fun perc(m: Int, t: Int) = String.format("%.2f", (m.toDouble() / t) * 100)

    private fun checkToko(res: AnalysisResult, gt: JSONObject): Boolean {
        val gtVal = gt.optString("nama_toko").lowercase().trim()
        val resVal = res.namaToko.lowercase().trim()
        if (gtVal.isEmpty()) return resVal.isEmpty()
        val dist = levenshtein(gtVal, resVal)
        val thres = (gtVal.length * 0.25).toInt().coerceAtLeast(1)
        if (dist <= thres) return true
        val cleanGt = gtVal.filter { it.isLetterOrDigit() }
        val cleanRes = resVal.filter { it.isLetterOrDigit() }
        return cleanRes.contains(cleanGt) || cleanGt.contains(cleanRes)
    }

    private fun checkTanggal(res: AnalysisResult, gt: JSONObject): Boolean {
        return res.tanggal.trim() == gt.optString("tanggal").trim()
    }

    private fun checkTotal(res: AnalysisResult, gt: JSONObject): Boolean {var gtStr = gt.optString("total_belanja").trim()

        // Menangani desimal: Hapus 1-2 digit di akhir jika diawali koma atau titik
        // Contoh: "25.000,0" -> "25.000", "35.000,00" -> "35.000"
        gtStr = gtStr.replace(Regex("[,.]\\d{1,2}$"), "")

        val gtVal = gtStr.filter { it.isDigit() }
        val calculatedTotal = res.items.sumOf { it.subtotal.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L }
        val diskon = res.diskon.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val finalResTotal = (calculatedTotal - diskon).coerceAtLeast(0).toString()

        return gtVal == finalResTotal && gtVal.isNotEmpty()
    }

    private fun checkItems(res: AnalysisResult, gt: JSONObject): Boolean {
        val gtItems = gt.optJSONArray("items") ?: return res.items.isEmpty()
        if (gtItems.length() != res.items.size) return false
        for (i in 0 until gtItems.length()) {
            val gtItem = gtItems.getJSONObject(i)
            val gtSub = gtItem.optString("harga_total_barang").filter { it.isDigit() }
            val resSub = res.items[i].subtotal.filter { it.isDigit() }
            if (gtSub != resSub) return false
        }
        return true
    }

    private fun calculateCER(gt: String, pred: String): Double {
        val s1 = gt.lowercase().trim(); val s2 = pred.lowercase().trim()
        if (s1.isEmpty()) return if (s2.isEmpty()) 0.0 else 100.0
        return (levenshtein(s1, s2).toDouble() / s1.length) * 100
    }

    private fun calculateWER(gt: String, pred: String): Double {
        val s1 = gt.lowercase().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val s2 = pred.lowercase().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (s1.isEmpty()) return if (s2.isEmpty()) 0.0 else 100.0
        val dp = Array(s1.size + 1) { IntArray(s2.size + 1) }
        for (i in 0..s1.size) dp[i][0] = i
        for (j in 0..s2.size) dp[0][j] = j
        for (i in 1..s1.size) {
            for (j in 1..s2.size) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return (dp[s1.size][s2.size].toDouble() / s1.size) * 100
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }
}
