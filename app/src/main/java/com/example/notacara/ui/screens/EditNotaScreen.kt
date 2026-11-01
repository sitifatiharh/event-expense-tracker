package com.example.notacara.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.notacara.data.model.AnalysisResult
import com.example.notacara.data.model.NotaItemEdit
import com.example.notacara.ui.components.SimpleTourPopUp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun EditNotaScreen(
    analysisResult: AnalysisResult?,
    imageUri: Uri?,
    isNewNota: Boolean,
    isFirstTimeEdit: Boolean = false,
    onDismissTour: () -> Unit = {},
    onSaveClick: (String, String, String, String, List<NotaItemEdit>, Uri?) -> Unit,
    onCancelClick: () -> Unit,
    onReScan: (Uri) -> Unit
) {
    val context = LocalContext.current
    val localeID = remember { Locale("id", "ID") }
    val numberFormat = remember { NumberFormat.getInstance(localeID) }

    var currentImageUri by remember(imageUri) { mutableStateOf(imageUri) }
    var namaToko by remember(analysisResult) { mutableStateOf(analysisResult?.namaToko ?: "") }
    var tanggal by remember(analysisResult) { 
        mutableStateOf(analysisResult?.tanggal?.ifBlank { 
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) 
        } ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) 
    }
    var diskon by remember(analysisResult) { mutableStateOf(analysisResult?.diskon ?: "0") }
    var items by remember(analysisResult) { mutableStateOf(analysisResult?.items ?: emptyList()) }

    fun String.cleanNumber(): Double = this.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

    val initialSnapshot = remember(analysisResult, imageUri) {
        EditSnapshot(
            namaToko = analysisResult?.namaToko ?: "",
            tanggal = analysisResult?.tanggal ?: "",
            diskon = analysisResult?.diskon ?: "0",
            items = analysisResult?.items?.toList() ?: emptyList(),
            imageUri = imageUri
        )
    }

    val hasChanges by remember(namaToko, tanggal, diskon, items, currentImageUri) {
        derivedStateOf {
            val current = EditSnapshot(namaToko, tanggal, diskon, items.toList(), currentImageUri)
            current != initialSnapshot
        }
    }

    val undoStack = remember { mutableStateListOf<EditSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditSnapshot>() }

    fun captureSnapshot() {
        val current = EditSnapshot(namaToko, tanggal, diskon, items.toList(), currentImageUri)
        if (undoStack.isEmpty() || undoStack.last() != current) {
            undoStack.add(current)
            redoStack.clear()
            if (undoStack.size > 30) undoStack.removeAt(0)
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.firstOrNull()?.imageUri?.let { uri ->
                onReScan(uri)
            }
        }
    }

    fun triggerReScan() {
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

    var showCancelConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isFullscreenImage by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val totalBelanja by remember(items, diskon) {
        derivedStateOf {
            val sumSubtotal = items.sumOf { it.subtotal.cleanNumber() }
            val diskonVal = diskon.cleanNumber()
            (sumSubtotal - diskonVal).coerceAtLeast(0.0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Edit Nota", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                    actions = {
                        IconButton(
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    redoStack.add(EditSnapshot(namaToko, tanggal, diskon, items.toList(), currentImageUri))
                                    val last = undoStack.removeAt(undoStack.size - 1)
                                    namaToko = last.namaToko; tanggal = last.tanggal; diskon = last.diskon; items = last.items; currentImageUri = last.imageUri
                                }
                            },
                            enabled = undoStack.isNotEmpty()
                        ) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = if (undoStack.isNotEmpty()) Color.Black else Color.LightGray) }
                        IconButton(
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    undoStack.add(EditSnapshot(namaToko, tanggal, diskon, items.toList(), currentImageUri))
                                    val next = redoStack.removeAt(redoStack.size - 1)
                                    namaToko = next.namaToko; tanggal = next.tanggal; diskon = next.diskon; items = next.items; currentImageUri = next.imageUri
                                }
                            },
                            enabled = redoStack.isNotEmpty()
                        ) { Icon(Icons.AutoMirrored.Filled.Redo, "Redo", tint = if (redoStack.isNotEmpty()) Color.Black else Color.LightGray) }
                    }
                )
            },
            bottomBar = {
                Surface(shadowElevation = 8.dp, color = Color.White, modifier = Modifier.navigationBarsPadding()) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = { 
                                if (hasChanges) showCancelConfirm = true 
                                else onCancelClick() 
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text("Batal", color = Color.Gray) }
                        Button(
                            onClick = { onSaveClick(namaToko, tanggal, diskon, totalBelanja.toLong().toString(), items, currentImageUri) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BC34A))
                        ) { Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color.White),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                stickyHeader {
                    Column(modifier = Modifier.background(Color.White).padding(horizontal = 16.dp)) {
                        Text("Nota", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                if (currentImageUri != null) {
                                    AsyncImage(model = currentImageUri, contentDescription = null, modifier = Modifier.fillMaxWidth().clickable { isFullscreenImage = true }, contentScale = ContentScale.FillWidth)
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().height(200.dp), contentAlignment = Alignment.Center) { Text("Gambar tidak tersedia", color = Color.Gray) }
                                }
                            }
                            if (isNewNota) {
                                Surface(
                                    onClick = { triggerReScan() },
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                                    shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.9f), shadowElevation = 2.dp
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Foto Ulang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("Nama Toko", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        BasicUnderlineInputField(value = namaToko, onValueChange = { namaToko = it }, onFocusStarted = { captureSnapshot() })
                    }
                }
                
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("Tanggal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }.padding(vertical = 12.dp)) {
                            Text(tanggal, fontSize = 16.sp)
                            HorizontalDivider(modifier = Modifier.align(Alignment.BottomStart), color = Color(0xFFE0E0E0), thickness = 1.dp)
                        }
                    }
                }

                item { Text("Rincian Barang", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp)) }

                itemsIndexed(items) { index, item ->
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DragHandle, null, tint = Color.LightGray,
                                modifier = Modifier.size(24.dp).clickable {
                                    if (index > 0) {
                                        captureSnapshot()
                                        val mutableItems = items.toMutableList()
                                        Collections.swap(mutableItems, index, index - 1)
                                        items = mutableItems
                                    }
                                }
                            )
                            BasicUnderlineInputField(
                                value = item.nama,
                                onValueChange = { val new = items.toMutableList(); new[index] = item.copy(nama = it); items = new },
                                onFocusStarted = { captureSnapshot() },
                                modifier = Modifier.weight(1f).padding(start = 8.dp, end = 0.dp)
                            )
                            Icon(
                                Icons.Default.Delete, null, tint = Color.Gray,
                                modifier = Modifier.size(24.dp).clickable { captureSnapshot(); items = items.filterIndexed { i, _ -> i != index } }
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 24.dp).padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Jumlah", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                                BasicUnderlineInputField(
                                    value = item.jumlah,
                                    onValueChange = { 
                                        val qtyVal = it.cleanNumber()
                                        val satVal = item.satuan.cleanNumber()
                                        val new = items.toMutableList()
                                        new[index] = item.copy(jumlah = it, subtotal = String.format("%.0f", qtyVal * satVal))
                                        items = new
                                    },
                                    onFocusStarted = { captureSnapshot() },
                                    keyboardType = KeyboardType.Number,
                                    isNumberField = true,
                                    textAlign = TextAlign.Start,
                                    isItemDetail = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Satuan", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                                BasicUnderlineInputField(
                                    value = item.satuan,
                                    onValueChange = { 
                                        val satVal = it.cleanNumber()
                                        val qtyVal = item.jumlah.cleanNumber()
                                        val new = items.toMutableList()
                                        new[index] = item.copy(satuan = it, subtotal = String.format("%.0f", qtyVal * satVal))
                                        items = new
                                    },
                                    onFocusStarted = { captureSnapshot() },
                                    keyboardType = KeyboardType.Number,
                                    isNumberField = true,
                                    textAlign = TextAlign.Start,
                                    isItemDetail = true
                                )
                            }

                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Subtotal", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                                BasicUnderlineInputField(
                                    value = item.subtotal,
                                    onValueChange = { 
                                        val subVal = it.cleanNumber()
                                        val qtyVal = item.jumlah.cleanNumber().coerceAtLeast(1.0)
                                        val new = items.toMutableList()
                                        new[index] = item.copy(subtotal = it, satuan = String.format("%.0f", subVal / qtyVal))
                                        items = new
                                    },
                                    onFocusStarted = { captureSnapshot() },
                                    keyboardType = KeyboardType.Number,
                                    isNumberField = true,
                                    textAlign = TextAlign.Start,
                                    isItemDetail = true
                                )
                            }
                        }
                    }
                }

                item {
                    TextButton(onClick = { captureSnapshot(); items = items + NotaItemEdit() }, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                        Text("Tambah Barang Manual", fontSize = 12.sp)
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Diskon", fontSize = 14.sp)
                            BasicUnderlineInputField(
                                value = diskon, onValueChange = { diskon = it }, onFocusStarted = { captureSnapshot() },
                                keyboardType = KeyboardType.Number, modifier = Modifier.width(120.dp), isNumberField = true
                            )
                        }
                        HorizontalDivider(color = Color.LightGray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Belanja", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                            Text("Rp ${numberFormat.format(totalBelanja.toLong())}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            if (isFullscreenImage) {
                Dialog(onDismissRequest = { isFullscreenImage = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        AsyncImage(model = currentImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        IconButton(onClick = { isFullscreenImage = false }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(24.dp))) { Icon(Icons.Default.Close, null, tint = Color.White) }
                    }
                }
            }

            if (showCancelConfirm) {
                AlertDialog(
                    onDismissRequest = { showCancelConfirm = false },
                    confirmButton = { Button(onClick = { showCancelConfirm = false }, shape = RoundedCornerShape(24.dp)) { Text("Lanjut Edit") } },
                    dismissButton = { TextButton(onClick = onCancelClick) { Text("Keluar", color = Color.Gray) } },
                    title = { Text("Batalkan perubahan?") }, text = { Text("Data yang Anda ketik akan hilang.") },
                    shape = RoundedCornerShape(24.dp), containerColor = Color.White
                )
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { 
                            datePickerState.selectedDateMillis?.let { captureSnapshot(); tanggal = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)) }
                            showDatePicker = false 
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }
                ) { DatePicker(state = datePickerState, showModeToggle = false) }
            }
        }

        if (isNewNota && isFirstTimeEdit) {
            SimpleTourPopUp(
                title = "🤖 AI Selesai Membaca!",
                description = "Silakan periksa kembali hasilnya. Kamu bisa mengetuk kolom teks untuk mengoreksi nama barang atau harga jika ada yang keliru. Oya, foto nota di layar juga bisa kamu scroll lho! Pastikan semuanya sudah sesuai sebelum disimpan.",
                buttonText = "Tutup",
                onDismiss = onDismissTour
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BasicUnderlineInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusStarted: () -> Unit = {},
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isNumberField: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    isItemDetail: Boolean = false
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && isFocused) {
            focusManager.clearFocus()
            textFieldValueState = textFieldValueState.copy(selection = TextRange(textFieldValueState.text.length))
        }
    }

    LaunchedEffect(value) {
        if (!isFocused && textFieldValueState.text != value) {
            textFieldValueState = textFieldValueState.copy(text = value)
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocusStarted()
            textFieldValueState = textFieldValueState.copy(
                selection = TextRange(0, textFieldValueState.text.length)
            )
        }
    }

    Column(modifier = modifier) {
        BasicTextField(
            value = textFieldValueState,
            onValueChange = {
                textFieldValueState = it
                onValueChange(it.text)
            },
            textStyle = TextStyle(fontSize = 16.sp, color = Color.Black, textAlign = textAlign),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).onFocusChanged { isFocused = it.isFocused },
            singleLine = true,
            cursorBrush = SolidColor(Color.Black)
        )
        val dividerColor = if (isFocused) Color(0xFF8BC34A) else if (isItemDetail) Color(0xFFE0E0E0) else Color.LightGray
        val dividerThickness = if (isFocused) 1.5.dp else 1.dp
        
        HorizontalDivider(color = dividerColor, thickness = dividerThickness)
    }
}

data class EditSnapshot(
    val namaToko: String,
    val tanggal: String,
    val diskon: String,
    val items: List<NotaItemEdit>,
    val imageUri: Uri?
)
