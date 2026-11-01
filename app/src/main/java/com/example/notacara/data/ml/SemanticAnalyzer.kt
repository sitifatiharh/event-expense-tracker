package com.example.notacara.data.ml

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.notacara.data.model.AnalysisResult
import com.example.notacara.data.model.NotaItemEdit
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class SemanticAnalyzer(private val context: Context) {

    private val textProcessor = TextProcessor(context)
    private val tfliteManager = TFLiteManager(context)
    private val viterbiDecoder = ViterbiDecoder(context)

    fun analyzeImage(imageUri: Uri): AnalysisResult {
        return try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromFilePath(context, imageUri)
            val visionText = Tasks.await(recognizer.process(image))
            if (visionText.text.isNotBlank()) {
                val result = analyze(visionText, imageUri.toString())
                result.copy(rawText = visionText.text)
            } else {
                AnalysisResult(namaToko = "Teks tidak terdeteksi", imageUri = imageUri.toString())
            }
        } catch (e: Exception) {
            Log.e("SemanticAnalyzer", "Error analyzeImage: ${e.message}")
            AnalysisResult(namaToko = "Gagal Deteksi (Edit Manual)", imageUri = imageUri.toString())
        }
    }

    fun analyze(visionText: Text, imageUri: String = ""): AnalysisResult {
        val reconstructedText = textProcessor.processVisionText(visionText)
        return analyze(reconstructedText, imageUri)
    }

    fun analyze(rawText: String, imageUri: String = ""): AnalysisResult {
        if (rawText.isBlank()) return AnalysisResult(imageUri = imageUri)

        val cleanedText = textProcessor.cleanRawText(rawText)
        val tokens = textProcessor.getTokens(cleanedText)
        val indices = textProcessor.preprocess(cleanedText)
        val emissions = tfliteManager.predict(indices)

        val aiResult = if (emissions.isNotEmpty()) {
            val actualEmissions = emissions.take(tokens.size).toTypedArray()
            val labels = viterbiDecoder.decode(actualEmissions).toMutableList()

            for (i in 0 until labels.size - 1) {
                if (tokens[i] == "20" && labels[i] == "O" && labels[i + 1].contains("tanggal")) {
                    labels[i] = "B-tanggal"
                    if (labels[i + 1].startsWith("B-")) labels[i + 1] = labels[i + 1].replace("B-", "I-")
                }
            }

            for (i in 0 until labels.size - 1) {
                if (labels[i] == "B-tanggal" && labels[i + 1] == "B-tanggal") {
                    labels[i] = "O"
                }
            }
            extractEntities(tokens, labels, imageUri)
        } else {
            AnalysisResult(imageUri = imageUri)
        }

        return refineResult(cleanedText, aiResult)
    }

    private fun extractEntities(tokens: List<String>, labels: List<String>, imageUri: String): AnalysisResult {
        val fixedLabels = labels.toMutableList()
        for (i in 1 until fixedLabels.size) {
            if (fixedLabels[i - 1] == "O" && fixedLabels[i].startsWith("I-")) {
                val category = fixedLabels[i].substring(2)
                fixedLabels[i - 1] = "B-$category"
            }
        }

        var namaToko = ""; var tanggal = ""; var totalBelanja = ""; var diskonStr = ""
        val items = mutableListOf<NotaItemEdit>()
        var curItemNama = ""; var curItemJumlah = ""; var curItemSatuan = ""; var curItemSubtotal = ""

        fun finalizeItem() {
            if (curItemNama.isNotBlank() || curItemJumlah.isNotBlank() || 
                curItemSatuan.isNotBlank() || curItemSubtotal.isNotBlank()) {
                val item = NotaItemEdit(
                    nama = curItemNama.trim(),
                    jumlah = curItemJumlah.trim(),
                    satuan = curItemSatuan.filter { it.isDigit() },
                    subtotal = curItemSubtotal.filter { it.isDigit() }
                )
                items.add(validateAndCompleteItem(item))
            }
            curItemNama = ""; curItemJumlah = ""; curItemSatuan = ""; curItemSubtotal = ""
        }

        for (i in tokens.indices) {
            val token = tokens[i]
            val label = fixedLabels[i]
            val cleanLabel = if (label.contains("-")) label.substringAfter("-") else label

            if (label.startsWith("B-")) {
                val isCollision = when (cleanLabel) {
                    "nama_barang" -> curItemNama.isNotBlank()
                    "jumlah" -> curItemJumlah.isNotBlank()
                    "harga_satuan" -> curItemSatuan.isNotBlank()
                    "harga_total_barang" -> curItemSubtotal.isNotBlank()
                    else -> false
                }
                if (isCollision) finalizeItem()
            }

            when {
                label.contains("nama_toko") -> namaToko = smartAppend(namaToko, token)
                label.contains("tanggal") -> tanggal = smartAppend(tanggal, token)
                label.contains("total_belanja") -> totalBelanja += token.filter { it.isDigit() }
                label.contains("diskon") -> diskonStr += token.filter { it.isDigit() }
                label.contains("nama_barang") -> curItemNama = smartAppend(curItemNama, token)
                label.contains("harga_total_barang") -> curItemSubtotal += token.filter { it.isDigit() }
                label.contains("jumlah") -> curItemJumlah += token.replace(",", ".")
                label.contains("harga_satuan") -> curItemSatuan += token.filter { it.isDigit() }
            }
        }
        finalizeItem()

        return AnalysisResult(
            namaToko = namaToko.trim(), tanggal = tanggal.trim(), items = items,
            diskon = diskonStr.ifBlank { "0" }, totalBelanja = formatPrice(totalBelanja),
            imageUri = imageUri
        )
    }

    internal fun smartAppend(current: String, token: String): String {
        if (current.isEmpty()) return token
        val last = current.last()
        val first = token.firstOrNull() ?: return current
        return when {
            last.isDigit() && first.isLetter() -> current + token
            !last.isLetterOrDigit() || !first.isLetterOrDigit() -> current + token
            else -> "$current $token"
        }
    }

    internal fun validateAndCompleteItem(item: NotaItemEdit): NotaItemEdit {
        var qty = item.jumlah.replace(",", ".").toDoubleOrNull() ?: 0.0
        var price = item.satuan.toDoubleOrNull() ?: 0.0
        var subtotal = item.subtotal.toDoubleOrNull() ?: 0.0
        if (subtotal == 0.0 && qty > 0 && price > 0) subtotal = qty * price
        if (subtotal > 0.0) {
            if (qty <= 0.0 && price <= 0.0) { qty = 1.0; price = subtotal }
            else if (qty > 0.0 && price <= 0.0) price = subtotal / qty
            else if (price > 0.0 && qty <= 0.0) qty = (subtotal / price).let { if (it < 1.0 && it > 0) 1.0 else it }
            else if (qty > 0.0 && price > 0.0 && abs((qty * price) - subtotal) > 1.0) price = subtotal / qty
        }
        if (item.nama.isNotBlank() && qty == 0.0) qty = 1.0
        return item.copy(
            jumlah = if (qty == 0.0) "" else if (qty % 1.0 == 0.0) String.format("%.0f", qty) else String.format("%.2f", qty),
            satuan = if (price == 0.0) "" else String.format("%.0f", price),
            subtotal = if (subtotal == 0.0) "" else String.format("%.0f", subtotal)
        )
    }

    private fun refineResult(rawText: String, result: AnalysisResult): AnalysisResult {
        return result.copy(
            namaToko = (if (result.namaToko.isBlank()) rawText.lines().firstOrNull { it.isNotBlank() } ?: "" else result.namaToko).uppercase(),
            tanggal = normalizeDate(result.tanggal)
        )
    }

    internal fun normalizeDate(rawDate: String): String {
        if (rawDate.isBlank()) return ""
        val monthsMap = mapOf(
            "jan" to "01", "feb" to "02", "mar" to "03", "apr" to "04",
            "mei" to "05", "may" to "05", "jun" to "06", "jul" to "07",
            "agu" to "08", "agt" to "08", "aug" to "08", "sep" to "09",
            "okt" to "10", "oct" to "10", "nov" to "11", "des" to "12",
            "dec" to "12")
        val separated = rawDate.replace(Regex("(\\d)([a-zA-Z])"), "$1 $2").replace(Regex("([a-zA-Z])(\\d)"), "$1 $2")
        val segments = separated.replace(Regex("[^a-zA-Z0-9]"), " ").split(Regex("\\s+")).filter { it.isNotBlank() }
        if (segments.size < 2) return rawDate.trim()

        var d = ""; var m = ""; var y = ""

        if (segments[0].length == 4 && segments[0].startsWith("20")) {
            y = segments[0]; m = segments[1]; d = segments[2]
        } else if (segments[0] == "20" && segments.size >= 4 && segments[1].length == 2 && segments[1].all { it.isDigit() }) {
            y = "20" + segments[1]; m = segments[2]; d = segments[3]
        } else {
            val yearIdx = segments.indexOfLast { (it.length == 4 && it.startsWith("20")) || (it.length == 2 && it.toIntOrNull() in 20..99) }
            if (yearIdx >= 2) {
                y = segments[yearIdx]; m = segments[yearIdx - 1]; d = segments[yearIdx - 2]
            } else if (segments.size >= 3) {
                d = segments[0]; m = segments[1]; y = segments[2]
            }
        }

        val dNum = d.filter { it.isDigit() }.takeLast(2).padStart(2, '0')
        val mNum = if (m.any { it.isLetter() }) getMonthVal(m, monthsMap) ?: "01" else m.filter { it.isDigit() }.padStart(2, '0').takeLast(2)
        val rawYNum = y.filter { it.isDigit() }
        val yNum = if (rawYNum.length == 2) "20$rawYNum" else rawYNum.takeLast(4).padStart(4, '0')

        return if (dNum != "00" && mNum != "00" && yNum.length == 4) "$dNum/$mNum/$yNum" else rawDate.trim()
    }

    private fun getMonthVal(s: String, monthsMap: Map<String, String>): String? {
        val low = s.lowercase()
        for ((key, value) in monthsMap) if (low.startsWith(key)) return value
        return s.filter { it.isDigit() }.toIntOrNull()?.let { if (it in 1..12) String.format("%02d", it) else null }
    }

    internal fun formatPrice(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return "0"
        return try {
            NumberFormat.getInstance(Locale("id", "ID")).format(digits.toLong())
        } catch (e: Exception) { digits }
    }
}
