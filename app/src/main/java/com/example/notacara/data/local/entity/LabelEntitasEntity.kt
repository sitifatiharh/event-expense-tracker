package com.example.notacara.data.local.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class LabelEntitasEntity : RealmObject {
    @PrimaryKey
    var label_id: Int = 0
    var nota_id: Int = 0
    var label: String = ""
    var nilai: String = ""
    var start_index: Int = 0
    var end_index: Int = 0
}
