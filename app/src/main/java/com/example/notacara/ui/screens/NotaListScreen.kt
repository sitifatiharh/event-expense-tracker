package com.example.notacara.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notacara.data.local.entity.NotaEntity
import com.example.notacara.ui.components.DateRangePickerDialog
import com.example.notacara.ui.components.HomeTopBar
import com.example.notacara.ui.components.SimpleTourPopUp
import com.example.notacara.ui.components.TotalExpenditureBottomBar
import com.example.notacara.ui.components.TourOverlay
import com.example.notacara.ui.viewmodel.HomeViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun NotaListScreen(
    acaraName: String,
    onScannerResult: (Uri) -> Unit,
    onNotaClick: (Int) -> Unit,
    onEditNota: () -> Unit,
    onHelpClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsState()
    
    var cachedAcaraId by rememberSaveable { mutableStateOf<Int?>(null) }
    var fabRect by remember { mutableStateOf<Rect?>(null) }
    var showScanTips by remember { mutableStateOf(false) }
    var isFocusLocked by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.toggleSelectionMode(false)
        focusManager.clearFocus(force = true)
        delay(500)
        isFocusLocked = false
    }

    LaunchedEffect(acaraName) {
        viewModel.allAcara.value.find { it.nama_acara == acaraName }?.let {
            cachedAcaraId = it.acara_id
        }
    }

    LaunchedEffect(uiState.tourStep) {
        if (uiState.tourStep == 5) {
            delay(600)
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    LaunchedEffect(cachedAcaraId) {
        cachedAcaraId?.let { viewModel.getNotasByAcaraId(it) }
    }

    val currentAcara by viewModel.currentAcaraDetails.collectAsState()
    val notaList by viewModel.filteredNotas.collectAsState()
    val listState = rememberLazyListState()

    var isEditingName by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var tempName by remember(currentAcara?.nama_acara) { 
        mutableStateOf(TextFieldValue(
            text = currentAcara?.nama_acara ?: acaraName,
            selection = TextRange(currentAcara?.nama_acara?.length ?: acaraName.length)
        )) 
    }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    fun saveName() {
        if (isEditingName) {
            val newName = tempName.text.trim()
            val acara = currentAcara

            isFocusLocked = true
            focusManager.clearFocus(force = true)
            isSearchFocused = false
            
            if (acara != null && newName.isNotBlank() && newName != acara.nama_acara) {
                viewModel.updateAcaraDescription(acara.acara_id, newName, acara.deskripsi)
            }
            
            scope.launch {
                delay(100)
                isEditingName = false
                delay(400)
                isFocusLocked = false
            }
        }
    }

    val clearAndBack = {
        if (isEditingName) {
            saveName()
        } else if (uiState.isSelectionMode) {
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
    var keyboardWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && isSearchFocused) {
        }
        keyboardWasVisible = isImeVisible
    }

    LaunchedEffect(uiState.tourStep) {
        if (uiState.tourStep != 0) {
            isFocusLocked = true
            focusManager.clearFocus(force = true)
        } else {
            delay(500)
            isFocusLocked = false
        }
    }

    if (isEditingName) {
        DisposableEffect(Unit) {
            onDispose {
                focusManager.clearFocus(force = true)
            }
        }
    }

    BackHandler(enabled = !isImeVisible && (uiState.isSelectionMode || uiState.searchQuery.isNotEmpty() || uiState.dateFilter != null || isEditingName || isSearchFocused)) {
        clearAndBack()
    }

    LaunchedEffect(isEditingName) {
        if (isEditingName) {
            tempName = tempName.copy(selection = TextRange(tempName.text.length))
            delay(100)
            focusRequester.requestFocus()
        }
    }

    var showEditDescDialog by remember { mutableStateOf(false) }
    var tempDesc by remember { mutableStateOf("") }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result: ActivityResult ->
        focusManager.clearFocus(force = true)
        isSearchFocused = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.firstOrNull()?.imageUri?.let { uri: Uri ->
                onScannerResult(uri)
            }
        }
    }

    fun launchScanner() {
        focusManager.clearFocus(force = true)
        isSearchFocused = false
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER)
            .build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(context as android.app.Activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
    }

    if (uiState.showDateRangeDialog) {
        DateRangePickerDialog(
            onDismiss = { viewModel.showDateRangeDialog(false) },
            onConfirm = { from, until -> viewModel.setDateRange(from, until) }
        )
    }

    if (showEditDescDialog && currentAcara != null) {
        val acara = currentAcara!!
        AlertDialog(
            onDismissRequest = { showEditDescDialog = false },
            title = { Text("Edit Deskripsi") },
            text = {
                OutlinedTextField(
                    value = tempDesc,
                    onValueChange = { tempDesc = it },
                    label = { Text("Deskripsi Acara") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    maxLines = 5
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateAcaraDescription(acara.acara_id, tempName.text, tempDesc)
                    showEditDescDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showEditDescDialog = false }) { Text("Batal") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
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
                    onEditClick = { 
                        viewModel.prepareEditFromSelectedNota()
                        viewModel.toggleSelectionMode(false)
                        onEditNota()
                    },
            onDeleteClick = { viewModel.setShowDeleteDialog(true) },
            onBack = { clearAndBack() },
            onClearSearch = { viewModel.onSearchQueryChange("") },
            enabled = uiState.tourStep == 0 && !isEditingName && !isFocusLocked,
            menuLabel = "Pilih Nota",
            onSearchFocused = { isSearchFocused = it }
        )
    },
            floatingActionButton = {
                if (!uiState.isSelectionMode) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if(isEditingName) saveName()
                            if (uiState.isFirstTimeScan) {
                                showScanTips = true
                            } else {
                                launchScanner()
                            }
                        },
                        containerColor = Color.White, contentColor = Color.Black, shape = RoundedCornerShape(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(4.dp), 
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .onGloballyPositioned { fabRect = it.boundsInRoot() }
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tambah Nota", fontWeight = FontWeight.Bold)
                    }
                }
            },
            bottomBar = {
                val filteredTotal by viewModel.filteredTotalExpense.collectAsState()
                TotalExpenditureBottomBar(label = "Total Pengeluaran", total = filteredTotal)
            }
        ) { innerPadding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isEditingName) saveName()
                    focusManager.clearFocus()
                }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (isEditingName) {
                                BasicTextField(
                                    value = tempName,
                                    onValueChange = { tempName = it },
                                    textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    singleLine = true,
                                    cursorBrush = SolidColor(Color.Black),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { saveName() })
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isEditingName = true }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = currentAcara?.nama_acara ?: tempName.text,
                                        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            val desc = currentAcara?.deskripsi
                            Text(
                                text = if (desc.isNullOrBlank()) "Tambah deskripsi..." else desc,
                                fontSize = 14.sp,
                                color = if (desc.isNullOrBlank()) Color.LightGray else Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (isEditingName) saveName()
                                    tempDesc = currentAcara?.deskripsi ?: ""
                                    showEditDescDialog = true
                                }
                            )

                            Text(text = "Tanggal: ${currentAcara?.dateRange ?: "-"}", fontSize = 14.sp, color = Color.Gray)
                            
                            Text(
                                text = "Total Nota: ${currentAcara?.totalNota ?: 0}",
                                fontSize = 14.sp, color = Color.Gray
                            )
                        }
                    }

                    if (notaList.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                                val emptyMsg = if (uiState.searchQuery.isNotEmpty() || uiState.dateFilter != null) {
                                    "Tidak ada nota yang sesuai pencarian."
                                } else {
                                    "Belum ada nota.\nKetuk + Tambah Nota untuk memulai."
                                }
                                Text(text = emptyMsg, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        items(notaList) { nota ->
                            val trans by viewModel.getTransaksiForNota(nota.nota_id).collectAsState(initial = emptyList())
                            val isSelected = uiState.selectedIds.contains(nota.nota_id)
                            NotaItemRow(
                                nota = nota, 
                                itemCount = trans.size,
                                isSelected = isSelected,
                                isSelectionMode = uiState.isSelectionMode,
                                onLongClick = {
                                    if (!uiState.isSelectionMode) {
                                        viewModel.toggleSelectionMode(true)
                                        viewModel.toggleItemSelection(nota.nota_id)
                                    }
                                },
                                onClick = { 
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleItemSelection(nota.nota_id)
                                    } else {
                                        if (isEditingName) saveName()
                                        onNotaClick(nota.nota_id)
                                    }
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }

                if (uiState.showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { 
                            viewModel.setShowDeleteDialog(false)
                            viewModel.toggleSelectionMode(false)
                        },
                        title = { Text("Hapus Nota") },
                        text = { Text("Apakah Anda yakin ingin menghapus ${uiState.selectedIds.size} nota yang dipilih?") },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.deleteSelectedNotas() },
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

        if (uiState.tourStep == 5) {
            TourOverlay(
                targetRect = fabRect,
                title = "Acara berhasil dibuat!",
                description = "Sekarang, ketuk tombol ini untuk mulai memindai nota belanjamu.",
                onNext = { },
                onSkip = { viewModel.skipTour() },
                currentStep = 5,
                totalSteps = 5,
                showNextButton = false
            )
        }

        if (showScanTips) {
            SimpleTourPopUp(
                title = "💡 Tips Hasil Scan Maksimal:",
                description = "• Pastikan ruangan cukup terang.\n" +
                              "• Ratakan nota jika terlipat.\n" +
                              "• Arahkan kamera tegak lurus sejajar di atas nota.\n" +
                              "• Pastikan teks nota tidak miring.\n" +
                              "• Gunakan fitur filter jika perlu.\n" +
                              "• Kamu juga bisa memilih foto nota langsung dari Galeri HP.",
                currentStep = 0,
                totalSteps = 0,
                onDismiss = {
                    showScanTips = false
                    viewModel.completeFirstTimeScan()
                    viewModel.skipTour()
                    launchScanner()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotaItemRow(
    nota: NotaEntity, 
    itemCount: Int, 
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val localeID = Locale.forLanguageTag("id-ID")
    val numberFormat = NumberFormat.getInstance(localeID)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) Color(0xFFEADDFF) else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF6750A4) else Color(0xFFEEEEEE)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nota.nama_toko.ifBlank { "Toko tidak diketahui" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onClick() }) }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Text(text = "Tanggal: ", fontSize = 14.sp)
                Text(text = sdf.format(Date(nota.tanggal)), fontSize = 14.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Row {
                    Text(text = "Total Belanja: ", fontSize = 14.sp)
                    Text(text = "Rp${numberFormat.format(nota.total_belanja)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "($itemCount Item)", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}
