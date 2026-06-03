package com.boox.atomic.habits.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.boox.atomic.habits.data.dao.HabitCompletionDao;
import com.boox.atomic.habits.data.dao.HabitCompletionDao_Impl;
import com.boox.atomic.habits.data.dao.HabitDao;
import com.boox.atomic.habits.data.dao.HabitDao_Impl;
import com.boox.atomic.habits.data.dao.IdentityGoalDao;
import com.boox.atomic.habits.data.dao.IdentityGoalDao_Impl;
import com.boox.atomic.habits.data.dao.ToDoDao;
import com.boox.atomic.habits.data.dao.ToDoDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile IdentityGoalDao _identityGoalDao;

  private volatile HabitDao _habitDao;

  private volatile ToDoDao _toDoDao;

  private volatile HabitCompletionDao _habitCompletionDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `identity_goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `identityStatement` TEXT NOT NULL, `icon` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `strokeData` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `identityGoalId` INTEGER NOT NULL, `name` TEXT NOT NULL, `frequencyType` TEXT NOT NULL, `intervalDays` INTEGER NOT NULL, `daysOfWeek` TEXT NOT NULL, `strokeData` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`identityGoalId`) REFERENCES `identity_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_identityGoalId` ON `habits` (`identityGoalId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `todos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `strokeData` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `habit_completions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `habitId` INTEGER NOT NULL, `date` TEXT NOT NULL, `completedAt` INTEGER NOT NULL, FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_completions_habitId_date` ON `habit_completions` (`habitId`, `date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '781af8264676b5de1c4f8d3b436d023f')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `identity_goals`");
        db.execSQL("DROP TABLE IF EXISTS `habits`");
        db.execSQL("DROP TABLE IF EXISTS `todos`");
        db.execSQL("DROP TABLE IF EXISTS `habit_completions`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsIdentityGoals = new HashMap<String, TableInfo.Column>(7);
        _columnsIdentityGoals.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIdentityGoals.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIdentityGoals.put("identityStatement", new TableInfo.Column("identityStatement", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIdentityGoals.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIdentityGoals.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIdentityGoals.put("strokeData", new TableInfo.Column("strokeData", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIdentityGoals.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysIdentityGoals = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesIdentityGoals = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoIdentityGoals = new TableInfo("identity_goals", _columnsIdentityGoals, _foreignKeysIdentityGoals, _indicesIdentityGoals);
        final TableInfo _existingIdentityGoals = TableInfo.read(db, "identity_goals");
        if (!_infoIdentityGoals.equals(_existingIdentityGoals)) {
          return new RoomOpenHelper.ValidationResult(false, "identity_goals(com.boox.atomic.habits.data.entity.IdentityGoal).\n"
                  + " Expected:\n" + _infoIdentityGoals + "\n"
                  + " Found:\n" + _existingIdentityGoals);
        }
        final HashMap<String, TableInfo.Column> _columnsHabits = new HashMap<String, TableInfo.Column>(10);
        _columnsHabits.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("identityGoalId", new TableInfo.Column("identityGoalId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("frequencyType", new TableInfo.Column("frequencyType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("intervalDays", new TableInfo.Column("intervalDays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("daysOfWeek", new TableInfo.Column("daysOfWeek", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("strokeData", new TableInfo.Column("strokeData", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHabits = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysHabits.add(new TableInfo.ForeignKey("identity_goals", "CASCADE", "NO ACTION", Arrays.asList("identityGoalId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesHabits = new HashSet<TableInfo.Index>(1);
        _indicesHabits.add(new TableInfo.Index("index_habits_identityGoalId", false, Arrays.asList("identityGoalId"), Arrays.asList("ASC")));
        final TableInfo _infoHabits = new TableInfo("habits", _columnsHabits, _foreignKeysHabits, _indicesHabits);
        final TableInfo _existingHabits = TableInfo.read(db, "habits");
        if (!_infoHabits.equals(_existingHabits)) {
          return new RoomOpenHelper.ValidationResult(false, "habits(com.boox.atomic.habits.data.entity.Habit).\n"
                  + " Expected:\n" + _infoHabits + "\n"
                  + " Found:\n" + _existingHabits);
        }
        final HashMap<String, TableInfo.Column> _columnsTodos = new HashMap<String, TableInfo.Column>(7);
        _columnsTodos.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodos.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodos.put("strokeData", new TableInfo.Column("strokeData", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodos.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodos.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodos.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodos.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTodos = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTodos = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTodos = new TableInfo("todos", _columnsTodos, _foreignKeysTodos, _indicesTodos);
        final TableInfo _existingTodos = TableInfo.read(db, "todos");
        if (!_infoTodos.equals(_existingTodos)) {
          return new RoomOpenHelper.ValidationResult(false, "todos(com.boox.atomic.habits.data.entity.ToDo).\n"
                  + " Expected:\n" + _infoTodos + "\n"
                  + " Found:\n" + _existingTodos);
        }
        final HashMap<String, TableInfo.Column> _columnsHabitCompletions = new HashMap<String, TableInfo.Column>(4);
        _columnsHabitCompletions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitCompletions.put("habitId", new TableInfo.Column("habitId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitCompletions.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitCompletions.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHabitCompletions = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysHabitCompletions.add(new TableInfo.ForeignKey("habits", "CASCADE", "NO ACTION", Arrays.asList("habitId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesHabitCompletions = new HashSet<TableInfo.Index>(1);
        _indicesHabitCompletions.add(new TableInfo.Index("index_habit_completions_habitId_date", true, Arrays.asList("habitId", "date"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoHabitCompletions = new TableInfo("habit_completions", _columnsHabitCompletions, _foreignKeysHabitCompletions, _indicesHabitCompletions);
        final TableInfo _existingHabitCompletions = TableInfo.read(db, "habit_completions");
        if (!_infoHabitCompletions.equals(_existingHabitCompletions)) {
          return new RoomOpenHelper.ValidationResult(false, "habit_completions(com.boox.atomic.habits.data.entity.HabitCompletion).\n"
                  + " Expected:\n" + _infoHabitCompletions + "\n"
                  + " Found:\n" + _existingHabitCompletions);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "781af8264676b5de1c4f8d3b436d023f", "3cbf5c09f92287bb0c943533edf15d38");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "identity_goals","habits","todos","habit_completions");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `identity_goals`");
      _db.execSQL("DELETE FROM `habits`");
      _db.execSQL("DELETE FROM `todos`");
      _db.execSQL("DELETE FROM `habit_completions`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(IdentityGoalDao.class, IdentityGoalDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HabitDao.class, HabitDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ToDoDao.class, ToDoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HabitCompletionDao.class, HabitCompletionDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public IdentityGoalDao identityGoalDao() {
    if (_identityGoalDao != null) {
      return _identityGoalDao;
    } else {
      synchronized(this) {
        if(_identityGoalDao == null) {
          _identityGoalDao = new IdentityGoalDao_Impl(this);
        }
        return _identityGoalDao;
      }
    }
  }

  @Override
  public HabitDao habitDao() {
    if (_habitDao != null) {
      return _habitDao;
    } else {
      synchronized(this) {
        if(_habitDao == null) {
          _habitDao = new HabitDao_Impl(this);
        }
        return _habitDao;
      }
    }
  }

  @Override
  public ToDoDao toDoDao() {
    if (_toDoDao != null) {
      return _toDoDao;
    } else {
      synchronized(this) {
        if(_toDoDao == null) {
          _toDoDao = new ToDoDao_Impl(this);
        }
        return _toDoDao;
      }
    }
  }

  @Override
  public HabitCompletionDao habitCompletionDao() {
    if (_habitCompletionDao != null) {
      return _habitCompletionDao;
    } else {
      synchronized(this) {
        if(_habitCompletionDao == null) {
          _habitCompletionDao = new HabitCompletionDao_Impl(this);
        }
        return _habitCompletionDao;
      }
    }
  }
}
