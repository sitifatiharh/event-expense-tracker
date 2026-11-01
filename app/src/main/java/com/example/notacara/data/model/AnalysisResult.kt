package com.example.notacara.data.model

data class AnalysisResult(
    val namaToko: String = "",
    val tanggal: String = "",
    val items: List<NotaItemEdit> = emptyList(),
    val diskon: String = "0",
    val totalBelanja: String = "0",
    val imageUri: String = "",
    val rawText: String = ""
)
