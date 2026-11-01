package com.example.notacara.ui.screens

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.notacara.ui.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailNotaScreen(
    notaId: Int?,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    viewModel: HomeViewModel
) {
    val nota by if (notaId != null) viewModel.getNotaById(notaId).collectAsState(initial = null) else remember { mutableStateOf(null) }
    val transaksiList by if (notaId != null) viewModel.getTransaksiForNota(notaId).collectAsState(initial = emptyList()) else remember { mutableStateOf(emptyList()) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isFullscreenImage by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val localeID = Locale.forLanguageTag("id-ID")
    val numberFormat = NumberFormat.getInstance(localeID)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detail Nota", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Black)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onEditClick,
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit Nota", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(nota?.nama_toko ?: "Memuat...", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(nota?.tanggal?.let { sdf.format(Date(it)) } ?: "-", fontSize = 14.sp, color = Color.Gray)
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5F5F5),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    val displayUri = nota?.image_uri?.let { Uri.parse(it) }
                    if (displayUri != null) {
                        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            AsyncImage(
                                model = displayUri, 
                                contentDescription = "Nota Image", 
                                modifier = Modifier.fillMaxWidth().clickable { isFullscreenImage = true }, 
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Foto tidak tersedia", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Rincian Barang", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                        Spacer(Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Nama Barang", modifier = Modifier.weight(2f), fontSize = 12.sp, color = Color.Gray)
                            Text("Jml", modifier = Modifier.weight(0.5f), fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            Text("Satuan", modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            Text("Subtotal", modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.End)
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))

                        transaksiList.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.nama_barang, modifier = Modifier.weight(2f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(item.jumlah.toString(), modifier = Modifier.weight(0.5f), fontSize = 14.sp, textAlign = TextAlign.Center)
                                Text(numberFormat.format(item.harga_satuan), modifier = Modifier.weight(1f), fontSize = 14.sp, textAlign = TextAlign.Center)
                                Text(numberFormat.format(item.harga_total_barang), modifier = Modifier.weight(1f), fontSize = 14.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Diskon", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Rp ${numberFormat.format(nota?.diskon ?: 0)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Belanja", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Rp ${numberFormat.format(nota?.total_belanja ?: 0)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        if (isFullscreenImage) {
            Dialog(onDismissRequest = { isFullscreenImage = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    val displayUri = nota?.image_uri?.let { Uri.parse(it) }
                    AsyncImage(model = displayUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    IconButton(
                        onClick = { isFullscreenImage = false },
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    ) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
            }
        }
        
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Hapus Nota") },
                text = { Text("Apakah Anda yakin ingin menghapus nota ini?") },
                confirmButton = {
                    Button(
                        onClick = { 
                            notaId?.let { 
                                viewModel.deleteNota(it)
                                onBackClick() 
                            }
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text("Hapus", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Batal", color = Color(0xFF4B6E4E))
                    }
                }
            )
        }
    }
}
