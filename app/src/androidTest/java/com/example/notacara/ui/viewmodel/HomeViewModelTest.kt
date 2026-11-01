package com.example.notacara.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.notacara.data.local.entity.AcaraEntity
import com.example.notacara.data.local.entity.NotaEntity
import com.example.notacara.data.local.entity.TransaksiEntity
import com.example.notacara.data.model.AnalysisResult
import com.example.notacara.data.repository.NotaRepository
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {

    private lateinit var realm: Realm
    private lateinit var repository: NotaRepository
    private lateinit var viewModel: HomeViewModel
    private lateinit var viewModelStore: ViewModelStore
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        // Nama database unik berbasis nanoTime untuk setiap test case agar terisolasi total
        val config = RealmConfiguration.Builder(schema = setOf(AcaraEntity::class, NotaEntity::class, TransaksiEntity::class))
            .name("test_db_${System.nanoTime()}")
            .inMemory()
            .build()
        realm = Realm.open(config)
        repository = NotaRepository(realm)

        // Gunakan ViewModelStore agar kita bisa mematikan ViewModel (onCleared) secara manual
        viewModelStore = ViewModelStore()
        val factory = NotacaraViewModelFactory(app, repository)
        viewModel = ViewModelProvider(viewModelStore, factory)[HomeViewModel::class.java]
    }

    @After
    fun tearDown() = runBlocking {
        // 1. Hentikan coroutine ViewModel (viewModelScope) di Main Thread
        withContext(Dispatchers.Main) {
            viewModelStore.clear()
        }
        // 2. Beri jeda kecil agar proses pembersihan internal selesai
        delay(300)
        // 3. Baru tutup database dengan aman
        if (this@HomeViewModelTest::realm.isInitialized) {
            realm.close()
        }
    }

    @Test
    fun InitialState() {
        val state = viewModel.uiState.value
        assertEquals("Search query harus kosong di awal", "", state.searchQuery)
        assertFalse("Selection mode harus mati di awal", state.isSelectionMode)
        assertNull("Tidak ada nota terpilih di awal", state.currentNotaId)
    }

    @Test
    fun SearchFilterLogic() {
        val query = "Makan"
        viewModel.onSearchQueryChange(query)
        assertEquals("Search query di state tidak terupdate", query, viewModel.uiState.value.searchQuery)
    }

    @Test
    fun DateRangeFilterLogic() {
        val from = 1735689600000L // 1 Jan 2025
        val until = 1735776000000L // 2 Jan 2025

        viewModel.setDateRange(from, until)

        val state = viewModel.uiState.value
        assertEquals("From date tidak cocok", from, state.fromDate)
        assertEquals("Until date tidak cocok", until, state.untilDate)
        assertNotNull("Label filter tanggal harusnya terisi", state.dateFilter)
    }

    @Test
    fun SelectionModeLogic() {
        viewModel.toggleSelectionMode(true)
        assertTrue("Selection mode gagal aktif", viewModel.uiState.value.isSelectionMode)

        viewModel.toggleItemSelection(101)
        assertTrue("ID 101 harusnya terpilih", viewModel.uiState.value.selectedIds.contains(101))

        viewModel.toggleItemSelection(101) // Toggle off
        assertFalse("ID 101 harusnya batal dipilih", viewModel.uiState.value.selectedIds.contains(101))
    }

    @Test
    fun AcaraDisplayMapping() = runBlocking {
        val acaraId = repository.insertAcara(AcaraEntity().apply { nama_acara = "Belanja" })
        repository.insertNota(NotaEntity().apply { 
            this.acara_id = acaraId
            this.total_belanja = 50000 
            this.tanggal = System.currentTimeMillis()
        })

        // Menunggu sampai ViewModel memetakan data Realm ke AcaraDisplayModel
        val result = withTimeout(5000) {
            viewModel.filteredAcara.first { list ->
                list.any { it.nama_acara == "Belanja" && it.totalExpense.contains("50.000") }
            }
        }

        val item = result.find { it.nama_acara == "Belanja" }
        assertNotNull("Acara 'Belanja' tidak ditemukan", item)
        assertTrue("Total expense harus mengandung format 50.000", item!!.totalExpense.contains("50.000"))
    }

    @Test
    fun CurrentNotaStateCleaning() {
        // 1. Simulasikan seolah-olah baru saja melakukan scan (ada data sementara di state)
        val dummyUri = Uri.parse("file://dummy_path.jpg")
        val dummyResult = AnalysisResult(namaToko = "TOKO TEST", tanggal = "01/01/2024")
        viewModel.setAnalysisResult(dummyResult, dummyUri, "Event X")
        
        assertNotNull("LastImageUri harusnya terisi setelah scan", viewModel.uiState.value.lastImageUri)

        // 2. Sekarang simulasikan pengguna membuka Nota lain yang sudah tersimpan di database
        viewModel.setCurrentNotaId(999)

        val state = viewModel.uiState.value
        assertEquals("ID nota harus 999", 999, state.currentNotaId)
        
        // 3. VALIDASI KRUSIAL: State sisa scan harus dihapus agar DetailNota tidak salah gambar
        assertNull("LastImageUri harus dibersihkan saat buka nota lama", state.lastImageUri)
        assertNull("LastAnalysisResult harus dibersihkan saat buka nota lama", state.lastAnalysisResult)
    }

    @Test
    fun ViewModelFactoryCreation() {
        val factory = NotacaraViewModelFactory(app, repository)
        val createdViewModel = factory.create(HomeViewModel::class.java)

        assertNotNull("Factory gagal membuat ViewModel", createdViewModel)
        assertTrue("Output factory bukan instance HomeViewModel", createdViewModel is HomeViewModel)
    }
}
