package com.example.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.model.SmsLogEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.PaisaBachaoRepository
import com.example.data.repository.TransactionInsertResult
import com.example.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsReaderHelper(
    private val context: Context,
    private val database: AppDatabase,
    private val securityManager: SecurityManager,
    private val repository: PaisaBachaoRepository = PaisaBachaoRepository(database)
) {

    data class ScanResult(
        val totalScanned: Int,
        val newTransactionsAdded: Int,
        val duplicatesSkipped: Int,
        val ignoredOtps: Int,
        val message: String
    )

    // Scans device inbox if permission is granted
    suspend fun scanDeviceInbox(): ScanResult = withContext(Dispatchers.IO) {
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")
        var scanned = 0
        var added = 0
        var duplicates = 0
        var otps = 0

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC LIMIT 100"
            )

            cursor?.use { c ->
                val addressIdx = c.getColumnIndex("address")
                val bodyIdx = c.getColumnIndex("body")
                val dateIdx = c.getColumnIndex("date")

                while (c.moveToNext()) {
                    scanned++
                    val address = if (addressIdx != -1) c.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) c.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()

                    val hash = securityManager.sha256(body + address + date)
                    val parsed = SmsParser.parse(body, address)

                    if (parsed.isBankTransaction) {
                        val encryptedBody = securityManager.encrypt(body)
                        val refId = parsed.referenceId.trim().ifBlank { null }
                        val txn = TransactionEntity(
                            amount = parsed.amount,
                            type = parsed.type,
                            category = parsed.category,
                            source = "SMS_AUTO",
                            merchant = parsed.merchant,
                            accountOrBank = parsed.accountOrBank,
                            referenceId = refId,
                            timestamp = date,
                            rawSmsBodyEncrypted = encryptedBody,
                            smsSender = address,
                            isReconciled = true,
                            notes = if (!refId.isNullOrBlank()) "Ref: $refId" else "Auto-synced from SMS"
                        )

                        // Centralized duplicate check & insertion
                        when (val insertResult = repository.insertTransactionSafely(txn)) {
                            is TransactionInsertResult.Success -> {
                                database.dao().insertSmsLog(
                                    SmsLogEntity(
                                        messageHash = hash,
                                        sender = address,
                                        timestamp = date,
                                        extractedAmount = parsed.amount,
                                        isBankTransaction = true,
                                        status = "AUTO_RECORDED"
                                    )
                                )
                                added++
                            }
                            is TransactionInsertResult.Duplicate -> {
                                duplicates++
                                database.dao().insertSmsLog(
                                    SmsLogEntity(
                                        messageHash = hash,
                                        sender = address,
                                        timestamp = date,
                                        extractedAmount = parsed.amount,
                                        isBankTransaction = true,
                                        status = "DUPLICATE_SKIPPED"
                                    )
                                )
                            }
                            is TransactionInsertResult.Error -> {
                                // Ignore insertion failure
                            }
                        }
                    } else {
                        if (parsed.ignoreReason.contains("OTP", ignoreCase = true)) {
                            otps++
                        }
                        database.dao().insertSmsLog(
                            SmsLogEntity(
                                messageHash = hash,
                                sender = address,
                                timestamp = date,
                                extractedAmount = 0.0,
                                isBankTransaction = false,
                                status = "IGNORED"
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            return@withContext ScanResult(
                totalScanned = 0,
                newTransactionsAdded = 0,
                duplicatesSkipped = 0,
                ignoredOtps = 0,
                message = "SMS Permission not granted. Please allow SMS permission in settings."
            )
        } catch (e: Exception) {
            return@withContext ScanResult(
                totalScanned = scanned,
                newTransactionsAdded = added,
                duplicatesSkipped = duplicates,
                ignoredOtps = otps,
                message = "Error scanning inbox: ${e.localizedMessage}"
            )
        }

        ScanResult(
            totalScanned = scanned,
            newTransactionsAdded = added,
            duplicatesSkipped = duplicates,
            ignoredOtps = otps,
            message = if (added > 0) {
                "Successfully imported $added new bank transactions!" + if (duplicates > 0) " ($duplicates duplicates skipped)" else ""
            } else if (duplicates > 0) {
                "All $duplicates detected transactions were already recorded (duplicates skipped)."
            } else if (scanned > 0) {
                "Scanned $scanned SMS messages, no new transactions found."
            } else {
                "No SMS found in device inbox."
            }
        )
    }

    // Process a single SMS string (used by BroadcastReceiver or Simulator)
    suspend fun processSingleSms(
        body: String,
        sender: String,
        timestamp: Long = System.currentTimeMillis()
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val hash = securityManager.sha256(body + sender + timestamp)
        val parsed = SmsParser.parse(body, sender)

        if (!parsed.isBankTransaction) {
            database.dao().insertSmsLog(
                SmsLogEntity(
                    messageHash = hash,
                    sender = sender,
                    timestamp = timestamp,
                    extractedAmount = 0.0,
                    isBankTransaction = false,
                    status = "IGNORED"
                )
            )
            return@withContext Pair(false, parsed.ignoreReason.ifBlank { "Not a bank transaction" })
        }

        val refId = parsed.referenceId.trim().ifBlank { null }
        val encryptedBody = securityManager.encrypt(body)
        val txn = TransactionEntity(
            amount = parsed.amount,
            type = parsed.type,
            category = parsed.category,
            source = "SMS_AUTO",
            merchant = parsed.merchant,
            accountOrBank = parsed.accountOrBank,
            referenceId = refId,
            timestamp = timestamp,
            rawSmsBodyEncrypted = encryptedBody,
            smsSender = sender,
            isReconciled = true,
            notes = if (!refId.isNullOrBlank()) "Ref: $refId" else "Auto-synced from SMS"
        )

        // Centralized duplicate check & insertion
        return@withContext when (val result = repository.insertTransactionSafely(txn)) {
            is TransactionInsertResult.Success -> {
                database.dao().insertSmsLog(
                    SmsLogEntity(
                        messageHash = hash,
                        sender = sender,
                        timestamp = timestamp,
                        extractedAmount = parsed.amount,
                        isBankTransaction = true,
                        status = "AUTO_RECORDED"
                    )
                )
                Pair(true, "Captured ${parsed.type.lowercase()}: ₹${"%,.2f".format(parsed.amount)} at ${parsed.merchant}")
            }
            is TransactionInsertResult.Duplicate -> {
                database.dao().insertSmsLog(
                    SmsLogEntity(
                        messageHash = hash,
                        sender = sender,
                        timestamp = timestamp,
                        extractedAmount = parsed.amount,
                        isBankTransaction = true,
                        status = "DUPLICATE_SKIPPED"
                    )
                )
                Pair(false, result.reason)
            }
            is TransactionInsertResult.Error -> {
                Pair(false, result.message)
            }
        }
    }
}
