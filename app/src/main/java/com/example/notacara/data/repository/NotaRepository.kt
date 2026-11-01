package com.example.notacara.data.repository

import com.example.notacara.data.local.entity.AcaraEntity
import com.example.notacara.data.local.entity.NotaEntity
import com.example.notacara.data.local.entity.TransaksiEntity
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotaRepository(private val realm: Realm) {

    private fun <T : io.realm.kotlin.types.RealmObject> safeCopy(listProvider: () -> List<T>): List<T> {
        return try {
            if (!realm.isClosed()) {
                realm.copyFromRealm(listProvider())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- ACARA ---
    val allAcara: Flow<List<AcaraEntity>> = realm.query<AcaraEntity>().asFlow()
        .map { safeCopy { it.list } }

    suspend fun insertAcara(acara: AcaraEntity): Int {
        return realm.write {
            if (acara.acara_id == 0) {
                val maxId = query<AcaraEntity>().find().maxOfOrNull { it.acara_id } ?: 0
                acara.acara_id = maxId + 1
            }
            copyToRealm(acara, UpdatePolicy.ALL).acara_id
        }
    }

    suspend fun updateAcara(acara: AcaraEntity) {
        realm.write {
            val liveAcara = query<AcaraEntity>("acara_id == $0", acara.acara_id).find().firstOrNull()
            liveAcara?.apply {
                nama_acara = acara.nama_acara
                deskripsi = acara.deskripsi
            }
        }
    }

    suspend fun deleteAcaraByIds(ids: List<Int>) {
        realm.write {
            ids.forEach { id ->
                val acara = query<AcaraEntity>("acara_id == $0", id).find().firstOrNull()
                if (acara != null) {
                    val notas = query<NotaEntity>("acara_id == $0", id).find()
                    notas.forEach { nota ->
                        val transaksis = query<TransaksiEntity>("nota_id == $0", nota.nota_id).find()
                        delete(transaksis)
                    }
                    delete(notas)
                    delete(acara)
                }
            }
        }
    }

    // --- NOTA ---
    val allNota: Flow<List<NotaEntity>> = realm.query<NotaEntity>().asFlow()
        .map { safeCopy { it.list } }

    suspend fun insertNota(nota: NotaEntity): Int {
        return realm.write {
            if (nota.nota_id == 0) {
                val maxId = query<NotaEntity>().find().maxOfOrNull { it.nota_id } ?: 0
                nota.nota_id = maxId + 1
            }
            copyToRealm(nota, UpdatePolicy.ALL).nota_id
        }
    }

    suspend fun updateNotaTransaksi(nota: NotaEntity, transaksiList: List<TransaksiEntity>): Int {
        return realm.write {
            if (nota.nota_id == 0) {
                val maxId = query<NotaEntity>().find().maxOfOrNull { it.nota_id } ?: 0
                nota.nota_id = maxId + 1
            }
            val managedNota = copyToRealm(nota, UpdatePolicy.ALL)
            val notaId = managedNota.nota_id

            val oldTransaksis = query<TransaksiEntity>("nota_id == $0", notaId).find()
            delete(oldTransaksis)

            var currentMaxTransId = query<TransaksiEntity>().find().maxOfOrNull { it.transaksi_id } ?: 0
            transaksiList.forEach { trans ->
                trans.nota_id = notaId
                if (trans.transaksi_id == 0) {
                    trans.transaksi_id = ++currentMaxTransId
                }
                copyToRealm(trans, UpdatePolicy.ALL)
            }
            notaId
        }
    }

    suspend fun deleteNotasByIds(ids: List<Int>) {
        realm.write {
            ids.forEach { id ->
                // Perbaikan: Query harus menggunakan nota_id, bukan acara_id
                val nota = query<NotaEntity>("nota_id == $0", id).find().firstOrNull()
                if (nota != null) {
                    val transaksis = query<TransaksiEntity>("nota_id == $0", id).find()
                    delete(transaksis)
                    delete(nota)
                }
            }
        }
    }

    fun getNotasByAcara(acaraId: Int): Flow<List<NotaEntity>> {
        return realm.query<NotaEntity>("acara_id == $0", acaraId).asFlow()
            .map { safeCopy { it.list } }
    }

    // --- TRANSAKSI ---
    val allTransaksi: Flow<List<TransaksiEntity>> = realm.query<TransaksiEntity>().asFlow()
        .map { safeCopy { it.list } }

    suspend fun insertAllTransaksi(transaksiList: List<TransaksiEntity>) {
        realm.write {
            var currentMaxId = query<TransaksiEntity>().find().maxOfOrNull { it.transaksi_id } ?: 0
            transaksiList.forEach { trans ->
                if (trans.transaksi_id == 0) {
                    trans.transaksi_id = ++currentMaxId
                }
                copyToRealm(trans, UpdatePolicy.ALL)
            }
        }
    }

    suspend fun deleteTransaksiByNotaId(notaId: Int) {
        realm.write {
            val transaksis = query<TransaksiEntity>("nota_id == $0", notaId).find()
            delete(transaksis)
        }
    }

    fun getTransaksiByNota(notaId: Int): Flow<List<TransaksiEntity>> {
        return realm.query<TransaksiEntity>("nota_id == $0", notaId).asFlow()
            .map { safeCopy { it.list } }
    }
}
