package com.enterprise.analytics.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateInMillis: Long,
    val isRecurring: Boolean,
    val recurrenceType: String, // e.g., "DAILY", "WEEKLY", "NONE"
    val videoUri: String? // Store the URI as a String
)
