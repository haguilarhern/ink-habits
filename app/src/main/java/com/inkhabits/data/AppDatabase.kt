package com.inkhabits.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inkhabits.data.dao.EconomyDao
import com.inkhabits.data.dao.HabitCompletionDao
import com.inkhabits.data.dao.HabitDao
import com.inkhabits.data.dao.IdentityGoalDao
import com.inkhabits.data.dao.RewardDao
import com.inkhabits.data.dao.StreakFreezeDao
import com.inkhabits.data.dao.TaskListDao
import com.inkhabits.data.dao.TaskStageDao
import com.inkhabits.data.dao.ToDoDao
import com.inkhabits.data.entity.EconomyState
import com.inkhabits.data.entity.Habit
import com.inkhabits.data.entity.HabitCompletion
import com.inkhabits.data.entity.IdentityGoal
import com.inkhabits.data.entity.Reward
import com.inkhabits.data.entity.StreakFreeze
import com.inkhabits.data.entity.TaskList
import com.inkhabits.data.entity.TaskStage
import com.inkhabits.data.entity.ToDo

@Database(
    entities = [
        IdentityGoal::class, Habit::class, HabitCompletion::class, ToDo::class, Reward::class,
        TaskList::class, StreakFreeze::class, EconomyState::class, TaskStage::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun identityGoalDao(): IdentityGoalDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun toDoDao(): ToDoDao
    abstract fun rewardDao(): RewardDao
    abstract fun taskListDao(): TaskListDao
    abstract fun streakFreezeDao(): StreakFreezeDao
    abstract fun economyDao(): EconomyDao
    abstract fun taskStageDao(): TaskStageDao

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

        /** v5→v6: per-habit goal streak (0 = inherit identity goal). */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN goalDays INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v6→v7: opt-in per-habit reminder notifications. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v7→v8: aura economy + protective totems, plus task lists / due dates /
         * Eisenhower priority / recurrence for to-dos.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Task classification lists.
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS task_lists (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL DEFAULT '',
                        colorHex TEXT NOT NULL DEFAULT '#8C1D1D',
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )"""
                )

                // Consumed streak freezes (one row = one protected day).
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS streak_freezes (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        habitId INTEGER NOT NULL DEFAULT 0,
                        identityId INTEGER NOT NULL DEFAULT 0,
                        date TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_streak_freezes_date ON streak_freezes(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_streak_freezes_habitId ON streak_freezes(habitId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_streak_freezes_identityId ON streak_freezes(identityId)")

                // Single-row wallet (id = 1).
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS economy (
                        id INTEGER NOT NULL PRIMARY KEY,
                        auraSpent INTEGER NOT NULL DEFAULT 0,
                        habitTotems INTEGER NOT NULL DEFAULT 0,
                        identityTotems INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL("INSERT OR IGNORE INTO economy (id, auraSpent, habitTotems, identityTotems) VALUES (1, 0, 0, 0)")

                // To-do classification / scheduling columns.
                db.execSQL("ALTER TABLE todos ADD COLUMN listId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE todos ADD COLUMN dueEpochDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE todos ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE todos ADD COLUMN recurType TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE todos ADD COLUMN recurInterval INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE todos ADD COLUMN recurDaysOfWeek TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v8→v9: Kanban stages + per-task stage assignment. */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS task_stages (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL DEFAULT '',
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL("ALTER TABLE todos ADD COLUMN stageId INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v9→v10: fixed/custom roles for Kanban stages. */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE task_stages ADD COLUMN role TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ink_habits.db"
                ).addMigrations(
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
