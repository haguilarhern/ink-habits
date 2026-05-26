package com.boox.atomic.habits.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.boox.atomic.habits.data.dao.HabitCompletionDao
import com.boox.atomic.habits.data.dao.HabitDao
import com.boox.atomic.habits.data.dao.IdentityGoalDao
import com.boox.atomic.habits.data.dao.ToDoDao
import com.boox.atomic.habits.data.entity.Habit
import com.boox.atomic.habits.data.entity.HabitCompletion
import com.boox.atomic.habits.data.entity.IdentityGoal
import com.boox.atomic.habits.data.entity.ToDo

@Database(
    entities = [
        IdentityGoal::class,
        Habit::class,
        ToDo::class,
        HabitCompletion::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun identityGoalDao(): IdentityGoalDao
    abstract fun habitDao(): HabitDao
    abstract fun toDoDao(): ToDoDao
    abstract fun habitCompletionDao(): HabitCompletionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "atomic_habits.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}