package com.example.notacara.data.ml

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TFLiteManagerTest {

    private lateinit var tfliteManager: TFLiteManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        tfliteManager = TFLiteManager(context)
    }

    @Test
    fun testModel_Initialization_IsSuccessful() {
        // Poin: Inisialisasi model .tflite berhasil dimuat ke memori interpreter
        assertNotNull("Interpreter TFLite tidak boleh null setelah inisialisasi", tfliteManager.interpreter)
    }

    @Test
    fun testTensor_Allocation_CorrectSize() {
        // Poin: Alokasi Tensor Input sukses memastikan ukuran array masuk (256 indeks)
        assertEquals("Max sequence length harus 256", 256, tfliteManager.maxSeqLength)
        assertEquals("Jumlah tag output harus 17", 17, tfliteManager.numTags)
    }

    @Test
    fun testInference_ReturnsEmissionsMatrix() {
        // Poin: Proses Inferensi Model sukses menghasilkan nilai matriks skor emisi
        val mockInput = IntArray(256) { 1 } // Simulasi token OOV semua
        val emissions = tfliteManager.predict(mockInput)
        
        assertNotNull(emissions)
        assertEquals(256, emissions.size)
        assertEquals(17, emissions[0].size)
    }

    @Test
    fun testResource_Release_OnClose() {
        // Poin: Penutupan interpreter (close) sukses membebaskan memori
        tfliteManager.close()
        assertNull("Interpreter harus null setelah di-close", tfliteManager.interpreter)
    }
}
