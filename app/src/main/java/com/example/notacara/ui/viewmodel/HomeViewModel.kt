package com.example.notacara.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notacara.data.local.datastore.UserTourManager
import com.example.notacara.data.local.entity.AcaraEntity
import com.example.notacara.data.local.entity.NotaEntity
import com.example.notacara.data.local.entity.TransaksiEntity
import com.example.notacara.data.model.AnalysisResult
import com.example.notacara.data.model.NotaItemEdit
import com.example.notacara.data.repository.NotaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class AcaraDisplayModel(
    val acara_id: Int,
    val nama_acara: String,
    val deskripsi: String,
    val dateRange: String,
    val totalNota: Int,
    val totalExpense: String
)

data class HomeUiState(
    val searchQuery: String = "",
    val dateFilter: String? = null,
    val fromDate: Long? = null,
    val untilDate: Long? = null,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Int> = emptySet(),
    val isMenuExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val showDateRangeDialog: Boolean = false,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val inputNamaAcara: String = "",
    val inputDeskripsi: String = "",
    val lastAnalysisResult: AnalysisResult? = null,
    val lastImageUri: Uri? = null,
    val selectedAcaraId: Int? = null,
    val currentAcaraName: String = "",
    val currentNotaId: Int? = null,
    
    // Tour State
    val tourStep: Int = 0, // 0: No tour, 1-4: Home, 5: NotaList
    val isFirstTimeScan: Boolean = false,
    val isFirstTimeEdit: Boolean = false
)

class HomeViewModel(
    application: Application, 
    private val repository: NotaRepository,
    private val userTourManager: UserTourManager
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val allAcara: StateFlow<List<AcaraEntity>> = repository.allAcara
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filteredAcara = MutableStateFlow<List<AcaraDisplayModel>>(emptyList())
    val filteredAcara: StateFlow<List<AcaraDisplayModel>> = _filteredAcara.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAcaraDetails: StateFlow<AcaraDisplayModel?> = combine(
        allAcara,
        repository.allNota,
        _uiState.map { it.selectedAcaraId }.distinctUntilChanged()
    ) { acaraList, notaList, selectedId ->
        val acara = acaraList.find { it.acara_id == selectedId } ?: return@combine null
        val notasForAcara = notaList.filter { it.acara_id == acara.acara_id }
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val nf = NumberFormat.getInstance(Locale.forLanguageTag("id-ID"))

        val dateRangeStr = if (notasForAcara.isEmpty()) "-" else {
            val minDate = notasForAcara.minOf { it.tanggal }
            val maxDate = notasForAcara.maxOf { it.tanggal }
            "${sdf.format(Date(minDate))} - ${sdf.format(Date(maxDate))}"
        }
        val totalExp = notasForAcara.sumOf { it.total_belanja }
        AcaraDisplayModel(
            acara.acara_id,
            acara.nama_acara,
            acara.deskripsi,
            dateRangeStr,
            notasForAcara.size,
            "Rp${nf.format(totalExp)}"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredNotas: StateFlow<List<NotaEntity>> = _uiState
        .flatMapLatest { state ->
            val acaraId = state.selectedAcaraId ?: return@flatMapLatest flowOf(emptyList())
            repository.getNotasByAcara(acaraId).map { notas ->
                var filtered = notas
                if (state.fromDate != null && state.untilDate != null) {
                    filtered = filtered.filter { it.tanggal in state.fromDate..state.untilDate }
                }
                if (state.searchQuery.isNotBlank()) {
                    val q = state.searchQuery.lowercase()
                    filtered = filtered.filter { it.nama_toko.lowercase().contains(q) }
                }
                filtered
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTotalExpense: StateFlow<String> = filteredNotas.map { notas ->
        val nf = NumberFormat.getInstance(Locale.forLanguageTag("id-ID"))
        "Rp${nf.format(notas.sumOf { it.total_belanja })}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Rp0")

    init {
        viewModelScope.launch {
            val isFirstTime = userTourManager.isFirstTimeUser.first()
            if (isFirstTime) {
                _uiState.update { it.copy(tourStep = 1) }
            }
            
            userTourManager.isFirstTimeScan.collect { isFirst ->
                _uiState.update { it.copy(isFirstTimeScan = isFirst) }
            }
        }
        
        viewModelScope.launch {
            userTourManager.isFirstTimeEdit.collect { isFirst ->
                _uiState.update { it.copy(isFirstTimeEdit = isFirst) }
            }
        }

        val uiFilterFlow = combine(
            _uiState.map { it.searchQuery }.distinctUntilChanged(),
            _uiState.map { it.fromDate }.distinctUntilChanged(),
            _uiState.map { it.untilDate }.distinctUntilChanged()
        ) { query, from, until -> Triple(query, from, until) }

        combine(allAcara, repository.allNota, repository.allTransaksi, uiFilterFlow) { acaraList, notaList, transaksiList, filters ->
            val (query, from, until) = filters
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val nf = NumberFormat.getInstance(Locale.forLanguageTag("id-ID"))
            
            val displayList = acaraList.map { acara ->
                val notasForAcara = notaList.filter { it.acara_id == acara.acara_id }
                val dateRangeStr = if (notasForAcara.isEmpty()) "-" else {
                    val minDate = notasForAcara.minOf { it.tanggal }
                    val maxDate = notasForAcara.maxOf { it.tanggal }
                    "${sdf.format(Date(minDate))} - ${sdf.format(Date(maxDate))}"
                }
                val totalExp = notasForAcara.sumOf { it.total_belanja }
                AcaraDisplayModel(
                    acara.acara_id, 
                    acara.nama_acara, 
                    acara.deskripsi, 
                    dateRangeStr, 
                    notasForAcara.size, 
                    "Rp${nf.format(totalExp)}"
                )
            }

            var filtered = displayList
            if (from != null && until != null) {
                filtered = filtered.filter { display ->
                    notaList.filter { it.acara_id == display.acara_id }.any { it.tanggal in from..until }
                }
            }
            if (query.isNotBlank()) {
                val q = query.lowercase()
                filtered = filtered.filter { it.nama_acara.lowercase().contains(q) || it.deskripsi.lowercase().contains(q) }
            }
            filtered
        }.onEach { _filteredAcara.value = it }.launchIn(viewModelScope)
    }

    fun nextTourStep() {
        _uiState.update { it.copy(tourStep = it.tourStep + 1) }
    }

    fun skipTour() {
        _uiState.update { it.copy(tourStep = 0) }
        viewModelScope.launch {
            userTourManager.setFirstTimeUser(false)
        }
    }
    
    fun completeFirstTimeScan() {
        viewModelScope.launch {
            userTourManager.setFirstTimeScan(false)
        }
    }

    fun completeFirstTimeEdit() {
        viewModelScope.launch {
            userTourManager.setFirstTimeEdit(false)
        }
    }

    fun onSearchQueryChange(query: String) { _uiState.update { it.copy(searchQuery = query) } }
    fun clearFilters() { _uiState.update { it.copy(searchQuery = "", fromDate = null, untilDate = null, dateFilter = null) } }
    fun toggleSelectionMode(enabled: Boolean) { _uiState.update { it.copy(isSelectionMode = enabled, selectedIds = emptySet()) } }
    fun toggleItemSelection(id: Int) {
        _uiState.update { state ->
            val newSelection = if (state.selectedIds.contains(id)) state.selectedIds - id else state.selectedIds + id
            var inputNama = state.inputNamaAcara
            var inputDesc = state.inputDeskripsi
            if (newSelection.size == 1) {
                val selectedAcara = allAcara.value.find { it.acara_id == newSelection.first() }
                selectedAcara?.let {
                    inputNama = it.nama_acara
                    inputDesc = it.deskripsi
                }
            }
            state.copy(selectedIds = newSelection, inputNamaAcara = inputNama, inputDeskripsi = inputDesc)
        }
    }
    fun onInputChange(nama: String, deskripsi: String) { _uiState.update { it.copy(inputNamaAcara = nama, inputDeskripsi = deskripsi) } }
    fun setShowAddDialog(show: Boolean) { 
        _uiState.update { it.copy(showAddDialog = show, inputNamaAcara = "", inputDeskripsi = "") }
    }
    fun setShowEditDialog(show: Boolean) { _uiState.update { it.copy(showEditDialog = show) } }
    fun setShowDeleteDialog(show: Boolean) { _uiState.update { it.copy(showDeleteDialog = show) } }
    fun toggleMenu(expanded: Boolean) { _uiState.update { it.copy(isMenuExpanded = expanded) } }
    fun showDateRangeDialog(show: Boolean) { _uiState.update { it.copy(showDateRangeDialog = show) } }
    fun setDateRange(from: Long?, until: Long?) { 
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        // Sesuaikan zona waktu: DatePicker (UTC) -> Local Time (Awal/Akhir Hari)
        val adjustedFrom = from?.let {
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }
            Calendar.getInstance().apply {
                set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        
        val adjustedUntil = until?.let {
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }
            Calendar.getInstance().apply {
                set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        val filterStr = if (adjustedFrom != null && adjustedUntil != null) {
            "${sdf.format(Date(adjustedFrom))} - ${sdf.format(Date(adjustedUntil))}"
        } else null
        
        _uiState.update { it.copy(
            fromDate = adjustedFrom, 
            untilDate = adjustedUntil, 
            dateFilter = filterStr, 
            showDateRangeDialog = false
        ) } 
    }

    fun setAnalysisResult(result: AnalysisResult, imageUri: Uri, acaraName: String) {
        val acara = allAcara.value.find { it.nama_acara == acaraName }
        _uiState.update { it.copy(
            lastAnalysisResult = result, 
            lastImageUri = imageUri, 
            selectedAcaraId = acara?.acara_id,
            currentAcaraName = acaraName,
            currentNotaId = null
        ) }
    }

    fun createAcara(onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            val newId = repository.insertAcara(AcaraEntity().apply {
                this.nama_acara = _uiState.value.inputNamaAcara
                this.deskripsi = _uiState.value.inputDeskripsi
            })
            if (_uiState.value.tourStep == 4) {
                nextTourStep() 
            }
            onSuccess(newId)
        }
    }

    fun updateAcara() {
        val selectedId = _uiState.value.selectedIds.firstOrNull() ?: return
        val nama = _uiState.value.inputNamaAcara
        val deskripsi = _uiState.value.inputDeskripsi
        if (nama.isBlank()) return
        viewModelScope.launch {
            repository.updateAcara(AcaraEntity().apply {
                this.acara_id = selectedId
                this.nama_acara = nama
                this.deskripsi = deskripsi
            })
            _uiState.update { it.copy(isSelectionMode = false, selectedIds = emptySet(), showEditDialog = false) }
        }
    }

    fun updateAcaraDescription(acaraId: Int, nama: String, newDescription: String) {
        viewModelScope.launch {
            repository.updateAcara(AcaraEntity().apply {
                this.acara_id = acaraId
                this.nama_acara = nama
                this.deskripsi = newDescription
            })
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = "nota_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveNota(namaToko: String, tanggalStr: String, diskon: String, total: String, items: List<NotaItemEdit>, imageUri: Uri?, onSuccess: () -> Unit) {
        val acaraId = _uiState.value.selectedAcaraId ?: return
        viewModelScope.launch {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateMillis = try { sdf.parse(tanggalStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
            
            val finalImageUri = (imageUri ?: _uiState.value.lastImageUri)?.let { uri ->
                if (uri.toString().startsWith("content://")) {
                    saveImageToInternalStorage(uri)
                } else {
                    uri
                }
            }

            val nota = NotaEntity().apply {
                this.nota_id = _uiState.value.currentNotaId ?: 0
                this.acara_id = acaraId
                this.teks_mentah = ""
                this.nama_toko = namaToko
                this.tanggal = dateMillis
                this.diskon = diskon.replace(".", "").replace("Rp", "").trim().toIntOrNull() ?: 0
                this.total_belanja = total.replace(".", "").replace("Rp", "").trim().toIntOrNull() ?: 0
                this.image_uri = finalImageUri?.toString()
            }
            
            val transaksiList = items.map {
                TransaksiEntity().apply {
                    this.nama_barang = it.nama
                    this.jumlah = it.jumlah.toIntOrNull() ?: 1
                    this.harga_satuan = it.satuan.replace(".", "").toIntOrNull() ?: 0
                    this.harga_total_barang = it.subtotal.replace(".", "").toIntOrNull() ?: 0
                }
            }

            val notaId = repository.updateNotaTransaksi(nota, transaksiList)
            
            _uiState.update { it.copy(
                currentNotaId = notaId, 
                lastImageUri = finalImageUri,
                lastAnalysisResult = null
            ) }
            onSuccess()
        }
    }

    fun deleteNota(notaId: Int) {
        viewModelScope.launch {
            repository.deleteNotasByIds(listOf(notaId))
        }
    }

    fun deleteSelectedAcara() {
        viewModelScope.launch {
            repository.deleteAcaraByIds(_uiState.value.selectedIds.toList())
            toggleSelectionMode(false)
            setShowDeleteDialog(false)
        }
    }

    fun deleteSelectedNotas() {
        viewModelScope.launch {
            repository.deleteNotasByIds(_uiState.value.selectedIds.toList())
            toggleSelectionMode(false)
            setShowDeleteDialog(false)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNotasForAcara(acaraName: String): Flow<List<NotaEntity>> {
        return allAcara.flatMapLatest { list ->
            val acaraId = list.find { it.nama_acara == acaraName }?.acara_id
            if (acaraId != null) {
                _uiState.update { it.copy(selectedAcaraId = acaraId, currentAcaraName = acaraName) }
                repository.getNotasByAcara(acaraId)
            } else flowOf(emptyList())
        }
    }

    fun getNotasByAcaraId(acaraId: Int): Flow<List<NotaEntity>> {
        val acaraName = allAcara.value.find { it.acara_id == acaraId }?.nama_acara ?: ""
        _uiState.update { it.copy(selectedAcaraId = acaraId, currentAcaraName = acaraName) }
        return repository.getNotasByAcara(acaraId)
    }

    fun getNotaById(id: Int): Flow<NotaEntity?> = repository.allNota.map { list -> list.find { it.nota_id == id } }
    fun getTransaksiForNota(notaId: Int): Flow<List<TransaksiEntity>> = repository.getTransaksiByNota(notaId)
    
    fun setCurrentNotaId(id: Int?) { 
        _uiState.update { state ->
            state.copy(
                currentNotaId = id,
                lastImageUri = if (id != null) null else state.lastImageUri,
                lastAnalysisResult = if (id != null) null else state.lastAnalysisResult
            )
        }
    }

    fun prepareEditFromCurrentNota() {
        val notaId = _uiState.value.currentNotaId ?: return
        viewModelScope.launch {
            val nota = repository.allNota.first().find { it.nota_id == notaId }
            val trans = repository.getTransaksiByNota(notaId).first()
            val acara = repository.allAcara.first().find { it.acara_id == nota?.acara_id }
            if (nota != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val result = AnalysisResult(
                    namaToko = nota.nama_toko,
                    tanggal = sdf.format(Date(nota.tanggal)),
                    items = trans.map { NotaItemEdit(id = it.transaksi_id.toLong(), nama = it.nama_barang, jumlah = it.jumlah.toString(), satuan = it.harga_satuan.toString(), subtotal = it.harga_total_barang.toString()) },
                    diskon = nota.diskon.toString(),
                    totalBelanja = nota.total_belanja.toString()
                )
                _uiState.update { it.copy(
                    lastAnalysisResult = result,
                    lastImageUri = if (nota.image_uri != null) Uri.parse(nota.image_uri) else null,
                    selectedAcaraId = nota.acara_id,
                    currentAcaraName = acara?.nama_acara ?: ""
                ) }
            }
        }
    }

    fun prepareEditFromSelectedNota() {
        val selectedId = _uiState.value.selectedIds.firstOrNull() ?: return
        setCurrentNotaId(selectedId)
        prepareEditFromCurrentNota()
    }
}
