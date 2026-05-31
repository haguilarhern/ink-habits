package com.inkhabits.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inkhabits.data.dao.HabitCompletionDao
import com.inkhabits.data.dao.HabitDao
import com.inkhabits.data.dao.IdentityGoalDao
import com.inkhabits.data.dao.RewardDao
import com.inkhabits.data.dao.ToDoDao
import com.inkhabits.data.entity.Habit
import com.inkhabits.data.entity.HabitCompletion
import com.inkhabits.data.entity.IdentityGoal
import com.inkhabits.data.entity.Reward
import com.inkhabits.data.entity.ToDo

@Database(
    entities = [IdentityGoal::class, Habit::class, HabitCompletion::class, ToDo::class, Reward::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun identityGoalDao(): IdentityGoalDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun toDoDao(): ToDoDao
    abstract fun rewardDao(): RewardDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v4→v5: granular gamification (per-reward target) + identity day goals. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rewards ADD COLUMN habitId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rewards ADD COLUMN identityId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE identity_goals ADD COLUMN goalDays INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ink_habits.db"
                ).addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
