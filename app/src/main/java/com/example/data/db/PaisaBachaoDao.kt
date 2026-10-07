package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BudgetEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.SmsLogEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaisaBachaoDao {

    // --- Transactions (Room DAO) ---

    // Uses IGNORE to honor database-level UNIQUE index constraint on referenceId
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 20): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    // --- Duplicate Detection Queries ---

    @Query("SELECT * FROM transactions WHERE referenceId IS NOT NULL AND referenceId = :referenceId COLLATE NOCASE LIMIT 1")
    suspend fun getTransactionByReferenceId(referenceId: String): TransactionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE referenceId IS NOT NULL AND referenceId = :referenceId COLLATE NOCASE)")
    suspend fun existsByReferenceId(referenceId: String): Boolean

    @Query("""
        SELECT * FROM transactions 
        WHERE (referenceId IS NULL OR referenceId = '') 
          AND ABS(amount - :amount) < 0.001 
          AND merchant = :merchant COLLATE NOCASE 
          AND type = :type 
          AND ABS(timestamp - :timestamp) <= :timeToleranceMillis 
        LIMIT 1
    """)
    suspend fun findDuplicateWithoutRef(
        amount: Double,
        merchant: String,
        type: String,
        timestamp: Long,
        timeToleranceMillis: Long = 60000L
    ): TransactionEntity?

    // --- Local storage queries ---

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionsCount(): Int

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    // --- Budgets ---
    @Query("SELECT * FROM budgets ORDER BY id ASC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    // --- Savings Goals ---
    @Query("SELECT * FROM savings_goals ORDER BY isAchieved ASC, targetDate ASC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)

    // --- SMS Audit Logs ---
    @Query("SELECT * FROM sms_logs WHERE messageHash = :hash LIMIT 1")
    suspend fun getSmsLogByHash(hash: String): SmsLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLog(log: SmsLogEntity): Long

    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllSmsLogs(): Flow<List<SmsLogEntity>>
}
