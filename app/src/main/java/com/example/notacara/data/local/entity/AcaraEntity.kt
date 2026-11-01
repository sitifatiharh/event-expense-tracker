package com.example.notacara.data.local.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class AcaraEntity : RealmObject {
    @PrimaryKey
    var acara_id: Int = 0
    var nama_acara: String = ""
    var deskripsi: String = ""
}
