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
import com.kkn.almufid.sekretaris.data.entity.Anggota;
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
public final class AnggotaDao_Impl implements AnggotaDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Anggota> __insertionAdapterOfAnggota;

  private final EntityDeletionOrUpdateAdapter<Anggota> __deletionAdapterOfAnggota;

  public AnggotaDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAnggota = new EntityInsertionAdapter<Anggota>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `anggota` (`id`,`nama`,`divisi`) VALUES (nullif(?, 0),?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Anggota entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getNama());
        statement.bindString(3, entity.getDivisi());
      }
    };
    this.__deletionAdapterOfAnggota = new EntityDeletionOrUpdateAdapter<Anggota>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `anggota` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Anggota entity) {
        statement.bindLong(1, entity.getId());
      }
    };
  }

  @Override
  public Object tambah(final Anggota anggota, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAnggota.insertAndReturnId(anggota);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object hapus(final Anggota anggota, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAnggota.handle(anggota);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Anggota>> semuaAnggota() {
    final String _sql = "SELECT * FROM anggota ORDER BY nama ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"anggota"}, new Callable<List<Anggota>>() {
      @Override
      @NonNull
      public List<Anggota> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNama = CursorUtil.getColumnIndexOrThrow(_cursor, "nama");
          final int _cursorIndexOfDivisi = CursorUtil.getColumnIndexOrThrow(_cursor, "divisi");
          final List<Anggota> _result = new ArrayList<Anggota>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Anggota _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpNama;
            _tmpNama = _cursor.getString(_cursorIndexOfNama);
            final String _tmpDivisi;
            _tmpDivisi = _cursor.getString(_cursorIndexOfDivisi);
            _item = new Anggota(_tmpId,_tmpNama,_tmpDivisi);
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
