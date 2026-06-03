package com.boox.atomic.habits.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.boox.atomic.habits.data.entity.IdentityGoal;
import java.lang.Class;
import java.lang.Exception;
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
public final class IdentityGoalDao_Impl implements IdentityGoalDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<IdentityGoal> __insertionAdapterOfIdentityGoal;

  private final EntityDeletionOrUpdateAdapter<IdentityGoal> __deletionAdapterOfIdentityGoal;

  private final EntityDeletionOrUpdateAdapter<IdentityGoal> __updateAdapterOfIdentityGoal;

  public IdentityGoalDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfIdentityGoal = new EntityInsertionAdapter<IdentityGoal>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `identity_goals` (`id`,`name`,`identityStatement`,`icon`,`sortOrder`,`strokeData`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IdentityGoal entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getIdentityStatement() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getIdentityStatement());
        }
        if (entity.getIcon() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getIcon());
        }
        statement.bindLong(5, entity.getSortOrder());
        if (entity.getStrokeData() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getStrokeData());
        }
        statement.bindLong(7, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfIdentityGoal = new EntityDeletionOrUpdateAdapter<IdentityGoal>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `identity_goals` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IdentityGoal entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfIdentityGoal = new EntityDeletionOrUpdateAdapter<IdentityGoal>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `identity_goals` SET `id` = ?,`name` = ?,`identityStatement` = ?,`icon` = ?,`sortOrder` = ?,`strokeData` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IdentityGoal entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getIdentityStatement() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getIdentityStatement());
        }
        if (entity.getIcon() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getIcon());
        }
        statement.bindLong(5, entity.getSortOrder());
        if (entity.getStrokeData() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getStrokeData());
        }
        statement.bindLong(7, entity.getCreatedAt());
        statement.bindLong(8, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final IdentityGoal goal, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfIdentityGoal.insertAndReturnId(goal);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final IdentityGoal goal, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfIdentityGoal.handle(goal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final IdentityGoal goal, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfIdentityGoal.handle(goal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<IdentityGoal>> getAll() {
    final String _sql = "SELECT * FROM identity_goals ORDER BY sortOrder";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"identity_goals"}, new Callable<List<IdentityGoal>>() {
      @Override
      @NonNull
      public List<IdentityGoal> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIdentityStatement = CursorUtil.getColumnIndexOrThrow(_cursor, "identityStatement");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfStrokeData = CursorUtil.getColumnIndexOrThrow(_cursor, "strokeData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<IdentityGoal> _result = new ArrayList<IdentityGoal>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IdentityGoal _item;
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
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpStrokeData;
            if (_cursor.isNull(_cursorIndexOfStrokeData)) {
              _tmpStrokeData = null;
            } else {
              _tmpStrokeData = _cursor.getString(_cursorIndexOfStrokeData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new IdentityGoal(_tmpId,_tmpName,_tmpIdentityStatement,_tmpIcon,_tmpSortOrder,_tmpStrokeData,_tmpCreatedAt);
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
  public Object getById(final long id, final Continuation<? super IdentityGoal> $completion) {
    final String _sql = "SELECT * FROM identity_goals WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<IdentityGoal>() {
      @Override
      @Nullable
      public IdentityGoal call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIdentityStatement = CursorUtil.getColumnIndexOrThrow(_cursor, "identityStatement");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfStrokeData = CursorUtil.getColumnIndexOrThrow(_cursor, "strokeData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final IdentityGoal _result;
          if (_cursor.moveToFirst()) {
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
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpStrokeData;
            if (_cursor.isNull(_cursorIndexOfStrokeData)) {
              _tmpStrokeData = null;
            } else {
              _tmpStrokeData = _cursor.getString(_cursorIndexOfStrokeData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new IdentityGoal(_tmpId,_tmpName,_tmpIdentityStatement,_tmpIcon,_tmpSortOrder,_tmpStrokeData,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
