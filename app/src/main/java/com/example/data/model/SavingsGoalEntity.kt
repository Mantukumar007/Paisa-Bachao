package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String, // e.g. "Emergency Fund", "New Electric Scooter", "Annual Insurance"
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000), // default 3 months
    val category: String = "General Savings",
    val iconName: String = "piggy_bank",
    val isAchieved: Boolean = false
)
