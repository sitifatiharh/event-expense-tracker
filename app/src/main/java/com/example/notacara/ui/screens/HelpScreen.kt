package com.example.notacara.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HelpItem(val title: String, val content: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val helpItems = listOf(
        HelpItem(
            "Kriteria Nota",
            "• Gunakan nota belanja cetakan komputer, bukan tulisan tangan.\n" +
            "• Pastikan nota tidak bertinta pudar atau kertasnya robek parah."
        ),
        HelpItem(
            "Mengelola Acara",
            "• Ketuk tombol (+ Tambah Acara) di halaman utama untuk membuat laporan acara baru.\n" +
            "• Isi nama acara (misal: \"Liburan\") dan deskripsi (opsional).\n" +
            "• Tahan (long-press) atau gunakan menu titik tiga opsi Pilih Acara pada halaman utama jika ingin hapus/edit Acara."
        ),
        HelpItem(
            "Memindai Nota Belanja",
            "• Buka salah satu Acara, lalu ketuk tombol (+ Tambah Nota).\n" +
            "• Ratakan nota jika terlipat.\n" +
            "• Arahkan kamera HP sejajar dan tegak lurus di atas nota atau pilih foto nota dari Galeri HP.\n" +
            "• Pastikan teks nota tidak miring.\n" +
            "• Gunakan fitur filter jika perlu agar teks nota mudah terdeteksi.\n" +
            "• Ketuk Berikutnya dan biarkan AI mengekstrak teksnya."
        ),
        HelpItem(
            "Mengoreksi Hasil AI",
            "• Mesin AI tidak selalu 100% sempurna.\n" +
            "• Setelah proses pemindaian selesai, ketuk langsung pada kolom teks atau nominal harga di layar jika ada yang salah baca.\n" +
            "• Jangan lupa scroll gambar nota di atas layar untuk mencocokkan hasil teks dengan foto aslinya.\n" +
            "• Ketuk Simpan untuk memasukkannya ke rekap pengeluaran."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bantuan Penggunaan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(helpItems) { item ->
                ExpandableHelpCard(item)
            }
        }
    }
}

@Composable
fun ExpandableHelpCard(item: HelpItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = item.content,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}
