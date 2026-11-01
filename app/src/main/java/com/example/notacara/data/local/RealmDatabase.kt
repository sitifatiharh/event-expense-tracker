package com.example.notacara.data.local

import com.example.notacara.data.local.entity.AcaraEntity
import com.example.notacara.data.local.entity.LabelEntitasEntity
import com.example.notacara.data.local.entity.NotaEntity
import com.example.notacara.data.local.entity.TransaksiEntity
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

object RealmDatabase {
    private val schema = setOf(
        AcaraEntity::class,
        NotaEntity::class,
        TransaksiEntity::class,
        LabelEntitasEntity::class
    )

    private val configuration = RealmConfiguration.Builder(schema)
        .name("notacara.realm")
        .schemaVersion(1)
        .deleteRealmIfMigrationNeeded()
        .build()

    val realm: Realm by lazy {
        Realm.open(configuration)
    }
}
