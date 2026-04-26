package com.sentinel.companion.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.sentinel.companion.data.model.DeviceProfile;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class DeviceDao_Impl implements DeviceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DeviceProfile> __insertionAdapterOfDeviceProfile;

  private final EntityInsertionAdapter<DeviceProfile> __insertionAdapterOfDeviceProfile_1;

  private final EntityDeletionOrUpdateAdapter<DeviceProfile> __deletionAdapterOfDeviceProfile;

  private final EntityDeletionOrUpdateAdapter<DeviceProfile> __updateAdapterOfDeviceProfile;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateState;

  private final SharedSQLiteStatement __preparedStmtOfSetFavorite;

  private final SharedSQLiteStatement __preparedStmtOfSetEnabled;

  public DeviceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDeviceProfile = new EntityInsertionAdapter<DeviceProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `devices` (`id`,`name`,`location`,`protocol`,`host`,`port`,`path`,`username`,`password`,`authType`,`state`,`latencyMs`,`isFavorite`,`isEnabled`,`snapshotUrl`,`lastSeenMs`,`addedMs`,`discoveredVia`,`serviceType`,`notes`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DeviceProfile entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getLocation());
        statement.bindString(4, entity.getProtocol());
        statement.bindString(5, entity.getHost());
        statement.bindLong(6, entity.getPort());
        statement.bindString(7, entity.getPath());
        statement.bindString(8, entity.getUsername());
        statement.bindString(9, entity.getPassword());
        statement.bindString(10, entity.getAuthType());
        statement.bindString(11, entity.getState());
        statement.bindLong(12, entity.getLatencyMs());
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(13, _tmp);
        final int _tmp_1 = entity.isEnabled() ? 1 : 0;
        statement.bindLong(14, _tmp_1);
        statement.bindString(15, entity.getSnapshotUrl());
        statement.bindLong(16, entity.getLastSeenMs());
        statement.bindLong(17, entity.getAddedMs());
        statement.bindString(18, entity.getDiscoveredVia());
        statement.bindString(19, entity.getServiceType());
        statement.bindString(20, entity.getNotes());
      }
    };
    this.__insertionAdapterOfDeviceProfile_1 = new EntityInsertionAdapter<DeviceProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `devices` (`id`,`name`,`location`,`protocol`,`host`,`port`,`path`,`username`,`password`,`authType`,`state`,`latencyMs`,`isFavorite`,`isEnabled`,`snapshotUrl`,`lastSeenMs`,`addedMs`,`discoveredVia`,`serviceType`,`notes`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DeviceProfile entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getLocation());
        statement.bindString(4, entity.getProtocol());
        statement.bindString(5, entity.getHost());
        statement.bindLong(6, entity.getPort());
        statement.bindString(7, entity.getPath());
        statement.bindString(8, entity.getUsername());
        statement.bindString(9, entity.getPassword());
        statement.bindString(10, entity.getAuthType());
        statement.bindString(11, entity.getState());
        statement.bindLong(12, entity.getLatencyMs());
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(13, _tmp);
        final int _tmp_1 = entity.isEnabled() ? 1 : 0;
        statement.bindLong(14, _tmp_1);
        statement.bindString(15, entity.getSnapshotUrl());
        statement.bindLong(16, entity.getLastSeenMs());
        statement.bindLong(17, entity.getAddedMs());
        statement.bindString(18, entity.getDiscoveredVia());
        statement.bindString(19, entity.getServiceType());
        statement.bindString(20, entity.getNotes());
      }
    };
    this.__deletionAdapterOfDeviceProfile = new EntityDeletionOrUpdateAdapter<DeviceProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `devices` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DeviceProfile entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfDeviceProfile = new EntityDeletionOrUpdateAdapter<DeviceProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `devices` SET `id` = ?,`name` = ?,`location` = ?,`protocol` = ?,`host` = ?,`port` = ?,`path` = ?,`username` = ?,`password` = ?,`authType` = ?,`state` = ?,`latencyMs` = ?,`isFavorite` = ?,`isEnabled` = ?,`snapshotUrl` = ?,`lastSeenMs` = ?,`addedMs` = ?,`discoveredVia` = ?,`serviceType` = ?,`notes` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DeviceProfile entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getLocation());
        statement.bindString(4, entity.getProtocol());
        statement.bindString(5, entity.getHost());
        statement.bindLong(6, entity.getPort());
        statement.bindString(7, entity.getPath());
        statement.bindString(8, entity.getUsername());
        statement.bindString(9, entity.getPassword());
        statement.bindString(10, entity.getAuthType());
        statement.bindString(11, entity.getState());
        statement.bindLong(12, entity.getLatencyMs());
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(13, _tmp);
        final int _tmp_1 = entity.isEnabled() ? 1 : 0;
        statement.bindLong(14, _tmp_1);
        statement.bindString(15, entity.getSnapshotUrl());
        statement.bindLong(16, entity.getLastSeenMs());
        statement.bindLong(17, entity.getAddedMs());
        statement.bindString(18, entity.getDiscoveredVia());
        statement.bindString(19, entity.getServiceType());
        statement.bindString(20, entity.getNotes());
        statement.bindString(21, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM devices WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateState = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE devices SET state = ?, latencyMs = ?, lastSeenMs = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetFavorite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE devices SET isFavorite = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetEnabled = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE devices SET isEnabled = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final DeviceProfile device, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDeviceProfile.insert(device);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<DeviceProfile> devices,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDeviceProfile_1.insert(devices);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final DeviceProfile device, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDeviceProfile.handle(device);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final DeviceProfile device, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDeviceProfile.handle(device);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateState(final String id, final String state, final int latencyMs,
      final long lastSeenMs, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateState.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, state);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, latencyMs);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, lastSeenMs);
        _argIndex = 4;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateState.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setFavorite(final String id, final boolean favorite,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetFavorite.acquire();
        int _argIndex = 1;
        final int _tmp = favorite ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetFavorite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setEnabled(final String id, final boolean enabled,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetEnabled.acquire();
        int _argIndex = 1;
        final int _tmp = enabled ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetEnabled.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DeviceProfile>> observeAll() {
    final String _sql = "SELECT * FROM devices ORDER BY isFavorite DESC, addedMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"devices"}, new Callable<List<DeviceProfile>>() {
      @Override
      @NonNull
      public List<DeviceProfile> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfHost = CursorUtil.getColumnIndexOrThrow(_cursor, "host");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final int _cursorIndexOfAuthType = CursorUtil.getColumnIndexOrThrow(_cursor, "authType");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfLatencyMs = CursorUtil.getColumnIndexOrThrow(_cursor, "latencyMs");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfSnapshotUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "snapshotUrl");
          final int _cursorIndexOfLastSeenMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenMs");
          final int _cursorIndexOfAddedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "addedMs");
          final int _cursorIndexOfDiscoveredVia = CursorUtil.getColumnIndexOrThrow(_cursor, "discoveredVia");
          final int _cursorIndexOfServiceType = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceType");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<DeviceProfile> _result = new ArrayList<DeviceProfile>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DeviceProfile _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpHost;
            _tmpHost = _cursor.getString(_cursorIndexOfHost);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpPassword;
            _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            final String _tmpAuthType;
            _tmpAuthType = _cursor.getString(_cursorIndexOfAuthType);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final int _tmpLatencyMs;
            _tmpLatencyMs = _cursor.getInt(_cursorIndexOfLatencyMs);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_1 != 0;
            final String _tmpSnapshotUrl;
            _tmpSnapshotUrl = _cursor.getString(_cursorIndexOfSnapshotUrl);
            final long _tmpLastSeenMs;
            _tmpLastSeenMs = _cursor.getLong(_cursorIndexOfLastSeenMs);
            final long _tmpAddedMs;
            _tmpAddedMs = _cursor.getLong(_cursorIndexOfAddedMs);
            final String _tmpDiscoveredVia;
            _tmpDiscoveredVia = _cursor.getString(_cursorIndexOfDiscoveredVia);
            final String _tmpServiceType;
            _tmpServiceType = _cursor.getString(_cursorIndexOfServiceType);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _item = new DeviceProfile(_tmpId,_tmpName,_tmpLocation,_tmpProtocol,_tmpHost,_tmpPort,_tmpPath,_tmpUsername,_tmpPassword,_tmpAuthType,_tmpState,_tmpLatencyMs,_tmpIsFavorite,_tmpIsEnabled,_tmpSnapshotUrl,_tmpLastSeenMs,_tmpAddedMs,_tmpDiscoveredVia,_tmpServiceType,_tmpNotes);
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
  public Flow<DeviceProfile> observeById(final String id) {
    final String _sql = "SELECT * FROM devices WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"devices"}, new Callable<DeviceProfile>() {
      @Override
      @Nullable
      public DeviceProfile call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfHost = CursorUtil.getColumnIndexOrThrow(_cursor, "host");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final int _cursorIndexOfAuthType = CursorUtil.getColumnIndexOrThrow(_cursor, "authType");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfLatencyMs = CursorUtil.getColumnIndexOrThrow(_cursor, "latencyMs");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfSnapshotUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "snapshotUrl");
          final int _cursorIndexOfLastSeenMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenMs");
          final int _cursorIndexOfAddedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "addedMs");
          final int _cursorIndexOfDiscoveredVia = CursorUtil.getColumnIndexOrThrow(_cursor, "discoveredVia");
          final int _cursorIndexOfServiceType = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceType");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final DeviceProfile _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpHost;
            _tmpHost = _cursor.getString(_cursorIndexOfHost);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpPassword;
            _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            final String _tmpAuthType;
            _tmpAuthType = _cursor.getString(_cursorIndexOfAuthType);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final int _tmpLatencyMs;
            _tmpLatencyMs = _cursor.getInt(_cursorIndexOfLatencyMs);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_1 != 0;
            final String _tmpSnapshotUrl;
            _tmpSnapshotUrl = _cursor.getString(_cursorIndexOfSnapshotUrl);
            final long _tmpLastSeenMs;
            _tmpLastSeenMs = _cursor.getLong(_cursorIndexOfLastSeenMs);
            final long _tmpAddedMs;
            _tmpAddedMs = _cursor.getLong(_cursorIndexOfAddedMs);
            final String _tmpDiscoveredVia;
            _tmpDiscoveredVia = _cursor.getString(_cursorIndexOfDiscoveredVia);
            final String _tmpServiceType;
            _tmpServiceType = _cursor.getString(_cursorIndexOfServiceType);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _result = new DeviceProfile(_tmpId,_tmpName,_tmpLocation,_tmpProtocol,_tmpHost,_tmpPort,_tmpPath,_tmpUsername,_tmpPassword,_tmpAuthType,_tmpState,_tmpLatencyMs,_tmpIsFavorite,_tmpIsEnabled,_tmpSnapshotUrl,_tmpLastSeenMs,_tmpAddedMs,_tmpDiscoveredVia,_tmpServiceType,_tmpNotes);
          } else {
            _result = null;
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
  public Flow<List<DeviceProfile>> observeByLocation(final String location) {
    final String _sql = "SELECT * FROM devices WHERE location = ? ORDER BY name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, location);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"devices"}, new Callable<List<DeviceProfile>>() {
      @Override
      @NonNull
      public List<DeviceProfile> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfHost = CursorUtil.getColumnIndexOrThrow(_cursor, "host");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final int _cursorIndexOfAuthType = CursorUtil.getColumnIndexOrThrow(_cursor, "authType");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfLatencyMs = CursorUtil.getColumnIndexOrThrow(_cursor, "latencyMs");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfSnapshotUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "snapshotUrl");
          final int _cursorIndexOfLastSeenMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenMs");
          final int _cursorIndexOfAddedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "addedMs");
          final int _cursorIndexOfDiscoveredVia = CursorUtil.getColumnIndexOrThrow(_cursor, "discoveredVia");
          final int _cursorIndexOfServiceType = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceType");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<DeviceProfile> _result = new ArrayList<DeviceProfile>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DeviceProfile _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpHost;
            _tmpHost = _cursor.getString(_cursorIndexOfHost);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpPassword;
            _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            final String _tmpAuthType;
            _tmpAuthType = _cursor.getString(_cursorIndexOfAuthType);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final int _tmpLatencyMs;
            _tmpLatencyMs = _cursor.getInt(_cursorIndexOfLatencyMs);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_1 != 0;
            final String _tmpSnapshotUrl;
            _tmpSnapshotUrl = _cursor.getString(_cursorIndexOfSnapshotUrl);
            final long _tmpLastSeenMs;
            _tmpLastSeenMs = _cursor.getLong(_cursorIndexOfLastSeenMs);
            final long _tmpAddedMs;
            _tmpAddedMs = _cursor.getLong(_cursorIndexOfAddedMs);
            final String _tmpDiscoveredVia;
            _tmpDiscoveredVia = _cursor.getString(_cursorIndexOfDiscoveredVia);
            final String _tmpServiceType;
            _tmpServiceType = _cursor.getString(_cursorIndexOfServiceType);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _item = new DeviceProfile(_tmpId,_tmpName,_tmpLocation,_tmpProtocol,_tmpHost,_tmpPort,_tmpPath,_tmpUsername,_tmpPassword,_tmpAuthType,_tmpState,_tmpLatencyMs,_tmpIsFavorite,_tmpIsEnabled,_tmpSnapshotUrl,_tmpLastSeenMs,_tmpAddedMs,_tmpDiscoveredVia,_tmpServiceType,_tmpNotes);
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
  public Flow<List<DeviceProfile>> observeFavorites() {
    final String _sql = "SELECT * FROM devices WHERE isFavorite = 1 ORDER BY name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"devices"}, new Callable<List<DeviceProfile>>() {
      @Override
      @NonNull
      public List<DeviceProfile> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfHost = CursorUtil.getColumnIndexOrThrow(_cursor, "host");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final int _cursorIndexOfAuthType = CursorUtil.getColumnIndexOrThrow(_cursor, "authType");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfLatencyMs = CursorUtil.getColumnIndexOrThrow(_cursor, "latencyMs");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfSnapshotUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "snapshotUrl");
          final int _cursorIndexOfLastSeenMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenMs");
          final int _cursorIndexOfAddedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "addedMs");
          final int _cursorIndexOfDiscoveredVia = CursorUtil.getColumnIndexOrThrow(_cursor, "discoveredVia");
          final int _cursorIndexOfServiceType = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceType");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<DeviceProfile> _result = new ArrayList<DeviceProfile>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DeviceProfile _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpHost;
            _tmpHost = _cursor.getString(_cursorIndexOfHost);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpPassword;
            _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            final String _tmpAuthType;
            _tmpAuthType = _cursor.getString(_cursorIndexOfAuthType);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final int _tmpLatencyMs;
            _tmpLatencyMs = _cursor.getInt(_cursorIndexOfLatencyMs);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_1 != 0;
            final String _tmpSnapshotUrl;
            _tmpSnapshotUrl = _cursor.getString(_cursorIndexOfSnapshotUrl);
            final long _tmpLastSeenMs;
            _tmpLastSeenMs = _cursor.getLong(_cursorIndexOfLastSeenMs);
            final long _tmpAddedMs;
            _tmpAddedMs = _cursor.getLong(_cursorIndexOfAddedMs);
            final String _tmpDiscoveredVia;
            _tmpDiscoveredVia = _cursor.getString(_cursorIndexOfDiscoveredVia);
            final String _tmpServiceType;
            _tmpServiceType = _cursor.getString(_cursorIndexOfServiceType);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _item = new DeviceProfile(_tmpId,_tmpName,_tmpLocation,_tmpProtocol,_tmpHost,_tmpPort,_tmpPath,_tmpUsername,_tmpPassword,_tmpAuthType,_tmpState,_tmpLatencyMs,_tmpIsFavorite,_tmpIsEnabled,_tmpSnapshotUrl,_tmpLastSeenMs,_tmpAddedMs,_tmpDiscoveredVia,_tmpServiceType,_tmpNotes);
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
  public Flow<List<DeviceProfile>> observeOnline() {
    final String _sql = "SELECT * FROM devices WHERE isEnabled = 1 AND state = 'ONLINE' ORDER BY name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"devices"}, new Callable<List<DeviceProfile>>() {
      @Override
      @NonNull
      public List<DeviceProfile> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfHost = CursorUtil.getColumnIndexOrThrow(_cursor, "host");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final int _cursorIndexOfAuthType = CursorUtil.getColumnIndexOrThrow(_cursor, "authType");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfLatencyMs = CursorUtil.getColumnIndexOrThrow(_cursor, "latencyMs");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfSnapshotUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "snapshotUrl");
          final int _cursorIndexOfLastSeenMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenMs");
          final int _cursorIndexOfAddedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "addedMs");
          final int _cursorIndexOfDiscoveredVia = CursorUtil.getColumnIndexOrThrow(_cursor, "discoveredVia");
          final int _cursorIndexOfServiceType = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceType");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<DeviceProfile> _result = new ArrayList<DeviceProfile>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DeviceProfile _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpHost;
            _tmpHost = _cursor.getString(_cursorIndexOfHost);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpPassword;
            _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            final String _tmpAuthType;
            _tmpAuthType = _cursor.getString(_cursorIndexOfAuthType);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final int _tmpLatencyMs;
            _tmpLatencyMs = _cursor.getInt(_cursorIndexOfLatencyMs);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_1 != 0;
            final String _tmpSnapshotUrl;
            _tmpSnapshotUrl = _cursor.getString(_cursorIndexOfSnapshotUrl);
            final long _tmpLastSeenMs;
            _tmpLastSeenMs = _cursor.getLong(_cursorIndexOfLastSeenMs);
            final long _tmpAddedMs;
            _tmpAddedMs = _cursor.getLong(_cursorIndexOfAddedMs);
            final String _tmpDiscoveredVia;
            _tmpDiscoveredVia = _cursor.getString(_cursorIndexOfDiscoveredVia);
            final String _tmpServiceType;
            _tmpServiceType = _cursor.getString(_cursorIndexOfServiceType);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _item = new DeviceProfile(_tmpId,_tmpName,_tmpLocation,_tmpProtocol,_tmpHost,_tmpPort,_tmpPath,_tmpUsername,_tmpPassword,_tmpAuthType,_tmpState,_tmpLatencyMs,_tmpIsFavorite,_tmpIsEnabled,_tmpSnapshotUrl,_tmpLastSeenMs,_tmpAddedMs,_tmpDiscoveredVia,_tmpServiceType,_tmpNotes);
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
  public Flow<List<String>> observeLocations() {
    final String _sql = "SELECT DISTINCT location FROM devices ORDER BY location";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"devices"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM devices";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object countOnline(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM devices WHERE state = 'ONLINE'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
