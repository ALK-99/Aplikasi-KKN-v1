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
import com.kkn.almufid.sekretaris.data.entity.NotaEntry;
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
public final class NotaDao_Impl implements NotaDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<NotaEntry> __insertionAdapterOfNotaEntry;

  private final EntityDeletionOrUpdateAdapter<NotaEntry> __deletionAdapterOfNotaEntry;

  public NotaDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfNotaEntry = new EntityInsertionAdapter<NotaEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `nota_entries` (`id`,`tanggalMillis`,`keterangan`,`nominal`,`prokerTerkait`,`fotoPath`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NotaEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTanggalMillis());
        statement.bindString(3, entity.getKeterangan());
        statement.bindLong(4, entity.getNominal());
        statement.bindString(5, entity.getProkerTerkait());
        if (entity.getFotoPath() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getFotoPath());
        }
      }
    };
    this.__deletionAdapterOfNotaEntry = new EntityDeletionOrUpdateAdapter<NotaEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `nota_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NotaEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
  }

  @Override
  public Object tambah(final NotaEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfNotaEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object hapus(final NotaEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfNotaEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<NotaEntry>> semuaNota() {
    final String _sql = "SELECT * FROM nota_entries ORDER BY tanggalMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"nota_entries"}, new Callable<List<NotaEntry>>() {
      @Override
      @NonNull
      public List<NotaEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTanggalMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "tanggalMillis");
          final int _cursorIndexOfKeterangan = CursorUtil.getColumnIndexOrThrow(_cursor, "keterangan");
          final int _cursorIndexOfNominal = CursorUtil.getColumnIndexOrThrow(_cursor, "nominal");
          final int _cursorIndexOfProkerTerkait = CursorUtil.getColumnIndexOrThrow(_cursor, "prokerTerkait");
          final int _cursorIndexOfFotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoPath");
          final List<NotaEntry> _result = new ArrayList<NotaEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NotaEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTanggalMillis;
            _tmpTanggalMillis = _cursor.getLong(_cursorIndexOfTanggalMillis);
            final String _tmpKeterangan;
            _tmpKeterangan = _cursor.getString(_cursorIndexOfKeterangan);
            final long _tmpNominal;
            _tmpNominal = _cursor.getLong(_cursorIndexOfNominal);
            final String _tmpProkerTerkait;
            _tmpProkerTerkait = _cursor.getString(_cursorIndexOfProkerTerkait);
            final String _tmpFotoPath;
            if (_cursor.isNull(_cursorIndexOfFotoPath)) {
              _tmpFotoPath = null;
            } else {
              _tmpFotoPath = _cursor.getString(_cursorIndexOfFotoPath);
            }
            _item = new NotaEntry(_tmpId,_tmpTanggalMillis,_tmpKeterangan,_tmpNominal,_tmpProkerTerkait,_tmpFotoPath);
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
  public Flow<Long> totalPengeluaran() {
    final String _sql = "SELECT COALESCE(SUM(nominal), 0) FROM nota_entries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"nota_entries"}, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
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
