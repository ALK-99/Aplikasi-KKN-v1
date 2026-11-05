package com.kkn.almufid.sekretaris.data;

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
import com.kkn.almufid.sekretaris.data.dao.AbsensiDao;
import com.kkn.almufid.sekretaris.data.dao.AbsensiDao_Impl;
import com.kkn.almufid.sekretaris.data.dao.AnggotaDao;
import com.kkn.almufid.sekretaris.data.dao.AnggotaDao_Impl;
import com.kkn.almufid.sekretaris.data.dao.InventarisDao;
import com.kkn.almufid.sekretaris.data.dao.InventarisDao_Impl;
import com.kkn.almufid.sekretaris.data.dao.JurnalDao;
import com.kkn.almufid.sekretaris.data.dao.JurnalDao_Impl;
import com.kkn.almufid.sekretaris.data.dao.NotaDao;
import com.kkn.almufid.sekretaris.data.dao.NotaDao_Impl;
import com.kkn.almufid.sekretaris.data.dao.ProkerDao;
import com.kkn.almufid.sekretaris.data.dao.ProkerDao_Impl;
import com.kkn.almufid.sekretaris.data.dao.SuratDao;
import com.kkn.almufid.sekretaris.data.dao.SuratDao_Impl;
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
  private volatile JurnalDao _jurnalDao;

  private volatile AnggotaDao _anggotaDao;

  private volatile AbsensiDao _absensiDao;

  private volatile SuratDao _suratDao;

  private volatile ProkerDao _prokerDao;

  private volatile NotaDao _notaDao;

  private volatile InventarisDao _inventarisDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `jurnal_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tanggalMillis` INTEGER NOT NULL, `jenis` TEXT NOT NULL, `judul` TEXT NOT NULL, `isi` TEXT NOT NULL, `pesertaRapat` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `anggota` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nama` TEXT NOT NULL, `divisi` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `absensi_entries` (`anggotaId` INTEGER NOT NULL, `tanggalMillis` INTEGER NOT NULL, `hadir` INTEGER NOT NULL, `keterangan` TEXT NOT NULL, PRIMARY KEY(`anggotaId`, `tanggalMillis`), FOREIGN KEY(`anggotaId`) REFERENCES `anggota`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_absensi_entries_anggotaId` ON `absensi_entries` (`anggotaId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `surat_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tanggalMillis` INTEGER NOT NULL, `jenis` TEXT NOT NULL, `nomorSurat` TEXT NOT NULL, `perihal` TEXT NOT NULL, `tujuanAtauPengirim` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `proker_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `namaProker` TEXT NOT NULL, `divisiPenanggungJawab` TEXT NOT NULL, `status` TEXT NOT NULL, `catatanEvaluasi` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `nota_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tanggalMillis` INTEGER NOT NULL, `keterangan` TEXT NOT NULL, `nominal` INTEGER NOT NULL, `prokerTerkait` TEXT NOT NULL, `fotoPath` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `inventaris_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `namaBarang` TEXT NOT NULL, `jumlah` INTEGER NOT NULL, `kondisi` TEXT NOT NULL, `keterangan` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '67563166b2c493210bbfcead874c84e3')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `jurnal_entries`");
        db.execSQL("DROP TABLE IF EXISTS `anggota`");
        db.execSQL("DROP TABLE IF EXISTS `absensi_entries`");
        db.execSQL("DROP TABLE IF EXISTS `surat_entries`");
        db.execSQL("DROP TABLE IF EXISTS `proker_entries`");
        db.execSQL("DROP TABLE IF EXISTS `nota_entries`");
        db.execSQL("DROP TABLE IF EXISTS `inventaris_entries`");
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
        final HashMap<String, TableInfo.Column> _columnsJurnalEntries = new HashMap<String, TableInfo.Column>(6);
        _columnsJurnalEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJurnalEntries.put("tanggalMillis", new TableInfo.Column("tanggalMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJurnalEntries.put("jenis", new TableInfo.Column("jenis", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJurnalEntries.put("judul", new TableInfo.Column("judul", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJurnalEntries.put("isi", new TableInfo.Column("isi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJurnalEntries.put("pesertaRapat", new TableInfo.Column("pesertaRapat", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysJurnalEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesJurnalEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoJurnalEntries = new TableInfo("jurnal_entries", _columnsJurnalEntries, _foreignKeysJurnalEntries, _indicesJurnalEntries);
        final TableInfo _existingJurnalEntries = TableInfo.read(db, "jurnal_entries");
        if (!_infoJurnalEntries.equals(_existingJurnalEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "jurnal_entries(com.kkn.almufid.sekretaris.data.entity.JurnalEntry).\n"
                  + " Expected:\n" + _infoJurnalEntries + "\n"
                  + " Found:\n" + _existingJurnalEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsAnggota = new HashMap<String, TableInfo.Column>(3);
        _columnsAnggota.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnggota.put("nama", new TableInfo.Column("nama", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnggota.put("divisi", new TableInfo.Column("divisi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAnggota = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAnggota = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAnggota = new TableInfo("anggota", _columnsAnggota, _foreignKeysAnggota, _indicesAnggota);
        final TableInfo _existingAnggota = TableInfo.read(db, "anggota");
        if (!_infoAnggota.equals(_existingAnggota)) {
          return new RoomOpenHelper.ValidationResult(false, "anggota(com.kkn.almufid.sekretaris.data.entity.Anggota).\n"
                  + " Expected:\n" + _infoAnggota + "\n"
                  + " Found:\n" + _existingAnggota);
        }
        final HashMap<String, TableInfo.Column> _columnsAbsensiEntries = new HashMap<String, TableInfo.Column>(4);
        _columnsAbsensiEntries.put("anggotaId", new TableInfo.Column("anggotaId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAbsensiEntries.put("tanggalMillis", new TableInfo.Column("tanggalMillis", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAbsensiEntries.put("hadir", new TableInfo.Column("hadir", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAbsensiEntries.put("keterangan", new TableInfo.Column("keterangan", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAbsensiEntries = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysAbsensiEntries.add(new TableInfo.ForeignKey("anggota", "CASCADE", "NO ACTION", Arrays.asList("anggotaId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesAbsensiEntries = new HashSet<TableInfo.Index>(1);
        _indicesAbsensiEntries.add(new TableInfo.Index("index_absensi_entries_anggotaId", false, Arrays.asList("anggotaId"), Arrays.asList("ASC")));
        final TableInfo _infoAbsensiEntries = new TableInfo("absensi_entries", _columnsAbsensiEntries, _foreignKeysAbsensiEntries, _indicesAbsensiEntries);
        final TableInfo _existingAbsensiEntries = TableInfo.read(db, "absensi_entries");
        if (!_infoAbsensiEntries.equals(_existingAbsensiEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "absensi_entries(com.kkn.almufid.sekretaris.data.entity.AbsensiEntry).\n"
                  + " Expected:\n" + _infoAbsensiEntries + "\n"
                  + " Found:\n" + _existingAbsensiEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsSuratEntries = new HashMap<String, TableInfo.Column>(6);
        _columnsSuratEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuratEntries.put("tanggalMillis", new TableInfo.Column("tanggalMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuratEntries.put("jenis", new TableInfo.Column("jenis", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuratEntries.put("nomorSurat", new TableInfo.Column("nomorSurat", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuratEntries.put("perihal", new TableInfo.Column("perihal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuratEntries.put("tujuanAtauPengirim", new TableInfo.Column("tujuanAtauPengirim", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSuratEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSuratEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSuratEntries = new TableInfo("surat_entries", _columnsSuratEntries, _foreignKeysSuratEntries, _indicesSuratEntries);
        final TableInfo _existingSuratEntries = TableInfo.read(db, "surat_entries");
        if (!_infoSuratEntries.equals(_existingSuratEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "surat_entries(com.kkn.almufid.sekretaris.data.entity.SuratEntry).\n"
                  + " Expected:\n" + _infoSuratEntries + "\n"
                  + " Found:\n" + _existingSuratEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsProkerEntries = new HashMap<String, TableInfo.Column>(5);
        _columnsProkerEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProkerEntries.put("namaProker", new TableInfo.Column("namaProker", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProkerEntries.put("divisiPenanggungJawab", new TableInfo.Column("divisiPenanggungJawab", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProkerEntries.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProkerEntries.put("catatanEvaluasi", new TableInfo.Column("catatanEvaluasi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProkerEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProkerEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProkerEntries = new TableInfo("proker_entries", _columnsProkerEntries, _foreignKeysProkerEntries, _indicesProkerEntries);
        final TableInfo _existingProkerEntries = TableInfo.read(db, "proker_entries");
        if (!_infoProkerEntries.equals(_existingProkerEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "proker_entries(com.kkn.almufid.sekretaris.data.entity.ProkerEntry).\n"
                  + " Expected:\n" + _infoProkerEntries + "\n"
                  + " Found:\n" + _existingProkerEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsNotaEntries = new HashMap<String, TableInfo.Column>(6);
        _columnsNotaEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotaEntries.put("tanggalMillis", new TableInfo.Column("tanggalMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotaEntries.put("keterangan", new TableInfo.Column("keterangan", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotaEntries.put("nominal", new TableInfo.Column("nominal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotaEntries.put("prokerTerkait", new TableInfo.Column("prokerTerkait", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotaEntries.put("fotoPath", new TableInfo.Column("fotoPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNotaEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesNotaEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoNotaEntries = new TableInfo("nota_entries", _columnsNotaEntries, _foreignKeysNotaEntries, _indicesNotaEntries);
        final TableInfo _existingNotaEntries = TableInfo.read(db, "nota_entries");
        if (!_infoNotaEntries.equals(_existingNotaEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "nota_entries(com.kkn.almufid.sekretaris.data.entity.NotaEntry).\n"
                  + " Expected:\n" + _infoNotaEntries + "\n"
                  + " Found:\n" + _existingNotaEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsInventarisEntries = new HashMap<String, TableInfo.Column>(5);
        _columnsInventarisEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInventarisEntries.put("namaBarang", new TableInfo.Column("namaBarang", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInventarisEntries.put("jumlah", new TableInfo.Column("jumlah", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInventarisEntries.put("kondisi", new TableInfo.Column("kondisi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInventarisEntries.put("keterangan", new TableInfo.Column("keterangan", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysInventarisEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesInventarisEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoInventarisEntries = new TableInfo("inventaris_entries", _columnsInventarisEntries, _foreignKeysInventarisEntries, _indicesInventarisEntries);
        final TableInfo _existingInventarisEntries = TableInfo.read(db, "inventaris_entries");
        if (!_infoInventarisEntries.equals(_existingInventarisEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "inventaris_entries(com.kkn.almufid.sekretaris.data.entity.InventarisEntry).\n"
                  + " Expected:\n" + _infoInventarisEntries + "\n"
                  + " Found:\n" + _existingInventarisEntries);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "67563166b2c493210bbfcead874c84e3", "85bfb72a746a7b7a6adf54a4fba0dc5f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "jurnal_entries","anggota","absensi_entries","surat_entries","proker_entries","nota_entries","inventaris_entries");
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
      _db.execSQL("DELETE FROM `jurnal_entries`");
      _db.execSQL("DELETE FROM `anggota`");
      _db.execSQL("DELETE FROM `absensi_entries`");
      _db.execSQL("DELETE FROM `surat_entries`");
      _db.execSQL("DELETE FROM `proker_entries`");
      _db.execSQL("DELETE FROM `nota_entries`");
      _db.execSQL("DELETE FROM `inventaris_entries`");
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
    _typeConvertersMap.put(JurnalDao.class, JurnalDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AnggotaDao.class, AnggotaDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AbsensiDao.class, AbsensiDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SuratDao.class, SuratDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ProkerDao.class, ProkerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(NotaDao.class, NotaDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(InventarisDao.class, InventarisDao_Impl.getRequiredConverters());
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
  public JurnalDao jurnalDao() {
    if (_jurnalDao != null) {
      return _jurnalDao;
    } else {
      synchronized(this) {
        if(_jurnalDao == null) {
          _jurnalDao = new JurnalDao_Impl(this);
        }
        return _jurnalDao;
      }
    }
  }

  @Override
  public AnggotaDao anggotaDao() {
    if (_anggotaDao != null) {
      return _anggotaDao;
    } else {
      synchronized(this) {
        if(_anggotaDao == null) {
          _anggotaDao = new AnggotaDao_Impl(this);
        }
        return _anggotaDao;
      }
    }
  }

  @Override
  public AbsensiDao absensiDao() {
    if (_absensiDao != null) {
      return _absensiDao;
    } else {
      synchronized(this) {
        if(_absensiDao == null) {
          _absensiDao = new AbsensiDao_Impl(this);
        }
        return _absensiDao;
      }
    }
  }

  @Override
  public SuratDao suratDao() {
    if (_suratDao != null) {
      return _suratDao;
    } else {
      synchronized(this) {
        if(_suratDao == null) {
          _suratDao = new SuratDao_Impl(this);
        }
        return _suratDao;
      }
    }
  }

  @Override
  public ProkerDao prokerDao() {
    if (_prokerDao != null) {
      return _prokerDao;
    } else {
      synchronized(this) {
        if(_prokerDao == null) {
          _prokerDao = new ProkerDao_Impl(this);
        }
        return _prokerDao;
      }
    }
  }

  @Override
  public NotaDao notaDao() {
    if (_notaDao != null) {
      return _notaDao;
    } else {
      synchronized(this) {
        if(_notaDao == null) {
          _notaDao = new NotaDao_Impl(this);
        }
        return _notaDao;
      }
    }
  }

  @Override
  public InventarisDao inventarisDao() {
    if (_inventarisDao != null) {
      return _inventarisDao;
    } else {
      synchronized(this) {
        if(_inventarisDao == null) {
          _inventarisDao = new InventarisDao_Impl(this);
        }
        return _inventarisDao;
      }
    }
  }
}
