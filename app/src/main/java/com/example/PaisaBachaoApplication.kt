package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.PaisaBachaoRepository
import com.example.security.SecurityManager
import com.example.sms.SmsReaderHelper
import com.example.sync.SyncBackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PaisaBachaoApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { PaisaBachaoRepository(database) }
    val securityManager by lazy { SecurityManager(this) }
    val syncBackupManager by lazy { SyncBackupManager(this, database, repository) }
    val smsReaderHelper by lazy { SmsReaderHelper(this, database, securityManager, repository) }
    val biometricAuthManager by lazy { com.example.security.BiometricAuthManager(this) }

    override fun onCreate() {
        super.onCreate()
        val appScope = CoroutineScope(Dispatchers.IO)

        // Seed default starter data locally in background if database is fresh
        appScope.launch {
            syncBackupManager.seedInitialDataIfEmpty()
        }
    }
}
