package com.example.notacara.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.notacara.data.local.entity.AcaraEntity
import com.example.notacara.data.local.entity.NotaEntity
import com.example.notacara.data.local.entity.TransaksiEntity
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotaRepositoryTest {

    private lateinit var realm: Realm
    private lateinit var repository: NotaRepository

    @Before
    fun setUp() {
        val config = RealmConfiguration.Builder(
            schema = setOf(AcaraEntity::class, NotaEntity::class, TransaksiEntity::class)
        )
            .inMemory()
            .build()
        realm = Realm.open(config)
        repository = NotaRepository(realm)
    }

    @After
    fun tearDown() {
        if (::realm.isInitialized && !realm.isClosed()) {
            realm.close()
        }
    }

    // --- 1. ACARA: insertAcara ---
    @Test
    fun test_insertAcara_incrementsId() = runBlocking {
        val id = repository.insertAcara(AcaraEntity().apply { nama_acara = "A1" })
        assertEquals(1, id)
    }

    // --- 2. ACARA: updateAcara ---
    @Test
    fun test_updateAcara_updatesData() = runBlocking {
        val id = repository.insertAcara(AcaraEntity().apply { nama_acara = "Old" })
        repository.updateAcara(AcaraEntity().apply { this.acara_id = id; this.nama_acara = "New" })
        val result = realm.query<AcaraEntity>("acara_id == $0", id).find().first()
        assertEquals("New", result.nama_acara)
    }

    // --- 3. ACARA: allAcara ---
    @Test
    fun test_allAcara_emitsList() = runBlocking {
        repository.insertAcara(AcaraEntity())
        val list = repository.allAcara.first()
        assertEquals(1, list.size)
    }

    // --- 4. ACARA: deleteAcaraByIds ---
    @Test
    fun test_deleteAcaraByIds_cascadeDelete() = runBlocking {
        val aId = repository.insertAcara(AcaraEntity())
        repository.insertNota(NotaEntity().apply { acara_id = aId })
        repository.deleteAcaraByIds(listOf(aId))
        assertEquals(0, realm.query<AcaraEntity>().find().size)
        assertEquals(0, realm.query<NotaEntity>().find().size)
    }

    // --- 5. NOTA: insertNota ---
    @Test
    fun test_insertNota_incrementsId() = runBlocking {
        val id = repository.insertNota(NotaEntity())
        assertEquals(1, id)
    }

    // --- 6. NOTA: updateNotaTransaksi ---
    @Test
    fun test_updateNotaTransaksi_atomicSave() = runBlocking {
        val notaId = repository.updateNotaTransaksi(NotaEntity(), listOf(TransaksiEntity()))
        assertEquals(1, realm.query<NotaEntity>().find().size)
        assertEquals(1, realm.query<TransaksiEntity>("nota_id == $0", notaId).find().size)
    }

    // --- 7. NOTA: allNota ---
    @Test
    fun test_allNota_emitsList() = runBlocking {
        repository.insertNota(NotaEntity())
        val list = repository.allNota.first()
        assertEquals(1, list.size)
    }

    // --- 8. NOTA: deleteNotasByIds ---
    @Test
    fun test_deleteNotasByIds_removesData() = runBlocking {
        val id = repository.insertNota(NotaEntity())
        repository.deleteNotasByIds(listOf(id))
        assertEquals(0, realm.query<NotaEntity>().find().size)
    }

    // --- 9. NOTA: getNotasByAcara ---
    @Test
    fun test_getNotasByAcara_filtersCorrectId() = runBlocking {
        repository.insertNota(NotaEntity().apply { acara_id = 99 })
        val result = repository.getNotasByAcara(99).first()
        assertEquals(1, result.size)
    }

    // --- 10. TRANSAKSI: insertAllTransaksi ---
    @Test
    fun test_insertAllTransaksi_savesList() = runBlocking {
        repository.insertAllTransaksi(listOf(TransaksiEntity(), TransaksiEntity()))
        assertEquals(2, realm.query<TransaksiEntity>().find().size)
    }

    // --- 11. TRANSAKSI: allTransaksi ---
    @Test
    fun test_allTransaksi_emitsList() = runBlocking {
        repository.insertAllTransaksi(listOf(TransaksiEntity()))
        val list = repository.allTransaksi.first()
        assertEquals(1, list.size)
    }

    // --- 12. TRANSAKSI: deleteTransaksiByNotaId ---
    @Test
    fun test_deleteTransaksiByNotaId_removesData() = runBlocking {
        repository.insertAllTransaksi(listOf(TransaksiEntity().apply { nota_id = 5 }))
        repository.deleteTransaksiByNotaId(5)
        assertEquals(0, realm.query<TransaksiEntity>().find().size)
    }

    // --- 13. TRANSAKSI: getTransaksiByNota ---
    @Test
    fun test_getTransaksiByNota_filtersCorrectId() = runBlocking {
        repository.insertAllTransaksi(listOf(TransaksiEntity().apply { nota_id = 10 }))
        val result = repository.getTransaksiByNota(10).first()
        assertEquals(1, result.size)
    }
}
