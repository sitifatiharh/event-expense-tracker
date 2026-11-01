package com.example.notacara.data.ml

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViterbiDecoderTest {

    private lateinit var viterbiDecoder: ViterbiDecoder

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        viterbiDecoder = ViterbiDecoder(context)
    }

    @Test
    fun testCrfData_Loading_IsSuccessful() {
        // Poin: Pembacaan file crf_transitions.json dan idx2label.json berhasil
        assertTrue("Matriks transisi tidak boleh kosong", viterbiDecoder.transitions.isNotEmpty())
        assertTrue("Kamus idx2label tidak boleh kosong", viterbiDecoder.idx2label.isNotEmpty())
        assertTrue("numTags harus lebih dari 0", viterbiDecoder.numTags > 0)
    }

    @Test
    fun testDecode_LogicalPath_IsCorrect() {
        // Poin: Fungsi Viterbi Decode sukses memproses skor emisi
        // Simulasi emisi sederhana untuk 3 token
        val numTags = viterbiDecoder.numTags
        val emissions = Array(3) { FloatArray(numTags) { -10f } }
        
        // Asumsikan label "O" ada di index 0 (biasanya)
        // Kita beri skor tinggi pada tag tertentu di tiap time step
        emissions[0][0] = 10f // Token 1 -> Tag 0
        emissions[1][0] = 10f // Token 2 -> Tag 0
        emissions[2][0] = 10f // Token 3 -> Tag 0
        
        val result = viterbiDecoder.decode(emissions)
        
        assertEquals(3, result.size)
        // Pastikan hasil decode sesuai dengan probabilitas emisi tertinggi yang kita set
        val expectedLabel = viterbiDecoder.idx2label[0] ?: "O"
        assertEquals(expectedLabel, result[0])
    }

    @Test
    fun testNumericalStability_ExtremeScores() {
        // Poin: Perhitungan tetap stabil dengan skor ekstrem/negatif
        val numTags = viterbiDecoder.numTags
        val emissions = Array(2) { FloatArray(numTags) { -1e20f } } // Skor sangat rendah
        emissions[0][1] = 0f // Paling mendingan dibanding yang lain
        emissions[1][2] = 0f
        
        val result = viterbiDecoder.decode(emissions)
        assertNotNull(result)
        assertEquals(2, result.size)
    }

    @Test
    fun testLabelTranslation_IsCorrect() {
        // Poin: Penerjemahan Indeks Label sukses mengubah angka menjadi tag IOB
        val emissions = Array(1) { FloatArray(viterbiDecoder.numTags) { 0f } }
        val result = viterbiDecoder.decode(emissions)
        
        // Cek apakah string yang keluar valid (mengandung B-, I-, atau O)
        val label = result[0]
        assertTrue(label == "O" || label.startsWith("B-") || label.startsWith("I-"))
    }
}
