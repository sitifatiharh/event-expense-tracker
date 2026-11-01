package com.example.notacara

import android.app.Application
import com.example.notacara.data.local.RealmDatabase
import com.example.notacara.data.local.datastore.UserTourManager
import com.example.notacara.data.repository.NotaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class NotacaraApplication : Application() {
    // Scope untuk operasi database yang berjalan selama aplikasi hidup
    val applicationScope = CoroutineScope(SupervisorJob())

    val realm by lazy { 
        RealmDatabase.realm 
    }
    
    val repository by lazy { 
        NotaRepository(realm) 
    }

    val userTourManager by lazy {
        UserTourManager(this)
    }
}
