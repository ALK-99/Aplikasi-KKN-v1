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
import com.kkn.almufid.sekretaris.data.entity.InventarisEntry;
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
public final class InventarisDao_Impl implements InventarisDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<InventarisEntry> __insertionAdapterOfInventarisEntry;

  private final EntityDeletionOrUpdateAdapter<InventarisEntry> __deletionAdapterOfInventarisEntry;

  private final EntityDeletionOrUpdateAdapter<InventarisEntry> __updateAdapterOfInventarisEntry;

  public InventarisDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfInventarisEntry = new EntityInsertionAdapter<InventarisEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `inventaris_entries` (`id`,`namaBarang`,`jumlah`,`kondisi`,`keterangan`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final InventarisEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getNamaBarang());
        statement.bindLong(3, entity.getJumlah());
        statement.bindString(4, entity.getKondisi());
        statement.bindString(5, entity.getKeterangan());
      }
    };
    this.__deletionAdapterOfInventarisEntry = new EntityDeletionOrUpdateAdapter<InventarisEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `inventaris_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final InventarisEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfInventarisEntry = new EntityDeletionOrUpdateAdapter<InventarisEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `inventaris_entries` SET `id` = ?,`namaBarang` = ?,`jumlah` = ?,`kondisi` = ?,`keterangan` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final InventarisEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getNamaBarang());
        statement.bindLong(3, entity.getJumlah());
        statement.bindString(4, entity.getKondisi());
        statement.bindString(5, entity.getKeterangan());
        statement.bindLong(6, entity.getId());
      }
    };
  }

  @Override
  public Object tambah(final InventarisEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfInventarisEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object hapus(final InventarisEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfInventarisEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object ubah(final InventarisEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfInventarisEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<InventarisEntry>> semuaInventaris() {
    final String _sql = "SELECT * FROM inventaris_entries ORDER BY namaBarang ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"inventaris_entries"}, new Callable<List<InventarisEntry>>() {
      @Override
      @NonNull
      public List<InventarisEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNamaBarang = CursorUtil.getColumnIndexOrThrow(_cursor, "namaBarang");
          final int _cursorIndexOfJumlah = CursorUtil.getColumnIndexOrThrow(_cursor, "jumlah");
          final int _cursorIndexOfKondisi = CursorUtil.getColumnIndexOrThrow(_cursor, "kondisi");
          final int _cursorIndexOfKeterangan = CursorUtil.getColumnIndexOrThrow(_cursor, "keterangan");
          final List<InventarisEntry> _result = new ArrayList<InventarisEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final InventarisEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpNamaBarang;
            _tmpNamaBarang = _cursor.getString(_cursorIndexOfNamaBarang);
            final int _tmpJumlah;
            _tmpJumlah = _cursor.getInt(_cursorIndexOfJumlah);
            final String _tmpKondisi;
            _tmpKondisi = _cursor.getString(_cursorIndexOfKondisi);
            final String _tmpKeterangan;
            _tmpKeterangan = _cursor.getString(_cursorIndexOfKeterangan);
            _item = new InventarisEntry(_tmpId,_tmpNamaBarang,_tmpJumlah,_tmpKondisi,_tmpKeterangan);
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
