package com.example.notacara.data.local.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class TransaksiEntity : RealmObject {
    @PrimaryKey
    var transaksi_id: Int = 0
    var nota_id: Int = 0
    var nama_barang: String = ""
    var jumlah: Int = 0
    var harga_satuan: Int = 0
    var harga_total_barang: Int = 0
}
