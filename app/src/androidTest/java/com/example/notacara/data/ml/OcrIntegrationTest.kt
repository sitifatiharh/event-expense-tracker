package com.example.notacara.data.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Instrumented test untuk Modul On-Device OCR (Google ML Kit).
 * Menguji integrasi hardware (kamera/file) dengan library ML Kit.
 */
@RunWith(AndroidJUnit4::class)
class OcrIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testOcr_Initialization_IsSuccessful() {
        // Poin 1: Inisialisasi komponen TextRecognizer berhasil
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        assertNotNull("TextRecognizer harus berhasil dimuat di memori", recognizer)
    }

    @Test
    fun testOcr_WithEmptyImage_ReturnsEmptyString() {
        // Poin 4: Menangani kondisi gambar tidak mengandung teks
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        // Buat bitmap hitam polos (tanpa teks)
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val image = InputImage.fromBitmap(bitmap, 0)
        
        val result = Tasks.await(recognizer.process(image))
        
        assertTrue("Hasil OCR harus string kosong jika gambar tidak ada teks", result.text.isEmpty())
    }

    @Test
    fun testOcr_WithValidText_ExtractsCorrectly() {
        // Poin 3: Berhasil mengekstraksi karakter dari gambar jelas
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        // Simulasi gambar nota dengan teks "TOTAL 50000"
        val bitmap = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
        }
        canvas.drawText("TOTAL 50000", 50f, 50f, paint)
        
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = Tasks.await(recognizer.process(image))
        
        // Cek apakah teks berhasil terbaca
        assertTrue("OCR harus mengandung kata 'TOTAL'", result.text.contains("TOTAL"))
        assertTrue("OCR harus mengandung angka '50000'", result.text.contains("50000"))
    }

    @Test
    fun testOcr_WithInvalidUri_ThrowsException() {
        // Poin 5: Exception handling jika URI rusak/null
        val invalidUri = Uri.fromFile(File(context.cacheDir, "non_existent.jpg"))
        
        assertThrows(Exception::class.java) {
            // ML Kit akan throw exception saat mencoba meload file yang tidak ada
            InputImage.fromFilePath(context, invalidUri)
        }
    }

    @Test
    fun testOcr_SpatialOrdering_Logic() {
        // Poin 6: Menyusun teks berurutan baris demi baris
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        // Buat gambar dengan dua baris teks
        val bitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 30f
        }
        canvas.drawText("BARIS ATAS", 50f, 50f, paint)
        canvas.drawText("BARIS BAWAH", 50f, 150f, paint)
        
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = Tasks.await(recognizer.process(image))
        
        // Verifikasi urutan (baris atas harus muncul sebelum baris bawah dalam teks mentah)
        val text = result.text
        val indexAtas = text.indexOf("BARIS ATAS")
        val indexBawah = text.indexOf("BARIS BAWAH")
        
        assertTrue("Teks harus tersusun berurutan (Atas -> Bawah)", indexAtas < indexBawah)
    }

    @Test
    fun testDocumentScanner_Intent_Check() {
        // Poin 2: Verifikasi komponen DocumentScanner (Mock-like check)
        // Karena DocumentScanner memanggil activity external, kita cek keberadaan library-nya
        try {
            Class.forName("com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions")
            assertTrue("Library Document Scanner tersedia", true)
        } catch (e: ClassNotFoundException) {
            fail("Library Document Scanner tidak ditemukan")
        }
    }
}
