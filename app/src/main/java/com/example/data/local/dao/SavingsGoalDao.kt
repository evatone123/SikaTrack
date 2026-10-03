package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.relation.SavingsGoalWithAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Transaction
    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllGoalsWithAccount(): Flow<List<SavingsGoalWithAccount>>

    @Query("SELECT * FROM savings_goals WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    fun getGoalById(id: Long): Flow<SavingsGoalEntity?>

    @Transaction
    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    fun getGoalWithAccountById(id: Long): Flow<SavingsGoalWithAccount?>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalByIdOnce(id: Long): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllGoalsSync(): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<SavingsGoalEntity>): List<Long>

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals")
    suspend fun clearAll()
}
