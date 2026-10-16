package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DoubtDao {
    @Query("SELECT * FROM doubts ORDER BY timestamp DESC")
    fun getAllDoubts(): Flow<List<Doubt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoubt(doubt: Doubt): Long

    @Query("DELETE FROM doubts WHERE id = :id")
    suspend fun deleteDoubt(id: Long)
}

@Dao
interface StreakDao {
    @Query("SELECT * FROM streaks")
    fun getAllStreaks(): Flow<List<Streak>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: Streak)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE date = :date")
    fun getGoalsForDate(date: String): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Int)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfile)

    @Update
    suspend fun updateUserProfile(userProfile: UserProfile)
}
