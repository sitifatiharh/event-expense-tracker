package com.example.notacara.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notacara.ui.components.DateRangePickerDialog
import com.example.notacara.ui.components.HomeTopBar
import com.example.notacara.ui.components.SimpleTourPopUp
import com.example.notacara.ui.components.TotalExpenditureBottomBar
import com.example.notacara.ui.components.TourOverlay
import com.example.notacara.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onAcaraClick: (String) -> Unit,
    onHelpClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredAcara by viewModel.filteredAcara.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    
    var isSearchFocused by remember { mutableStateOf(false) }
    var isFocusLocked by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(500)
        isFocusLocked = false
    }

    var searchRect by remember { mutableStateOf<Rect?>(null) }
    var dateRect by remember { mutableStateOf<Rect?>(null) }
    var menuRect by remember { mutableStateOf<Rect?>(null) }
    var fabRect by remember { mutableStateOf<Rect?>(null) }

    val clearAndBack = {
        if (uiState.isSelectionMode) {
            viewModel.toggleSelectionMode(false)
        } else {
            isSearchFocused = false
            isFocusLocked = true
            focusManager.clearFocus(force = true)
            scope.launch {
                viewModel.clearFilters()
                delay(100)
                onBack()
            }
        }
    }

    val isImeVisible = WindowInsets.isImeVisible
    BackHandler(enabled = !isImeVisible && (uiState.isSelectionMode || uiState.searchQuery.isNotEmpty() || uiState.dateFilter != null || isSearchFocused)) {
        clearAndBack()
    }

    val totalSemuaAcara = remember(filteredAcara) {
        val total = filteredAcara.sumOf { 
            it.totalExpense.replace("Rp", "").replace(".", "").toDoubleOrNull() ?: 0.0 
        }
        val nf = NumberFormat.getInstance(Locale("id", "ID"))
        "Rp${nf.format(total)}"
    }

    LaunchedEffect(filteredAcara.size) {
        if (filteredAcara.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    if (uiState.showDateRangeDialog) {
        DateRangePickerDialog(
            onDismiss = { viewModel.showDateRangeDialog(false) },
            onConfirm = { from, until -> viewModel.setDateRange(from, until) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.background(Color.White),
            containerColor = Color.White,
            topBar = {
                HomeTopBar(
                    isSelectionMode = uiState.isSelectionMode,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { 
                        viewModel.onSearchQueryChange(it)
                        isSearchFocused = true
                    },
                    dateFilter = uiState.dateFilter,
                    onDateClick = { viewModel.showDateRangeDialog(true) },
                    onClearDate = { viewModel.setDateRange(null, null) },
                    onMenuClick = { viewModel.toggleMenu(true) },
                    isMenuExpanded = uiState.isMenuExpanded,
                    onDismissMenu = { viewModel.toggleMenu(false) },
                    onSelectItemsClick = { viewModel.toggleSelectionMode(true) },
                    onHelpClick = onHelpClick,
                    selectedCount = uiState.selectedIds.size,
                    onCloseSelection = { viewModel.toggleSelectionMode(false) },
                    onEditClick = { viewModel.setShowEditDialog(true) },
                    onDeleteClick = { viewModel.setShowDeleteDialog(true) },
                    onBack = { clearAndBack() },
                    onClearSearch = { viewModel.onSearchQueryChange("") },
                    enabled = uiState.tourStep == 0 && !isFocusLocked,
                    onSearchFocused = { isSearchFocused = it },
                    onSearchPositioned = { searchRect = it.boundsInRoot() },
                    onDatePositioned = { dateRect = it.boundsInRoot() },
                    onMenuPositioned = { menuRect = it.boundsInRoot() }
                )
            },
            floatingActionButton = {
                if (!uiState.isSelectionMode) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.setShowAddDialog(true) },
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(4.dp),
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .onGloballyPositioned { fabRect = it.boundsInRoot() }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tambah Acara", fontWeight = FontWeight.Bold)
                    }
                }
            },
            bottomBar = {
                TotalExpenditureBottomBar(
                    label = "Total Pengeluaran",
                    total = totalSemuaAcara
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color.White)) {
                if (filteredAcara.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val message = if (uiState.searchQuery.isNotEmpty() || uiState.dateFilter != null) {
                            "Tidak ada acara yang sesuai pencarian."
                        } else {
                            "Belum ada acara. Ketuk + untuk membuat."
                        }
                        Text(message, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredAcara) { acara ->
                            val isSelected = uiState.selectedIds.contains(acara.acara_id)
                            AcaraCard(
                                title = acara.nama_acara,
                                description = acara.deskripsi,
                                dateRange = acara.dateRange,
                                totalNota = acara.totalNota,
                                totalExpense = acara.totalExpense,
                                isSelected = isSelected,
                                isSelectionMode = uiState.isSelectionMode,
                                onLongClick = {
                                    if (!uiState.isSelectionMode) {
                                        viewModel.toggleSelectionMode(true)
                                        viewModel.toggleItemSelection(acara.acara_id)
                                    }
                                },
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleItemSelection(acara.acara_id)
                                    } else {
                                        onAcaraClick(acara.nama_acara)
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }

                if (uiState.showAddDialog) {
                    AcaraInputDialog(
                        title = "Buat Acara", confirmLabel = "Buat",
                        nama = uiState.inputNamaAcara, deskripsi = uiState.inputDeskripsi,
                        onNamaChange = { viewModel.onInputChange(it, uiState.inputDeskripsi) },
                        onDeskripsiChange = { viewModel.onInputChange(uiState.inputNamaAcara, it) },
                        onDismiss = { viewModel.setShowAddDialog(false) },
                        onConfirm = {
                            viewModel.createAcara { newId ->
                                viewModel.setShowAddDialog(false)
                                onAcaraClick(uiState.inputNamaAcara)
                            }
                        }
                    )
                }

                if (uiState.showEditDialog) {
                    AcaraInputDialog(
                        title = "Edit Acara", confirmLabel = "Simpan",
                        nama = uiState.inputNamaAcara, deskripsi = uiState.inputDeskripsi,
                        onNamaChange = { viewModel.onInputChange(it, uiState.inputDeskripsi) },
                        onDeskripsiChange = { viewModel.onInputChange(uiState.inputNamaAcara, it) },
                        onDismiss = { 
                            viewModel.setShowEditDialog(false)
                            viewModel.toggleSelectionMode(false)
                        },
                        onConfirm = { viewModel.updateAcara() }
                    )
                }

                if (uiState.showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { 
                            viewModel.setShowDeleteDialog(false)
                            viewModel.toggleSelectionMode(false)
                        },
                        title = { Text("Hapus Acara") },
                        text = { Text("Apakah Anda yakin ingin menghapus ${uiState.selectedIds.size} acara yang dipilih? Semua nota di dalamnya juga akan terhapus.") },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.deleteSelectedAcara() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("Hapus", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { 
                                viewModel.setShowDeleteDialog(false)
                                viewModel.toggleSelectionMode(false)
                            }) {
                                Text("Batal", color = Color(0xFF4B6E4E))
                            }
                        }
                    )
                }
            }
        }

        // --- TOUR OVERLAY LOGIC ---
        if (uiState.tourStep in 1..4) {
            val (currentRect, title, desc) = when (uiState.tourStep) {
                1 -> Triple(searchRect, "Selamat datang!", "Di sini kamu bisa mencari nama acara atau laporan pengeluaranmu dengan cepat.")
                2 -> Triple(dateRect, "Filter Tanggal", "Gunakan fitur ini untuk menyaring dan melihat daftar pengeluaran berdasarkan rentang waktu tertentu.")
                3 -> Triple(menuRect, "Menu Bantuan", "Ketuk menu titik tiga ini untuk mengakses opsi 'Pilih Acara' atau 'Bantuan' kapan saja.")
                4 -> Triple(fabRect, "Buat Acara", "Yuk, mulai rekap pengeluaranmu! Ketuk tombol yang menyala ini untuk membuat acara pertamamu.")
                else -> Triple(null, "", "")
            }

            TourOverlay(
                targetRect = currentRect,
                title = title,
                description = desc,
                onNext = { viewModel.nextTourStep() },
                onSkip = { viewModel.skipTour() },
                currentStep = uiState.tourStep,
                totalSteps = 5,
                showNextButton = uiState.tourStep != 4
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AcaraCard(
    title: String,
    description: String,
    dateRange: String,
    totalNota: Int,
    totalExpense: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFEADDFF) else Color.White),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF6750A4) else Color(0xFFE0E0E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                if (isSelectionMode) { Checkbox(checked = isSelected, onCheckedChange = { onClick() }) }
            }
            
            if (description.isNotBlank()) {
                Text(text = description, color = Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Tanggal: $dateRange", fontSize = 13.sp)
                val notaCountStr = if (totalNota == 0) "-" else totalNota.toString()
                Text(
                    "Total Nota: $notaCountStr  |  Total Pengeluaran: $totalExpense",
                    fontSize = 13.sp, color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun AcaraInputDialog(
    title: String, confirmLabel: String, nama: String, deskripsi: String,
    onNamaChange: (String) -> Unit, onDeskripsiChange: (String) -> Unit,
    onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nama, onValueChange = onNamaChange, label = { Text("Nama Acara") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = deskripsi, onValueChange = onDeskripsiChange, label = { Text("Deskripsi (Opsional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = nama.isNotBlank()) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
