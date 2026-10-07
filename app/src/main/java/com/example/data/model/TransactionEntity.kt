package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity for 'Transaction' with database-level UNIQUE index
 * on referenceId (UTR / UPI Ref / Bank Txn ID) to prevent duplicate transactions.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["referenceId"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val category: String, // e.g. "Food & Dining", "Groceries", "Shopping", "Bills & Utilities", "Transport & Fuel", "Cash", etc.
    val merchant: String, // e.g. "Swiggy", "Amazon", "Local Cash Payment"
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "MANUAL_CASH", // "SMS_AUTO", "MANUAL_CASH", "MANUAL_ONLINE"
    val accountOrBank: String = "Cash in Hand", // e.g. "Cash in Hand", "HDFC Bank XX4512", "SBI XX9012"
    val referenceId: String? = null, // UTR / UPI Ref / Bank Txn ID / Reference Number (Unique constraint)
    val notes: String = "",
    val rawSmsBodyEncrypted: String = "", // AES encrypted body if imported from bank SMS
    val smsSender: String = "", // e.g. "VM-HDFCBK"
    val isReconciled: Boolean = true,
    val isSyncedToCloud: Boolean = false, // Offline-first flag
    val cloudSyncTimestamp: Long = 0L // Timestamp of last successful cloud upload
)

// Alias so both Transaction and TransactionEntity refer to the Room entity
typealias Transaction = TransactionEntity
