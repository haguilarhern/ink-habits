package com.boox.atomic.habits.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.boox.atomic.habits.data.entity.Habit;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HabitDao_Impl implements HabitDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Habit> __insertionAdapterOfHabit;

  private final EntityDeletionOrUpdateAdapter<Habit> __deletionAdapterOfHabit;

  private final EntityDeletionOrUpdateAdapter<Habit> __updateAdapterOfHabit;

  public HabitDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHabit = new EntityInsertionAdapter<Habit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `habits` (`id`,`identityGoalId`,`name`,`frequencyType`,`intervalDays`,`daysOfWeek`,`strokeData`,`isActive`,`sortOrder`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Habit entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getIdentityGoalId());
        if (entity.getName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getName());
        }
        if (entity.getFrequencyType() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getFrequencyType());
        }
        statement.bindLong(5, entity.getIntervalDays());
        if (entity.getDaysOfWeek() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDaysOfWeek());
        }
        if (entity.getStrokeData() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getStrokeData());
        }
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindLong(9, entity.getSortOrder());
        statement.bindLong(10, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfHabit = new EntityDeletionOrUpdateAdapter<Habit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `habits` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Habit entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfHabit = new EntityDeletionOrUpdateAdapter<Habit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `habits` SET `id` = ?,`identityGoalId` = ?,`name` = ?,`frequencyType` = ?,`intervalDays` = ?,`daysOfWeek` = ?,`strokeData` = ?,`isActive` = ?,`sortOrder` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Habit entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getIdentityGoalId());
        if (entity.getName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getName());
        }
        if (entity.getFrequencyType() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getFrequencyType());
        }
        statement.bindLong(5, entity.getIntervalDays());
        if (entity.getDaysOfWeek() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDaysOfWeek());
        }
        if (entity.getStrokeData() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getStrokeData());
        }
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindLong(9, entity.getSortOrder());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final Habit habit, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfHabit.insertAndReturnId(habit);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Habit habit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfHabit.handle(habit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Habit habit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfHabit.handle(habit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Habit>> getHabitsForGoal(final long goalId) {
    final String _sql = "SELECT * FROM habits WHERE identityGoalId = ? AND isActive = 1 ORDER BY sortOrder";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, goalId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habits"}, new Callable<List<Habit>>() {
      @Override
      @NonNull
      public List<Habit> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIdentityGoalId = CursorUtil.getColumnIndexOrThrow(_cursor, "identityGoalId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFrequencyType = CursorUtil.getColumnIndexOrThrow(_cursor, "frequencyType");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "intervalDays");
          final int _cursorIndexOfDaysOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "daysOfWeek");
          final int _cursorIndexOfStrokeData = CursorUtil.getColumnIndexOrThrow(_cursor, "strokeData");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Habit> _result = new ArrayList<Habit>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Habit _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpIdentityGoalId;
            _tmpIdentityGoalId = _cursor.getLong(_cursorIndexOfIdentityGoalId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpFrequencyType;
            if (_cursor.isNull(_cursorIndexOfFrequencyType)) {
              _tmpFrequencyType = null;
            } else {
              _tmpFrequencyType = _cursor.getString(_cursorIndexOfFrequencyType);
            }
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final String _tmpDaysOfWeek;
            if (_cursor.isNull(_cursorIndexOfDaysOfWeek)) {
              _tmpDaysOfWeek = null;
            } else {
              _tmpDaysOfWeek = _cursor.getString(_cursorIndexOfDaysOfWeek);
            }
            final String _tmpStrokeData;
            if (_cursor.isNull(_cursorIndexOfStrokeData)) {
              _tmpStrokeData = null;
            } else {
              _tmpStrokeData = _cursor.getString(_cursorIndexOfStrokeData);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Habit(_tmpId,_tmpIdentityGoalId,_tmpName,_tmpFrequencyType,_tmpIntervalDays,_tmpDaysOfWeek,_tmpStrokeData,_tmpIsActive,_tmpSortOrder,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<DashboardItem>> getFullDashboard(final String today) {
    final String _sql = "\n"
            + "        SELECT g.id, g.name, g.identityStatement, g.icon,\n"
            + "               g.sortOrder as goalSortOrder,\n"
            + "               h.id as habitId, h.name as habitName,\n"
            + "               h.strokeData as habitStrokeData,\n"
            + "               h.frequencyType, h.intervalDays, h.daysOfWeek,\n"
            + "               h.sortOrder as habitSortOrder,\n"
            + "               CASE WHEN hc.id IS NOT NULL THEN 1 ELSE 0 END as isCompletedToday\n"
            + "        FROM identity_goals g\n"
            + "        LEFT JOIN habits h ON h.identityGoalId = g.id AND h.isActive = 1\n"
            + "        LEFT JOIN habit_completions hc ON hc.habitId = h.id AND hc.date = ?\n"
            + "        ORDER BY g.sortOrder, h.sortOrder\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (today == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, today);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"identity_goals", "habits",
        "habit_completions"}, new Callable<List<DashboardItem>>() {
      @Override
      @NonNull
      public List<DashboardItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfName = 1;
          final int _cursorIndexOfIdentityStatement = 2;
          final int _cursorIndexOfIcon = 3;
          final int _cursorIndexOfGoalSortOrder = 4;
          final int _cursorIndexOfHabitId = 5;
          final int _cursorIndexOfHabitName = 6;
          final int _cursorIndexOfHabitStrokeData = 7;
          final int _cursorIndexOfFrequencyType = 8;
          final int _cursorIndexOfIntervalDays = 9;
          final int _cursorIndexOfDaysOfWeek = 10;
          final int _cursorIndexOfHabitSortOrder = 11;
          final int _cursorIndexOfIsCompletedToday = 12;
          final List<DashboardItem> _result = new ArrayList<DashboardItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DashboardItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpIdentityStatement;
            if (_cursor.isNull(_cursorIndexOfIdentityStatement)) {
              _tmpIdentityStatement = null;
            } else {
              _tmpIdentityStatement = _cursor.getString(_cursorIndexOfIdentityStatement);
            }
            final String _tmpIcon;
            if (_cursor.isNull(_cursorIndexOfIcon)) {
              _tmpIcon = null;
            } else {
              _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            }
            final int _tmpGoalSortOrder;
            _tmpGoalSortOrder = _cursor.getInt(_cursorIndexOfGoalSortOrder);
            final Long _tmpHabitId;
            if (_cursor.isNull(_cursorIndexOfHabitId)) {
              _tmpHabitId = null;
            } else {
              _tmpHabitId = _cursor.getLong(_cursorIndexOfHabitId);
            }
            final String _tmpHabitName;
            if (_cursor.isNull(_cursorIndexOfHabitName)) {
              _tmpHabitName = null;
            } else {
              _tmpHabitName = _cursor.getString(_cursorIndexOfHabitName);
            }
            final String _tmpHabitStrokeData;
            if (_cursor.isNull(_cursorIndexOfHabitStrokeData)) {
              _tmpHabitStrokeData = null;
            } else {
              _tmpHabitStrokeData = _cursor.getString(_cursorIndexOfHabitStrokeData);
            }
            final String _tmpFrequencyType;
            if (_cursor.isNull(_cursorIndexOfFrequencyType)) {
              _tmpFrequencyType = null;
            } else {
              _tmpFrequencyType = _cursor.getString(_cursorIndexOfFrequencyType);
            }
            final Integer _tmpIntervalDays;
            if (_cursor.isNull(_cursorIndexOfIntervalDays)) {
              _tmpIntervalDays = null;
            } else {
              _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            }
            final String _tmpDaysOfWeek;
            if (_cursor.isNull(_cursorIndexOfDaysOfWeek)) {
              _tmpDaysOfWeek = null;
            } else {
              _tmpDaysOfWeek = _cursor.getString(_cursorIndexOfDaysOfWeek);
            }
            final Integer _tmpHabitSortOrder;
            if (_cursor.isNull(_cursorIndexOfHabitSortOrder)) {
              _tmpHabitSortOrder = null;
            } else {
              _tmpHabitSortOrder = _cursor.getInt(_cursorIndexOfHabitSortOrder);
            }
            final int _tmpIsCompletedToday;
            _tmpIsCompletedToday = _cursor.getInt(_cursorIndexOfIsCompletedToday);
            _item = new DashboardItem(_tmpId,_tmpName,_tmpIdentityStatement,_tmpIcon,_tmpGoalSortOrder,_tmpHabitId,_tmpHabitName,_tmpHabitStrokeData,_tmpFrequencyType,_tmpIntervalDays,_tmpDaysOfWeek,_tmpHabitSortOrder,_tmpIsCompletedToday);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
