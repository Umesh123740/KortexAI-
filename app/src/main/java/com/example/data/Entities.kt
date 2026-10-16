package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "doubts")
data class Doubt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val question: String,
    val solution: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSolved: Boolean = true,
    val status: String = "Solved", // Solved, Pending, In Progress
    val imagePath: String? = null
)

@Entity(tableName = "streaks")
data class Streak(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val isCompleted: Boolean = true
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val date: String // yyyy-MM-dd
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 0,
    val userId: String = "",
    val name: String,
    val language: String,
    val board: String,
    val className: String,
    val isPro: Boolean = false,
    val mobileNumber: String,
    val profilePicture: String? = null,
    val isOnboarded: Boolean = false
)

@Serializable
data class StudyGroup(
    val id: Int,
    val name: String,
    val members: Int,
    val isActive: Boolean = true
)

@Serializable
data class Notification(
    val id: Int,
    val title: String,
    val body: String,
    val route: String
)
