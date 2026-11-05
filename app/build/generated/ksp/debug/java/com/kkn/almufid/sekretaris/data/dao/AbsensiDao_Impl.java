package com.kkn.almufid.sekretaris.data.dao;

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
import com.kkn.almufid.sekretaris.data.entity.AbsensiEntry;
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
public final class AbsensiDao_Impl implements AbsensiDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AbsensiEntry> __insertionAdapterOfAbsensiEntry;

  private final EntityDeletionOrUpdateAdapter<AbsensiEntry> __deletionAdapterOfAbsensiEntry;

  public AbsensiDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAbsensiEntry = new EntityInsertionAdapter<AbsensiEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `absensi_entries` (`anggotaId`,`tanggalMillis`,`hadir`,`keterangan`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AbsensiEntry entity) {
        statement.bindLong(1, entity.getAnggotaId());
        statement.bindLong(2, entity.getTanggalMillis());
        final int _tmp = entity.getHadir() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindString(4, entity.getKeterangan());
      }
    };
    this.__deletionAdapterOfAbsensiEntry = new EntityDeletionOrUpdateAdapter<AbsensiEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `absensi_entries` WHERE `anggotaId` = ? AND `tanggalMillis` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AbsensiEntry entity) {
        statement.bindLong(1, entity.getAnggotaId());
        statement.bindLong(2, entity.getTanggalMillis());
      }
    };
  }

  @Override
  public Object simpan(final AbsensiEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAbsensiEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object hapus(final AbsensiEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAbsensiEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AbsensiEntry>> absensiPadaTanggal(final long tanggalMillis) {
    final String _sql = "SELECT * FROM absensi_entries WHERE tanggalMillis = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, tanggalMillis);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"absensi_entries"}, new Callable<List<AbsensiEntry>>() {
      @Override
      @NonNull
      public List<AbsensiEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAnggotaId = CursorUtil.getColumnIndexOrThrow(_cursor, "anggotaId");
          final int _cursorIndexOfTanggalMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "tanggalMillis");
          final int _cursorIndexOfHadir = CursorUtil.getColumnIndexOrThrow(_cursor, "hadir");
          final int _cursorIndexOfKeterangan = CursorUtil.getColumnIndexOrThrow(_cursor, "keterangan");
          final List<AbsensiEntry> _result = new ArrayList<AbsensiEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AbsensiEntry _item;
            final long _tmpAnggotaId;
            _tmpAnggotaId = _cursor.getLong(_cursorIndexOfAnggotaId);
            final long _tmpTanggalMillis;
            _tmpTanggalMillis = _cursor.getLong(_cursorIndexOfTanggalMillis);
            final boolean _tmpHadir;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHadir);
            _tmpHadir = _tmp != 0;
            final String _tmpKeterangan;
            _tmpKeterangan = _cursor.getString(_cursorIndexOfKeterangan);
            _item = new AbsensiEntry(_tmpAnggotaId,_tmpTanggalMillis,_tmpHadir,_tmpKeterangan);
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
  public Flow<List<AbsensiEntry>> semuaAbsensi() {
    final String _sql = "SELECT * FROM absensi_entries ORDER BY tanggalMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"absensi_entries"}, new Callable<List<AbsensiEntry>>() {
      @Override
      @NonNull
      public List<AbsensiEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAnggotaId = CursorUtil.getColumnIndexOrThrow(_cursor, "anggotaId");
          final int _cursorIndexOfTanggalMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "tanggalMillis");
          final int _cursorIndexOfHadir = CursorUtil.getColumnIndexOrThrow(_cursor, "hadir");
          final int _cursorIndexOfKeterangan = CursorUtil.getColumnIndexOrThrow(_cursor, "keterangan");
          final List<AbsensiEntry> _result = new ArrayList<AbsensiEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AbsensiEntry _item;
            final long _tmpAnggotaId;
            _tmpAnggotaId = _cursor.getLong(_cursorIndexOfAnggotaId);
            final long _tmpTanggalMillis;
            _tmpTanggalMillis = _cursor.getLong(_cursorIndexOfTanggalMillis);
            final boolean _tmpHadir;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHadir);
            _tmpHadir = _tmp != 0;
            final String _tmpKeterangan;
            _tmpKeterangan = _cursor.getString(_cursorIndexOfKeterangan);
            _item = new AbsensiEntry(_tmpAnggotaId,_tmpTanggalMillis,_tmpHadir,_tmpKeterangan);
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
