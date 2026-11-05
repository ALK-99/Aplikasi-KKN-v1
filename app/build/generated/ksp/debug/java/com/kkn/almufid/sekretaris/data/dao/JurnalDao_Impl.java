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
import com.kkn.almufid.sekretaris.data.Converters;
import com.kkn.almufid.sekretaris.data.entity.JenisJurnal;
import com.kkn.almufid.sekretaris.data.entity.JurnalEntry;
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
public final class JurnalDao_Impl implements JurnalDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<JurnalEntry> __insertionAdapterOfJurnalEntry;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<JurnalEntry> __deletionAdapterOfJurnalEntry;

  private final EntityDeletionOrUpdateAdapter<JurnalEntry> __updateAdapterOfJurnalEntry;

  public JurnalDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfJurnalEntry = new EntityInsertionAdapter<JurnalEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `jurnal_entries` (`id`,`tanggalMillis`,`jenis`,`judul`,`isi`,`pesertaRapat`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final JurnalEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTanggalMillis());
        final String _tmp = __converters.jenisJurnalKeString(entity.getJenis());
        statement.bindString(3, _tmp);
        statement.bindString(4, entity.getJudul());
        statement.bindString(5, entity.getIsi());
        statement.bindString(6, entity.getPesertaRapat());
      }
    };
    this.__deletionAdapterOfJurnalEntry = new EntityDeletionOrUpdateAdapter<JurnalEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `jurnal_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final JurnalEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfJurnalEntry = new EntityDeletionOrUpdateAdapter<JurnalEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `jurnal_entries` SET `id` = ?,`tanggalMillis` = ?,`jenis` = ?,`judul` = ?,`isi` = ?,`pesertaRapat` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final JurnalEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTanggalMillis());
        final String _tmp = __converters.jenisJurnalKeString(entity.getJenis());
        statement.bindString(3, _tmp);
        statement.bindString(4, entity.getJudul());
        statement.bindString(5, entity.getIsi());
        statement.bindString(6, entity.getPesertaRapat());
        statement.bindLong(7, entity.getId());
      }
    };
  }

  @Override
  public Object tambah(final JurnalEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfJurnalEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object hapus(final JurnalEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfJurnalEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object ubah(final JurnalEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfJurnalEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<JurnalEntry>> semuaJurnal() {
    final String _sql = "SELECT * FROM jurnal_entries ORDER BY tanggalMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"jurnal_entries"}, new Callable<List<JurnalEntry>>() {
      @Override
      @NonNull
      public List<JurnalEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTanggalMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "tanggalMillis");
          final int _cursorIndexOfJenis = CursorUtil.getColumnIndexOrThrow(_cursor, "jenis");
          final int _cursorIndexOfJudul = CursorUtil.getColumnIndexOrThrow(_cursor, "judul");
          final int _cursorIndexOfIsi = CursorUtil.getColumnIndexOrThrow(_cursor, "isi");
          final int _cursorIndexOfPesertaRapat = CursorUtil.getColumnIndexOrThrow(_cursor, "pesertaRapat");
          final List<JurnalEntry> _result = new ArrayList<JurnalEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final JurnalEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTanggalMillis;
            _tmpTanggalMillis = _cursor.getLong(_cursorIndexOfTanggalMillis);
            final JenisJurnal _tmpJenis;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfJenis);
            _tmpJenis = __converters.stringKeJenisJurnal(_tmp);
            final String _tmpJudul;
            _tmpJudul = _cursor.getString(_cursorIndexOfJudul);
            final String _tmpIsi;
            _tmpIsi = _cursor.getString(_cursorIndexOfIsi);
            final String _tmpPesertaRapat;
            _tmpPesertaRapat = _cursor.getString(_cursorIndexOfPesertaRapat);
            _item = new JurnalEntry(_tmpId,_tmpTanggalMillis,_tmpJenis,_tmpJudul,_tmpIsi,_tmpPesertaRapat);
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
