package com.example.notacara.data.model

data class NotaItemEdit(
    val id: Long = System.currentTimeMillis(),
    val nama: String = "",
    val jumlah: String = "1",
    val satuan: String = "0",
    val subtotal: String = "0"
)
