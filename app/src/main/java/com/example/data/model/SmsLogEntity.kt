package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageHash: String, // SHA-256 hash to deduplicate
    val sender: String,
    val timestamp: Long,
    val extractedAmount: Double,
    val isBankTransaction: Boolean,
    val status: String // "AUTO_RECORDED", "MANUALLY_VERIFIED", "IGNORED"
)
