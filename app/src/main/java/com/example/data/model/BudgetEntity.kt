package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // e.g. "Overall", "Food & Dining", "Shopping", "Entertainment"
    val monthlyLimit: Double,
    val monthYear: String = "ALL", // "ALL" or "YYYY-MM"
    val alertThresholdPercent: Int = 80 // alert when reaching 80% or 100%
)
