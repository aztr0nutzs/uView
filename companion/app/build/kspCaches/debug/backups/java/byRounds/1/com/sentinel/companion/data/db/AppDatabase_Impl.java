package com.sentinel.companion.data.db;

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
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile DeviceDao _deviceDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `devices` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `location` TEXT NOT NULL, `protocol` TEXT NOT NULL, `host` TEXT NOT NULL, `port` INTEGER NOT NULL, `path` TEXT NOT NULL, `username` TEXT NOT NULL, `password` TEXT NOT NULL, `authType` TEXT NOT NULL, `state` TEXT NOT NULL, `latencyMs` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL, `snapshotUrl` TEXT NOT NULL, `lastSeenMs` INTEGER NOT NULL, `addedMs` INTEGER NOT NULL, `discoveredVia` TEXT NOT NULL, `serviceType` TEXT NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd22f74dd0eedfa08ffcd0204b75db8f2')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `devices`");
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
        final HashMap<String, TableInfo.Column> _columnsDevices = new HashMap<String, TableInfo.Column>(20);
        _columnsDevices.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("location", new TableInfo.Column("location", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("protocol", new TableInfo.Column("protocol", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("host", new TableInfo.Column("host", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("port", new TableInfo.Column("port", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("path", new TableInfo.Column("path", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("username", new TableInfo.Column("username", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("password", new TableInfo.Column("password", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("authType", new TableInfo.Column("authType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("state", new TableInfo.Column("state", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("latencyMs", new TableInfo.Column("latencyMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("isEnabled", new TableInfo.Column("isEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("snapshotUrl", new TableInfo.Column("snapshotUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("lastSeenMs", new TableInfo.Column("lastSeenMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("addedMs", new TableInfo.Column("addedMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("discoveredVia", new TableInfo.Column("discoveredVia", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("serviceType", new TableInfo.Column("serviceType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevices.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDevices = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDevices = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDevices = new TableInfo("devices", _columnsDevices, _foreignKeysDevices, _indicesDevices);
        final TableInfo _existingDevices = TableInfo.read(db, "devices");
        if (!_infoDevices.equals(_existingDevices)) {
          return new RoomOpenHelper.ValidationResult(false, "devices(com.sentinel.companion.data.model.DeviceProfile).\n"
                  + " Expected:\n" + _infoDevices + "\n"
                  + " Found:\n" + _existingDevices);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "d22f74dd0eedfa08ffcd0204b75db8f2", "67d4040a1697944ac92bb28d008bba7e");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "devices");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `devices`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
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
    _typeConvertersMap.put(DeviceDao.class, DeviceDao_Impl.getRequiredConverters());
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
  public DeviceDao deviceDao() {
    if (_deviceDao != null) {
      return _deviceDao;
    } else {
      synchronized(this) {
        if(_deviceDao == null) {
          _deviceDao = new DeviceDao_Impl(this);
        }
        return _deviceDao;
      }
    }
  }
}
