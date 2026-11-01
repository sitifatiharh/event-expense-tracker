package com.example.notacara.data.ml

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextProcessorTest {

    private lateinit var textProcessor: TextProcessor
    
    // Dataset Raw Text dari User
    private val rawText1 = """
        PT ACEHARDWARE INDONISIA Ibk
        ACEQ MBam
        Emall.
        NPWP
        PORIKINA          .RW.
        KIMBANGAN  AIAN EIMIAN  JAKARIA
        BARAIDNIJAKARIA
        RRA 1 C 2 ABQM
        166890  NOUR.130 M.y 204  10.40
        Reces     04120240.30.56
        103      LA MAISINEIAYER PIIKPYC
        2          10.O          322.000
        152.200
        103/6858 Y 0 GAMA 1         IOSCA PV
        2          161.000          322
        -1  .200
        YOGAMAINGLELAYER BLUE PVC
        103.000
        2          161..          322.000
        -152.200
        103/689 YOUA MAOINLL LAIER GREYPVC
        2          161.000    :          322.000
        Saving           152.200
        10485351 YOGA ROLLER BLACK 33 XIALM
        1+          183.000       :          183.000
        01 sc 30%          -54.900
        10485352 Y 0 GAR 0 LLER PINK 33 X 14 CM
        1          183,000          183.000
        Disc 30%          -54.900
        10568539 A 8 D 0 M 1 NAL WHEEL AND PLANKTRAIN
        X          213.90      :          213.900
        10015703 TWISTER
        1 X         323.000          :          323.000
        10 O 96730 NEOPRENE DUMBBELL  1 KGPINK
        6 X          58,900         :          353.400
        1        1,298.000      :         1.98.000
        Saving :          119,000
        6430043473099 EXERC     BAL.L.     MIRON GY
        2+          268.000          36.000
        Saying          -236.200
        1 O 56 O 297 HAND GRIP ADJUSIABL.E WITHCOUNT
        2          106,900          213.800
        10560329 C 0 UNTER SKIP R 0 PE BLACK RED
        2          96.900       :          193.800
        10560295 S 1 TUP H 0 LDER BLACK
        1          96.900      .          96.900
        10560326 PUSH UP BAR CURLBLACK
        2          106.900        :          213.800
        10560327 TPRTONING TUBE BUE
        1 X          64.900          .          64.900
        10560302 SOFBODY TRIMMER BLUE
        1          85.900          85.900
        10190773 ANTI-BURSTGYM BALL 65 CM 1000 G
        2+          173.000      :          346.000
        Saving          146.200
        Total          :          5.593.400
        Total savin 9          :         -1.220.000
        Total sales          :          4.373.400
        Cash          (RP)          :          4400000
        Total payment          :          4.373.400
        Change          ..          26.600
    """.trimIndent()
    
    private val rawText3 = """
        HAJI LEBAR 2 08118895806
        JL KAVLING MERUYA BLOK 35 NO 2  11620
        29.11.21-16:06/2.1.97/113 S  3639/RAKA/01
        NTSCTN CFKIKARA 109 G     1  6800    6.800
        SGM ANANDA 2 BOX 600 G     1  52400    52.400
        1  7000     7.000
        INDOMILK SKM CKT 6 X 37     1  7000    7.000
        IDM GULA PSR PRM 1 KG       12500   12.500
        I/OCHA GRN TEA 350 ML         1   2900   2.900
        HARGA JUAL  88.600
        VC ENAK K/MNS PUTIH 6 S/INDOLAK   2,900
        TOTAL:  85.700
        TUNAI   100.000
        KEMBALI     14.300
        ANDA HEMAT      2.900
        PPN   DPP=69.182 PPN=6,918
        NONPPNDPP=12.500
        LAYANAN KONSUMEN SMS 0811 1500 280
        CALL 1500 280-KONTAKINDOMARET.C 0.ID
        Fndmarel
    """.trimIndent()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        textProcessor = TextProcessor(context)
    }

    @Test
    fun testCleanRawText_AllRules() {
        // Rule 1: Huruf nempel Angka
        assertEquals("ITEM 123", textProcessor.cleanRawText("ITEM123"))
        
        // Rule 2: Angka nempel Huruf
        assertEquals("123 ITEM", textProcessor.cleanRawText("123ITEM"))
        
        // Rule 3: Tangal nempel Jam (Contoh: 202310:30)
        assertEquals("2023 10:30", textProcessor.cleanRawText("202310:30"))
        assertEquals("2023 10.30", textProcessor.cleanRawText("202310.30"))
        
        // Rule 4: Tanggal nempel Noise Format Numerik di Depan (Contoh: 12320/12/2023)
        assertEquals("123 20/12/2023", textProcessor.cleanRawText("12320/12/2023"))
        
        // Rule 5: Tanggal nempel Noise Format Nama Bulan di Depan (Contoh: 12320 Jan 2023)
        assertEquals("123 20 Jan 2023", textProcessor.cleanRawText("12320 Jan 2023"))
        
        // Rule 6: Tanggal Numerik nempel Noise Format Numerik di Belakanag (Contoh: 20.12.2023456)
        assertEquals("20.12.2023 456", textProcessor.cleanRawText("20.12.2023456"))
        
        // Rule 7: Tanggal Nama Bulan nempel Noise Format Numerik di Belakang (Contoh: 20 Jan 2023456)
        assertEquals("20/Jan/2023 456", textProcessor.cleanRawText("20/Jan/2023456"))
        
        // Rule 8: Variasi Tanggal nempel Noise (Contoh: 20-Jan-2023456)
        assertEquals("20-Jan-23 45 6", textProcessor.cleanRawText("20-Jan-23456"))
    }

    @Test
    fun testGetTokens_ConsistentSplitting() {
        val tokens = textProcessor.getTokens(rawText3)
        assertTrue("Token harus mengandung 'HAJI'", tokens.contains("HAJI"))
        assertTrue("Token harus mengandung 'TOTAL'", tokens.contains("TOTAL"))
        assertTrue("Simbol ':' harus dipisah menjadi token sendiri", tokens.contains(":"))
    }

    @Test
    fun testPreprocess_LengthAndIndexing() {
        val indices = textProcessor.preprocess(rawText1)
        assertEquals("Ukuran array indices harus 256", 256, indices.size)
        assertNotEquals("Index pertama tidak boleh 0", 0, indices[0])
    }

    @Test
    fun testOOV_Handling() {
        val indices = textProcessor.preprocess("KwakwakwakwkXYZ123")
        assertEquals("Kata tak dikenal harus dipetakan ke index OOV (1)", 1, indices[0])
    }

    @Test
    fun testNumericSubstitution_Heuristic() {
        val indices = textProcessor.preprocess("99")
        assertNotEquals("Angka harusnya mendapat index valid (bukan padding 0)", 0, indices[0])
    }

    @Test
    fun testEmptyInput_DoesNotCrash() {
        val indices = textProcessor.preprocess("")
        assertEquals(256, indices.size)
        assertTrue("Input kosong harus menghasilkan semua index 0", indices.all { it == 0 })
    }

    @Test
    fun testLongText_IsTruncatedCorrectly() {
        val longText = (1..300).joinToString(" ") { "item" }
        val tokens = textProcessor.getTokens(longText)
        assertEquals("Token harus dipotong pada batas 256", 256, tokens.size)
    }
}
