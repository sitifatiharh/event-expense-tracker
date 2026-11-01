package com.example.notacara.data.ml

import android.content.Context
import android.util.Log
import com.example.notacara.data.model.AnalysisResult
import org.json.JSONObject

object AccuracyTest {

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

    private var currentScanCount = 0
    private val cleanStats = Stats()
    private val dirtyStats = Stats()
    private val overallStats = Stats()
    
    private var imageFileList: List<String> = emptyList()

    fun evaluate(context: Context, result: AnalysisResult, rawOcrText: String) {
        val assetManager = context.assets
        
        // Mode Debug: Prioritaskan file 0.json dan 0.txt jika ada
        val manualJsonExists = try { assetManager.open("groundtruth/json/0.json").use { true } } catch (e: Exception) { false }
        val manualTxtExists = try { assetManager.open("groundtruth/txt/0.txt").use { true } } catch (e: Exception) { false }

        if (manualJsonExists && manualTxtExists) {
            runManualSingleTest(context, result, rawOcrText)
            return
        }

        // Mode Sekuensial: Untuk pengujian 50 sampel
        if (imageFileList.isEmpty()) {
            imageFileList = (assetManager.list("groundtruth/img") ?: emptyArray()).sorted().toList()
        }

        if (currentScanCount >= imageFileList.size) {
            Log.w("OCR_EVAL", "Semua sampel groundtruth ($currentScanCount) sudah diuji.")
            return
        }

        val fileName = imageFileList[currentScanCount]
        val baseName = fileName.substringBeforeLast(".")
        val isClean = fileName.startsWith("a_")
        val currentStats = if (isClean) cleanStats else dirtyStats

        try {
            val gtText = assetManager.open("groundtruth/txt/$baseName.txt").bufferedReader().use { it.readText() }
            val gtJson = JSONObject(assetManager.open("groundtruth/json/$baseName.json").bufferedReader().use { it.readText() })

            val cer = calculateCER(gtText, rawOcrText)
            val wer = calculateWER(gtText, rawOcrText)

            val mToko = checkToko(result, gtJson)
            val mTgl = checkTanggal(result, gtJson)
            val mTot = checkTotal(result, gtJson)
            val mItems = checkItems(result, gtJson)
            val isSempurna = mToko && mTgl && mTot

            updateStats(currentStats, fileName, cer, wer, mToko, mTgl, mTot, mItems, isSempurna)
            updateStats(overallStats, fileName, cer, wer, mToko, mTgl, mTot, mItems, isSempurna)

            currentScanCount++

            Log.i("OCR_EVAL", "--- HASIL SCAN NOTA KE-$currentScanCount ($fileName) ---")
            Log.i("OCR_EVAL", "CER: ${String.format("%.2f", cer)}% | WER: ${String.format("%.2f", wer)}%")
            Log.i("OCR_EVAL", "Toko: ${mark(mToko)} | Tgl: ${mark(mTgl)} | Total: ${mark(mTot)} | Item: ${mark(mItems)}")
            Log.i("OCR_EVAL", "Status: ${if (isSempurna) "✅ SEMPURNA" else "❌ TIDAK SEMPURNA"}")

            if (currentScanCount == 25) {
                printSummary("RINGKASAN KELOMPOK A (BERSIH)", cleanStats)
            } else if (currentScanCount == 50) {
                printSummary("RINGKASAN KELOMPOK B (KOTOR)", dirtyStats)
                printSummary("RINGKASAN KESELURUHAN", overallStats)
            }

        } catch (e: Exception) {
            Log.e("OCR_EVAL", "Error evaluating $fileName: ${e.message}")
        }
    }

    private fun runManualSingleTest(context: Context, result: AnalysisResult, rawOcrText: String) {
        try {
            val assetManager = context.assets
            val gtText = assetManager.open("groundtruth/txt/0.txt").bufferedReader().use { it.readText() }
            val gtJson = JSONObject(assetManager.open("groundtruth/json/0.json").bufferedReader().use { it.readText() })

            val cer = calculateCER(gtText, rawOcrText)
            val wer = calculateWER(gtText, rawOcrText)

            val mToko = checkToko(result, gtJson)
            val mTgl = checkTanggal(result, gtJson)
            val mTot = checkTotal(result, gtJson)
            val mItems = checkItems(result, gtJson)
            val isSempurna = mToko && mTgl && mTot

            Log.i("OCR_EVAL", "***************************************************")
            Log.i("OCR_EVAL", "*** DEBUG MODE: SINGLE TEST (Menggunakan 0.json) ***")
            Log.i("OCR_EVAL", "CER: ${String.format("%.2f", cer)}% | WER: ${String.format("%.2f", wer)}%")
            Log.i("OCR_EVAL", "Toko: ${mark(mToko)} | Tgl: ${mark(mTgl)} | Total: ${mark(mTot)} | Item: ${mark(mItems)}")
            Log.i("OCR_EVAL", "Status: ${if (isSempurna) "✅ SEMPURNA" else "❌ TIDAK SEMPURNA"}")
            Log.i("OCR_EVAL", "***************************************************")
        } catch (e: Exception) {
            Log.e("OCR_EVAL", "Gagal menjalankan Single Test: ${e.message}")
        }
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
            fileName, mark(toko), mark(tgl), mark(tot), mark(items), if(sempurna) "✅" else "❌")
        s.detailedLogs.append(line)
    }

    private fun printSummary(title: String, s: Stats) {
        if (s.count == 0) return
        Log.i("OCR_EVAL", """
            
            ================================================================================
            --- $title ---
            ================================================================================
            Jumlah Sampel     : ${s.count}
            Rerata CER        : ${String.format("%.2f", s.totalCER / s.count)}%
            Rerata WER        : ${String.format("%.2f", s.totalWER / s.count)}%
            --------------------------------------------------------------------------------
            Akurasi Toko      : ${perc(s.matchToko, s.count)}%
            Akurasi Tanggal   : ${perc(s.matchTanggal, s.count)}%
            Akurasi Total     : ${perc(s.matchTotal, s.count)}%
            Akurasi Jml Item  : ${perc(s.matchItemSize, s.count)}%
            --------------------------------------------------------------------------------
            LULUS SEMPURNA    : ${perc(s.matchSempurna, s.count)}%
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

    private fun checkTotal(res: AnalysisResult, gt: JSONObject): Boolean {
        var gtStr = gt.optString("total_belanja").trim()
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
