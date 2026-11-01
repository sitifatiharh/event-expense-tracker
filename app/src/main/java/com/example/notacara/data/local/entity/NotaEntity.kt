package com.example.notacara.data.local.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class NotaEntity : RealmObject {
    @PrimaryKey
    var nota_id: Int = 0
    var acara_id: Int = 0
    var teks_mentah: String = ""
    var nama_toko: String = ""
    var tanggal: Long = 0
    var diskon: Int = 0
    var total_belanja: Int = 0
    var image_uri: String? = null
}
