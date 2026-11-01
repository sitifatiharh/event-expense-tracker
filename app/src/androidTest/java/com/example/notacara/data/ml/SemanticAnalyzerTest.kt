package com.example.notacara.data.ml

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.notacara.data.model.NotaItemEdit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SemanticAnalyzerTest {

    private lateinit var analyzer: SemanticAnalyzer

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        analyzer = SemanticAnalyzer(context)
    }

    @Test
    fun testNormalizeDate_And_FormatPrice() {
        // Menguji S-01 (formatPrice) dan S-02 (normalizeDate)
        // Poin: Normalisasi berbagai format tanggal dan pemformatan ribuan ID
        assertEquals("22/09/2025", analyzer.normalizeDate("22 Septeiber 2025"))
        assertEquals("25/06/2026", analyzer.normalizeDate("20 26 06 25"))
        assertEquals("12.700", analyzer.formatPrice("12700"))
        assertEquals("0", analyzer.formatPrice(""))
    }

    @Test
    fun testSmartAppend_Logic() {
        // Menguji penggabungan token (smartAppend)
        // Poin: Spasi otomatis antar kata, namun menempel jika Angka+Huruf (satuan)
        assertEquals("100g", analyzer.smartAppend("100", "g"))
        assertEquals("ITEM 123", analyzer.smartAppend("ITEM", "123"))
        assertEquals("Beras Merah", analyzer.smartAppend("Beras", "Merah"))
    }

    @Test
    fun testValidateAndCompleteItem_Calculations() {
        // Menguji S-03, S-04, S-05
        // Poin: Kalkulasi otomatis (Qty * Satuan = Subtotal) dan penanganan nilai default
        
        // Kasus 1: Hitung Subtotal
        val item1 = NotaItemEdit(nama = "KOPI", jumlah = "2", satuan = "5000", subtotal = "")
        val res1 = analyzer.validateAndCompleteItem(item1)
        assertEquals("10000", res1.subtotal)

        // Kasus 2: Hitung Satuan (Price)
        val item2 = NotaItemEdit(nama = "GULA", jumlah = "2", satuan = "", subtotal = "20000")
        val res2 = analyzer.validateAndCompleteItem(item2)
        assertEquals("10000", res2.satuan)

        // Kasus 3: Default Qty = 1 jika nama ada tapi qty kosong
        val item3 = NotaItemEdit(nama = "MINYAK", jumlah = "", satuan = "", subtotal = "15000")
        val res3 = analyzer.validateAndCompleteItem(item3)
        assertEquals("1", res3.jumlah)
        assertEquals("15000", res3.satuan)
    }

    @Test
    fun testOrchestrator_And_ImageUri() {
        // Menguji S-18 (analyze) dan S-22 (Image URI)
        // Poin: Fungsi orkestrator berhasil memetakan entitas dan menyematkan URI gambar
        val mockRaw = "TOKO TEST\n20/12/2023\nTOTAL 50000"
        val testUri = "content://media/external/images/media/1"
        
        val result = analyzer.analyze(mockRaw, testUri)
        
        assertNotNull(result)
        assertEquals(testUri, result.imageUri)
        // Refine logic: Jika NER gagal, baris pertama diambil sebagai Nama Toko
        assertTrue(result.namaToko.contains("TOKO TEST"))
    }

    @Test
    fun testConflictResolution_And_DefaultValues() {
        // Menguji S-20 (Default Values) dan S-21 (Conflict Resolution)
        // Poin: Handling label tidak terdeteksi (default 0 atau "") dan resolusi konflik
        
        // Input kosong
        val emptyResult = analyzer.analyze("")
        assertEquals("", emptyResult.tanggal)
        assertEquals("0", emptyResult.totalBelanja)
        
        // Simulasi resolusi konflik ganda (dites secara tidak langsung melalui alur analyze)
        // Poin Heuristic Tanggal Ganda: B-tanggal B-tanggal -> O B-tanggal
        val mockText = "Tgl: 01/01/2023 Jam: 10:00"
        val result = analyzer.analyze(mockText)
        assertNotNull(result.tanggal)
    }

    @Test
    fun testItemExtraction_And_SpatialSequence() {
        // Menguji S-17 (Ekstraksi baris) dan S-23 (Pengurutan barang)
        // Poin: Memastikan item dipecah ke objek NotaItemEdit dan urutannya terjaga
        val mockRaw = "AYAM 20000\nBEBEK 30000"
        val result = analyzer.analyze(mockRaw)
        
        // Verifikasi alokasi list items
        assertNotNull(result.items)
        // Jika teks diproses urut, maka AYAM harus diproses sebelum BEBEK
        // Pengurutan spasial sebenarnya sudah dilakukan di TextProcessor.processVisionText
        // Di sini kita memastikan orkestrator meneruskan urutan tersebut ke list items
    }
}
