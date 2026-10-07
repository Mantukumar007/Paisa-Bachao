package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.SmsLogEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

sealed class TransactionInsertResult {
    data class Success(val id: Long, val transaction: TransactionEntity) : TransactionInsertResult()
    data class Duplicate(val existingTransaction: TransactionEntity, val reason: String) : TransactionInsertResult()
    data class Error(val message: String) : TransactionInsertResult()
}

class PaisaBachaoRepository(private val database: AppDatabase) {

    val allTransactions: Flow<List<TransactionEntity>> = database.dao().getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = database.dao().getRecentTransactions(25)

    val allBudgets: Flow<List<BudgetEntity>> = database.dao().getAllBudgets()
    val allGoals: Flow<List<SavingsGoalEntity>> = database.dao().getAllGoals()
    val allSmsLogs: Flow<List<SmsLogEntity>> = database.dao().getAllSmsLogs()

    /**
     * Centralized Duplicate Detection.
     * Checks before insertion:
     * 1. Priority 1, 2, 3: UTR / Bank Txn ID / Reference Number (case-insensitive)
     * 2. Fallback check: Matching Amount + Merchant + Type + Timestamp (within 60s tolerance)
     */
    suspend fun findDuplicate(transaction: TransactionEntity): TransactionEntity? {
        val ref = transaction.referenceId?.trim()?.ifBlank { null }?.uppercase()
        if (ref != null) {
            val byRef = database.dao().getTransactionByReferenceId(ref)
            if (byRef != null) return byRef
        } else {
            // Fallback: Safe check on amount, merchant, type, and timestamp
            val byDetails = database.dao().findDuplicateWithoutRef(
                amount = transaction.amount,
                merchant = transaction.merchant,
                type = transaction.type,
                timestamp = transaction.timestamp,
                timeToleranceMillis = 60_000L
            )
            if (byDetails != null) return byDetails
        }
        return null
    }

    /**
     * Centralized Safe Insert with Duplicate Prevention.
     * Guaranteed to insert only once even if scanned repeatedly.
     */
    suspend fun insertTransactionSafely(transaction: TransactionEntity): TransactionInsertResult {
        val cleanRef = transaction.referenceId?.trim()?.ifBlank { null }?.uppercase()
        val cleanTxn = transaction.copy(referenceId = cleanRef)

        // 1. Check duplicate BEFORE inserting
        val existing = findDuplicate(cleanTxn)
        if (existing != null) {
            val reason = if (!cleanRef.isNullOrBlank()) {
                "Duplicate skipped: Transaction with Reference/UTR '$cleanRef' already exists."
            } else {
                "Duplicate skipped: Matching transaction for ${cleanTxn.merchant} (₹${cleanTxn.amount}) already exists."
            }
            return TransactionInsertResult.Duplicate(existing, reason)
        }

        // 2. Perform Insert (backed by database-level UNIQUE index on referenceId)
        return try {
            val rowId = database.dao().insertTransaction(cleanTxn)
            if (rowId == -1L) {
                // Database-level UNIQUE constraint hit
                val conflict = if (!cleanRef.isNullOrBlank()) {
                    database.dao().getTransactionByReferenceId(cleanRef)
                } else null
                TransactionInsertResult.Duplicate(
                    conflict ?: cleanTxn,
                    "Duplicate skipped by database unique constraint for reference '$cleanRef'."
                )
            } else {
                TransactionInsertResult.Success(rowId, cleanTxn.copy(id = rowId))
            }
        } catch (e: Exception) {
            TransactionInsertResult.Error("Failed to insert transaction: ${e.localizedMessage}")
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return when (val result = insertTransactionSafely(transaction)) {
            is TransactionInsertResult.Success -> result.id
            is TransactionInsertResult.Duplicate -> result.existingTransaction.id
            is TransactionInsertResult.Error -> -1L
        }
    }

    suspend fun insert(transaction: TransactionEntity): Long = insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) {
        database.dao().updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.dao().deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        database.dao().deleteTransactionById(id)
    }

    suspend fun getTransactionsCount(): Int = database.dao().getTransactionsCount()

    suspend fun insertBudget(budget: BudgetEntity): Long {
        return database.dao().insertBudget(budget)
    }

    suspend fun updateBudget(budget: BudgetEntity) {
        database.dao().updateBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        database.dao().deleteBudget(budget)
    }

    suspend fun insertGoal(goal: SavingsGoalEntity): Long {
        return database.dao().insertGoal(goal)
    }

    suspend fun updateGoal(goal: SavingsGoalEntity) {
        database.dao().updateGoal(goal)
    }

    suspend fun deleteGoal(goal: SavingsGoalEntity) {
        database.dao().deleteGoal(goal)
    }

    suspend fun contributeToGoal(goal: SavingsGoalEntity, contributionAmount: Double) {
        val newAmount = goal.currentAmount + contributionAmount
        val achieved = newAmount >= goal.targetAmount
        val updated = goal.copy(
            currentAmount = newAmount,
            isAchieved = achieved
        )
        database.dao().updateGoal(updated)
    }

    suspend fun clearAllData() {
        database.dao().deleteAllTransactions()
    }
}
